package thosakwe.fray.pipeline;

import org.antlr.v4.runtime.tree.ParseTreeWalker;
import thosakwe.fray.grammar.FrayBaseListener;
import thosakwe.fray.grammar.FrayParser;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

public class InlineFunctionExpanderTransformer extends FrayTransformer {
    @Override
    public boolean claim(FrayAsset asset) {
        return asset.getExtension().equals("fray");
    }

    @Override
    public String getName() {
        return "Inline Function Expansion";
    }

    @Override
    public FrayAsset transform(FrayAsset asset) throws IOException {
        final String src = asset.readAsString();
        final FrayParser parser = parse(src);
        final Map<String, String> replace = new HashMap<>();

        ParseTreeWalker.DEFAULT.walk(new FrayBaseListener() {
            @Override
            public void enterExpressionBody(FrayParser.ExpressionBodyContext ctx) {
                final FrayParser.ExpressionContext expr = ctx.expression();
                final String returnValue = src.substring(expr.start.getStartIndex(), expr.stop.getStopIndex() + 1);
                final String sourceText = src.substring(ctx.start.getStartIndex(), ctx.stop.getStopIndex() + 1);
                replace.put(sourceText, String.format("{ return %s; }", returnValue));
                super.enterExpressionBody(ctx);
            }
        }, parser.compilationUnit());

        String transformed = src;

        for (String key : replace.keySet()) {
            asset.getPipeline().printDebug(String.format("Inline function expansion transformer replacing %s with '%s'...", key, replace.get(key)));
            transformed = transformed.replaceAll(Pattern.quote(key), replace.get(key));
        }

        return asset.changeText(transformed);
    }
}
