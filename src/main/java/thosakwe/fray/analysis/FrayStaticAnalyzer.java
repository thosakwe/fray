package thosakwe.fray.analysis;

import org.antlr.v4.runtime.Token;
import sun.jvm.hotspot.debugger.cdbg.Sym;
import thosakwe.fray.grammar.FrayParser;
import thosakwe.fray.lang.*;
import thosakwe.fray.pipeline.FrayAsset;

import java.io.PrintStream;

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
        FrayFunction function = new FrayFunction(ctx, null);
        function.setName(name);
        function.setFunctionBody(ctx.functionBody());

        for (Token paramName : ctx.functionBody().parameters().names) {
            function.getParameters().add(new FrayFunctionParameter(paramName.getText()));
        }

        return function;
    }

    public FrayType analyzeClassDefinition(FrayParser.ClassDefinitionContext ctx) {
        final String name = ctx.name.getText();

        FrayType clazz = new FrayType(ctx, null, null) {
            @Override
            public String getName() {
                return name;
            }
        };

        if (ctx.superClass != null) {
            FrayType parentType = null;
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

            if (parentType == null) {
                // TODO: Throw an error
            } else clazz.setParentType(parentType);
        }

        return clazz;
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
