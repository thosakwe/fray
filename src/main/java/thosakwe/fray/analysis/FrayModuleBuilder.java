package thosakwe.fray.analysis;

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
            }
        }

        return module;
    }

    private void visitTopLevelVarDecl(FrayParser.TopLevelVariableDeclarationContext ctx, FrayModule module) {
        final boolean isFinal = ctx.FINAL() != null;

        for (FrayParser.VariableDeclarationContext decl: ctx.variableDeclaration()) {
            final String name = decl.name.getText();
        }
    }
}
