package thosakwe.fray.lang;

import org.antlr.v4.runtime.tree.ParseTree;
import thosakwe.fray.interpreter.FrayInterpreter;
import thosakwe.fray.analysis.Symbol;
import thosakwe.fray.pipeline.FrayAsset;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class FrayLibrary extends FrayDatum {
    private FrayDatum defaultExport = null;
    private final List<Symbol> exportedSymbols = new ArrayList<>();
    private final FrayAsset sourceAsset;

    public FrayLibrary(ParseTree source, FrayInterpreter interpreter, FrayAsset sourceAsset) {
        super(source, interpreter);
        this.sourceAsset = sourceAsset;
    }

    public FrayDatum getDefaultExport() {
        return defaultExport;
    }

    public List<Symbol> getExportedSymbols() {
        return exportedSymbols;
    }

    public List<Symbol> getPublicSymbols() {
        return exportedSymbols.stream().filter(symbol -> !symbol.isFinal()).collect(Collectors.toList());
    }

    public FrayAsset getSourceAsset() {
        return sourceAsset;
    }

    public void setDefaultExport(FrayDatum defaultExport) {
        this.defaultExport = defaultExport;
    }
}
