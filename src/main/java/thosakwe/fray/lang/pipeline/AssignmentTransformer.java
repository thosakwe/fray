package thosakwe.fray.lang.pipeline;

import org.antlr.v4.runtime.ANTLRInputStream;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.tree.ParseTreeWalker;
import org.apache.commons.io.IOUtils;
import thosakwe.fray.grammar.FrayBaseListener;
import thosakwe.fray.grammar.FrayLexer;
import thosakwe.fray.grammar.FrayParser;

import java.io.ByteArrayInputStream;
import java.io.IOException;

/// Transforms shorthand assignments expressions into full-length assignments.
public class AssignmentTransformer implements FrayTransformer {
    private String src;

    @Override
    public boolean claim(FrayAsset asset) {
        return asset.getExtension().equals("fray");
    }

    @Override
    public FrayAsset transform(FrayAsset asset) throws IOException {
        setSrc(IOUtils.toString(asset.getInputStream()));
        asset.getPipeline().printDebug(String.format("Source: '%s'", src));

        final ANTLRInputStream transformed = new ANTLRInputStream(getSrc());
        final FrayLexer lexer = new FrayLexer(transformed);
        final CommonTokenStream tokenStream = new CommonTokenStream(lexer);
        final FrayParser parser = new FrayParser(tokenStream);
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
                    final String substr = getSrc().substring(beginIndex, endIndex);
                    // asset.getPipeline().printDebug(String.format("begin: %d, end: %d, SUBSTR: '%s'%n", beginIndex, endIndex, substr));
                    // asset.getPipeline().printDebug(String.format("Old assignment: %s', New: '%s'%n", substr, newAssignment));

                    final String transformed = getSrc().substring(0, beginIndex) + newAssignment + getSrc().substring(endIndex);
                    asset.getPipeline().printDebug(String.format("Transformed source: '%s'%n", transformed));
                    setSrc(transformed);
                }

                super.enterAssignmentExpression(ctx);
            }
        }, compilationUnit);

        return asset.changeInputStream(new ByteArrayInputStream(getSrc().getBytes()));
    }

    public String getSrc() {
        return src;
    }

    public void setSrc(String src) {
        this.src = src;
    }
}
