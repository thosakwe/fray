package thosakwe.fray.lang;

import org.antlr.v4.runtime.tree.ParseTree;
import thosakwe.fray.interpreter.FrayInterpreter;
import thosakwe.fray.interpreter.errors.FrayException;

public class FrayTypeChecker<T extends FrayDatum> {
    public T enforce(FrayDatum input, Class<T> type, ParseTree source, FrayInterpreter interpreter) throws FrayException {
        return enforce(input, type,
                String.format("Data of the wrong type was provided, expected a %s", type.getSimpleName().replaceAll("^Fray", "")),
                source,
                interpreter);
    }

    public T enforce(FrayDatum input, Class<T> type, String message, ParseTree source, FrayInterpreter interpreter)
            throws FrayException {
        if (!type.isInstance(input))
            throw new FrayException(message, source, interpreter);
        return (T) input;
    }
}