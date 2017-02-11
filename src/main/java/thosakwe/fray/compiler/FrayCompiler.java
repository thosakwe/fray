package thosakwe.fray.compiler;

import thosakwe.fray.grammar.FrayBaseVisitor;
import thosakwe.fray.grammar.FrayParser;
import thosakwe.fray.pipeline.FrayAsset;
import thosakwe.fray.pipeline.FrayTransformer;

public abstract class FrayCompiler extends FrayBaseVisitor {
    protected final CodeBuilder builder = new CodeBuilder();
    private final boolean debug;
    private final String name;
    private final String outputExtension;
    private final FrayAsset sourceAsset;

    public FrayCompiler(String name, String outputExtension, FrayAsset sourceAsset, boolean debug) {
        this.debug = debug;
        this.name = name;
        this.outputExtension = outputExtension;
        this.sourceAsset = sourceAsset;
    }

    public abstract Object compile(FrayParser.CompilationUnitContext ctx);

    public String getName() {
        return name;
    }

    public String getOutputExtension() {
        return outputExtension;
    }

    public abstract FrayTransformer toTransformer();

    public FrayAsset getSourceAsset() {
        return sourceAsset;
    }

    protected void printDebug(String message) {
        if (debug) {
            System.out.println(message);
        }
    }
}
