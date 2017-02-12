package thosakwe.fray.analysis.symbols;

import org.antlr.v4.runtime.ParserRuleContext;

public abstract class FrayTypeMember {
    public abstract String getName();
    public abstract ParserRuleContext getSource();
}
