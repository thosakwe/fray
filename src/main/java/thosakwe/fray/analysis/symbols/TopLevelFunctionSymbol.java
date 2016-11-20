package thosakwe.fray.analysis.symbols;

import org.antlr.v4.runtime.ParserRuleContext;
import thosakwe.fray.analysis.AnalysisSymbol;
import thosakwe.fray.analysis.StructuredScope;
import thosakwe.fray.analysis.mirrors.FunctionMirror;
import thosakwe.fray.grammar.FrayParser;

import java.util.List;

public class TopLevelFunctionSymbol extends AnalysisSymbol {
    private final FunctionMirror function;

    public TopLevelFunctionSymbol(String name, FrayParser.TopLevelFunctionDefinitionContext sourceElement, String source, StructuredScope scope) {
        super(name, sourceElement, source, true);
        this.function = new FunctionMirror(sourceElement.functionBody(), source, scope);
    }

    public List<FrayParser.AnnotationContext> getAnnotations() {
        return getSourceElement().functionSignature().annotation();
    }

    public FunctionMirror getFunction() {
        return function;
    }

    @Override
    public FrayParser.TopLevelFunctionDefinitionContext getSourceElement() {
        return (FrayParser.TopLevelFunctionDefinitionContext) super.getSourceElement();
    }
}
