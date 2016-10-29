package thosakwe.fray.lang.shim;

import thosakwe.fray.lang.FrayInterpreter;
import thosakwe.fray.lang.errors.FrayException;

public class CoreShim implements FrayShim {
    private final FrayShim[] shims = {
            new ExceptionShim(),
            new PrintShim(),
            new ProcessShim()
    };

    @Override
    public void inject(FrayInterpreter interpreter) throws FrayException {
        interpreter.printDebug("Shimming <core> library...");
        for (FrayShim shim : shims) {
            shim.inject(interpreter);
        }
    }
}
