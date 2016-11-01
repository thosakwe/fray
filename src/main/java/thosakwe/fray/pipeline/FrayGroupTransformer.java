package thosakwe.fray.pipeline;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Created on 11/1/2016.
 */
public abstract class FrayGroupTransformer extends FrayTransformer {
    private final List<FrayTransformer> transformers = new ArrayList<>();

    public FrayGroupTransformer(FrayTransformer[] transformers) {
        Collections.addAll(this.transformers, transformers);
    }

    public List<FrayTransformer> getTransformers() {
        return transformers;
    }

    @Override
    public FrayAsset transform(FrayAsset asset) throws IOException {
        FrayAsset result = asset;

        for (FrayTransformer transformer : transformers) {
            result = transformer.transform(asset);
        }

        return result;
    }
}
