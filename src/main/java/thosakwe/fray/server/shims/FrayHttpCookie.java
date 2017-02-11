package thosakwe.fray.server.shims;

import org.antlr.v4.runtime.tree.ParseTree;
import thosakwe.fray.interpreter.FrayInterpreter;
import thosakwe.fray.lang.FrayDatum;
import thosakwe.fray.lang.FrayDatumUtils;
import thosakwe.fray.lang.FrayString;

public class FrayHttpCookie extends FrayDatum {
    private String name, value, expires, domain;

    public FrayHttpCookie(ParseTree source, FrayInterpreter interpreter) {
        super(source, interpreter);
        initSymbols();
    }

    private void initSymbols() {
        FrayDatumUtils.addField(this, "name", () -> FrayString.parse(getName()), (FrayDatum name) -> {
            setName(name.toString());
        });


        FrayDatumUtils.addField(this, "value", () -> FrayString.parse(getValue()), (FrayDatum value) -> {
            setValue(value.toString());
        });


        FrayDatumUtils.addField(this, "expires", () -> FrayString.parse(getName()), (FrayDatum expires) -> {
            setExpires(expires.toString());
        });


        FrayDatumUtils.addField(this, "domain", () -> FrayString.parse(getName()), (FrayDatum domain) -> {
            setDomain(domain.toString());
        });
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public String getExpires() {
        return expires;
    }

    public void setExpires(String expires) {
        this.expires = expires;
    }

    public String getDomain() {
        return domain;
    }

    public void setDomain(String domain) {
        this.domain = domain;
    }

    @Override
    public String toString() {
        return "[Instance of HttpCookie]";
    }
}
