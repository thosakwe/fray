package thosakwe.fray.analysis;

import org.antlr.v4.runtime.ParserRuleContext;

import java.util.ArrayList;
import java.util.List;

public class StructuredScope {
    private List<StructuredScope> children = new ArrayList<>();
    private StructuredScope current = this;
    private final StructuredScope parent;
    private final String source;
    private final List<AnalysisSymbol> symbols = new ArrayList<>();

    public StructuredScope(String source) {
        this.source = source;
        this.parent = null;
    }

    public StructuredScope(StructuredScope parent, String source) {
        this.parent = parent;
        this.source = source;
    }

    public StructuredScope down() {
        if (current.parent != null) {
            return current = current.parent;
        } else return current;
    }

    public StructuredScope fork() {
        final StructuredScope result = new StructuredScope(current, source);
        result.getSymbols().addAll(allUnique(true));
        return result.up();
    }

    public List<StructuredScope> getChildren() {
        return children;
    }

    public StructuredScope getCurrent() {
        return current;
    }

    public StructuredScope getParent() {
        return parent;
    }

    public List<AnalysisSymbol> getSymbols() {
        return symbols;
    }

    public AnalysisSymbol put(String name, ParserRuleContext sourceElement) {
        final AnalysisSymbol result = new AnalysisSymbol(name, sourceElement, source);
        current.symbols.add(result);
        return result;
    }

    public AnalysisSymbol putFinal(String name, ParserRuleContext sourceElement) {
        final AnalysisSymbol result = put(name, sourceElement);
        result.markAsFinal();
        return result;
    }

    public StructuredScope up() {
        final StructuredScope result = new StructuredScope(current, source);
        current.children.add(result);
        return current = result;
    }

    public List<AnalysisSymbol> allUnique(boolean importPrivate) {
        final List<AnalysisSymbol> result = new ArrayList<>();
        final List<String> names = new ArrayList<>();
        StructuredScope search = current;

        while (search != null) {
            for (AnalysisSymbol symbol : search.getSymbols()) {
                if (!names.contains(symbol.getName())) {
                    if (!symbol.getName().startsWith("_") || importPrivate) {
                        names.add(symbol.getName());
                        result.add(symbol);
                    }
                }
            }

            search = search.parent;
        }

        return result;
    }

    public List<AnalysisSymbol> allUnique() {
        return allUnique(false);
    }
}
