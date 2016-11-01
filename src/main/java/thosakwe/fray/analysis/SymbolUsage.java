package thosakwe.fray.analysis;

import org.antlr.v4.runtime.ParserRuleContext;

/**
 * Created on 10/31/2016.
 */
public abstract class SymbolUsage {
    private final ParserRuleContext source;

    public SymbolUsage(ParserRuleContext source) {
        this.source = source;
    }
}