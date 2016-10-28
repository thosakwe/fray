package thosakwe.fray.lang.data;

import org.antlr.v4.runtime.tree.ParseTree;
import thosakwe.fray.grammar.FrayParser;
import thosakwe.fray.lang.FrayInterpreter;
import thosakwe.fray.lang.Scope;
import thosakwe.fray.lang.errors.FrayException;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

// Todo: Custom operators via annotation ;)
public class FrayDatum {
    private final FrayInterpreter interpreter;
    private final Map<String, FrayFunction> operators = new HashMap<>();
    private final ParseTree source;
    private final Scope symbolTable = new Scope();

    public FrayDatum(ParseTree source, FrayInterpreter interpreter) {
        this.interpreter = interpreter;
        this.source = source;
    }

    public FrayBoolean and(ParseTree source, FrayDatum right) throws FrayException {
        return FrayBoolean.FALSE;
    }

    public FrayDatum call(FrayInterpreter interpreter, ParseTree source, List<FrayDatum> args) throws FrayException {
        final FrayDatum callee = symbolTable.getValue("call");

        if (callee == null)
            throw new FrayException(String.format("'%s' is not a function.", this.toString()), source, interpreter);
        else return callee.call(interpreter, source, args);
    }

    public FrayDatum divide(ParseTree source, FrayDatum right) throws FrayException {
        return maybeArithmeticOperator("/", source, right);
    }

    public FrayInterpreter getInterpreter() {
        return interpreter;
    }

    public FrayIterator getIterator(FrayInterpreter interpreter) throws FrayException {
        final FrayDatum iterator = symbolTable.getValue("iterator");

        if (iterator != null && !(iterator instanceof FrayIterator))
            throw new FrayException(String.format("'%s' is not an iterator.", iterator.toString()), iterator.getSource(), interpreter);
        else return iterator != null ? (FrayIterator) iterator : null;
    }

    public Map<String, FrayFunction> getOperators() {
        return operators;
    }

    public boolean isNull() {
        return false;
    }

    public boolean isTrue() {
        return false;
    }

    public FrayBoolean gequ(ParseTree source, FrayDatum right) throws FrayException {
        return maybeBooleanOperator(">=", source, right);
    }

    public ParseTree getSource() {
        return source;
    }

    public Scope getSymbolTable() {
        return symbolTable;
    }

    public FrayBoolean gt(ParseTree source, FrayDatum right) throws FrayException {
        return maybeBooleanOperator(">", source, right);
    }

    public FrayBoolean lt(ParseTree source, FrayDatum right) throws FrayException {
        return maybeBooleanOperator("<", source, right);
    }

    public FrayDatum modulo(ParseTree source, FrayDatum right) throws FrayException {
        return maybeArithmeticOperator("%", source, right);
    }

    public FrayBoolean equ(ParseTree source, FrayDatum other) throws FrayException {
        if (other != null) {
            if (operators.containsKey("==")) {
                final List<FrayDatum> args = new ArrayList<>();
                final FrayFunction run = operators.get("==");
                final FrayDatum result = run.call(interpreter, source, args);

                if (!(result instanceof FrayBoolean))
                    throw new FrayException("Operator == must return a boolean value.", run.getSource(), interpreter);
                else return (FrayBoolean) result;
            }
            return equalsOther(source, other);
        }

        return FrayBoolean.FALSE;
    }

    public FrayBoolean equalsOther(ParseTree source, FrayDatum other) {
        return other.hashCode() == hashCode() ? FrayBoolean.TRUE : FrayBoolean.FALSE;
    }

    public FrayBoolean lequ(ParseTree source, FrayDatum right) throws FrayException {
        return maybeBooleanOperator("<=", source, right);
    }

    private FrayDatum maybeArithmeticOperator(String op, ParseTree source, FrayDatum right) throws FrayException {
        if (operators.containsKey(op)) {
            final List<FrayDatum> args = new ArrayList<>();
            final FrayFunction run = operators.get(op);
            return run.call(interpreter, source, args);
        } else
            throw new FrayException(String.format("%s does not support the %s operator.", toString(), op), source, interpreter);
    }

    private FrayBoolean maybeBooleanOperator(String op, ParseTree source, FrayDatum right) throws FrayException {
        if (operators.containsKey(op)) {
            final List<FrayDatum> args = new ArrayList<>();
            final FrayFunction run = operators.get(op);
            final FrayDatum result = run.call(interpreter, source, args);

            if (result instanceof FrayBoolean)
                return (FrayBoolean) result;
            else
                throw new FrayException(String.format("Operator %s must return a boolean value.", op), run.getSource(), interpreter);
        } else
            throw new FrayException(String.format("'%s' does not support the %s operator.", toString(), op), source, interpreter);
    }

    public FrayDatum minus(ParseTree source, FrayDatum right) throws FrayException {
        return maybeArithmeticOperator("-", source, right);
    }

    public FrayBoolean nequ(ParseTree source, FrayDatum right) throws FrayException {
        return maybeBooleanOperator("!=", source, right);
    }

    public FrayBoolean or(ParseTree source, FrayDatum right) throws FrayException {
        return FrayBoolean.FALSE;
    }

    public FrayDatum plus(ParseTree source, FrayDatum right) throws FrayException {
        return maybeArithmeticOperator("+", source, right);
    }

    public FrayDatum pow(ParseTree source, FrayDatum right) throws FrayException {
        return maybeArithmeticOperator("^", source, right);
    }

    public FrayDatum times(ParseTree source, FrayDatum right) throws FrayException {
        return maybeArithmeticOperator("*", source, right);
    }

    @Override
    public String toString() {
        return String.format("[Instance of %s]", getClass().getName());
    }

    public String curses() {
        return toString();
    }
}
