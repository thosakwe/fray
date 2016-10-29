package thosakwe.fray.lang.pipeline;

import org.antlr.v4.runtime.ANTLRInputStream;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.tree.ParseTreeWalker;
import thosakwe.fray.grammar.*;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

public class StringInterpolatorTransformer extends FrayTransformer {
    @Override
    public boolean claim(FrayAsset asset) {
        return asset.getExtension().equals("fray");
    }

    @Override
    public String getName() {
        return "String Interpolation Expansion";
    }

    @Override
    public FrayAsset transform(FrayAsset asset) throws IOException {
        final String src = asset.readAsString();
        final FrayParser parser = parse(src);
        final Map<String, String> replace = new HashMap<>();

        ParseTreeWalker.DEFAULT.walk(new FrayBaseListener() {
            @Override
            public void enterSimpleString(FrayParser.SimpleStringContext ctx) {
                final String oldString = getNodeText(src, ctx);

                // If the literal doesn't contain '%', no need to interpolate it.

                if (oldString.contains("%")) {
                    asset.getPipeline().printDebug(String.format("Original string literal: %s", oldString));
                    final ANTLRInputStream inputStream = new ANTLRInputStream(oldString.replaceAll("(^')|('$)", ""));
                    final FrayInterpolationLexer lexer = new FrayInterpolationLexer(inputStream);
                    final CommonTokenStream tokenStream = new CommonTokenStream(lexer);
                    final FrayInterpolationParser interpolationParser = new FrayInterpolationParser(tokenStream);
                    final StringInterpolatorListener listener = new StringInterpolatorListener(asset);
                    ParseTreeWalker.DEFAULT.walk(listener, interpolationParser.string());
                    final String newString = listener.getBuilder().toString();
                    asset.getPipeline().printDebug(String.format("Literal after interpolation: %s", newString));
                    replace.put(oldString, newString);
                }

                super.enterSimpleString(ctx);
            }
        }, parser.compilationUnit());

        String transformed = src;

        for (String key : replace.keySet()) {
            asset.getPipeline().printDebug(String.format("String interpolator transformer replacing %s with '%s'...", key, replace.get(key)));
            transformed = transformed.replaceAll(Pattern.quote(key), replace.get(key));
            // asset.getPipeline().printDebug(String.format("Transformed source: '%s'%n", transformed));
        }

        return asset.changeText(transformed);
    }
}

class StringInterpolatorListener extends FrayInterpolationBaseListener {
    private final FrayAsset asset;
    private final StringBuilder builder = new StringBuilder();
    private int nAppended = 0;

    public StringInterpolatorListener(FrayAsset asset) {
        this.asset = asset;
    }

    private void append(String str) {
        if (nAppended++ > 0)
            builder.append(" + ");
        builder.append(str);
    }

    public StringBuilder getBuilder() {
        return builder;
    }

    @Override
    public void enterNoInterpolation(FrayInterpolationParser.NoInterpolationContext ctx) {
        append(String.format("('%s')", ctx.getText()));
        super.enterNoInterpolation(ctx);
    }

    @Override
    public void enterIdentifierInterpolation(FrayInterpolationParser.IdentifierInterpolationContext ctx) {
        append(String.format("(%s)", ctx.getText().replaceAll("^%", "")));
        super.enterIdentifierInterpolation(ctx);
    }

    @Override
    public void enterComplexInterpolation(FrayInterpolationParser.ComplexInterpolationContext ctx) {
        append(String.format("(%s)", ctx.getText().replaceAll("(^%\\{)|(}$)", "")));
        super.enterComplexInterpolation(ctx);
    }
}