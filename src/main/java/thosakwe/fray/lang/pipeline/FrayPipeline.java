package thosakwe.fray.lang.pipeline;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

public class FrayPipeline {
    public static final FrayPipeline DEFAULT = new FrayPipeline(new FrayTransformer[]{
            new RemoveExternalTransformer(),
            new StringInterpolatorTransformer(),
            new AssignmentTransformer()
    });

    public static final String STDIN = "[stdin]";

    private boolean debug = false;
    private final List<FrayTransformer> transformers = new ArrayList<>();

    public FrayPipeline() {
    }

    public FrayPipeline(FrayTransformer[] transformers) {
        Collections.addAll(this.transformers, transformers);
    }

    public FrayPipeline(Collection<FrayTransformer> transformers) {
        this.transformers.addAll(transformers);
    }

    public FrayPipeline chain(FrayPipeline other) {
        final FrayPipeline pipeline = new FrayPipeline();
        pipeline.transformers.addAll(transformers);
        pipeline.transformers.addAll(other.transformers);
        pipeline.setDebug(isDebug() || other.isDebug());
        return pipeline;
    }

    public boolean isDebug() {
        return debug;
    }

    public List<FrayTransformer> getTransformers() {
        return transformers;
    }

    public void printDebug(String message) {
        if (debug)
            System.out.println(message);
    }

    public void setDebug(boolean debug) {
        this.debug = debug;
    }

    public FrayAsset transform(FrayAsset input) throws IOException {
        FrayAsset output = input.setPipeline(this);

        if (debug) {
            final String src = output.readAsString();
            printDebug(String.format("Original source before running any transformers: \n'%s'", src));
            output = input.changeText(src).setPipeline(this);
        }

        for (FrayTransformer transformer : transformers) {
            if (transformer.claim(output)) {
                if (debug) {
                    printDebug(String.format("Now running %s transformer on asset '%s'...", transformer.getName(), output.getName()));
                    final String src = output.readAsString();
                    printDebug(String.format("Source before running %s transformer: \n'%s'", transformer.getName(), src));
                    output = input.changeText(src).setPipeline(this);
                }

                output = transformer.transform(output).setPipeline(this);

                if (debug) {
                    final String src = output.readAsString();
                    printDebug(String.format("Source after running %s transformer: \n'%s'", transformer.getName(), src));
                    output = input.changeText(src).setPipeline(this);
                }
            }
        }

        if (debug) {
            final String src = output.readAsString();
            printDebug(String.format("Final source after running all transformers: \n'%s'", src));
            output = input.changeText(src).setPipeline(this);
        }

        return output;
    }
}
