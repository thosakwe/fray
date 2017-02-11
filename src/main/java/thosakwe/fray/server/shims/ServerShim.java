package thosakwe.fray.server.shims;

import fi.iki.elonen.NanoHTTPD;
import thosakwe.fray.interpreter.FrayInterpreter;
import thosakwe.fray.interpreter.errors.FrayException;
import thosakwe.fray.interpreter.shim.FrayShim;

/**
 * Created on 2/7/2017.
 */
public class ServerShim implements FrayShim {
    private final NanoHTTPD.IHTTPSession session;

    public ServerShim(NanoHTTPD.IHTTPSession session) {
        this.session = session;
    }

    @Override
    public void inject(FrayInterpreter interpreter) throws FrayException {
        interpreter.getSymbolTable().putFinal("Cookie", new FrayCookieType(null, interpreter));
    }
}

