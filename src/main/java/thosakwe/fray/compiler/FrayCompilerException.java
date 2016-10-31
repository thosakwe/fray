package thosakwe.fray.compiler;

import org.antlr.v4.runtime.ParserRuleContext;

public class FrayCompilerException extends Exception {
    private final FrayCompiler compiler;
    private final String message;
    private final ParserRuleContext source;

    public FrayCompilerException(String message, ParserRuleContext source, FrayCompiler compiler) {
        this.compiler = compiler;
        this.message = message;
        this.source = source;
    }

    public void explain() {
        System.err.printf(
                "Compilation error: %s (%s:%d:%d)%n",
                message,
                compiler.getSourceAsset().getSourcePath(),
                source.start.getLine(),
                source.stop.getCharPositionInLine());
        System.exit(1);
    }
}
