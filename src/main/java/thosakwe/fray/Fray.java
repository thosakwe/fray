package thosakwe.fray;

import thosakwe.fray.grammar.FrayParser;

import java.util.List;

public class Fray {
    public static final String VERSION = "1.0-SNAPSHOT";

    public static boolean annotationsContainExternal(List<FrayParser.AnnotationContext> annotations) {
        for (FrayParser.AnnotationContext annotation : annotations) {
            if (annotation.expression() instanceof FrayParser.IdentifierExpressionContext
                    && annotation.expression().getText().equals("external"))
                return true;
        }

        return false;
    }
}
