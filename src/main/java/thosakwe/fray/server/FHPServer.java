package thosakwe.fray.server;

import fi.iki.elonen.NanoHTTPD;
import org.antlr.v4.runtime.ANTLRInputStream;
import org.antlr.v4.runtime.CommonTokenStream;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.io.FilenameUtils;
import thosakwe.fray.grammar.FrayLexer;
import thosakwe.fray.grammar.FrayParser;
import thosakwe.fray.interpreter.FrayInterpreter;
import thosakwe.fray.interpreter.errors.FrayException;
import thosakwe.fray.lang.FrayDatum;
import thosakwe.fray.pipeline.*;
import thosakwe.fray.server.shims.FrayHttpRequest;
import thosakwe.fray.server.shims.ServerShim;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

/**
 * Created on 2/7/2017.
 */
public class FHPServer extends NanoHTTPD {
    private final String host;
    private final int port;
    private final File dir;
    private final CommandLine commandLine;

    public FHPServer(String host, int port, File dir, CommandLine commandLine) {
        super(port);
        this.host = host;
        this.port = port;
        this.dir = dir;
        this.commandLine = commandLine;
    }

    public void start() throws IOException {
        super.start(NanoHTTPD.SOCKET_READ_TIMEOUT, false);
        System.out.printf("FHP server listening at http://localhost:%d%n", port);
    }

    @Override
    public Response serve(IHTTPSession session) {
        File requestedFile = new File(dir.toURI().resolve(
                session.getUri().equals("/")
                        ? "." :
                        session.getUri().replaceAll("^\\/", "")));

        try {
            if (!requestedFile.exists())
                return serve404(session);
            else if (requestedFile.isDirectory())
                return serveDirectory(session, requestedFile);
            else return serveFile(session, requestedFile);
        } catch (IOException e) {
            return newFixedLengthResponse(status500(), "text/plain",
                    String.format("Error serving path \"%s\".", session.getUri()));
        }
    }

    private Response serve404(IHTTPSession session) {
        return newFixedLengthResponse(status404(), "text/html",
                "<DOCTYPE html><html><head><title>404 Not Found</title></head><body><h1>"
                        + String.format("The path <i>%s</i> does not exist on this server.", session.getUri())
                        + "</h1></body></html>");
    }

    private Response serveDirectory(IHTTPSession session, File dir) throws IOException {
        String[] indices = new String[]{
                "index.fhp",
                "index.fray",
                "index.html"
        };

        for (String index : indices) {
            File file = new File(dir.toURI().resolve(index));
            if (file.exists())
                return serveFile(session, file);
        }

        return serve404(session);
    }

    private Response serveFile(IHTTPSession session, File file) throws IOException {
        String extension = FilenameUtils.getExtension(file.getName());

        if (!extension.equalsIgnoreCase("fhp") && !extension.equalsIgnoreCase("fray"))
            return newChunkedResponse(status200(), Files.probeContentType(file.toPath()), new FileInputStream(file));
        else {
            return serveFray(session, file);
        }
    }

    private Response serveFray(IHTTPSession session, File file) throws IOException {
        FrayPipeline pipeline = new FrayPipeline(new FrayTransformer[]{
                new CoreImporterTransformer(),
                new StringInterpolatorTransformer(),
                new InlineFunctionExpanderTransformer()
        });
        pipeline.setDebug(commandLine.hasOption("verbose"));

        FrayAsset asset = pipeline.transform(FrayAsset.forFile(file));
        ANTLRInputStream inputStream = new ANTLRInputStream(asset.getInputStream());
        FrayLexer lexer = new FrayLexer(inputStream);
        CommonTokenStream tokenStream = new CommonTokenStream(lexer);
        FrayParser parser = new FrayParser(tokenStream);
        FrayParser.CompilationUnitContext ast = parser.compilationUnit();

        try {
            FrayInterpreter interpreter = new FrayInterpreter(commandLine, pipeline, asset);
            new ServerShim(session).inject(interpreter);
            FrayHttpRequest fraySession = new FrayHttpRequest(session, interpreter);
            List<FrayDatum> args = new ArrayList<>();
            args.add(fraySession);
            interpreter.visitCompilationUnit(ast);
            final FrayDatum mainFunction = interpreter.getSymbolTable().getValue("main");
            if (mainFunction != null) {
                FrayDatum result = mainFunction.call(interpreter, ast, args);
                if (result != null)
                    return newFixedLengthResponse(result.toString());
                else
                    throw new FrayException("Top-level function \"main\" defined in Fray program returned `null`.", ast, interpreter);
            } else throw new FrayException("No top-level function \"main\" defined in Fray program.", ast, interpreter);
        } catch (FrayException e) {
            String msg = "<DOCTYPE html><html><head><title>Fray Error</title></head><body><h1>"
                    + e.getMessage()
                    + "</h1><ul>";

            for (StackTraceElement traceElement : e.getStackTrace())
                msg += String.format("<li>%s</li>", traceElement);

            return newFixedLengthResponse(status500(), "text/html", msg + "</ul></body></html>");
        } finally {

        }
    }

    private Response.IStatus status200() {
        return new Response.IStatus() {
            @Override
            public String getDescription() {
                return "200 OK";
            }

            @Override
            public int getRequestStatus() {
                return 200;
            }
        };
    }

    private Response.IStatus status404() {
        return new Response.IStatus() {
            @Override
            public String getDescription() {
                return "404 Not Found";
            }

            @Override
            public int getRequestStatus() {
                return 404;
            }
        };
    }

    private Response.IStatus status500() {
        return new Response.IStatus() {
            @Override
            public String getDescription() {
                return "500 Internal Server Error";
            }

            @Override
            public int getRequestStatus() {
                return 500;
            }
        };
    }
}
