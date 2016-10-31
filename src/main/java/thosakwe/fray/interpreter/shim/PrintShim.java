package thosakwe.fray.interpreter.shim;

import org.antlr.v4.runtime.tree.ParseTree;
import thosakwe.fray.interpreter.FrayInterpreter;
import thosakwe.fray.interpreter.data.FrayDatum;
import thosakwe.fray.interpreter.data.FrayFunction;
import thosakwe.fray.interpreter.data.FrayNull;
import thosakwe.fray.interpreter.errors.FrayException;

import java.util.List;

public class PrintShim implements FrayShim {
    @Override
    public void inject(FrayInterpreter interpreter) throws FrayException {
        interpreter.getSymbolTable().setFinal("print", new FrayFunction(null, interpreter) {
            @Override
            public FrayDatum call(FrayInterpreter interpreter, ParseTree source, List<FrayDatum> args) throws FrayException {
                if (args.size() != 1)
                    throw new FrayException("'print' must be called with exactly one argument.", source, interpreter);
                else System.out.println(args.get(0));
                return new FrayNull();
            }

            @Override
            public String toString() {
                return "[print]";
            }
        }, null, interpreter);
    }
}
