package thosakwe.fray.analysis;

import org.antlr.v4.runtime.ANTLRFileStream;
import org.antlr.v4.runtime.ANTLRInputStream;
import org.antlr.v4.runtime.CommonTokenStream;
import thosakwe.fray.grammar.FrayLexer;
import thosakwe.fray.grammar.FrayParser;
import thosakwe.fray.pipeline.FrayAsset;
import thosakwe.fray.pipeline.FrayPipeline;
import thosakwe.fray.pipeline.FrayTransformer;
import thosakwe.fray.pipeline.StringInterpolatorTransformer;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketAddress;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

public class FrayAnalysisServer {
    public static final int REQUEST_CODE_COMPLETION = 34;
    private final boolean debug;
    private final List<SocketAddress> subscribers = new ArrayList<>();
    private final FrayPipeline pipeline = new FrayPipeline(new FrayTransformer[]{new StringInterpolatorTransformer()});

    public FrayAnalysisServer(boolean debug) {
        this.debug = debug;
    }

    public void watch() {
        try {
            final WatchService service = FileSystems.getDefault().newWatchService();
            final WatchKey key = new File("").getAbsoluteFile().toPath().register(service, StandardWatchEventKinds.ENTRY_CREATE, StandardWatchEventKinds.ENTRY_MODIFY);

            while (true) {
                for (WatchEvent ev : key.pollEvents()) {
                    final Path filename = (Path) ev.context();
                    final File file = filename.toFile();
                    final FrayAsset input = FrayAsset.forFile(file);
                    final FrayAsset output = pipeline.transform(input);
                    final ANTLRInputStream inputStream = new ANTLRInputStream(output.getInputStream());
                    final FrayLexer lexer = new FrayLexer(inputStream);
                    final CommonTokenStream tokenStream = new CommonTokenStream(lexer);
                    final FrayParser parser = new FrayParser(tokenStream);

                    final FrayParser.CompilationUnitContext compilationUnit = parser.compilationUnit();
                    printDebug(String.format("Source from '%s': \n'%s'", filename.toString(), compilationUnit.getText()));
                }
            }
        } catch (IOException exc) {
            System.err.println(String.format("Analysis server error on file watcher: %s", exc.getMessage()));
            exc.printStackTrace();
        }
    }

    public ServerSocket listen(int port) throws IOException {
        final ServerSocket socket = new ServerSocket(port);
        new Thread(listener(socket)).start();
        return socket;
    }

    public void suggest(Suggestion suggestion) {
        printDebug(String.format(
                "%s: %s (%s:%d:%d)",
                suggestion.getType() == Suggestion.WARNING ? "warning" : "error",
                suggestion.getMessage(),
                suggestion.getFilename(),
                suggestion.getLine(),
                suggestion.getPos()
        ));

        for (SocketAddress subscriber : subscribers) {
            new Thread(() -> {
                try {
                    final Socket socket = new Socket();
                    socket.connect(subscriber);
                    final PrintStream out = new PrintStream(socket.getOutputStream());
                    suggestion.serialize(out);
                } catch (IOException exc) {
                    System.err.printf("Cannot broadcast to %s. %s", subscriber.toString(), exc.getMessage());
                    exc.printStackTrace();
                }
            }).start();
        }
    }

    private Runnable listener(ServerSocket socket) {
        return () -> {
            try {
                final Socket client = socket.accept();
                subscribers.add(client.getRemoteSocketAddress());

                // Check if suggestion
                final Scanner scanner = new Scanner(client.getInputStream());
                if (scanner.nextInt() == REQUEST_CODE_COMPLETION) {
                    final int row = scanner.nextInt();
                    final int col = scanner.nextInt();
                    final String filename = scanner.nextLine();
                    final ANTLRInputStream inputStream = new ANTLRFileStream(filename);
                    final FrayLexer lexer = new FrayLexer(inputStream);
                    final CommonTokenStream tokenStream = new CommonTokenStream(lexer);
                    final FrayParser parser = new FrayParser(tokenStream);
                    final FrayAnalyzer analyzer = new FrayAnalyzer(debug);
                    analyzer.analyzeProgram(parser.compilationUnit());
                    final PrintStream out = new PrintStream(client.getOutputStream());
                    analyzer.codeCompletion(out, row, col);
                    out.close();
                    client.close();
                } else {
                    final PrintStream out = new PrintStream(client.getOutputStream());
                    out.print(socket.getLocalPort());
                    out.close();
                    client.close();
                }
            } catch (Exception exc) {
                System.err.println(String.format("Analysis server error on client: %s", exc.getMessage()));
                exc.printStackTrace();
            }
        };
    }

    private void printDebug(String msg) {
        if (debug) {
            System.out.println(msg);
        }
    }
}
