package thosakwe.fray.lang.pipeline;

import org.antlr.v4.runtime.ANTLRInputStream;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.ParserRuleContext;
import thosakwe.fray.grammar.FrayLexer;
import thosakwe.fray.grammar.FrayParser;

import java.io.IOException;
import java.io.InputStream;

public abstract class FrayTransformer {
    abstract boolean claim(FrayAsset asset);

    public abstract String getName();

    public String getNodeText(String source, ParserRuleContext node) {
        return source.substring(node.start.getStartIndex(), node.stop.getStopIndex() + 1);
    }

    abstract FrayAsset transform(FrayAsset asset) throws IOException;

    public FrayParser parse(String text) {
        final ANTLRInputStream antlrInputStream = new ANTLRInputStream(text);
        final FrayLexer lexer = new FrayLexer(antlrInputStream);
        final CommonTokenStream tokenStream = new CommonTokenStream(lexer);
        return new FrayParser(tokenStream);
    }

    String replaceNode(ParserRuleContext needle, String haystack, String with) {
        return haystack.substring(0, needle.start.getStartIndex()) + with + haystack.substring(needle.stop.getStopIndex());
    }
}
