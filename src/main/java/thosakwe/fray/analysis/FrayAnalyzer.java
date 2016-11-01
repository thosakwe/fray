package thosakwe.fray.analysis;

import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.tree.ParseTree;
import thosakwe.fray.grammar.FrayBaseVisitor;
import thosakwe.fray.grammar.FrayParser;
import thosakwe.fray.lang.FrayFunction;

import java.io.PrintStream;

/**
 * Created on 10/31/2016.
 */
public class FrayAnalyzer  {
    private final StructuredScope symbolTable = new StructuredScope();
    private final boolean debug;

    public FrayAnalyzer(boolean debug) {
        this.debug = debug;
    }

    private void printDebug(String msg) {
        if (debug) {
            System.out.println(msg);
        }
    }

    public void analyzeProgram(FrayParser.CompilationUnitContext ctx) {
        for (FrayParser.TopLevelDefinitionContext topLevelDefinitionContext : ctx.topLevelDefinition()) {
            if (topLevelDefinitionContext.topLevelFunctionDefinition() != null) {
                final FrayParser.TopLevelFunctionDefinitionContext def = topLevelDefinitionContext.topLevelFunctionDefinition();
                final String name = def.functionSignature().name.getText();
                symbolTable.put(name, new FrayFunction(def, null));
                analyzeTopLevelFunction(def);
            }
        }
    }

    private void analyzeTopLevelFunction(FrayParser.TopLevelFunctionDefinitionContext ctx) {

    }

    public StructuredScope getSymbolTable() {
        return symbolTable;
    }

    public void codeCompletion(PrintStream out, int line, int col) {
        for (Symbol symbol : symbolTable.allUnique(true)) {
            final ParseTree sourceTree = symbol.getValue().getSource();

            if (sourceTree instanceof ParserRuleContext) {
                final ParserRuleContext source = (ParserRuleContext) sourceTree;

                if ((source.stop.getLine() < line || line == -1) || ((source.stop.getLine() == line && source.stop.getCharPositionInLine() <= col) || col == -1)) {
                    System.out.printf("Found symbol %s(of Type %s). Declaration: '%s'%n", symbol.getName(), symbol.getValue().getType().getName(), source.getText());
                    out.printf("%s:%s%n", symbol.getName(), symbol.getValue().getType().getName());
                }
            }
        }
    }
}
