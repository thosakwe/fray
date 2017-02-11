package thosakwe.fray.compiler.jvm;

import thosakwe.fray.compiler.FrayCompiler;
import thosakwe.fray.grammar.FrayParser;
import thosakwe.fray.pipeline.FrayAsset;
import thosakwe.fray.pipeline.FrayTransformer;

import java.io.IOException;

public class FrayToJvmCompiler extends FrayCompiler {
    public FrayToJvmCompiler(FrayAsset sourceAsset, boolean debug) {
        super("jvm", "class", sourceAsset, debug);
    }

    @Override
    public Object compile(FrayParser.CompilationUnitContext ctx) {
        return null;
    }

    @Override
    public FrayTransformer toTransformer() {
        return new FrayTransformer() {
            @Override
            public boolean claim(FrayAsset asset) {
                return asset.getExtension().equals("fray");
            }

            @Override
            public String getName() {
                return "Fray to JVM Compiler";
            }

            @Override
            public FrayAsset transform(FrayAsset asset) throws IOException {
                /* final String src = asset.readAsString();
                final FrayParser parser = parse(src);
                return null;
                return asset.changeText(compile(parser.compilationUnit())).changeExtension(getOutputExtension());
                */
                return asset.changeText("TODO: JVM compilation!!!").changeExtension(getOutputExtension());
            }
        };
    }
}
