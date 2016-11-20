package thosakwe.fray.analysis;

import thosakwe.fray.analysis.symbols.TopLevelFunctionSymbol;
import thosakwe.fray.grammar.FrayParser;

/**
 * Created on 11/18/2016.
 */
public class FrayModuleBuilder {
    public FrayModule buildModule(String name, String source, FrayParser.CompilationUnitContext ast) {
        final FrayModule module = new FrayModule(name, source);

        for (FrayParser.TopLevelDefinitionContext def : ast.topLevelDefinition()) {
            if (def.topLevelVariableDeclaration() != null) {
                visitTopLevelVarDecl(def.topLevelVariableDeclaration(), module);
            } else if (def.topLevelFunctionDefinition() != null) {
                visitTopLevelFuncDecl(def.topLevelFunctionDefinition(), module);
            }
        }

        return module;
    }

    private void visitTopLevelFuncDecl(FrayParser.TopLevelFunctionDefinitionContext ctx, FrayModule module) {
        final String name = ctx.functionSignature().name.getText();
        module.getExportedSymbols().put(name, new TopLevelFunctionSymbol(name, ctx, module.getSource()));
    }

    private void visitTopLevelVarDecl(FrayParser.TopLevelVariableDeclarationContext ctx, FrayModule module) {
        final boolean isFinal = ctx.FINAL() != null;

        for (FrayParser.VariableDeclarationContext decl: ctx.variableDeclaration()) {
            final String name = decl.name.getText();
            final Symbol symbol = module.getScope().put(name, decl.expression());
            if (isFinal) symbol.markAsFinal();
            module.getExportedSymbols().add(symbol);
        }
    }
}
