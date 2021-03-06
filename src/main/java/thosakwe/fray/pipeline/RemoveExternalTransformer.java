package thosakwe.fray.pipeline;

import thosakwe.fray.Fray;
import thosakwe.fray.grammar.FrayBaseVisitor;
import thosakwe.fray.grammar.FrayParser;
import thosakwe.fray.grammar.FrayVisitor;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Removes top-level definitions marked as @external.
 */
public class RemoveExternalTransformer extends FrayTransformer {
    @Override
    public boolean claim(FrayAsset asset) {
        return asset.getExtension().equals("fray");
    }

    @Override
    public String getName() {
        return "Remove External Definitions";
    }

    @Override
    public FrayAsset transform(FrayAsset asset) throws IOException {
        final String src = asset.readAsString();
        final FrayParser parser = parse(src);
        final List<String> remove = new ArrayList<>();

        final FrayVisitor visitor = new FrayBaseVisitor() {
            @Override
            public Object visitCompilationUnit(FrayParser.CompilationUnitContext ctx) {

                for (FrayParser.TopLevelDefinitionContext def : ctx.topLevelDefinition()) {
                    if (def.classDefinition() != null || def.topLevelFunctionDefinition() != null || def.topLevelVariableDeclaration() != null)
                        visitTopLevelDefinition(def);
                }

                return null;
            }

            @Override
            public Object visitClassDefinition(FrayParser.ClassDefinitionContext ctx) {
                if (Fray.annotationsContainExternal(ctx.annotation())) {
                    remove.add(getNodeText(src, ctx));
                    return null;
                }

                return super.visitClassDefinition(ctx);
            }

            @Override
            public Object visitTopLevelFunctionDefinition(FrayParser.TopLevelFunctionDefinitionContext ctx) {
                if (Fray.annotationsContainExternal(ctx.functionSignature().annotation())) {
                    remove.add(getNodeText(src, ctx));
                    return null;
                }

                return super.visitTopLevelFunctionDefinition(ctx);
            }

            @Override
            public Object visitTopLevelVariableDeclaration(FrayParser.TopLevelVariableDeclarationContext ctx) {
                if (Fray.annotationsContainExternal(ctx.annotation())) {
                    remove.add(getNodeText(src, ctx));
                    return null;
                }

                return super.visitTopLevelVariableDeclaration(ctx);
            }
        };

        visitor.visitCompilationUnit(parser.compilationUnit());

        String transformed = src;

        for (String key : remove) {
            asset.getPipeline().printDebug(String.format("Remove External transformer removing '%s'...", key));
            transformed = transformed.replaceAll(Pattern.quote(key), "");
        }

        return asset.changeText(transformed);
    }
}
