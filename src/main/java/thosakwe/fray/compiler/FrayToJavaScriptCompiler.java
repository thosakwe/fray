package thosakwe.fray.compiler;

import org.antlr.v4.runtime.ANTLRInputStream;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.tree.TerminalNode;
import thosakwe.fray.Fray;
import thosakwe.fray.grammar.FrayLexer;
import thosakwe.fray.grammar.FrayParser;
import thosakwe.fray.lang.FrayInterpreter;
import thosakwe.fray.lang.pipeline.FrayAsset;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.List;

public class FrayToJavaScriptCompiler extends FrayCompiler {
    public FrayToJavaScriptCompiler(FrayAsset sourceAsset, boolean debug) {
        super("JavaScript", "js", sourceAsset, debug);
    }

    @Override
    public String compile(FrayParser.CompilationUnitContext ctx) {
        builder.println("(function() {");
        builder.indent();
        visitCompilationUnit(ctx);
        builder.println();
        builder.println("if (typeof main !== 'undefined') {");
        builder.indent();
        builder.println("return main([]);");
        builder.outdent();
        builder.println("} else {");
        builder.indent();
        builder.println("throw new Error('No top-level function \\'main\\' declared in Fray program.');");
        builder.outdent();
        builder.println("}");
        builder.outdent();
        builder.println("})();");

        return builder.toString();
    }

    private void printParams(List<TerminalNode> trees, CodeBuilder builder) {
        for (int i = 0; i < trees.size(); i++) {
            if (i > 0)
                builder.write(", ");
            builder.write(trees.get(i).getText());
        }
    }

    private void printParams(List<TerminalNode> trees) {
        printParams(trees, builder);
    }

    private void printArgs(List<FrayParser.ExpressionContext> expressions, CodeBuilder builder) {
        for (int i = 0; i < expressions.size(); i++) {
            if (i > 0)
                builder.write(", ");
            builder.write(visitExpression(expressions.get(i)));
        }
    }

    private void printArgs(List<FrayParser.ExpressionContext> expressions) {
        printArgs(expressions, builder);
    }


    private InputStream resolveImport(FrayParser.ImportSourceContext source) throws FrayCompilerException, IOException {
        if (source.standardImport() != null) {
            final String resourceName = String.format("inc/%s.fray", source.standardImport().source.getText());
            final URL url = FrayInterpreter.class.getClassLoader().getResource(resourceName);

            if (url == null)
                throw new FrayCompilerException(String.format("Failed to import library %s. Invalid location.", source.getText()), source, this);

            printDebug(String.format("Importing stdlib: '%s'", url));
            return url.openStream();
        } else if (source.expression() instanceof FrayParser.StringLiteralExpressionContext) {
            final String filename = source.expression().getText().replaceAll("(^')|('$)", "");
            return new FileInputStream(filename);
        } else throw new FrayCompilerException(String.format("Invalid import source: '%s'", source.getText()), source, this);
    }

    @Override
    public Object visitClassDefinition(FrayParser.ClassDefinitionContext ctx) {
        final String className = ctx.name.getText();
        builder.print(String.format("function %s(", className));
        builder.writeln(") {");
        // All members
        builder.indent();

        for (FrayParser.TopLevelVariableDeclarationContext topLevelVariableDeclarationContext : ctx.topLevelVariableDeclaration()) {
            for (FrayParser.VariableDeclarationContext decl : topLevelVariableDeclarationContext.variableDeclaration()) {
                final String memberName = decl.name.getText();
                final String value = decl.expression() != null ? visitExpression(decl.expression()) : "null";
                builder.println(String.format("this.%s = %s;", memberName, value));
            }
        }

        builder.outdent();
        builder.println("}");
        builder.println();
        builder.println(String.format("%s.str = function() {", className));
        builder.indent();
        builder.println(String.format("return '[Type:%s]';", className));
        builder.outdent();
        builder.println("};");
        builder.println(String.format("%s.prototype.str = function() {", className));
        builder.indent();
        builder.println(String.format("return '[Instance of %s]';", className));
        builder.outdent();
        builder.println("};");
        builder.println();
        // Todo: Named constructors with a switch statement

        // All func defs
        for (FrayParser.TopLevelFunctionDefinitionContext func : ctx.topLevelFunctionDefinition()) {
            final String methodName = func.functionSignature().name.getText();
            builder.print(String.format("%s.prototype.%s = function(", className, methodName));
            printParams(func.functionBody().parameters().IDENTIFIER());
            builder.writeln(") {");
            builder.indent();
            builder.println("var _self = this;"); // Todo: See if 'this' is even necessary
            visitFunctionBody(func.functionBody());
            builder.outdent();
            builder.println("};");
            builder.println();
        }

        return null;
    }

    @Override
    public String visitCompilationUnit(FrayParser.CompilationUnitContext ctx) {
        printDebug(String.format("Visiting compilation unit: '%s'", ctx.getText()));
        ctx.topLevelDefinition().forEach(this::visitTopLevelDefinition);
        return null;
    }

    public String visitExpression(FrayParser.ExpressionContext ctx) {
        if (ctx instanceof FrayParser.BinaryExpressionContext) {
            final FrayParser.BinaryExpressionContext binaryExpressionContext = (FrayParser.BinaryExpressionContext) ctx;
            final String left = visitExpression(binaryExpressionContext.left);
            final String right = visitExpression(binaryExpressionContext.right);
            final String op = binaryExpressionContext.binaryOperator().getText();

            switch (op) {
                case "^":
                    return String.format("Math.pow(%s, %s)", left, right);
                default:
                    return String.format("%s %s %s", left, op, right);
            }
        }

        if (ctx instanceof FrayParser.IdentifierExpressionContext) {
            return ((FrayParser.IdentifierExpressionContext) ctx).IDENTIFIER().getText();
        }

        if (ctx instanceof FrayParser.InvocationExpressionContext) {
            final FrayParser.InvocationExpressionContext invocation = (FrayParser.InvocationExpressionContext) ctx;
            final String callee = visitExpression(invocation.callee);
            final CodeBuilder builder = new CodeBuilder();
            builder.append(String.format("%s(", callee));
            printArgs(invocation.args, builder);
            builder.append(")");
            return builder.toString();
        }

        if (ctx instanceof FrayParser.MemberExpressionContext) {
            final FrayParser.MemberExpressionContext memberExpressionContext = (FrayParser.MemberExpressionContext) ctx;
            final String target = visitExpression(memberExpressionContext.expression());
            final String name = memberExpressionContext.IDENTIFIER().getText();
            return String.format("%s.%s", target, name);
        }

        if (ctx instanceof FrayParser.NewExpressionContext) {
            final FrayParser.NewExpressionContext newExpressionContext = (FrayParser.NewExpressionContext) ctx;

            // Todo: Support named constructors...
            final CodeBuilder builder = new CodeBuilder();
            builder.print(String.format("new %s(", visitExpression(newExpressionContext.type)));
            printArgs(newExpressionContext.args, builder);
            builder.write(")");
            return builder.toString();
        }

        if (ctx instanceof FrayParser.NumericLiteralExpressionContext) {
            if (((FrayParser.NumericLiteralExpressionContext) ctx).HEX() == null && ctx.getText().contains("e")) {
                // Todo: Scientific notation...
            } else return ctx.getText();
        }

        if (ctx instanceof FrayParser.ParenthesizedExpressionContext) {
            final FrayParser.ParenthesizedExpressionContext parenthesizedExpressionContext = (FrayParser.ParenthesizedExpressionContext) ctx;
            return String.format("(%s)", visitExpression(((FrayParser.ParenthesizedExpressionContext) ctx).expression()));
        }

        if (ctx instanceof FrayParser.StringLiteralExpressionContext) {
            // Todo: Raw strings
            if (((FrayParser.StringLiteralExpressionContext) ctx).string() instanceof FrayParser.SimpleStringContext) {
                return ((FrayParser.SimpleStringContext) ((FrayParser.StringLiteralExpressionContext) ctx).string()).STRING().getText();
            }
        }

        if (ctx instanceof FrayParser.ThisExpressionContext) {
            return "_self";
        }

        return "null";
    }

    @Override
    public Object visitFunctionBody(FrayParser.FunctionBodyContext ctx) {
        if (ctx.blockBody() != null) {
            for (FrayParser.StatementContext stmt : ctx.blockBody().block().statement()) {
                visitStatement(stmt);
            }
        } else if (ctx.expressionBody() != null) {
            final String expression = visitExpression(ctx.expressionBody().expression());
            builder.println(String.format("return %s;", expression));
        }

        return null;
    }

    @Override
    public Object visitImportDeclaration(FrayParser.ImportDeclarationContext ctx) {
        try {
            // Todo: of, as...
            if (ctx.importAs() == null && ctx.importOf() == null) {
                final ANTLRInputStream inputStream = new ANTLRInputStream(resolveImport(ctx.source));
                final FrayLexer lexer = new FrayLexer(inputStream);
                final CommonTokenStream tokenStream = new CommonTokenStream(lexer);
                final FrayParser parser = new FrayParser(tokenStream);
                visitCompilationUnit(parser.compilationUnit());
            }
        } catch (FrayCompilerException exc) {
            exc.explain();
        } catch (IOException exc) {
            // Todo: import errors call System.exit(1);
        }

        return null;
    }

    public Object visitStatement(FrayParser.StatementContext ctx) {
        if (ctx instanceof FrayParser.ExpressionStatementContext) {
            builder.println(String.format("%s;", visitExpression(((FrayParser.ExpressionStatementContext) ctx).expression())));
        }

        if (ctx instanceof FrayParser.VariableDeclarationStatementContext) {
            for (FrayParser.VariableDeclarationContext decl : ((FrayParser.VariableDeclarationStatementContext) ctx).variableDeclaration()) {
                final String name = decl.name.getText();

                if (decl.expression() != null) {
                    final String value = visitExpression(decl.expression());
                    builder.println(String.format("var %s = %s;", name, value));
                } else builder.println(String.format("var %s;", name));
            }
        }

        return null;
    }

    @Override
    public String visitTopLevelFunctionDefinition(FrayParser.TopLevelFunctionDefinitionContext ctx) {
        if (Fray.annotationsContainExternal(ctx.functionSignature().annotation())) {
            // Todo: Shim external functions
            if (ctx.functionSignature().name.getText().equals("print")) {
                builder.println("function print(x) {");
                builder.indent();
                builder.println("console.log(x.str !== undefined ? x.str() : x);");
                builder.outdent();
                builder.println("}");
            }
        } else {
            final String name = ctx.functionSignature().name.getText();
            builder.print(String.format("function %s(", name));
            printParams(ctx.functionBody().parameters().IDENTIFIER());
            builder.writeln(") {");
            builder.indent();
            visitFunctionBody(ctx.functionBody());
            builder.outdent();
            builder.println("}");
        }

        builder.println();
        return null;
    }
}
