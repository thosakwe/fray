package thosakwe.fray.lang.data;

import org.antlr.v4.runtime.tree.ParseTree;
import thosakwe.fray.lang.FrayInterpreter;
import thosakwe.fray.lang.errors.FrayException;

import java.util.Objects;

public class FrayNumber extends FrayDatum {
    private final Double value;

    public FrayNumber(ParseTree source, FrayInterpreter interpreter, Double value) {
        super(source, interpreter);
        this.value = value;
    }

    public FrayNumber(ParseTree source, FrayInterpreter interpreter, Integer value) {
        super(source, interpreter);
        this.value = value * 1.0;
    }

    @Override
    public String curses() {
        return String.format("\033[36m%s", toString());
    }

    @Override
    public FrayBoolean equalsOther(ParseTree source, FrayDatum other) {
        return new FrayBoolean(source, getInterpreter(), other instanceof FrayNumber && Objects.equals(((FrayNumber) other).getValue(), getValue()));
    }

    public Double getValue() {
        return value;
    }

    @Override
    public FrayDatum modulo(ParseTree source, FrayDatum right) throws FrayException {
        if (right instanceof FrayNumber)
            return new FrayNumber(source, getInterpreter(), getValue() % ((FrayNumber) right).getValue());
        throw new FrayException(String.format("Expression is not a number: '%s'", right.toString()), source, getInterpreter());
    }

    @Override
    public FrayDatum plus(ParseTree source, FrayDatum right) throws FrayException {
        if (right instanceof FrayNumber)
            return new FrayNumber(source, getInterpreter(), getValue() + ((FrayNumber) right).getValue());
        throw new FrayException(String.format("Expression is not a number: '%s'", right.toString()), source, getInterpreter());
    }
    @Override
    public FrayDatum times(ParseTree source, FrayDatum right) throws FrayException {
        if (right instanceof FrayNumber)
            return new FrayNumber(source, getInterpreter(), getValue() * ((FrayNumber) right).getValue());
        throw new FrayException(String.format("Expression is not a number: '%s'", right.toString()), source, getInterpreter());
    }

    @Override
    public String toString() {
        if (value == value.intValue())
            return String.valueOf(value.intValue());
        return getValue().toString();
    }
}
