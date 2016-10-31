package thosakwe.fray.compiler;

import org.antlr.v4.runtime.ANTLRInputStream;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.tree.TerminalNode;
import thosakwe.fray.Fray;
import thosakwe.fray.grammar.FrayLexer;
import thosakwe.fray.grammar.FrayParser;
import thosakwe.fray.interpreter.FrayInterpreter;
import thosakwe.fray.pipeline.FrayAsset;
import thosakwe.fray.pipeline.FrayPipeline;
import thosakwe.fray.pipeline.FrayTransformer;
import thosakwe.fray.pipeline.StringInterpolatorTransformer;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

public class FrayToJavaScriptTranspiler extends FrayTranspiler {
    public FrayToJavaScriptTranspiler(FrayAsset sourceAsset, boolean debug) {
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
        final List<String> args = new ArrayList<>();

        for (int i = 0; i < expressions.size(); i++) {
            args.add(visitExpression(expressions.get(i)));
        }

        for (int i = 0; i < args.size(); i++) {
            if (i > 0)
                builder.write(", ");
            builder.write(args.get(i));
        }
    }

    private void printArgs(List<FrayParser.ExpressionContext> expressions) {
        printArgs(expressions, builder);
    }

    private InputStream resolveImport(FrayParser.ImportSourceContext source) throws FrayCompilerException, IOException {
        InputStream inputStream;
        String name;
        String resourceName;

        if (source.standardImport() != null) {
            name = source.standardImport().source.getText();
            resourceName = String.format("inc/%s.fray", name);
            final URL url = FrayInterpreter.class.getClassLoader().getResource(resourceName);

            if (url == null)
                throw new FrayCompilerException(String.format("Failed to import library %s. Invalid location.", source.getText()), source, this);

            printDebug(String.format("Importing stdlib: '%s'", url));
            inputStream = url.openStream();
        } else if (source.expression() instanceof FrayParser.StringLiteralExpressionContext) {
            name = source.expression().getText().replaceAll("(^')|('$)", "");
            resourceName = name + ".fray";
            inputStream = new FileInputStream(resourceName);
        } else
            throw new FrayCompilerException(String.format("Invalid import source: '%s'", source.getText()), source, this);

        final FrayAsset asset = new FrayAsset("fray", resourceName, name, inputStream);
        final FrayPipeline pipeline = new FrayPipeline(new FrayTransformer[]{new StringInterpolatorTransformer()});
        return pipeline.transform(asset).getInputStream();
    }

    private void shimExternal(String name, String prefix) {
        final String resourceName = String.format("external/js/%s.%s.js", name, prefix);
        printDebug(String.format("Shimming top-level function: %s from '%s'...", name, resourceName));
        final URL url = FrayToJavaScriptTranspiler.class.getClassLoader().getResource(resourceName);
        printDebug(String.format("URL: '%s'", url));

        try {
            if (url == null)
                throw new NullPointerException();

            final InputStream resource = url.openStream();
            final Scanner resourceScanner = new Scanner(resource);
            builder.println();

            while (resourceScanner.hasNextLine()) {
                builder.println(resourceScanner.nextLine());
            }

            builder.println();
            resourceScanner.close();
        } catch (Exception exc) {
            printDebug("Hm, couldn't open resource... :/");
            System.err.println(String.format("Could not find definition for external '%s'.", name));
            System.exit(1);
        }
    }

    @Override
    public Object visitBlock(FrayParser.BlockContext ctx) {
        ctx.statement().forEach(this::visitStatement);
        return null;
    }

    @Override
    public Object visitClassDefinition(FrayParser.ClassDefinitionContext ctx) {
        final String className = ctx.name.getText();

        if (Fray.annotationsContainExternal(ctx.annotation())) {
            shimExternal(className, "class");
            return null;
        }

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

        // Todo: Named constructors with a switch statement
        builder.println("var constructorName = arguments.length > 0 ? arguments[0] : undefined;");

        for (FrayParser.ConstructorDefinitionContext def : ctx.constructorDefinition()) {
            final String constructorName = def.name != null ? def.name.getText() : "";
            builder.println();
            builder.println(String.format("if (constructorName === '%s') {", constructorName));
            builder.indent();
            builder.println("var _self = this;");

            for (int i = 0; i < def.functionBody().parameters().IDENTIFIER().size(); i++) {
                final String paramName = def.functionBody().parameters().IDENTIFIER(i).getText();
                builder.println(String.format("var %s = arguments.length > %d ? arguments[%d] : undefined;", paramName, i + 1, i + 1));
            }

            visitFunctionBody(def.functionBody());
            builder.outdent();
            builder.println("}");
        }

        builder.outdent();
        builder.println("}");
        builder.println();
        builder.println(String.format("%s.str = function() {", className));
        builder.indent();
        builder.println(String.format("return '[Type:%s]';", className));
        builder.outdent();
        builder.println("};");
        builder.println();
        builder.println(String.format("%s.prototype.str = function() {", className));
        builder.indent();
        builder.println(String.format("return '[Instance of %s]';", className));
        builder.outdent();
        builder.println("};");
        builder.println();

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
        ctx.topLevelDefinition().forEach(this::visitTopLevelDefinition);
        return null;
    }

    public String visitExpression(FrayParser.ExpressionContext ctx) {
        if (ctx instanceof FrayParser.AssignmentExpressionContext) {
            final FrayParser.AssignmentExpressionContext assignmentExpressionContext = (FrayParser.AssignmentExpressionContext) ctx;
            final String left = visitExpression(assignmentExpressionContext.left);
            final String right = visitExpression(assignmentExpressionContext.right);
            final String op = assignmentExpressionContext.assignmentOperator().getText();

            switch (op) {
                case "^=":
                    return String.format("%s = Math.pow(%s, %s)", left, left, right);
                default:
                    return String.format("%s %s %s", left, op, right);
            }
        }

        if (ctx instanceof FrayParser.BinaryExpressionContext) {
            final FrayParser.BinaryExpressionContext binaryExpressionContext = (FrayParser.BinaryExpressionContext) ctx;
            final String left = visitExpression(binaryExpressionContext.left);
            final String right = visitExpression(binaryExpressionContext.right);
            final String op = binaryExpressionContext.binaryOperator().getText();

            // Todo: Custom operators
            switch (op) {
                case "^":
                    return String.format("Math.pow(%s, %s)", left, right);
                case "==":
                    return String.format("%s === %s", left, right);
                case "!=":
                    return String.format("%s !== %s", left, right);
                default:
                    return String.format("%s %s %s", left, op, right);
            }
        }

        if (ctx instanceof FrayParser.BooleanLiteralExpressionContext) {
            return ctx.getText();
        }

        if (ctx instanceof FrayParser.FunctionExpressionContext) {
            builder.print("function __func(");
            printParams(((FrayParser.FunctionExpressionContext) ctx).functionBody().parameters().IDENTIFIER());
            builder.writeln(") {");
            builder.indent();
            visitFunctionBody(((FrayParser.FunctionExpressionContext) ctx).functionBody());
            builder.outdent();
            builder.println("}");
            return "__func";
        }

        if (ctx instanceof FrayParser.IdentifierExpressionContext) {
            return ((FrayParser.IdentifierExpressionContext) ctx).IDENTIFIER().getText();
        }

        if (ctx instanceof FrayParser.InclusiveRangeExpressionContext) {
            final FrayParser.InclusiveRangeExpressionContext inclusiveRangeExpressionContext = (FrayParser.InclusiveRangeExpressionContext) ctx;
            final String lower = visitExpression(inclusiveRangeExpressionContext.lower);
            final String upper = visitExpression(inclusiveRangeExpressionContext.upper);
            final StringWriter writer = new StringWriter();
            writer.write("(function () {");
            writer.write("var a = [];");
            writer.write(String.format("for (var i = %s; i <= %s; i++) {", lower, upper));
            writer.write("a.push(i);");
            writer.write("}");
            writer.write("a.iterator = new Iterator('', a);");
            writer.write("return a;");
            writer.write("})()");
            return writer.toString();
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

            if (newExpressionContext.type instanceof FrayParser.MemberExpressionContext) {
                final FrayParser.MemberExpressionContext memberExpressionContext = (FrayParser.MemberExpressionContext) newExpressionContext.type;
                builder.write(String.format("new %s('%s'", visitExpression(memberExpressionContext.expression()), memberExpressionContext.IDENTIFIER().getText()));

                if (!newExpressionContext.args.isEmpty())
                    builder.write(", ");

                printArgs(newExpressionContext.args, builder);
                builder.write(")");
            } else {
                builder.write(String.format("new %s(''", visitExpression(newExpressionContext.type)));

                if (!newExpressionContext.args.isEmpty())
                    builder.write(", ");

                printArgs(newExpressionContext.args, builder);
                builder.write(")");
            }

            return builder.toString();
        }

        if (ctx instanceof FrayParser.NullLiteralExpressionContext) {
            return "null";
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

        if (ctx instanceof FrayParser.SetIndexerExpressionContext) {
            final FrayParser.SetIndexerExpressionContext setIndexerExpressionContext = (FrayParser.SetIndexerExpressionContext) ctx;
            final String target = visitExpression(setIndexerExpressionContext.target);
            final String index = visitExpression(setIndexerExpressionContext.index);
            return String.format("%s[%s]", target, index);
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

        return String.format("'Cannot compile expressions of type %s yet. ):'", ctx.getClass().getSimpleName());
    }

    @Override
    public Object visitFunctionBody(FrayParser.FunctionBodyContext ctx) {
        if (ctx.blockBody() != null) {
            ctx.blockBody().block().statement().forEach(this::visitStatement);
        } else if (ctx.expressionBody() != null) {
            final String expression = visitExpression(ctx.expressionBody().expression());
            builder.println(String.format("return %s;", expression));
        }

        return null;
    }

    @Override
    public Object visitIfStatement(FrayParser.IfStatementContext ctx) {
        for (int i = 0; i < ctx.ifBlock().size(); i++) {
            final FrayParser.IfBlockContext ifBlockContext = ctx.ifBlock(i);
            builder.println(String.format("if ((%s) === true) {", visitExpression(ifBlockContext.condition)));
            builder.indent();
            ifBlockContext.block().statement().forEach(this::visitStatement);
            builder.outdent();
            builder.println("}");
        }

        if (ctx.elseBlock() != null) {
            builder.println("else {");
            builder.indent();
            ctx.elseBlock().block().statement().forEach(this::visitStatement);
            builder.outdent();
            builder.println("}");
        }

        return null;
    }

    @Override
    public Object visitImportDeclaration(FrayParser.ImportDeclarationContext ctx) {
        try {
            // Todo: of, as...
            final ANTLRInputStream inputStream = new ANTLRInputStream(resolveImport(ctx.source));
            final FrayLexer lexer = new FrayLexer(inputStream);
            final CommonTokenStream tokenStream = new CommonTokenStream(lexer);
            final FrayParser parser = new FrayParser(tokenStream);
            final FrayParser.CompilationUnitContext compilationUnit = parser.compilationUnit();

            if (ctx.importAs() == null && ctx.importOf() == null) {
                visitCompilationUnit(compilationUnit);
            } else if (ctx.importAs() != null) {
                if (ctx.importOf() == null) {
                    final String alias = ctx.importAs().alias.getText();
                    builder.println(String.format("var %s = {};", alias));

                    for (FrayParser.TopLevelDefinitionContext topLevelDefinitionContext : compilationUnit.topLevelDefinition()) {
                        if (topLevelDefinitionContext.topLevelFunctionDefinition() != null) {
                            final FrayParser.TopLevelFunctionDefinitionContext def = topLevelDefinitionContext.topLevelFunctionDefinition();
                            final String functionName = def.functionSignature().name.getText();
                            builder.print(String.format("%s.%s = function(", alias, functionName));
                            printParams(def.functionBody().parameters().IDENTIFIER());
                            builder.writeln(") {");
                            builder.indent();
                            visitFunctionBody(def.functionBody());
                            builder.outdent();
                            builder.println("};");
                        } else if (topLevelDefinitionContext.classDefinition() != null) {
                            final String className = topLevelDefinitionContext.classDefinition().name.getText();
                            visitClassDefinition(topLevelDefinitionContext.classDefinition());
                            builder.println(String.format("%s.%s = %s;", alias, className, className));
                        } else if (topLevelDefinitionContext.topLevelVariableDeclaration() != null) {
                            for (FrayParser.VariableDeclarationContext decl : topLevelDefinitionContext.topLevelVariableDeclaration().variableDeclaration()) {
                                final String variableName = decl.name.getText();
                                final String value = decl.expression() != null ? visitExpression(decl.expression()) : "null";
                                builder.println(String.format("%s.%s = %s;", alias, variableName, value));
                            }
                        }
                    }
                }
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
            final FrayParser.ExpressionContext expressionContext = ((FrayParser.ExpressionStatementContext) ctx).expression();

            if (expressionContext instanceof FrayParser.InvocationExpressionContext || expressionContext instanceof FrayParser.AssignmentExpressionContext) {
                builder.println(String.format("%s;", visitExpression(expressionContext)));
            }
        }

        if (ctx instanceof FrayParser.ForStatementContext) {
            builder.println(String.format("var _it = new Iterator('', %s);", visitExpression(((FrayParser.ForStatementContext) ctx).in)));
            builder.println("while (_it.moveNext() === true) {");
            builder.indent();
            final FrayParser.ForStatementContext forStatementContext = (FrayParser.ForStatementContext) ctx;
            builder.println(String.format("var %s = _it.current;", forStatementContext.as.getText()));
            visitBlock(forStatementContext.block());
            builder.outdent();
            builder.println("}");
        }

        if (ctx instanceof FrayParser.IfStatementContext) {
            return visitIfStatement((FrayParser.IfStatementContext) ctx);
        }

        if (ctx instanceof FrayParser.ReturnStatementContext) {
            final FrayParser.ExpressionContext expr = ((FrayParser.ReturnStatementContext) ctx).expression();
            builder.println(String.format("return %s;", visitExpression(expr)));
        }

        if (ctx instanceof FrayParser.ThrowStatementContext) {
            final String exception = visitExpression(((FrayParser.ThrowStatementContext) ctx).expression());
            builder.println(String.format("throw %s;", exception));
        }

        if (ctx instanceof FrayParser.TryStatementContext) {
            final FrayParser.TryStatementContext tryStatementContext = (FrayParser.TryStatementContext) ctx;
            builder.println("try {");
            builder.indent();
            visitBlock(tryStatementContext.tryBlock);
            builder.outdent();
            builder.println("}");

            if (tryStatementContext.catchBlock() != null) {
                builder.print("catch(");
                builder.write(tryStatementContext.catchBlock().name.getText());
                builder.writeln(") {");
                builder.indent();
                visitBlock(tryStatementContext.catchBlock().block());
                builder.outdent();
                builder.println("}");
            }

            if (tryStatementContext.finallyBlock() != null) {
                builder.println("finally {");
                builder.indent();
                visitBlock(tryStatementContext.finallyBlock().block());
                builder.outdent();
                builder.println("}");
            }
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

        if (ctx instanceof FrayParser.WhileStatementContext) {
            builder.println(String.format("while ((%s) === true) {", visitExpression(((FrayParser.WhileStatementContext) ctx).expression())));
            builder.indent();
            ((FrayParser.WhileStatementContext) ctx).block().statement().forEach(this::visitStatement);
            builder.outdent();
            builder.println("}");
        }

        return null;
    }

    @Override
    public String visitTopLevelFunctionDefinition(FrayParser.TopLevelFunctionDefinitionContext ctx) {
        if (Fray.annotationsContainExternal(ctx.functionSignature().annotation())) {
            shimExternal(ctx.functionSignature().name.getText(), "func");
        } else {
            final String name = ctx.functionSignature().name.getText();
            builder.println();
            builder.print(String.format("function %s(", name));
            printParams(ctx.functionBody().parameters().IDENTIFIER());
            builder.writeln(") {");
            builder.indent();
            visitFunctionBody(ctx.functionBody());
            builder.outdent();
            builder.println("}");
        }

        return null;
    }

    @Override
    public Object visitTopLevelVariableDeclaration(FrayParser.TopLevelVariableDeclarationContext ctx) {
        final boolean hasExternal = Fray.annotationsContainExternal(ctx.annotation());

        for (FrayParser.VariableDeclarationContext decl : ctx.variableDeclaration()) {
            final String name = decl.name.getText();

            if (hasExternal) {
                shimExternal(name, "var");
            } else {
                if (decl.expression() != null) {
                    builder.println(String.format("var %s = %s;", name, visitExpression(decl.expression())));
                } else builder.println(String.format("var %s;", name));
            }
        }

        return null;
    }
}
