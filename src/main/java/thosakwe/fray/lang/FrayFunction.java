package thosakwe.fray.lang;

import org.antlr.v4.runtime.tree.ParseTree;
import thosakwe.fray.interpreter.FrayInterpreter;

public class FrayFunction extends FrayDatum {
    public static final FrayType TYPE = new FrayType(null, null, FrayType.OBJECT) {
        @Override
        public String getName() {
            return "Function";
        }
    };

    public FrayFunction(ParseTree source, FrayInterpreter interpreter) {
        super(source, interpreter);
    }

    @Override
    public FrayType getType() {
        return TYPE;
    }

    @Override
    public String toString() {
        return "Function";
    }
}
