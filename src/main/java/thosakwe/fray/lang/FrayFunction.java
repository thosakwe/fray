package thosakwe.fray.lang;

import org.antlr.v4.runtime.tree.ParseTree;
import thosakwe.fray.grammar.FrayParser;
import thosakwe.fray.interpreter.FrayInterpreter;

import java.util.ArrayList;
import java.util.List;

public class FrayFunction extends FrayDatum {
    private FrayParser.FunctionBodyContext functionBody = null;
    private List<FrayFunctionParameter> parameters = new ArrayList<>();
    private String name = null;
    private FrayType returnType = FrayType.OBJECT;
    private FrayDatum thisContext = null;

    public FrayFunction(ParseTree source, FrayInterpreter interpreter) {
        super(source, interpreter);
    }

    public FrayParser.FunctionBodyContext getFunctionBody() {
        return functionBody;
    }

    public void setFunctionBody(FrayParser.FunctionBodyContext functionBody) {
        this.functionBody = functionBody;
    }

    public List<FrayFunctionParameter> getParameters() {
        return parameters;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public FrayType getReturnType() {
        return returnType;
    }

    public void setReturnType(FrayType returnType) {
        this.returnType = returnType;
    }

    public FrayDatum getThisContext() {
        return thisContext;
    }

    public void setThisContext(FrayDatum thisContext) {
        this.thisContext = thisContext;
    }

    @Override
    public FrayType getType() {
        return FrayType.FUNCTION;
    }

    @Override
    public String toString() {
        return "Function";
    }
}
