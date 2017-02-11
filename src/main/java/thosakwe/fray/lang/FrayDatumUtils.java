package thosakwe.fray.lang;

import thosakwe.fray.analysis.Symbol;

import java.util.function.Consumer;
import java.util.function.Supplier;

public class FrayDatumUtils {
    public static void addField(FrayDatum datum, String name, Supplier<FrayDatum> getter, Consumer<FrayDatum> setter) {
        datum.getSymbolTable().getInnerMostScope().getSymbols().add(new Symbol(name) {
            @Override
            public FrayDatum getValue() {
                return getter.get();
            }

            @Override
            public void setValue(FrayDatum value) {
                setter.accept(value);
            }
        });
    }
}