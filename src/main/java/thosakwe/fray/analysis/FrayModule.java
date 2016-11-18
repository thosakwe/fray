package thosakwe.fray.analysis;

import java.util.ArrayList;
import java.util.List;

/**
 * Created on 11/18/2016.
 */
public class FrayModule {
    private final String name;
    private final String source;
    private final List<AnalysisSymbol> exportedSymbols = new ArrayList<>();

    public FrayModule(String name, String source) {
        this.name = name;
        this.source = source;
    }

    public String getName() {
        return name;
    }

    public String getSource() {
        return source;
    }

    public List<AnalysisSymbol> getExportedSymbols() {
        return exportedSymbols;
    }
}
