package thosakwe.fray.lang.shim;

import thosakwe.fray.lang.FrayInterpreter;

public interface Shim {
    void inject(FrayInterpreter interpreter);
}
