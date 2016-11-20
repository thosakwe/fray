package thosakwe.fray.analysis;

import org.antlr.v4.runtime.ParserRuleContext;

public class AnalysisSymbol {
    private final String name;
    private final ParserRuleContext sourceElement;
    private final String source;
    private boolean isFinal = false;

    public AnalysisSymbol(String name, ParserRuleContext sourceElement, String source, boolean isFinal) {
        this.name = name;
        this.sourceElement = sourceElement;
        this.source = source;
        this.isFinal = isFinal;
    }

    public AnalysisSymbol(String name, ParserRuleContext sourceElement, String source) {
        this.name = name;
        this.sourceElement = sourceElement;
        this.source = source;
    }

    public String getName() {
        return name;
    }

    public ParserRuleContext getSourceElement() {
        return sourceElement;
    }

    public String getSource() {
        return source;
    }

    public boolean isFinal() {
        return isFinal;
    }

    public void markAsFinal() {
        this.isFinal = true;
    }
}
