package pipeline;

import org.junit.Test;
import thosakwe.fray.pipeline.FrayAsset;

import static org.junit.Assert.assertEquals;

public class AssetTests {
    @Test
    public void testChangeExtension() {
        final FrayAsset frayAsset = new FrayAsset("fray", "change-extension", null, null);
        final FrayAsset dartAsset = frayAsset.changeExtension("dart");

        assertEquals(dartAsset.getExtension(), "dart");
        assertEquals(frayAsset.getExtension(), "fray");
        assertEquals(dartAsset.getName(), frayAsset.getName());
    }
}
