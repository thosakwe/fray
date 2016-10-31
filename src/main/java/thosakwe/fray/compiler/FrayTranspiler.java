package thosakwe.fray.compiler;

import thosakwe.fray.grammar.FrayBaseVisitor;
import thosakwe.fray.grammar.FrayParser;
import thosakwe.fray.pipeline.FrayAsset;
import thosakwe.fray.pipeline.FrayTransformer;

import java.io.IOException;

public abstract class FrayTranspiler extends FrayBaseVisitor {
    final CodeBuilder builder = new CodeBuilder();
    private final boolean debug;
    private final String name;
    private final String outputExtension;
    private final FrayAsset sourceAsset;

    public FrayTranspiler(String name, String outputExtension, FrayAsset sourceAsset, boolean debug) {
        this.debug = debug;
        this.name = name;
        this.outputExtension = outputExtension;
        this.sourceAsset = sourceAsset;
    }

    public abstract String compile(FrayParser.CompilationUnitContext ctx);

    public String getName() {
        return name;
    }

    public String getOutputExtension() {
        return outputExtension;
    }

    public FrayTransformer toTransformer() {
        final FrayTranspiler self = this;

        return new FrayTransformer() {
            @Override
            public boolean claim(FrayAsset asset) {
                return asset.getExtension().equals("fray");
            }

            @Override
            public String getName() {
                return String.format("Fray to %s Compiler", self.getName());
            }

            @Override
            public FrayAsset transform(FrayAsset asset) throws IOException {
                final String src = asset.readAsString();
                final FrayParser parser = parse(src);
                return asset.changeExtension(getOutputExtension()).changeText(compile(parser.compilationUnit()));
            }
        };
    }

    public FrayAsset getSourceAsset() {
        return sourceAsset;
    }

    void printDebug(String message) {
        if (debug) {
            System.out.println(message);
        }
    }
}
