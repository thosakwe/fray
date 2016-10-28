package thosakwe.fray.lang.data;

import org.antlr.v4.runtime.tree.ParseTree;
import thosakwe.fray.lang.FrayInterpreter;
import thosakwe.fray.lang.errors.FrayException;

import java.util.List;

public class FrayNull extends FrayDatum {
    public FrayNull() {
        super(null);
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
