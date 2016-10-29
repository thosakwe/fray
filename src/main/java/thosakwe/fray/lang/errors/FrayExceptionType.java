package thosakwe.fray.lang.errors;

import org.antlr.v4.runtime.tree.ParseTree;
import thosakwe.fray.lang.FrayInterpreter;
import thosakwe.fray.lang.data.FrayDatum;
import thosakwe.fray.lang.data.FrayString;
import thosakwe.fray.lang.data.FrayType;

import java.util.List;

public class FrayExceptionType extends FrayType {
    public FrayExceptionType(FrayInterpreter interpreter) {
        super(null, interpreter, null);
    }

    @Override
    public FrayDatum construct(String constructorName, ParseTree source, List<FrayDatum> args) throws FrayException {
        if (args.isEmpty() || !(args.get(0) instanceof FrayString))
            throw new FrayException("The Exception constructor expects to be passed a string.", source, getInterpreter());
        final FrayString str = (FrayString) args.get(0);
        return new FrayExceptionDatum(source, getInterpreter(), str.toString());
    }

    @Override
    public String getName() {
        return "Exception";
    }
}
