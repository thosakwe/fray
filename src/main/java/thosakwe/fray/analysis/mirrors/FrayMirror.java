package thosakwe.fray.analysis.mirrors;

import org.antlr.v4.runtime.ParserRuleContext;

public class FrayMirror {
    private final ParserRuleContext sourceElement;
    private final String source;

    public FrayMirror(ParserRuleContext sourceElement, String source) {
        this.sourceElement = sourceElement;
        this.source = source;
    }

    public ParserRuleContext getSourceElement() {
        return sourceElement;
    }

    public String getSource() {
        return source;
    }
}
