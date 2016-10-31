package thosakwe.fray.interpreter.data;

import org.antlr.v4.runtime.tree.ParseTree;
import thosakwe.fray.interpreter.FrayInterpreter;

public class FrayFunction extends FrayDatum {
    public FrayFunction(ParseTree source, FrayInterpreter interpreter) {
        super(source, interpreter);
    }

    @Override
    public String toString() {
        return "Function";
    }
}
