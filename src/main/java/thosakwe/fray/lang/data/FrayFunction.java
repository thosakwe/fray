package thosakwe.fray.lang.data;

import org.antlr.v4.runtime.tree.ParseTree;
import thosakwe.fray.lang.FrayInterpreter;

public class FrayFunction extends FrayDatum {
    public FrayFunction(ParseTree source, FrayInterpreter interpreter) {
        super(source, interpreter);
    }

    @Override
    public String toString() {
        return "Function";
    }
}
