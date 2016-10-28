package thosakwe.fray.lang.data;

import org.antlr.v4.runtime.tree.ParseTree;

public class FrayFunction extends FrayDatum {
    public FrayFunction(ParseTree source) {
        super(source);
    }

    @Override
    public String toString() {
        return "Function";
    }
}
