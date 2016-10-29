package thosakwe.fray.lang;

import thosakwe.fray.lang.data.FrayDatum;

public class Symbol {
    private boolean _isFinal = false;
    private final String name;
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

    public FrayDatum getValue() {
        return value;
    }

    public void markAsFinal() {
        _isFinal = true;
    }

    public boolean isFinal() {
        return _isFinal;
    }

    public void setValue(FrayDatum value) {
        this.value = value;
    }
}
