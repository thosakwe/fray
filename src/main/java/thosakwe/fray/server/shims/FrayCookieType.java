package thosakwe.fray.server.shims;

import org.antlr.v4.runtime.tree.ParseTree;
import thosakwe.fray.interpreter.FrayInterpreter;
import thosakwe.fray.interpreter.errors.FrayException;
import thosakwe.fray.lang.*;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class FrayCookieType extends FrayType {
    private final Pattern _pair = Pattern.compile("([^\\s=]+)=([^\\s=]+)");

    public FrayCookieType(ParseTree source, FrayInterpreter interpreter) throws FrayException {
        super(source, interpreter, FrayType.OBJECT);
        initConstructors();
    }

    @Override
    public String getName() {
        return "Cookie";
    }

    @Override
    public FrayDatum getPrototype() throws FrayException {
        return new FrayHttpCookie(getSource(), getInterpreter());
    }

    private void initConstructors() throws FrayException {
        getConstructors().put("", new FrayFunction(null, getInterpreter()) {
            @Override
            public FrayDatum call(FrayInterpreter interpreter, ParseTree source, List<FrayDatum> args) throws FrayException {
                return null;
            }
        });

        getConstructors().put("parse", new FrayFunction(null, getInterpreter()) {
            @Override
            public FrayDatum call(FrayInterpreter interpreter, ParseTree source, List<FrayDatum> args) throws FrayException {
                if (args.isEmpty())
                    throw FrayException.expectedAtLeast(1);
                FrayString str = new FrayTypeChecker<FrayString>().enforce(args.get(0), FrayString.class, source, interpreter);
                String text = str.toString();
                FrayHttpCookie cookie = (FrayHttpCookie) interpreter.getSymbolTable().getInnerMostScope().getThisContext();

                for (String pair : text.split(";")) {
                    Matcher m = _pair.matcher(pair);

                    if (m.matches()) {
                        String name = m.group(1), value = m.group(2);

                        if (!name.isEmpty() && !value.isEmpty()) {
                            cookie.setName(name);
                            cookie.setValue(value);
                        }
                    }
                }

                return null;
            }

            @Override
            public String toString() {
                return "[Constructor:Cookie.parse]";
            }
        });

        final FrayType parent = this;

        registerFinalMember("parse", new FrayType(getSource(), getInterpreter(), FrayType.OBJECT) {
            @Override
            public String getName() {
                return null;
            }

            @Override
            public FrayDatum construct(String constructorName, ParseTree source, List<FrayDatum> args) throws FrayException {
                return parent.construct("parse", source, args);
            }
        });
    }
}
