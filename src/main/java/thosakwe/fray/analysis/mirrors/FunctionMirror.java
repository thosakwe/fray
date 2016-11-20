package thosakwe.fray.analysis.mirrors;

import thosakwe.fray.analysis.StructuredScope;
import thosakwe.fray.grammar.FrayParser;

import java.util.List;

public class FunctionMirror extends ExpressionMirror {
    private final StructuredScope scope;
    public List<StatementMirror> statements;

    public FunctionMirror(FrayParser.FunctionBodyContext sourceElement, String source, StructuredScope scope) {
        super(sourceElement, source);
        this.scope = scope;
    }

    public StructuredScope getScope() {
        return scope;
    }

    @Override
    public FrayParser.FunctionBodyContext getSourceElement() {
        return (FrayParser.FunctionBodyContext) super.getSourceElement();
    }
}
