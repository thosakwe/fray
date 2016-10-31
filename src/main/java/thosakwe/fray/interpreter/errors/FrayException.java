package thosakwe.fray.interpreter.errors;

import org.antlr.v4.runtime.tree.ParseTree;
import thosakwe.fray.interpreter.FrayInterpreter;
import thosakwe.fray.interpreter.FrayStackElement;

import java.util.List;

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
    public void printStackTrace() {
        final List<FrayStackElement> elements = interpreter.getStack().getElements();

        for (int i = elements.size() - 1; i >= 0; i--) {
            final FrayStackElement element = elements.get(i);
            System.err.printf(
                    "    at %s(%s:%d:%d)%n",
                    element.getName(),
                    element.getSourceFile(),
                    element.getSourceTree().start.getLine(),
                    element.getSourceTree().start.getCharPositionInLine());
        }
    }

    @Override
    public String toString() {
        return String.format("Unhandled exception: %s", message);
    }
}
