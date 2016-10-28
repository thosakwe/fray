package thosakwe.fray.lang.data;

import org.antlr.v4.runtime.tree.ParseTree;

public class FrayString extends FrayDatum {
    private final String value;

    public FrayString(ParseTree source, String value) {
        super(source);
        this.value = value;
    }

    @Override
    public String toString() {
        return value;
    }
}
