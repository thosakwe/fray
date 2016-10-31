package thosakwe.fray.pipeline;

import org.antlr.v4.runtime.tree.ParseTreeWalker;
import thosakwe.fray.grammar.FrayBaseListener;
import thosakwe.fray.grammar.FrayParser;

import java.io.IOException;
import java.util.HashMap;
import java.util.regex.Pattern;

/**
 * Transforms shorthand assignments expressions into full-length assignments.
 */
public class AssignmentTransformer extends FrayTransformer {

    @Override
    public boolean claim(FrayAsset asset) {
        return asset.getExtension().equals("fray");
    }

    @Override
    public String getName() {
        return "Assignment Expansion";
    }

    @Override
    public FrayAsset transform(FrayAsset asset) throws IOException {
        final HashMap<String, String> replace = new HashMap<>();
        final String src = asset.readAsString();

        final FrayParser parser = parse(src);
        final FrayParser.CompilationUnitContext compilationUnit = parser.compilationUnit();

        ParseTreeWalker.DEFAULT.walk(new FrayBaseListener() {
            @Override
            public void enterAssignmentExpression(FrayParser.AssignmentExpressionContext ctx) {
                final String op = ctx.assignmentOperator().getText();

                if (!op.equals("=")) {
                    final String newOp = op.replace("=", "");
                    final String left = ctx.left.getText(), right = ctx.right.getText();
                    // asset.getPipeline().printDebug(String.format("Left: '%s', right: '%s', orig: %s, new: %s%n", left, right, op, newOp));
                    final String newAssignment = String.format("%s = ((%s) %s (%s));", left, left, newOp, right);
                    // asset.getPipeline().printDebug(String.format("New assignment: '%s'%n", newAssignment));
                    final int beginIndex = ctx.start.getStartIndex(), endIndex = ctx.stop.getStopIndex() + 1;
                    final String substr = src.substring(beginIndex, endIndex);
                    // asset.getPipeline().printDebug(String.format("begin: %d, end: %d, SUBSTR: '%s'%n", beginIndex, endIndex, substr));
                    // asset.getPipeline().printDebug(String.format("Old assignment: %s', New: '%s'%n", substr, newAssignment));
                    replace.put(substr, newAssignment);
                }

                super.enterAssignmentExpression(ctx);
            }
        }, compilationUnit);

        String transformed = src;

        for (String key : replace.keySet()) {
            asset.getPipeline().printDebug(String.format("Assignment transformer replacing '%s' with '%s'...", key, replace.get(key)));
            transformed = src.replaceAll(Pattern.quote(key), replace.get(key));
            // asset.getPipeline().printDebug(String.format("Transformed source: '%s'%n", transformed));
        }

        return asset.changeText(transformed);
    }
}
