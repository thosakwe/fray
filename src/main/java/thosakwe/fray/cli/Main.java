package thosakwe.fray.cli;

import org.antlr.v4.runtime.ANTLRInputStream;
import org.antlr.v4.runtime.CommonTokenStream;
import org.apache.commons.cli.*;
import thosakwe.fray.Fray;
import thosakwe.fray.grammar.FrayLexer;
import thosakwe.fray.grammar.FrayParser;
import thosakwe.fray.lang.FrayInterpreter;
import thosakwe.fray.lang.pipeline.FrayAsset;
import thosakwe.fray.lang.pipeline.FrayPipeline;
import thosakwe.fray.lang.data.FrayDatum;
import thosakwe.fray.lang.data.FrayFunction;
import thosakwe.fray.lang.data.FrayNumber;
import thosakwe.fray.lang.errors.FrayException;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

public class Main {
    public static void main(String[] args) {
        final Options options = cliOptions();

        try {
            final CommandLine commandLine = new DefaultParser().parse(options, args);

            if (commandLine.hasOption("help")) {
                printUsage(options);
                return;
            }

            if (commandLine.hasOption("version")) {
                System.out.println(Fray.VERSION);
                return;
            }

            final FrayPipeline pipeline = createPipeline();
            pipeline.setDebug(commandLine.hasOption("verbose"));

            if (commandLine.hasOption("repl")) {
                runRepl(pipeline, commandLine);
                return;
            }

            if (commandLine.hasOption("read-stdin")) {
                runProgram(new FrayAsset("fray", FrayPipeline.STDIN, FrayPipeline.STDIN, System.in), pipeline, commandLine);
            } else {
                if (commandLine.getArgList().isEmpty()) {
                    printUsage(options);
                    System.exit(1);
                }

                final String filename = commandLine.getArgList().get(0);
                commandLine.getArgList().remove(0);
                runProgram(FrayAsset.forFile(filename), pipeline, commandLine);
            }
        } catch (ParseException exc) {
            printUsage(options);
            System.exit(1);
        } catch (Exception exc) {
            System.err.println(exc.getMessage());
            exc.printStackTrace();
            System.exit(1);
        }
    }

    private static Options cliOptions() {
        final Options options = new Options();
        options
                .addOption("d", "verbose", false, "Enable verbose debug output.")
                .addOption("h", "help", false, "Show this usage information.")
                .addOption("a", "repl", false, "Run the interactive REPL.")
                .addOption("stdin", "read-stdin", false, "Interpret input from stdin.")
                .addOption("v", "version", false, "Print the interpreter version.");
        return options;
    }

    private static FrayPipeline createPipeline() {
        return FrayPipeline.DEFAULT;
    }

    private static void printUsage(Options options) {
        new HelpFormatter().printHelp("fray [args...] <filenames>", options);
    }

    private static void runRepl(FrayPipeline pipeline, CommandLine commandLine) throws FrayException {
        final FrayInterpreter interpreter = new FrayInterpreter(commandLine, pipeline) {
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
                final FrayDatum result = interpreter.visitCompilationUnit(compilationUnit);

                for (FrayException warning : interpreter.getWarnings()) {
                    System.err.printf("Warning: %s%n", warning.getMessage());
                    warning.printStackTrace();
                }

                for (FrayException error : interpreter.getErrors()) {
                    System.err.printf("Warning: %s%n", error.getMessage());
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
            final FrayInterpreter interpreter = new FrayInterpreter(commandLine, pipeline);
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
                    System.err.printf("Warning: %s%n", error.getMessage());
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
}
