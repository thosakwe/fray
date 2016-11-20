package thosakwe.fray.analysis;

import java.util.HashMap;
import java.util.Map;

/**
 * Created on 11/18/2016.
 */
public class FrayModule {
    private final String name;
    private final StructuredScope scope;
    private final String source;
    private final Map<String, AnalysisSymbol> exportedSymbols = new HashMap<>();

    public FrayModule(String name, String source) {
        this.name = name;
        this.scope = new StructuredScope(this.source = source);
    }

    public String getName() {
        return name;
    }

    public StructuredScope getScope() {
        return scope;
    }

    public String getSource() {
        return source;
    }

    public Map<String, AnalysisSymbol> getExportedSymbols() {
        return exportedSymbols;
    }
}
