package thosakwe.fray.lang;

import org.antlr.v4.runtime.tree.ParseTree;
import thosakwe.fray.interpreter.FrayInterpreter;

import java.util.ArrayList;
import java.util.List;

public class FrayGenericType extends FrayType {
    private final String name;
    private final List<FrayType> childTypes = new ArrayList<>();

    public FrayGenericType(String name, ParseTree source, FrayInterpreter interpreter, FrayType parentType) {
        super(source, interpreter, parentType);
        this.name = name;
    }

    @Override
    public String getName() {
        return name;
    }

    public List<FrayType> getChildTypes() {
        return childTypes;
    }

    @Override
    public boolean isAssignableTo(FrayType expectedType) {
        return super.isAssignableTo(expectedType);
    }

    @Override
    public String toString() {
        StringBuilder buf = new StringBuilder();

        buf.append("[Type:");
        buf.append(getName());
        buf.append("<");

        for (int i = 0; i < childTypes.size(); i++) {
            if (i > 0)
                buf.append(", ");
            buf.append(childTypes.get(i).getName());
        }

        buf.append(">]");

        return buf.toString();
    }
}
