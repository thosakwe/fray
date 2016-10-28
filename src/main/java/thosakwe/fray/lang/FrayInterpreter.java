package thosakwe.fray.lang;

import org.antlr.v4.runtime.ANTLRFileStream;
import org.antlr.v4.runtime.ANTLRInputStream;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.tree.ParseTree;
import org.apache.commons.cli.CommandLine;
import thosakwe.fray.grammar.FrayBaseVisitor;
import thosakwe.fray.grammar.FrayLexer;
import thosakwe.fray.grammar.FrayParser;
import thosakwe.fray.lang.data.*;
import thosakwe.fray.lang.errors.FrayException;
import thosakwe.fray.lang.pipeline.FrayAsset;
import thosakwe.fray.lang.pipeline.FrayPipeline;
import thosakwe.fray.lang.shim.PrintShim;
import thosakwe.fray.lang.shim.ProcessShim;
import thosakwe.fray.lang.shim.Shim;

import java.io.FileInputStream;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class FrayInterpreter extends FrayBaseVisitor<FrayDatum> {
    private static final String QUOTES = "(^r?')|('$)";
    private final CommandLine commandLine;
    private final boolean debug;
    private final List<FrayException> errors = new ArrayList<>();
    // private FrayFunction mainFunction;
    private final Scope symbolTable = new Scope();
    private final List<FrayException> warnings = new ArrayList<>();
    private final FrayPipeline pipeline;

    public FrayInterpreter(CommandLine commandLine, FrayPipeline pipeline) throws FrayException {
        this.commandLine = commandLine;
        this.debug = commandLine.hasOption("verbose");
        this.pipeline = pipeline;

        final List<Shim> shims = new ArrayList<>();

        shims.add(new PrintShim());
        shims.add(new ProcessShim());

        for (Shim shim : shims) {
            shim.inject(this);
        }
    }

    private FrayFunction compileFunctionBody(FrayParser.FunctionBodyContext ctx) {
        return new FrayFunction(ctx, this) {
            @Override
            public FrayDatum call(FrayInterpreter interpreter, ParseTree source, List<FrayDatum> args) throws FrayException {
                // Todo: Inject params
                printDebug(String.format("Now running this function: '%s'", ctx.getText()));
                symbolTable.create();
                printDebug("Injecting arguments...");

                for (int i = 0; i < ctx.parameters().IDENTIFIER().size(); i++) {
                    final String name = ctx.parameters().IDENTIFIER(i).getText();
                    final FrayDatum value = i < args.size() ? args.get(i) : new FrayNull();
                    printDebug(String.format("Injecting '%s' => %s", name, value.toString()));
                    symbolTable.createNew(name, value, ctx.parameters().IDENTIFIER(i), interpreter);
                }

                final FrayDatum result = visitFunctionBody(ctx);
                symbolTable.destroy();
                printDebug(String.format("Result of running function: %s", String.valueOf(result)));
                return result;
            }

            @Override
            public String toString() {
                return String.format("[Function:%s]", ctx.parameters().getText());
            }

            @Override
            public String curses() {
                return String.format("\033[35m%s", toString());
            }
        };
    }

    public List<FrayException> getErrors() {
        return errors;
    }

    public FrayPipeline getPipeline() {
        return pipeline;
    }

    public Scope getSymbolTable() {
        return symbolTable;
    }

    public List<FrayException> getWarnings() {
        return warnings;
    }

    public void printDebug(String message) {
        if (debug) {
            System.out.println(message);
        }
    }

    @Override
    public FrayDatum visitAssignmentExpression(FrayParser.AssignmentExpressionContext ctx) {
        try {
            if (ctx.left instanceof FrayParser.IdentifierExpressionContext) {
                final String name = ((FrayParser.IdentifierExpressionContext) ctx.left).IDENTIFIER().getText();
                final Symbol symbol = symbolTable.getSymbol(name);

                if (symbol == null)
                    throw new FrayException(String.format("'%s' does not exist within this context.", name), ctx.left, this);

                printDebug(String.format("Symbol to assign: name=%s, final?: %s", name, symbol.isFinal() ? "true" : "false"));

                if (symbol.isFinal())
                    throw new FrayException(String.format("Cannot re-assign final variable '%s'.", name), ctx.left, this);

                final FrayDatum right = visitExpression(ctx.right);
                printDebug(String.format("Re-assigning %s to %s...", name, right.toString()));
                symbol.setValue(right);
                return visitExpression(ctx.left);
            } else {
                throw new FrayException(String.format("Cannot assign a value to expression '%s'.", ctx.left.getText()), ctx.left, this);
            }
        } catch (FrayException exc) {
            errors.add(exc);
            return null;
        }
    }

    @Override
    public FrayDatum visitBinaryExpression(FrayParser.BinaryExpressionContext ctx) {
        try {
            final FrayDatum left = visitExpression(ctx.left), right = visitExpression(ctx.right);
            final String op = ctx.binaryOperator().getText();

            if (left == null)
                throw new FrayException(String.format("Invalid left side of binary expression: '%s'", ctx.left.getText()), ctx.left, this);
            if (right == null)
                throw new FrayException(String.format("Invalid right side of binary expression: '%s'", ctx.right.getText()), ctx.right, this);

            printDebug(String.format("Left: %s, right: %s, operator: %s", left.toString(), right.toString(), op));

            switch (op) {
                case "+":
                    return left.plus(ctx, right);
                case "-":
                    return left.minus(ctx, right);
                case "*":
                    return left.times(ctx, right);
                case "/":
                    return left.divide(ctx, right);
                case "%":
                    return left.modulo(ctx, right);
                case "^":
                    return left.pow(ctx, right);
                case "==":
                    return left.equ(ctx, right);
                case "!=":
                    return left.nequ(ctx, right);
                case "<=":
                    return left.lequ(ctx, right);
                case ">=":
                    return left.gequ(ctx, right);
                case "&&":
                    return left.and(ctx, right);
                case "||":
                    return left.or(ctx, right);
                case "<":
                    return left.lt(ctx, right);
                case ">":
                    return left.gt(ctx, right);
                default:
                    return null;
            }
        } catch (FrayException exc) {
            errors.add(exc);
            return null;
        }
    }

    @Override
    public FrayDatum visitBlock(FrayParser.BlockContext ctx) {
        // Todo: Handle returns and breaks
        for (FrayParser.StatementContext stmt : ctx.statement()) {
            if (stmt instanceof FrayParser.ReturnStatementContext)
                return visitExpression(((FrayParser.ReturnStatementContext) stmt).expression());
            else visitStatement(stmt);
        }

        return null;
    }

    @Override
    public FrayDatum visitBooleanLiteralExpression(FrayParser.BooleanLiteralExpressionContext ctx) {
        try {
            if (ctx.TRUE() != null)
                return FrayBoolean.TRUE;
            else if (ctx.FALSE() != null)
                return FrayBoolean.FALSE;
            else throw new FrayException(String.format("Invalid boolean literal: '%s'", ctx.getText()), ctx, this);
        } catch (FrayException exc) {
            errors.add(exc);
        }

        return null;
    }

    @Override
    public FrayDatum visitCompilationUnit(FrayParser.CompilationUnitContext ctx) {
        FrayDatum result = null;

        for (FrayParser.TopLevelDefinitionContext def : ctx.topLevelDefinition()) {
            if (def.topLevelStatement() != null) {
                if (def.topLevelStatement().statement() instanceof FrayParser.ExpressionStatementContext) {
                    result = visitExpression(((FrayParser.ExpressionStatementContext) def.topLevelStatement().statement()).expression());
                }
            } else visitTopLevelDefinition(def);
        }

        if (debug) {
            symbolTable.dumpSymbols();
        }

        return result;
    }

    @Override
    public FrayDatum visitExclusiveRangeExpression(FrayParser.ExclusiveRangeExpressionContext ctx) {
        try {
            final FraySet result = new FraySet(ctx, this);
            final FrayDatum lower = visitExpression(ctx.lower), upper = visitExpression(ctx.upper);

            if (!(lower instanceof FrayNumber))
                throw new FrayException("Lower bound must be an integer", ctx.lower, this);

            if (!(upper instanceof FrayNumber))
                throw new FrayException("Upper bound must be an integer", ctx.upper, this);

            final Double lowerBound = ((FrayNumber) lower).getValue();
            final Double upperBound = ((FrayNumber) upper).getValue();

            printDebug(String.format("Range: %s, lower: %d, upper: %d", ctx.getText(), lowerBound.intValue(), upperBound.intValue()));

            for (int i = lowerBound.intValue(); i < upperBound; i++) {
                printDebug(String.format("Adding item to set: %d", i));
                result.getItems().add(new FrayNumber(ctx, this, i));
            }

            return result;
        } catch (FrayException exc) {
            errors.add(exc);
            return null;
        }
    }

    public FrayDatum visitExpression(FrayParser.ExpressionContext ctx) {
        printDebug(String.format("Resolving this %s: '%s'", ctx.getClass().getSimpleName().replaceAll("ExpressionContext$", ""), ctx.getText()));
        final FrayDatum result = ctx.accept(this);
        printDebug(String.format("Got value: %s", String.valueOf(result)));
        return result;
    }

    @Override
    public FrayDatum visitForStatement(FrayParser.ForStatementContext ctx) {
        try {
            final boolean isFinal = ctx.FINAL() != null;
            final FrayDatum target = visitExpression(ctx.expression());

            if (target == null)
                throw new FrayException(String.format("Invalid expression for loop: '%s'", ctx.expression().getText()), ctx.expression(), this);

            printDebug(String.format("For statement target: %s", target.toString()));
            final FrayIterator iterator = target.getIterator(this);

            if (iterator == null)
                throw new FrayException(String.format("%s does not expose an iterator.", target.toString()), ctx.expression(), this);

            printDebug(String.format("Iterator: %s", iterator.toString()));

            final List<FrayDatum> args = new ArrayList<>();
            final String name = ctx.as.getText();
            FrayDatum condition = iterator.getSymbolTable().getValue("moveNext").call(this, ctx, args);

            while (condition != null && condition.isTrue()) {
                symbolTable.create();
                final FrayDatum current = iterator.getSymbolTable().getValue("current");
                printDebug(String.format("For statement injecting: %s, value: %s, final?: %s", name, current.toString(), isFinal ? "true" : "false"));

                if (debug) {
                    symbolTable.dumpSymbols();
                }

                symbolTable.setValue(name, current, ctx, this, isFinal);
                visitBlock(ctx.block());
                symbolTable.destroy();
                condition = iterator.getSymbolTable().getValue("moveNext").call(this, ctx, args);
            }
        } catch (FrayException exc) {
            errors.add(exc);
        }

        return null;
    }

    @Override
    public FrayDatum visitFunctionBody(FrayParser.FunctionBodyContext ctx) {
        if (ctx.blockBody() != null)
            return visitBlock(ctx.blockBody().block());
        else if (ctx.expressionBody() != null)
            return visitExpression(ctx.expressionBody().expression());
        return null;
    }

    @Override
    public FrayDatum visitFunctionExpression(FrayParser.FunctionExpressionContext ctx) {
        return compileFunctionBody(ctx.functionBody());
    }

    @Override
    public FrayDatum visitIdentifierExpression(FrayParser.IdentifierExpressionContext ctx) {
        if (debug) {
            symbolTable.dumpSymbols();
        }

        return symbolTable.getValue(ctx.IDENTIFIER().getText());
    }

    @Override
    public FrayDatum visitIfStatement(FrayParser.IfStatementContext ctx) {
        try {
            for (FrayParser.IfBlockContext ifBlock : ctx.ifBlock()) {
                printDebug(String.format("Evaluating this condition: '%s'", ifBlock.condition.getText()));
                final FrayDatum condition = visitExpression(ifBlock.condition);

                if (condition == null)
                    throw new FrayException(String.format("Invalid condition for if statement: '%s'", ifBlock.condition.getText()), ifBlock.condition, this);

                if (condition.isTrue()) {
                    return visitBlock(ifBlock.block());
                }
            }

            if (ctx.elseBlock() != null)
                return visitBlock(ctx.elseBlock().block());
        } catch (FrayException exc) {
            errors.add(exc);
        }

        return null;
    }

    @Override
    public FrayDatum visitImportDeclaration(FrayParser.ImportDeclarationContext ctx) {
        try {
            FrayAsset asset;
            String source;

            if (ctx.source.string() != null) {
                final String filename = source = ctx.source.string().getText().replaceAll(QUOTES, "");
                printDebug(String.format("Now importing '%s'...", filename));
                asset = FrayAsset.forFile(filename);
            } else if (ctx.source.standardImport() != null) {
                final String name = ctx.source.standardImport().source.getText();
                source = ctx.source.standardImport().getText();
                final String resourceName = String.format("inc/%s.fray", name);
                printDebug(String.format("Now importing resource from stdlib: '%s'", resourceName));
                final URL url = FrayInterpreter.class.getClassLoader().getResource(resourceName);
                asset = FrayAsset.forUrl(url);
            } else
                throw new FrayException(String.format("Invalid source given for import: '%s'", ctx.getText()), ctx, this);

            final FrayAsset processed = pipeline.transform(asset);
            final FrayLexer lexer = new FrayLexer(new ANTLRInputStream(processed.getInputStream()));
            final CommonTokenStream tokenStream = new CommonTokenStream(lexer);
            final FrayParser parser = new FrayParser(tokenStream);
            final FrayParser.CompilationUnitContext compilationUnitContext = parser.compilationUnit();
            final FrayInterpreter interpreter = new FrayInterpreter(commandLine, pipeline);
            interpreter.visitCompilationUnit(compilationUnitContext);

            if (ctx.importAs() == null) {
                symbolTable.load(interpreter.symbolTable);
                return super.visitImportDeclaration(ctx);
            } else {
                final FrayDatum library = new FrayDatum(ctx, this) {
                    @Override
                    public String toString() {
                        return String.format("[Library:%s]", source);
                    }

                    @Override
                    public String curses() {
                        return String.format("\033[32m%s", toString());
                    }
                };

                library.getSymbolTable().load(interpreter.symbolTable);
                symbolTable.setValue(ctx.importAs().alias.getText(), library, ctx, this, true);

                return library;
            }
        } catch (Exception exc) {
            errors.add(new FrayException(
                    String.format("Could not import source '%s'. %s", ctx.source.getText(), exc.getMessage()),
                    ctx,
                    this));
            return null;
        }
    }

    @Override
    public FrayDatum visitInclusiveRangeExpression(FrayParser.InclusiveRangeExpressionContext ctx) {
        try {
            final FraySet result = new FraySet(ctx, this);
            final FrayDatum lower = visitExpression(ctx.lower), upper = visitExpression(ctx.upper);

            if (!(lower instanceof FrayNumber))
                throw new FrayException("Lower bound must be an integer", ctx.lower, this);

            if (!(upper instanceof FrayNumber))
                throw new FrayException("Upper bound must be an integer", ctx.upper, this);

            final Double lowerBound = ((FrayNumber) lower).getValue();
            final Double upperBound = ((FrayNumber) upper).getValue();

            printDebug(String.format("Range: %s, lower: %d, upper: %d", ctx.getText(), lowerBound.intValue(), upperBound.intValue()));

            for (int i = lowerBound.intValue(); i <= upperBound; i++) {
                printDebug(String.format("Adding item to set: %d", i));
                result.getItems().add(new FrayNumber(ctx, this, i));
            }

            return result;
        } catch (FrayException exc) {
            errors.add(exc);
            return null;
        }
    }

    @Override
    public FrayDatum visitInvocationExpression(FrayParser.InvocationExpressionContext ctx) {
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
    public FrayDatum visitMemberExpression(FrayParser.MemberExpressionContext ctx) {
        try {
            final FrayDatum target = visitExpression(ctx.expression());
            final String name = ctx.IDENTIFIER().getText();
            printDebug(String.format("Expression: %s, member: %s", String.valueOf(target), name));

            if (target == null || target instanceof FrayNull)
                throw new FrayException(String.format("Invalid expression: '%s'", ctx.expression().getText()), ctx.expression(), this);

            final FrayDatum resolved = target.getSymbolTable().getValue(name);

            if (resolved == null)
                throw new FrayException(String.format("%s has no member '%s'.", target.toString(), name), ctx.IDENTIFIER(), this);

            return resolved;
        } catch (FrayException exc) {
            errors.add(exc);
            return null;
        }
    }

    @Override
    public FrayDatum visitNullLiteralExpression(FrayParser.NullLiteralExpressionContext ctx) {
        return new FrayNull();
    }

    @Override
    public FrayDatum visitParenthesizedExpression(FrayParser.ParenthesizedExpressionContext ctx) {
        return visitExpression(ctx.expression());
    }

    @Override
    public FrayDatum visitNumericLiteralExpression(FrayParser.NumericLiteralExpressionContext ctx) {
        if (ctx.INT() != null)
            return new FrayNumber(ctx, this, Integer.parseInt(ctx.INT().getText()));
        else if (ctx.DOUBLE() != null)
            return new FrayNumber(ctx, this, Double.parseDouble(ctx.DOUBLE().getText()));
        else if (ctx.HEX() != null)
            return new FrayNumber(ctx, this, Integer.parseInt(ctx.HEX().getText().replaceAll("^0x", ""), 16));
        return super.visitNumericLiteralExpression(ctx);
    }

    public FrayDatum visitStatement(FrayParser.StatementContext ctx) {
        printDebug(String.format("Now visiting this %s: '%s'", ctx.getClass().getSimpleName(), ctx.getText()));
        return ctx.accept(this);
    }

    @Override
    public FrayString visitStringLiteralExpression(FrayParser.StringLiteralExpressionContext ctx) {
        if (ctx.string() instanceof FrayParser.SimpleStringContext) {
            // Todo: interpolation :)
            final String str = ((FrayParser.SimpleStringContext) ctx.string()).STRING().getText().replaceAll(QUOTES, "");
            return new FrayString(ctx, this, str);
        }

        return null;
    }

    @Override
    public FrayDatum visitTopLevelFunctionDefinition(FrayParser.TopLevelFunctionDefinitionContext ctx) {
        try {
            final String name = ctx.functionSignature().name.getText();
            symbolTable.setValue(name, compileFunctionBody(ctx.functionBody()), ctx, this, true);
        } catch (FrayException exc) {
            errors.add(exc);
        }

        return null;
    }

    @Override
    public FrayDatum visitTopLevelStatement(FrayParser.TopLevelStatementContext ctx) {
        try {
            throw new FrayException("This statement must be within a function.", ctx, this);
        } catch (FrayException exc) {
            errors.add(exc);
        }
        return null;
    }

    @Override
    public FrayDatum visitTopLevelVariableDeclaration(FrayParser.TopLevelVariableDeclarationContext ctx) {
        try {
            final boolean isFinal = ctx.FINAL() != null;

            for (FrayParser.VariableDeclarationContext declaration : ctx.variableDeclaration()) {
                final String name = declaration.name.getText();
                final FrayDatum value = declaration.expression() != null ? visitExpression(declaration.expression()) : new FrayNull();
                symbolTable.setValue(name, value, ctx, this, isFinal);
            }
        } catch (FrayException exc) {
            errors.add(exc);
        }

        return null;
    }

    @Override
    public FrayDatum visitVariableDeclarationStatement(FrayParser.VariableDeclarationStatementContext ctx) {
        try {
            final boolean isFinal = ctx.FINAL() != null;

            for (FrayParser.VariableDeclarationContext declaration : ctx.variableDeclaration()) {
                final String name = declaration.name.getText();
                final FrayDatum value = declaration.expression() != null ? visitExpression(declaration.expression()) : new FrayNull();
                printDebug(String.format("Setting '%s' to %s", name, value.toString()));
                symbolTable.createNew(name, value, ctx, this, isFinal);
            }
        } catch (FrayException exc) {
            errors.add(exc);
        }

        return null;
    }
}
