package thosakwe.fray.lang.pipeline;

import java.io.IOException;
import java.io.InputStream;

public interface FrayTransformer {
    boolean claim(FrayAsset asset);

    FrayAsset transform(FrayAsset asset) throws IOException;
}
