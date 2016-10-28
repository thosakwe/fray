package thosakwe.fray.lang.pipeline;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

public class FrayPipeline {
    public static final FrayPipeline DEFAULT = new FrayPipeline(new FrayTransformer[]{
            new AssignmentTransformer()
    });
    public static final String STDIN = "[stdin]";

    private boolean debug = false;
    private final List<FrayTransformer> transformers = new ArrayList<>();

    public FrayPipeline() {}

    public FrayPipeline(FrayTransformer[] transformers) {
        Collections.addAll(this.transformers, transformers);
    }

    public FrayPipeline(Collection<FrayTransformer> transformers) {
        this.transformers.addAll(transformers);
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

    public FrayAsset transform(FrayAsset asset) throws IOException {
        FrayAsset result = asset.setPipeline(this);

        for (FrayTransformer transformer : transformers) {
            if (transformer.claim(result))
                result = transformer.transform(asset).setPipeline(this);
        }

        return result;
    }
}
