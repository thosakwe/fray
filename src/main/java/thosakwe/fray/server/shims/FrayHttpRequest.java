package thosakwe.fray.server.shims;

import fi.iki.elonen.NanoHTTPD;
import thosakwe.fray.interpreter.FrayInterpreter;
import thosakwe.fray.interpreter.errors.FrayException;
import thosakwe.fray.lang.FrayDatum;
import thosakwe.fray.lang.FraySet;
import thosakwe.fray.lang.FrayString;

/**
 * Created on 2/7/2017.
 */
public class FrayHttpRequest extends FrayDatum {
    private final NanoHTTPD.IHTTPSession session;

    public FrayHttpRequest(NanoHTTPD.IHTTPSession session, FrayInterpreter interpreter) throws FrayException {
        super(null, interpreter);
        this.session = session;
        initSymbols();
    }


    public void initSymbols() throws FrayException {
        getSymbolTable().putFinal("host", new FrayString(null, getInterpreter(), session.getRemoteHostName()));
        getSymbolTable().putFinal("ip", new FrayString(null, getInterpreter(), session.getRemoteIpAddress()));
        getSymbolTable().putFinal("method", new FrayString(null, getInterpreter(), session.getMethod().name()));
        getSymbolTable().putFinal("uri", new FrayString(null, getInterpreter(), session.getUri()));

        FraySet cookieSet = new FraySet(getSource(), getInterpreter());
        getSymbolTable().putFinal("cookies", cookieSet);

        for (String name : session.getCookies()) {
            FrayHttpCookie cookie = new FrayHttpCookie(getSource(), getInterpreter());

            cookie.setName(name);
            cookie.setValue(session.getCookies().read(name));
            cookieSet.getItems().add(cookie);
        }
    }


    @Override
    public String toString() {
        return String.format("[HttpRequest: %s]", session.getUri());
    }
}
