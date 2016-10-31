package thosakwe.fray.interpreter.shim;

import thosakwe.fray.interpreter.FrayInterpreter;
import thosakwe.fray.interpreter.errors.FrayException;
import thosakwe.fray.interpreter.errors.FrayExceptionType;

public class ExceptionShim implements FrayShim {
    @Override
    public void inject(FrayInterpreter interpreter) throws FrayException {
        interpreter.getSymbolTable().setFinal("Exception", new FrayExceptionType(interpreter), null, interpreter);
    }
}
