package thosakwe.fray.analysis;

import thosakwe.fray.grammar.FrayBaseVisitor;
import thosakwe.fray.grammar.FrayParser;
import thosakwe.fray.lang.FrayFunction;

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
}
