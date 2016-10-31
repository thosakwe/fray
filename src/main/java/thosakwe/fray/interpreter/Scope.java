package thosakwe.fray.interpreter;

import org.antlr.v4.runtime.tree.ParseTree;
import thosakwe.fray.interpreter.data.FrayDatum;
import thosakwe.fray.interpreter.data.FrayLibrary;
import thosakwe.fray.interpreter.errors.FrayException;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class Scope {
    private Scope child = null;
    private final List<Symbol> symbols = new ArrayList<>();
    private Scope parent = null;
    private FrayDatum thisContext = null;

    public Scope getInnerMostScope() {
        Scope innermost = this;

        while (innermost.child != null)
            innermost = innermost.child;

        return innermost;
    }

    public void create() {
        final Scope innermost = getInnerMostScope();
        final Scope child = new Scope();
        child.parent = innermost;
        innermost.child = child;
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
        final Scope innermost = getInnerMostScope();

        if (innermost.parent != null) {
            innermost.parent.child = null;
        }
    }

    public void dumpSymbols() {
        int level = 1;
        Scope currentScope = this;
        System.out.println("DUMPING SYMBOLS:");

        do {
            if (!currentScope.symbols.isEmpty())
                System.out.printf("Level %d (%d symbol(s)):%n", level++, currentScope.symbols.size());
            else System.out.printf("Level %d (empty)%n", level++);

            for (Symbol symbol : currentScope.symbols) {
                System.out.printf("  - %s: ", symbol.getName());
                System.out.println(symbol.getValue());
            }

            currentScope = currentScope.child;
        } while (currentScope != null);
    }

    public void load(Scope other, boolean importPrivate) {
        create();

        // Go top to bottom
        Scope currentScope = other;
        do {
            final Scope innermost = getInnerMostScope();

            innermost.symbols
                    .addAll(currentScope.symbols.stream()
                            .filter(symbol -> !symbol.getName().startsWith("_") || importPrivate)
                            .collect(Collectors.toList()));

            currentScope = currentScope.child;

            if (currentScope != null)
                create();
        } while (currentScope != null);
    }

    public void load(Scope other) {
        load(other, false);
    }

    public Symbol setFinal(String name, FrayDatum value, ParseTree source, FrayInterpreter interpreter) throws FrayException {
        return setValue(name, value, source, interpreter, true);
    }

    public Symbol setValue(String name, FrayDatum value, ParseTree source, FrayInterpreter interpreter, boolean isFinal) throws FrayException {
        final Symbol resolved = getSymbol(name);

        if (resolved == null) {
            final Symbol symbol = new Symbol(name, value, isFinal);
            getInnerMostScope().symbols.add(symbol);
            return symbol;
        } else if (!resolved.isFinal()) {
            resolved.setValue(value);
            return resolved;
        }
        else
            throw new FrayException(String.format("Cannot overwrite final variable '%s'.", resolved.getName()), source, interpreter);

    }

    public Symbol setValue(String name, FrayDatum value, ParseTree source, FrayInterpreter interpreter) throws FrayException {
        return setValue(name, value, source, interpreter, false);
    }

    public List<Symbol> getSymbols() {
        return symbols;
    }

    public Symbol resolveOrCreate(String name) {
        final Symbol resolved = getSymbol(name);

        if (resolved != null)
            return resolved;
        else {
            final Symbol symbol = new Symbol(name, null);
            getInnerMostScope().symbols.add(symbol);
            return symbol;
        }
    }

    public void createNew(String name, FrayDatum value, ParseTree source, FrayInterpreter interpreter, boolean isFinal) throws FrayException {
        final List<Symbol> symbols = getInnerMostScope().symbols;
        Symbol predefined = null;

        for (Symbol symbol : symbols) {
            if (symbol.getName().equals(name))
                predefined = symbol;
        }

        if (predefined != null)
            throw new FrayException(String.format("Symbol '%s' is already defined with this scope.", name), source, interpreter);
        else {
            symbols.add(new Symbol(name, value, isFinal));
        }
    }

    public void createNew(String name, FrayDatum value, ParseTree source, FrayInterpreter interpreter) throws FrayException {
        createNew(name, value, source, interpreter, false);
    }

    public FrayDatum getThisContext() {
        Scope currentScope = getInnerMostScope();

        while (currentScope != null) {
            if (currentScope.thisContext != null)
                return currentScope.thisContext;

            currentScope = currentScope.parent;
        }

        return null;
    }

    public void setThisContext(FrayDatum thisContext) {
        this.thisContext = thisContext;
    }

    public List<Symbol> allUnique(boolean importPrivate) {
        final List<Symbol> result = new ArrayList<>();
        final List<String> added = new ArrayList<>();
        Scope currentScope = getInnerMostScope();

        while (currentScope != null) {
            for (Symbol symbol : currentScope.symbols) {
                if (!added.contains(symbol.getName())) {
                    if (!symbol.getName().startsWith("_") || importPrivate) {
                        added.add(symbol.getName());
                        result.add(symbol);
                    }
                }
            }

            currentScope = currentScope.parent;
        }

        return result;
    }

    public List<Symbol> allUnique() {
        return allUnique(false);
    }

    public void load(FrayLibrary from, boolean importPrivate) {
        getInnerMostScope().symbols.addAll((importPrivate ? from.getExportedSymbols() : from.getPublicSymbols()));
    }

    public void load(FrayLibrary from) {
        load(from, false);
    }
}
