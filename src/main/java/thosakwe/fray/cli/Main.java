package thosakwe.fray.cli;

import org.antlr.v4.runtime.ANTLRFileStream;
import org.antlr.v4.runtime.ANTLRInputStream;
import org.antlr.v4.runtime.CommonTokenStream;
import org.apache.commons.cli.*;
import thosakwe.fray.grammar.FrayBaseVisitor;
import thosakwe.fray.grammar.FrayLexer;
import thosakwe.fray.grammar.FrayParser;
import thosakwe.fray.lang.FrayInterpreter;
import thosakwe.fray.lang.errors.FrayException;

import java.util.List;

public class Main {
    public static void main(String[] args) {
        final Options options = cliOptions();

        try {
            final CommandLine commandLine = new DefaultParser().parse(options, args);

            if (commandLine.hasOption("help")) {
                printUsage(options);
                return;
            }

            if (commandLine.hasOption("read-stdin")) {
                runProgram(new ANTLRInputStream(System.in), commandLine);
            } else {
                if (commandLine.getArgList().isEmpty()) {
                    printUsage(options);
                    System.exit(1);
                }

                final String filename = commandLine.getArgList().get(0);
                commandLine.getArgList().remove(0);
                runProgram(new ANTLRFileStream(filename), commandLine);
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
                .addOption("stdin", "read-stdin", false, "Interpret input from stdin.");
        return options;
    }

    private static void printUsage(Options options) {
        new HelpFormatter().printHelp("fray [args...] <filenames>", options);
    }

    private static void runProgram(ANTLRInputStream inputStream, CommandLine commandLine) {
        final FrayLexer lexer = new FrayLexer(inputStream);
        final CommonTokenStream tokenStream = new CommonTokenStream(lexer);
        final FrayParser parser = new FrayParser(tokenStream);
        final FrayParser.CompilationUnitContext compilationUnitContext = parser.compilationUnit();
        final FrayInterpreter interpreter = new FrayInterpreter(commandLine);
        interpreter.visitCompilationUnit(compilationUnitContext);

        for (FrayException warning : interpreter.getWarnings()) {
            System.err.printf("Warning: %s%n", warning.getMessage());
            warning.printStackTrace();
        }

        for (FrayException error : interpreter.getErrors()) {
            System.err.printf("Warning: %s%n", error.getMessage());
            error.printStackTrace();
        }
    }
}
