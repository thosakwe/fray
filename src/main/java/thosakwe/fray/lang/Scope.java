package thosakwe.fray.lang;

import thosakwe.fray.lang.data.FrayDatum;

import java.util.ArrayList;
import java.util.List;

public class Scope {
    private Scope child = null;
    private Scope parent = null;
    private final List<Symbol> symbols = new ArrayList<>();

    private Scope getInnerMostScope() {
        Scope innermost = this;

        while (innermost.child != null)
            innermost = innermost.child;

        return innermost;
    }

    public void create() {
        final Scope scope = new Scope();
        scope.parent = this;
        child = scope;
    }

    public Symbol getSymbol(String name) {
        Scope currentScope = getInnerMostScope();

        // Now, backtrack to top
        do {
            for (Symbol symbol : currentScope.symbols) {
                if (symbol.getName().equals(name))
                    return symbol;
            }

            currentScope = currentScope.parent;
        } while (currentScope != null);

        return null;
    }

    public FrayDatum getValue(String name) {
        final Symbol resolved = getSymbol(name);

        if (resolved == null)
            return null;
        else return resolved.getValue();
    }

    public void destroy() {
        if (child != null)
            child = null;
    }

    public void dumpSymbols() {
        int level = 1;
        Scope currentScope = this;

        do {
            if (!currentScope.symbols.isEmpty())
                System.out.printf("Level %d (%d symbol(s)):%n", level++, currentScope.symbols.size());

            for (Symbol symbol : currentScope.symbols) {
                System.out.printf("  - %s: ", symbol.getName());
                System.out.println(symbol.getValue());
            }

            currentScope = currentScope.child;
        } while (currentScope != null);

        System.out.println("Done dumping.");
    }

    public void load(Scope other) {
        create();

        // Go top to bottom
        Scope currentScope = other;
        do {
            getInnerMostScope().symbols.addAll(currentScope.symbols);
            currentScope = currentScope.child;

            if (currentScope != null)
                create();
        } while (currentScope != null);
    }

    public void setValue(String name, FrayDatum value, boolean isFinal) {
        getInnerMostScope().symbols.add(new Symbol(name, value, isFinal));
    }

    public void setValue(String name, FrayDatum value) {
        setValue(name, value, false);
    }
}
