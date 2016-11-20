package thosakwe.fray.analysis;

import org.antlr.v4.runtime.ParserRuleContext;
import thosakwe.fray.grammar.FrayParser;
import thosakwe.fray.lang.FrayDatum;

import java.util.ArrayList;
import java.util.List;

public class Symbol {
    private ParserRuleContext source = null;
    private boolean isFinal = false;
    private final String name;
    private final List<SymbolUsage> usages = new ArrayList<>();
    private FrayDatum value = null;

    public Symbol(String name) {
        this.name = name;
        this.isFinal = false;
    }

    public Symbol(String name, FrayDatum value, boolean isFinal) {
        this.isFinal = isFinal;
        this.name = name;
        this.value = value;
    }

    Symbol(String name, FrayDatum value) {
        this.name = name;
        this.value = value;
    }

    Symbol(String name, ParserRuleContext source) {
        this.name = name;
        this.source = source;
    }

    public String getName() {
        return name;
    }

    public ParserRuleContext getSource() {
        return source;
    }

    public List<SymbolUsage> getUsages() {
        return usages;
    }

    public FrayDatum getValue() {
        return value;
    }

    public void markAsFinal() {
        isFinal = true;
    }

    public boolean isFinal() {
        return isFinal;
    }

    public String safeDelete(String src) {
        // Todo: Safe delete
        return src;
    }

    public void setValue(FrayDatum value) {
        this.value = value;
    }
}
