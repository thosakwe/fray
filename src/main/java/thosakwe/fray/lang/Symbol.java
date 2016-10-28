package thosakwe.fray.lang;

import thosakwe.fray.lang.data.FrayDatum;

class Symbol {
    private boolean _isFinal = false;
    private final String name;
    private FrayDatum value;

    Symbol(String name, FrayDatum value, boolean isFinal) {
        this._isFinal = isFinal;
        this.name = name;
        this.value = value;
    }

    Symbol(String name, FrayDatum value) {
        this.name = name;
        this.value = value;
    }

    String getName() {
        return name;
    }

    FrayDatum getValue() {
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