package thosakwe.fray.lang.errors;

import org.antlr.v4.runtime.tree.ParseTree;
import thosakwe.fray.lang.FrayInterpreter;
import thosakwe.fray.lang.data.FrayDatum;

public class FrayExceptionDatum extends FrayDatum {
    private final String message;

    public FrayExceptionDatum(ParseTree source, FrayInterpreter interpreter, String message) {
        super(source, interpreter);
        this.message = message;
    }

    public FrayException toException() {
        return new FrayException(message, getSource(), getInterpreter());
    }

    @Override
    public String toString() {
        return "[Exception]";
    }
}
