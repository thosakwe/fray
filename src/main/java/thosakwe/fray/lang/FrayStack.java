package thosakwe.fray.lang;

import java.util.ArrayList;
import java.util.List;

public class FrayStack {
    private final List<FrayStackElement> elements = new ArrayList<>();
    private final FrayInterpreter interpreter;

    FrayStack(FrayInterpreter interpreter) {
        this.interpreter = interpreter;
    }

    public List<FrayStackElement> getElements() {
        return elements;
    }

    public void push(FrayStackElement element) {
        elements.add(element);
    }

    public void pop() {
        if (!elements.isEmpty() && interpreter.getErrors().isEmpty()) {
            elements.remove(elements.size() - 1);
        }
    }
}
