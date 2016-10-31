package thosakwe.fray.analysis;

import org.antlr.v4.runtime.ParserRuleContext;
import thosakwe.fray.lang.FrayDatum;

import java.util.ArrayList;
import java.util.List;

public class Symbol {
    private boolean _isFinal = false;
    private final String name;
    private final List<SymbolUsage> usages = new ArrayList<>();
    private FrayDatum value;

    public Symbol(String name, FrayDatum value, boolean isFinal) {
        this._isFinal = isFinal;
        this.name = name;
        this.value = value;
    }

    Symbol(String name, FrayDatum value) {
        this.name = name;
        this.value = value;
    }

    public String getName() {
        return name;
    }

    public List<SymbolUsage> getUsages() {
        return usages;
    }

    public FrayDatum getValue() {
        return value;
    }

    public void markAsFinal() {
        _isFinal = true;
    }

    public boolean isFinal() {
        return _isFinal;
    }

    public String safeDelete(String src) {
        // Todo: Safe delete
        return src;
    }

    public void setValue(FrayDatum value) {
        this.value = value;
    }
}
