package thosakwe.fray.lang.data;

import org.antlr.v4.runtime.tree.ParseTree;
import thosakwe.fray.lang.FrayInterpreter;
import thosakwe.fray.lang.errors.FrayException;

public class FrayString extends FrayDatum {
    private final String value;

    public FrayString(ParseTree source, FrayInterpreter interpreter, String value) throws FrayException {
        super(source, interpreter);
        this.value = value;

        registerFinalMember("length", new FrayNumber(source, interpreter, value.length()));
    }

    @Override
    public String curses() {
        return String.format("\033[33m'%s'", toString());
    }

    @Override
    public FrayDatum plus(ParseTree source, FrayDatum right) throws FrayException {
        return new FrayString(source, getInterpreter(), value + right.toString());
    }

    @Override
    public String toString() {
        return value;
    }
}
