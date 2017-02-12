package thosakwe.fray.analysis.symbols;

import org.antlr.v4.runtime.ParserRuleContext;
import thosakwe.fray.lang.FrayFunction;

public class FrayTypeGetterMember extends FrayTypeMember {
    private final String name;
    private FrayFunction function;
    private final ParserRuleContext source;

    public FrayTypeGetterMember(String name, FrayFunction function, ParserRuleContext source) {
        this.name = name;
        this.function = function;
        this.source = source;
    }

    public FrayTypeGetterMember(String name, ParserRuleContext source) {
        this(name, null, source);
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public ParserRuleContext getSource() {
        return source;
    }
}
