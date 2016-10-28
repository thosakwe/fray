package thosakwe.fray.lang.data;

import org.antlr.v4.runtime.tree.ParseTree;
import thosakwe.fray.lang.FrayInterpreter;

public class FrayString extends FrayDatum {
    private final String value;

    public FrayString(ParseTree source, FrayInterpreter interpreter, String value) {
        super(source, interpreter);
        this.value = value;
    }

    @Override
    public String curses() {
        return String.format("\033[33m'%s'", toString());
    }

    @Override
    public String toString() {
        return value;
    }
}
