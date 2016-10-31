package thosakwe.fray.interpreter.errors;

import org.antlr.v4.runtime.tree.ParseTree;
import thosakwe.fray.interpreter.FrayInterpreter;
import thosakwe.fray.lang.FrayDatum;
import thosakwe.fray.lang.FrayType;

public class FrayExceptionDatum extends FrayDatum {
    private final String message;

    public FrayExceptionDatum(ParseTree source, FrayInterpreter interpreter, String message) {
        super(source, interpreter);
        this.message = message;
    }

    @Override
    public FrayType getType() {
        return FrayExceptionType.TYPE;
    }

    public FrayException toException() {
        return new FrayException(message, getSource(), getInterpreter());
    }

    @Override
    public String toString() {
        return "[Exception]";
    }
}
