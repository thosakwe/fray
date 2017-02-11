package thosakwe.fray.cli;

import org.antlr.v4.runtime.ANTLRFileStream;
import org.antlr.v4.runtime.ANTLRInputStream;
import org.antlr.v4.runtime.CommonTokenStream;
import org.apache.commons.cli.*;
import thosakwe.fray.Fray;
import thosakwe.fray.analysis.FrayAnalysisServer;
import thosakwe.fray.analysis.FrayAnalyzer;
import thosakwe.fray.compiler.FrayCompiler;
import thosakwe.fray.compiler.js.FrayToES5Compiler;
import thosakwe.fray.compiler.jvm.FrayToJvmCompiler;
import thosakwe.fray.grammar.FrayLexer;
import thosakwe.fray.grammar.FrayParser;
import thosakwe.fray.interpreter.FrayInterpreter;
import thosakwe.fray.lang.FrayLibrary;
import thosakwe.fray.pipeline.*;
import thosakwe.fray.lang.FrayDatum;
import thosakwe.fray.lang.FrayFunction;
import thosakwe.fray.lang.FrayNumber;
import thosakwe.fray.interpreter.errors.FrayException;
import thosakwe.fray.server.FHPServer;

import java.io.*;
import java.net.ServerSocket;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

public class Main {
    public static void main(String[] args) {
        final Options options = cliOptions();

        try {
            final CommandLine commandLine = new DefaultParser().parse(options, args);
            String[] rest = commandLine.getArgs();

            if (commandLine.hasOption("help")) {
                printUsage(options);
                return;
            }

            if (commandLine.hasOption("version")) {
                System.out.println(Fray.VERSION);
                return;
            }

            if (rest.length > 0 && rest[0].equals("serve")) {
                String[] serverArgs = new String[rest.length - 1];
                System.arraycopy(args, 1, serverArgs, 0, rest.length - 1);
                serverMain(serverArgs);
                return;
            }

            final FrayPipeline pipeline = createPipeline();
            pipeline.setDebug(commandLine.hasOption("verbose"));

            if (commandLine.hasOption("analyze")) {
                final int port = Integer.parseInt(commandLine.getOptionValue("port", "0"));
                final FrayAnalysisServer analysisServer = new FrayAnalysisServer(commandLine.hasOption("verbose"));
                final ServerSocket serverSocket = analysisServer.listen(port);
                System.out.printf("Fray analysis server listening on port %d%n", serverSocket.getLocalPort());
                analysisServer.watch();
                return;
            }

            if (commandLine.hasOption("code-completion")) {
                final String filename = commandLine.getOptionValue("code-completion");
                final int row = Integer.parseInt(commandLine.getOptionValue("row", "-1"));
                final int col = Integer.parseInt(commandLine.getOptionValue("col", "-1"));

                final ANTLRInputStream inputStream = new ANTLRFileStream(filename);
                final FrayLexer lexer = new FrayLexer(inputStream);
                final CommonTokenStream tokenStream = new CommonTokenStream(lexer);
                final FrayParser parser = new FrayParser(tokenStream);
                final FrayParser.CompilationUnitContext compilationUnit = parser.compilationUnit();
                final FrayAnalyzer analyzer = new FrayAnalyzer(commandLine.hasOption("verbose"));
                analyzer.analyzeProgram(compilationUnit);
                analyzer.codeCompletion(System.out, row, col);

                return;
            }

            if (commandLine.hasOption("repl")) {
                runRepl(pipeline, commandLine);
                return;
            }

            if (commandLine.hasOption("compile")) {
                FrayAsset compilationAsset;
                FrayCompiler compiler;

                if (commandLine.hasOption("read-stdin")) {
                    compilationAsset = new FrayAsset("fray", FrayPipeline.STDIN, FrayPipeline.STDIN, System.in);
                } else {
                    if (commandLine.getArgList().isEmpty()) {
                        printUsage(options);
                        System.exit(1);
                        return;
                    }

                    compilationAsset = FrayAsset.forFile(commandLine.getArgList().get(0));
                }

                final String to = commandLine.getOptionValue("compile").toLowerCase().trim();

                switch (to) {
                    case "javascript":
                    case "js":
                        compiler = new FrayToES5Compiler(compilationAsset, commandLine.hasOption("verbose"));
                        break;
                    case "jvm":
                        compiler = new FrayToJvmCompiler(compilationAsset, commandLine.hasOption("verbose"));
                        break;
                    default:
                        throw new Exception(String.format("Fray does not support compilation to %s. Yet. :)", to));
                }

                // Build a pipeline
                final FrayTransformer coreImporter = new CoreImporterTransformer();
                final FrayPipeline compilationPipeline = new FrayPipeline(new FrayTransformer[]{
                        coreImporter,
                        new StringInterpolatorTransformer(),
                        compiler.toTransformer()});
                compilationPipeline.setDebug(commandLine.hasOption("verbose"));

                final FrayAsset compilerOutput = compilationPipeline.transform(compilationAsset);

                if (commandLine.hasOption("write-stdout")) {
                    if (commandLine.hasOption("verbose")) {
                        System.out.println("------------");
                        System.out.println();
                        System.out.println("COMPILER OUTPUT STARTS BELOW:");
                        System.out.println();
                    }

                    System.out.print(compilerOutput.readAsString());
                } else if (commandLine.hasOption("out")) {
                    final String filename = commandLine.getOptionValue("out");
                    final PrintStream outputStream = new PrintStream(filename);
                    outputStream.print(compilerOutput.readAsString());
                    outputStream.close();
                } else {
                    System.err.println("No output filename provided.");
                    System.exit(1);
                }

                return;
            }

            if (commandLine.hasOption("read-stdin")) {
                runProgram(new FrayAsset("fray", FrayPipeline.STDIN, FrayPipeline.STDIN, System.in), pipeline, commandLine);
            } else {
                if (commandLine.getArgList().isEmpty()) {
                    printUsage(options);
                    System.exit(1);
                    return;
                }

                final String filename = commandLine.getArgList().get(0);
                commandLine.getArgList().remove(0);
                runProgram(FrayAsset.forFile(filename), pipeline, commandLine);
            }
        } catch (ParseException exc) {
            printUsage(options);
            System.exit(1);
        } catch (FrayException exc) {
            System.err.println(exc.toString());
            exc.printStackTrace();
            System.exit(1);
        } catch (Exception exc) {
            System.err.println(exc.getMessage());
            exc.printStackTrace();
            System.exit(1);
        }
    }

    private static Options cliOptions() {
        return new Options()
                .addOption("a", "analyze", false, "Start the Fray analysis server.")
                .addOption("cc", "code-completion", true, "Spits out all symbol names available at the given index within a source file.")
                .addOption("col", "column", true, "Specifies a column for code completion.")
                .addOption("to", "compile", true, "Compile Fray source to another language (js/javascript, dart).")
                .addOption("debug", "verbose", false, "Enable verbose debug output.")
                .addOption("h", "help", false, "Show this usage information.")
                .addOption("o", "out", true, "Write compiler output to the given file.")
                .addOption(Option.builder().longOpt("offline").hasArg(false).desc("Prints suggestion to stdout, rather than starting a server.").build())
                .addOption("p", "port", true, "Designate a port for the analysis server to listen on.")
                .addOption("r", "row", true, "Specifies a row for code completion.")
                .addOption(Option.builder().longOpt("repl").hasArg(false).desc("Run the interactive REPL.").build())
                .addOption("stdin", "read-stdin", false, "Interpret input from stdin.")
                .addOption("stdout", "write-stdout", false, "Print compiler output to stdout.")
                .addOption("v", "version", false, "Print the interpreter version.");
    }

    private static Options serverCliOptions() {
        return new Options()
                .addOption("h", "host", true, "The host to run on (default: 127.0.0.1).")
                .addOption("p", "port", true, "The port to listen on (default: 8080).")
                .addOption("d", "verbose", false, "Print verbose debug output.");
    }

    private static FrayPipeline createPipeline() {
        return FrayPipeline.DEFAULT;
    }

    private static void printUsage(Options options) {
        new HelpFormatter().printHelp("fray [options...] <filename>", options);
    }

    private static void runRepl(FrayPipeline pipeline, CommandLine commandLine) throws FrayException {
        final FrayInterpreter interpreter = new FrayInterpreter(commandLine, pipeline, null) {
            @Override
            public FrayDatum visitTopLevelStatement(FrayParser.TopLevelStatementContext ctx) {
                return super.visitStatement(ctx.statement());
            }
        };
        final Scanner scanner = new Scanner(System.in);
        System.out.print("> ");

        while (scanner.hasNextLine()) {
            try {
                final String line = scanner.nextLine();
                final FrayAsset asset = new FrayAsset("fray", FrayPipeline.STDIN, FrayPipeline.STDIN, new ByteArrayInputStream(line.getBytes()));
                final FrayAsset processed = pipeline.transform(asset);
                final ANTLRInputStream transformed = new ANTLRInputStream(processed.getInputStream());
                final FrayLexer lexer = new FrayLexer(transformed);
                final CommonTokenStream tokenStream = new CommonTokenStream(lexer);
                final FrayParser parser = new FrayParser(tokenStream);
                final FrayParser.CompilationUnitContext compilationUnit = parser.compilationUnit();
                interpreter.setSource(processed);
                final FrayLibrary library = interpreter.visitCompilationUnit(compilationUnit);
                final FrayDatum result = library.getDefaultExport();

                for (FrayException warning : interpreter.getWarnings()) {
                    System.err.printf("Warning: %s%n", warning.getMessage());
                    warning.printStackTrace();
                }

                for (FrayException error : interpreter.getErrors()) {
                    System.err.println(error.toString());
                    error.printStackTrace();
                }

                if (result != null)
                    System.out.printf("%s\033[0m%n", result.curses());
            } catch (Exception exc) {
                System.err.println(exc.getMessage());
                exc.printStackTrace();
            } finally {
                interpreter.getWarnings().clear();
                interpreter.getErrors().clear();
                System.out.print("> ");
            }
        }

        scanner.close();
    }

    private static void runProgram(FrayAsset asset, FrayPipeline pipeline, CommandLine commandLine) throws IOException {
        try {
            final FrayAsset processed = pipeline.transform(asset);
            final ANTLRInputStream transformed = new ANTLRInputStream(processed.getInputStream());
            final FrayLexer lexer = new FrayLexer(transformed);
            final CommonTokenStream tokenStream = new CommonTokenStream(lexer);
            final FrayParser parser = new FrayParser(tokenStream);
            final FrayParser.CompilationUnitContext compilationUnitContext = parser.compilationUnit();
            final FrayInterpreter interpreter = new FrayInterpreter(commandLine, pipeline, processed);
            interpreter.visitCompilationUnit(compilationUnitContext);

            final FrayDatum mainFunction = interpreter.getSymbolTable().getValue("main");

            if (!(mainFunction instanceof FrayFunction)) {
                System.err.println("No top-level function 'main' defined in Fray program.");
                System.exit(1);
            } else {
                final List<FrayDatum> args = new ArrayList<>();
                final FrayDatum result = mainFunction.call(interpreter, mainFunction.getSource(), args);

                for (FrayException warning : interpreter.getWarnings()) {
                    System.err.printf("Warning: %s%n", warning.getMessage());
                    warning.printStackTrace();
                }

                for (FrayException error : interpreter.getErrors()) {
                    System.err.println(error.toString());
                    error.printStackTrace();
                }

                if (!interpreter.getErrors().isEmpty())
                    System.exit(1);

                if (result instanceof FrayNumber) {
                    final Double value = ((FrayNumber) result).getValue();

                    if (value == value.intValue())
                        System.exit(value.intValue());
                }
            }
        } catch (FrayException exc) {
            System.err.println(exc.getMessage());
            exc.printStackTrace();
            System.exit(1);
        }
    }

    private static void serverMain(String[] args) {
        try {
            Options options = serverCliOptions();
            CommandLine commandLine = new DefaultParser().parse(options, args);
            List<String> rest = commandLine.getArgList();
            String host = commandLine.getOptionValue("h", "127.0.0.1");
            int port = Integer.parseInt(commandLine.getOptionValue("p", "8080"));
            File dir;

            if (rest.isEmpty())
                dir = new File(".");
            else dir = new File(rest.get(0));

            if (!dir.exists() || !dir.isDirectory())
                throw new InvalidObjectException(String.format("Entity at path \"%s\" is not a directory.", dir.getAbsolutePath()));

            new FHPServer(host, port, dir, commandLine).start();
        } catch (ParseException e) {
            new HelpFormatter().printHelp("fray serve [options...] [directory]", serverCliOptions());
            System.exit(1);
        } catch (Exception e) {
            System.err.printf("Error in FHP: %s%n", e.getMessage());
            e.printStackTrace();
        }
    }
}
