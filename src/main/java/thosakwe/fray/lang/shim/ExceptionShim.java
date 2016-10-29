package thosakwe.fray.lang.shim;

import thosakwe.fray.lang.FrayInterpreter;
import thosakwe.fray.lang.errors.FrayException;
import thosakwe.fray.lang.errors.FrayExceptionType;

public class ExceptionShim implements FrayShim {
    @Override
    public void inject(FrayInterpreter interpreter) throws FrayException {
        interpreter.getSymbolTable().setValue("Exception", new FrayExceptionType(interpreter), null, interpreter, true);
    }
}
