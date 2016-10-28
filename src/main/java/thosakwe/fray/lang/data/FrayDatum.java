package thosakwe.fray.lang.data;

import org.antlr.v4.runtime.tree.ParseTree;
import thosakwe.fray.lang.FrayInterpreter;
import thosakwe.fray.lang.Scope;
import thosakwe.fray.lang.errors.FrayException;

import java.util.List;

public class FrayDatum {
    private final ParseTree source;
    private final Scope symbolTable = new Scope();

    public FrayDatum(ParseTree source) {
        this.source = source;
    }

    public FrayDatum call(FrayInterpreter interpreter, ParseTree source, List<FrayDatum> args) throws FrayException {
        final FrayDatum callee = symbolTable.getValue("call");

        if (callee == null)
            throw new FrayException(String.format("'%s' is not a function.", this.toString()), source, interpreter);
        else return callee.call(interpreter, source, args);
    }

    public boolean isNull() {
        return false;
    }

    @Override
    public String toString() {
        return String.format("[Instance of %s]", getClass().getName());
    }
}
