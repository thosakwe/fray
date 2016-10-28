package thosakwe.fray.lang.data;

import org.antlr.v4.runtime.tree.ParseTree;
import thosakwe.fray.lang.FrayInterpreter;
import thosakwe.fray.lang.errors.FrayException;

import java.util.List;

public class FrayNull extends FrayDatum {
    public FrayNull() {
        super(null, null);
    }

    @Override
    public String curses() {
        return "\033[34m null";
    }

    @Override
    public FrayBoolean equalsOther(ParseTree source, FrayDatum other) {
        return (other instanceof FrayNull && other.isNull()) ? FrayBoolean.TRUE : FrayBoolean.FALSE;
    }

    @Override
    public boolean isNull() {
        return true;
    }

    @Override
    public String toString() {
        return "null";
    }
}
