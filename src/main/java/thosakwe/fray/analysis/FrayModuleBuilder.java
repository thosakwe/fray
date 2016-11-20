package thosakwe.fray.analysis;

import thosakwe.fray.analysis.mirrors.FunctionMirror;
import thosakwe.fray.analysis.mirrors.StatementMirror;
import thosakwe.fray.analysis.symbols.TopLevelFunctionSymbol;
import thosakwe.fray.grammar.FrayParser;

import java.util.stream.Collectors;

/**
 * Created on 11/18/2016.
 */
public class FrayModuleBuilder {
    private final FrayModule module;

    public FrayModuleBuilder(String name, String source, FrayParser.CompilationUnitContext ast) {
        this.module = new FrayModule(name, source);

        for (FrayParser.TopLevelDefinitionContext def : ast.topLevelDefinition()) {
            if (def.topLevelVariableDeclaration() != null) {
                visitTopLevelVarDecl(def.topLevelVariableDeclaration());
            } else if (def.topLevelFunctionDefinition() != null) {
                visitTopLevelFuncDecl(def.topLevelFunctionDefinition());
            }
        }
    }

    private FunctionMirror visitFunctionBody(FrayParser.FunctionBodyContext ctx, StructuredScope scope) {
        final FunctionMirror result = new FunctionMirror(ctx, module.getSource(), scope.fork());
        final FrayParser.BlockBodyContext body = ctx.blockBody();
        result.statements.addAll(body.block().statement()
                .stream()
                .map(stmt -> visitStatement(stmt, scope)).collect(Collectors.toList()));

        return result;
    }

    private StatementMirror visitStatement(FrayParser.StatementContext ctx, StructuredScope scope) {
        return null;
    }

    private void visitTopLevelFuncDecl(FrayParser.TopLevelFunctionDefinitionContext ctx) {
        final String name = ctx.functionSignature().name.getText();
        final TopLevelFunctionSymbol symbol = new TopLevelFunctionSymbol(name, ctx, module.getSource(), module.getScope());
        module.getExportedSymbols().put(name, symbol);
    }

    private void visitTopLevelVarDecl(FrayParser.TopLevelVariableDeclarationContext ctx) {
        final boolean isFinal = ctx.FINAL() != null;

        for (FrayParser.VariableDeclarationContext decl : ctx.variableDeclaration()) {
            final String name = decl.name.getText();
            final AnalysisSymbol symbol = module.getScope().put(name, decl.expression());
            if (isFinal) symbol.markAsFinal();
            module.getExportedSymbols().put(name, symbol);
        }
    }
}
