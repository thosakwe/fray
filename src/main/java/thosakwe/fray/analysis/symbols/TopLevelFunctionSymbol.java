package thosakwe.fray.analysis.symbols;

import thosakwe.fray.analysis.AnalysisSymbol;
import thosakwe.fray.grammar.FrayParser;

public class TopLevelFunctionSymbol extends AnalysisSymbol {
    public TopLevelFunctionSymbol(String name, FrayParser.TopLevelFunctionDefinitionContext sourceElement, String source) {
        super(name, sourceElement, source, true);

    }
}
