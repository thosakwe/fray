package thosakwe.fray.analysis;

import thosakwe.fray.grammar.FrayBaseVisitor;

/**
 * Created on 10/31/2016.
 */
public class UsageTracker extends FrayBaseVisitor {
    private final Scope symbolTable = new Scope();

    public Scope getSymbolTable() {
        return symbolTable;
    }
}
