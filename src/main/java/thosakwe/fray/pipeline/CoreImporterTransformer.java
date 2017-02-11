package thosakwe.fray.pipeline;

import java.io.IOException;

/**
 * Created on 2/7/2017.
 */
public class CoreImporterTransformer extends FrayTransformer {
    @Override
    public boolean claim(FrayAsset asset) {
        return asset.getExtension().equals("fray");
    }

    @Override
    public String getName() {
        return "<core> Importer";
    }

    @Override
    public FrayAsset transform(FrayAsset asset) throws IOException {
        final String src = asset.readAsString();
        return asset.changeText("import <core>;\n" + src);
    }
}
