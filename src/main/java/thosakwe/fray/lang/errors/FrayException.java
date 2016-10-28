package thosakwe.fray.lang.errors;

import org.antlr.v4.runtime.tree.ParseTree;
import thosakwe.fray.grammar.FrayParser;
import thosakwe.fray.lang.FrayInterpreter;

import java.io.PrintWriter;

public class FrayException extends Exception {
    private final FrayInterpreter interpreter;
    private final String message;
    private final ParseTree source;

    public FrayException(String message, ParseTree source, FrayInterpreter interpreter) {
        this.interpreter = interpreter;
        this.message = message;
        this.source = source;
    }

    @Override
    public String getMessage() {
        return message;
    }

    public ParseTree getSource() {
        return source;
    }

    @Override
    public void printStackTrace(PrintWriter s) {
        // Todo: Custom stack traces :)
        super.printStackTrace(s);
    }
}
