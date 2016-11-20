package thosakwe.fray.analysis;

import org.antlr.v4.runtime.ParserRuleContext;
import thosakwe.fray.lang.FrayDatum;

import java.util.ArrayList;
import java.util.List;

public class StructuredScope {
    private List<StructuredScope> children = new ArrayList<>();
    private StructuredScope current = this;
    private final StructuredScope parent;
    private final List<Symbol> symbols = new ArrayList<>();

    public StructuredScope() {
        this.parent = null;
    }

    private StructuredScope(StructuredScope parent) {
        this.parent = parent;
    }

    public StructuredScope down() {
        if (current.parent != null) {
            return current = current.parent;
        } else return current;
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

    public List<Symbol> getSymbols() {
        return symbols;
    }

    public Symbol put(String name, FrayDatum value) {
        final Symbol result = new Symbol(name, value);
        current.symbols.add(result);
        return result;
    }

    public Symbol putFinal(String name, FrayDatum value) {
        final Symbol result = put(name, value);
        result.markAsFinal();
        return result;
    }

    public Symbol put(String name, ParserRuleContext source) {
        final Symbol result = new Symbol(name, source);
        current.symbols.add(result);
        return result;
    }

    public Symbol putFinal(String name, ParserRuleContext source) {
        final Symbol result = put(name, source);
        result.markAsFinal();
        return result;
    }

    public StructuredScope up() {
        final StructuredScope result = new StructuredScope(current);
        current.children.add(result);
        return current = result;
    }

    public List<Symbol> allUnique(boolean importPrivate) {
        final List<Symbol> result = new ArrayList<>();
        final List<String> names = new ArrayList<>();
        StructuredScope search = current;

        while (search != null) {
            for (Symbol symbol : search.getSymbols()) {
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

    public List<Symbol> allUnique() {
        return allUnique(false);
    }
}
