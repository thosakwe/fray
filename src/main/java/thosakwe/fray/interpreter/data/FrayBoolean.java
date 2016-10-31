package thosakwe.fray.interpreter.data;

import org.antlr.v4.runtime.tree.ParseTree;
import thosakwe.fray.interpreter.FrayInterpreter;
import thosakwe.fray.interpreter.errors.FrayException;

public class FrayBoolean extends FrayDatum {
    public static FrayBoolean TRUE = new FrayBoolean(null, null, true);
    public static FrayBoolean FALSE = new FrayBoolean(null, null, false);
    private final Boolean value;

    public FrayBoolean(ParseTree source, FrayInterpreter interpreter, Boolean value) {
        super(source, interpreter);
        this.value = value;
    }

    @Override
    public FrayBoolean and(ParseTree source, FrayDatum right) throws FrayException {
        if (right instanceof FrayBoolean) {
            return (value && ((FrayBoolean) right).value) ? FrayBoolean.TRUE : FrayBoolean.FALSE;
        } else throw new FrayException("Booleans must be compared to booleans only.", source, getInterpreter());
    }

    @Override
    public String curses() {
        return String.format("\033[35m%s", value.toString());
    }

    public Boolean getValue() {
        return value;
    }

    @Override
    public boolean isTrue() {
        return getValue();
    }

    @Override
    public FrayBoolean or(ParseTree source, FrayDatum right) throws FrayException {
        if (right instanceof FrayBoolean) {
            return (value || ((FrayBoolean) right).value) ? FrayBoolean.TRUE : FrayBoolean.FALSE;
        } else throw new FrayException("Booleans must be compared to booleans only.", source, getInterpreter());
    }

    @Override
    public String toString() {
        return value.toString();
    }
}
