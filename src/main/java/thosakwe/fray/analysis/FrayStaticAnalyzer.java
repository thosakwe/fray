package thosakwe.fray.analysis;

import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.tree.ParseTree;
import thosakwe.fray.analysis.symbols.FrayAccessorKind;
import thosakwe.fray.analysis.symbols.FrayAccessorMember;
import thosakwe.fray.grammar.FrayParser;
import thosakwe.fray.interpreter.FrayInterpreter;
import thosakwe.fray.interpreter.errors.FrayException;
import thosakwe.fray.lang.*;
import thosakwe.fray.pipeline.FrayAsset;

import java.io.PrintStream;
import java.util.List;

/**
 * Created on 10/31/2016.
 */
public class FrayStaticAnalyzer {
    private final FrayAsset sourceAsset;
    private final Scope symbolTable = new Scope();
    private final boolean debug;

    public FrayStaticAnalyzer(FrayAsset sourceAsset, boolean debug) {
        this.sourceAsset = sourceAsset;
        this.debug = debug;
    }

    public FrayStaticAnalyzer(boolean debug) {
        this(null, debug);
    }


    private void printDebug(String msg) {
        if (debug) {
            System.out.println(msg);
        }
    }

    public FrayLibrary analyzeCompilationUnit(FrayParser.CompilationUnitContext ctx) {
        FrayLibrary library = new FrayLibrary(ctx, null, sourceAsset);

        for (FrayParser.TopLevelDefinitionContext topLevelDefinitionContext : ctx.topLevelDefinition()) {
            if (topLevelDefinitionContext.topLevelFunctionDefinition() != null) {
                FrayFunction function = analyzeTopLevelFunction(topLevelDefinitionContext.topLevelFunctionDefinition());
                Symbol symbol = symbolTable.putFinal(function.getName(), function);
                library.getExportedSymbols().add(symbol);
            }

            if (topLevelDefinitionContext.classDefinition() != null) {
                FrayType clazz = analyzeClassDefinition(topLevelDefinitionContext.classDefinition());
                Symbol symbol = symbolTable.putFinal(clazz.getName(), clazz);
                library.getExportedSymbols().add(symbol);
            }
        }

        return library;
    }

    public FrayFunction analyzeTopLevelFunction(FrayParser.TopLevelFunctionDefinitionContext ctx) {
        final String name = ctx.functionSignature().name.getText();
        FrayFunction function = analyzeFunctionBody(ctx.functionBody());
        function.setName(name);
        return function;
    }

    public FrayType analyzeClassDefinition(FrayParser.ClassDefinitionContext ctx) {
        final String name = ctx.name.getText();
        FrayType parentType = null;

        if (ctx.superClass != null) {
            FrayParser.ExpressionContext superClass = ctx.superClass;

            if (superClass instanceof FrayParser.IdentifierExpressionContext) {
                // TODO: Type-check this whole mess :)
                parentType = (FrayType) symbolTable.getValue(superClass.getText());
            } else if (superClass instanceof FrayParser.MemberExpressionContext) {
                FrayParser.ExpressionContext target = ((FrayParser.MemberExpressionContext) superClass).expression();
                String member = ((FrayParser.MemberExpressionContext) superClass).IDENTIFIER().getText();

                if (target instanceof FrayParser.IdentifierExpressionContext) {
                    FrayLibrary source = (FrayLibrary) symbolTable.getValue(target.getText());
                    parentType = (FrayType) source.getExportedSymbols().stream().filter((Symbol symbol) -> symbol.getName().equals(member)).findFirst().get().getValue();
                }
            }
        }

        FrayType clazz = new FrayType(ctx, null, parentType) {
            @Override
            public String getName() {
                return name;
            }
        };

        // All members

        // Variables
        for (FrayParser.TopLevelVariableDeclarationContext declStmt : ctx.topLevelVariableDeclaration()) {
            boolean isFinal = declStmt.FINAL() != null;

            for (FrayParser.VariableDeclarationContext decl : declStmt.variableDeclaration()) {
                // Create a getter :)
                // Well, at least, one with a resolved return type
                String fieldName = decl.name.getText();
                FrayAccessorMember getter = new FrayAccessorMember(FrayAccessorKind.GETTER, fieldName, decl);

                FrayDatum value;
                if (decl.expression() != null)
                    value = analyzeExpression(decl.expression());
                else value = new FrayNull();

                FrayFunction getterFunction = new FrayFunction(decl, null);
                getterFunction.setReturnType(value == null ? FrayType.OBJECT : value.getType());
                getter.setFunction(getterFunction);
                clazz.getMembers().add(getter);

                if (!isFinal) {
                    // Setter time, baby
                    FrayFunction setterFunction = new FrayFunction(decl, null);
                    setterFunction.getParameters().add(new FrayFunctionParameter(fieldName));
                    // parameter.setExpectedType(...);
                    getterFunction.setReturnType(FrayType.VOID);
                    clazz.getMembers().add(new FrayAccessorMember(FrayAccessorKind.SETTER, fieldName, setterFunction, decl));
                }
            }
        }

        // Methods
        for (FrayParser.TopLevelFunctionDefinitionContext decl: ctx.topLevelFunctionDefinition()) {
            FrayFunction function = analyzeTopLevelFunction(decl);

            FrayFunction getterFunction = new FrayFunction(decl, null) {
                @Override
                public FrayDatum call(FrayInterpreter interpreter, ParseTree source, List<FrayDatum> args) throws FrayException {
                    return function;
                }
            };

            getterFunction.setReturnType(FrayType.FUNCTION);
            clazz.getMembers().add(new FrayAccessorMember(FrayAccessorKind.GETTER, function.getName(), getterFunction, decl));
        }

        // Constructors
        for (FrayParser.ConstructorDefinitionContext def: ctx.constructorDefinition()) {
            final String constructorName = def.name != null ? def.name.getText() : "";
            FrayFunction constructor = analyzeFunctionBody(def.functionBody());
            constructor.setName(constructorName);
            // `void` return type
            constructor.setReturnType(FrayType.VOID);
            // Inject `this`
            FrayDatum thisCtx = new FrayDatum(null, null) {
                @Override
                public FrayType getType() {
                    return clazz;
                }
            };

            constructor.setThisContext(thisCtx);
            clazz.getConstructors().put(constructorName, constructor);
        }

        return clazz;
    }

    public FrayDatum analyzeExpression(FrayParser.ExpressionContext ctx) {
        if (ctx instanceof FrayParser.FunctionExpressionContext) {
            return analyzeFunctionBody(((FrayParser.FunctionExpressionContext) ctx).functionBody());
        }

        return null;
    }

    public FrayFunction analyzeFunctionBody(FrayParser.FunctionBodyContext ctx) {
        FrayFunction function = new FrayFunction(ctx, null);
        function.setFunctionBody(ctx);

        for (Token paramName : ctx.parameters().names) {
            function.getParameters().add(new FrayFunctionParameter(paramName.getText()));
        }

        return function;
    }

    public Scope getSymbolTable() {
        return symbolTable;
    }

    public void codeCompletion(PrintStream out, int line, int col) {
        /* for (AnalysisSymbol symbol : symbolTable.allUnique(true)) {
            final ParseTree sourceTree = symbol.getValue().getSource();

            if (sourceTree instanceof ParserRuleContext) {
                final ParserRuleContext source = (ParserRuleContext) sourceTree;

                if ((source.stop.getLine() < line || line == -1) || ((source.stop.getLine() == line && source.stop.getCharPositionInLine() <= col) || col == -1)) {
                    System.out.printf("Found symbol %s(of Type %s). Declaration: '%s'%n", symbol.getName(), symbol.getValue().getType().getName(), source.getText());
                    out.printf("%s:%s%n", symbol.getName(), symbol.getValue().getType().getName());
                }
            }
        } */
    }
}
