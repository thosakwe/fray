package thosakwe.fray.interpreter;

import org.antlr.v4.runtime.ParserRuleContext;

public class FrayStackElement {
    private final String name;
    private final String sourceFile;
    private final ParserRuleContext sourceTree;

    public FrayStackElement(String name, String sourceFile, ParserRuleContext sourceTree) {
        this.name = name;
        this.sourceFile = sourceFile;
        this.sourceTree = sourceTree;
    }

    public String getName() {
        return name;
    }

    public String getSourceFile() {
        return sourceFile;
    }

    public ParserRuleContext getSourceTree() {
        return sourceTree;
    }
}
