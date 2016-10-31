package thosakwe.fray.interpreter.data;

import org.antlr.v4.runtime.tree.ParseTree;
import thosakwe.fray.interpreter.FrayInterpreter;
import thosakwe.fray.interpreter.errors.FrayException;

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

    public FrayNumber(ParseTree source, FrayInterpreter interpreter, Long value) {
        super(source, interpreter);
        this.value = value == value.intValue() ? value.intValue() : value * 1.0;
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
    public FrayBoolean gt(ParseTree source, FrayDatum right) throws FrayException {
        if (right instanceof FrayNumber)
            return value > ((FrayNumber) right).value ? FrayBoolean.TRUE : FrayBoolean.FALSE;
        throw new FrayException(String.format("Expression is not a number: '%s'", right.toString()), source, getInterpreter());
    }

    @Override
    public FrayBoolean lt(ParseTree source, FrayDatum right) throws FrayException {
        if (right instanceof FrayNumber)
            return value < ((FrayNumber) right).value ? FrayBoolean.TRUE : FrayBoolean.FALSE;
        throw new FrayException(String.format("Expression is not a number: '%s'", right.toString()), source, getInterpreter());
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
    public FrayDatum pow(ParseTree source, FrayDatum right) throws FrayException {
        if (right instanceof FrayNumber)
            return new FrayNumber(source, getInterpreter(), Math.pow(getValue(), ((FrayNumber) right).getValue()));
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
