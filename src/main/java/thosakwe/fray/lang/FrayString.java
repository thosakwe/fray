package thosakwe.fray.lang;

import org.antlr.v4.runtime.tree.ParseTree;
import thosakwe.fray.interpreter.FrayInterpreter;
import thosakwe.fray.interpreter.errors.FrayException;

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

    public static FrayDatum parse(String str) {
        try {
            return new FrayString(null, null, String.valueOf(str));
        } catch (FrayException exc) {
            return null;
        }
    }

    @Override
    public String toString() {
        return value;
    }
}
