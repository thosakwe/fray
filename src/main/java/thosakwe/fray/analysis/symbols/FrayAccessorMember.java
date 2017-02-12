package thosakwe.fray.analysis.symbols;

import org.antlr.v4.runtime.ParserRuleContext;
import thosakwe.fray.lang.FrayFunction;

public class FrayAccessorMember extends FrayTypeMember {
    private final FrayAccessorKind kind;
    private final String name;
    private FrayFunction function;
    private final ParserRuleContext source;

    public FrayAccessorMember(FrayAccessorKind kind, String name, FrayFunction function, ParserRuleContext source) {
        this.kind = kind;
        this.name = name;
        this.function = function;
        this.source = source;
    }

    public FrayAccessorMember(FrayAccessorKind kind, String name, ParserRuleContext source) {
        this(kind, name, null, source);
    }

    public FrayFunction getFunction() {
        return function;
    }

    public void setFunction(FrayFunction function) {
        this.function = function;
    }

    public FrayAccessorKind getKind() {
        return kind;
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
