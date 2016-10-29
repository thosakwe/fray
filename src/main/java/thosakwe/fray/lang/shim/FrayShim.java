package thosakwe.fray.lang.shim;

import thosakwe.fray.lang.FrayInterpreter;
import thosakwe.fray.lang.errors.FrayException;

public interface FrayShim {
    void inject(FrayInterpreter interpreter) throws FrayException;
}
