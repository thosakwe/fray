package thosakwe.fray.interpreter.shim;

import thosakwe.fray.interpreter.FrayInterpreter;
import thosakwe.fray.interpreter.errors.FrayException;

public interface FrayShim {
    void inject(FrayInterpreter interpreter) throws FrayException;
}
