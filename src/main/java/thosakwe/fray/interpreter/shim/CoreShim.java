package thosakwe.fray.interpreter.shim;

import thosakwe.fray.interpreter.FrayInterpreter;
import thosakwe.fray.interpreter.errors.FrayException;

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
