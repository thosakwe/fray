package thosakwe.fray.lang;

public class FrayFunctionParameter {
    private final String name;
    private FrayType expectedType = FrayType.OBJECT;

    public FrayFunctionParameter(String name) {
        this.name = name;
    }

    public FrayType getExpectedType() {
        return expectedType;
    }

    public void setExpectedType(FrayType expectedType) {
        this.expectedType = expectedType;
    }

    public String getName() {
        return name;
    }

    boolean check(FrayDatum arg) {
        FrayType inputType = arg.getType();
        return inputType.isAssignableTo(expectedType);
    }
}
