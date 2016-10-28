package thosakwe.fray.lang;

import org.antlr.v4.runtime.ANTLRFileStream;
import org.antlr.v4.runtime.ANTLRInputStream;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.tree.ParseTree;
import org.apache.commons.cli.CommandLine;
import thosakwe.fray.grammar.FrayBaseVisitor;
import thosakwe.fray.grammar.FrayLexer;
import thosakwe.fray.grammar.FrayParser;
import thosakwe.fray.lang.data.FrayDatum;
import thosakwe.fray.lang.data.FrayFunction;
import thosakwe.fray.lang.data.FrayString;
import thosakwe.fray.lang.errors.FrayException;
import thosakwe.fray.lang.shim.PrintShim;
import thosakwe.fray.lang.shim.Shim;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class FrayInterpreter extends FrayBaseVisitor {
    private static final String QUOTES = "(^r?')|('$)";
    private final CommandLine commandLine;
    private final boolean debug;
    private final List<FrayException> errors = new ArrayList<>();
    // private FrayFunction mainFunction;
    private final Scope symbolTable = new Scope();
    private final List<FrayException> warnings = new ArrayList<>();

    public FrayInterpreter(CommandLine commandLine) {
        this.commandLine = commandLine;
        this.debug = commandLine.hasOption("verbose");

        final List<Shim> shims = new ArrayList<>();
        shims.add(new PrintShim());

        for (Shim shim : shims) {
            shim.inject(this);
        }
    }

    public List<FrayException> getErrors() {
        return errors;
    }

    public Scope getSymbolTable() {
        return symbolTable;
    }

    public List<FrayException> getWarnings() {
        return warnings;
    }

    private void printDebug(String message) {
        if (debug) {
            System.out.println(message);
        }
    }

    @Override
    public Object visitCompilationUnit(FrayParser.CompilationUnitContext ctx) {
        final Object result = super.visitCompilationUnit(ctx);

        printDebug("Final symbol table:");

        if (debug) {
            symbolTable.dumpSymbols();
        }

        return result;
    }

    public FrayDatum visitExpression(FrayParser.ExpressionContext ctx) {
        printDebug(String.format("Resolving this %s: '%s'", ctx.getClass().getSimpleName(), ctx.getText()));
        return (FrayDatum) ctx.accept(this);
    }

    @Override
    public Object visitIdentifierExpression(FrayParser.IdentifierExpressionContext ctx) {
        final String name = ctx.IDENTIFIER().getText();
        printDebug(String.format("Resolving ID: '%s'", name));
        return symbolTable.getValue(name);
    }

    @Override
    public Object visitImportDeclaration(FrayParser.ImportDeclarationContext ctx) {
        try {
            ANTLRInputStream inputStream;

            if (ctx.source.string() != null) {
                final String filename = ctx.source.string().getText().replaceAll(QUOTES, "");
                printDebug(String.format("Now importing '%s'...", filename));
                inputStream = new ANTLRFileStream(filename);
            } else if (ctx.source.standardImport() != null) {
                final String name = ctx.source.standardImport().source.getText();
                final String resourceName = String.format("inc/%s.fray", name);
                printDebug(String.format("Now importing resource for stdlib: '%s'", resourceName));
                inputStream = new ANTLRInputStream(FrayInterpreter.class.getClassLoader().getResourceAsStream(resourceName));
            } else
                throw new FrayException(String.format("Invalid source given for import: '%s'", ctx.getText()), ctx, this);

            final FrayLexer lexer = new FrayLexer(inputStream);
            final CommonTokenStream tokenStream = new CommonTokenStream(lexer);
            final FrayParser parser = new FrayParser(tokenStream);
            final FrayParser.CompilationUnitContext compilationUnitContext = parser.compilationUnit();
            final FrayInterpreter interpreter = new FrayInterpreter(commandLine);
            interpreter.visitCompilationUnit(compilationUnitContext);
            symbolTable.load(interpreter.symbolTable);
            return super.visitImportDeclaration(ctx);
        } catch (Exception exc) {
            errors.add(new FrayException(
                    String.format("Could not import source '%s'. %s", ctx.source.getText(), exc.getMessage()),
                    ctx,
                    this));
            return null;
        }
    }

    @Override
    public Object visitInvocationExpression(FrayParser.InvocationExpressionContext ctx) {
        try {
            final FrayDatum callee = visitExpression(ctx.callee);
            final List<FrayDatum> args = ctx.args.stream().map(this::visitExpression).collect(Collectors.toList());

            if (callee == null)
                throw new FrayException(String.format("Invalid target for invocation: '%s'", ctx.callee.getText()), ctx, this);
            return callee.call(this, ctx, args);
        } catch (FrayException exc) {
            errors.add(exc);
            return null;
        }
    }

    @Override
    public FrayString visitStringLiteralExpression(FrayParser.StringLiteralExpressionContext ctx) {
        if (ctx.string() instanceof FrayParser.SimpleStringContext) {
            // Todo: interpolation :)
            final String str = ((FrayParser.SimpleStringContext) ctx.string()).STRING().getText().replaceAll(QUOTES, "");
            return new FrayString(ctx, str);
        }

        return null;
    }

    @Override
    public Object visitTopLevelFunctionDefinition(FrayParser.TopLevelFunctionDefinitionContext ctx) {
        final String name = ctx.functionSignature().name.getText();
        symbolTable.setValue(name, new FrayFunction(ctx) {
            @Override
            public FrayDatum call(FrayInterpreter interpreter, ParseTree source, List<FrayDatum> args) throws FrayException {
                return super.call(interpreter, source, args);
            }

            @Override
            public String toString() {
                return String.format("[Function:%s]", ctx.functionBody().parameters().getText());
            }
        }, true);
        return super.visitTopLevelFunctionDefinition(ctx);
    }
}
