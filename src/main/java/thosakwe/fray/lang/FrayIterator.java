package thosakwe.fray.lang;

import org.antlr.v4.runtime.tree.ParseTree;
import thosakwe.fray.interpreter.FrayInterpreter;
import thosakwe.fray.analysis.Symbol;
import thosakwe.fray.interpreter.errors.FrayException;

import java.util.List;

public class FrayIterator extends FrayDatum {
    private FrayDatum current = new FrayNull();
    private int position = -1;
    private final FraySet set;

    public FrayIterator(ParseTree source, FrayInterpreter interpreter, FraySet set) throws FrayException {
        super(source, interpreter);
        this.set = set;
        getSymbolTable().getSymbols().add(currentSymbol());
        registerFinalMember("moveNext", moveNext());
    }

    private Symbol currentSymbol() {
        return new Symbol("current", null, true) {
            @Override
            public FrayDatum getValue() {
                return current;
            }

            @Override
            public String toString() {
                return "[Iterator.current]";
            }
        };
    }

    private FrayFunction moveNext() {
        return new FrayFunction(getSource(), getInterpreter()) {
            @Override
            public FrayBoolean call(FrayInterpreter interpreter, ParseTree source, List<FrayDatum> args) throws FrayException {
                if (++position >= set.getItems().size()) {
                    current = new FrayNull();
                    return FrayBoolean.FALSE;
                } else {
                    current = set.getItems().get(position);
                    return FrayBoolean.TRUE;
                }
            }

            @Override
            public String toString() {
                return "[Iterator.moveNext]";
            }
        };
    }

    private FrayFunction peek() {
        return new FrayFunction(getSource(), getInterpreter()) {
            @Override
            public FrayDatum call(FrayInterpreter interpreter, ParseTree source, List<FrayDatum> args) throws FrayException {
                if (position + 1 >= set.getItems().size()) {
                    return set.getItems().get(position + 1);
                } else return new FrayNull();
            }
            @Override
            public String toString() {
                return "[Iterator.peek]";
            }
        };
    }

    @Override
    public String toString() {
        return "[Iterator]";
    }
}
