package thosakwe.fray.interpreter.shim;

import org.antlr.v4.runtime.tree.ParseTree;
import thosakwe.fray.Fray;
import thosakwe.fray.interpreter.FrayInterpreter;
import thosakwe.fray.interpreter.errors.FrayException;
import thosakwe.fray.lang.*;

import java.util.List;

public class ProcessShim implements FrayShim {
    @Override
    public void inject(FrayInterpreter interpreter) throws FrayException {
        interpreter.getSymbolTable().setFinal("process", process(interpreter), null, interpreter);
    }

    private FrayDatum process(FrayInterpreter interpreter) throws FrayException {
        final FrayDatum process = new FrayDatum(null, interpreter) {
            @Override
            public String toString() {
                return "[Process]";
            }
        };

        process.registerFinalMember("exit", new FrayFunction(null, interpreter) {
            @Override
            public FrayDatum call(FrayInterpreter interpreter, ParseTree source, List<FrayDatum> args) throws FrayException {
                int status = 0;

                if (!args.isEmpty()) {
                    final FrayDatum first = args.get(0);

                    if (first instanceof FrayNumber)
                        status = ((FrayNumber) first).getValue().intValue();
                }

                System.exit(status);
                return new FrayNull();
            }

            @Override
            public String toString() {
                return "[Process.exit(code?)]";
            }
        });

        process.getSymbolTable().setFinal("version", new FrayString(null, interpreter, Fray.VERSION), null, interpreter);

        return process;
    }
}
