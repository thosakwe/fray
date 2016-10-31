package thosakwe.fray.analysis;

import org.antlr.v4.runtime.ParserRuleContext;

import java.io.InputStream;
import java.io.PrintStream;
import java.util.Scanner;

public class Suggestion {
    public static final int ERROR = 0;
    public static final int WARNING = 1;

    private final int line;
    private final int pos;
    private final String filename;
    private final String message;
    private final int type;

    public Suggestion(int type, String message, String filename, ParserRuleContext source) {
        this.line = source.start.getLine();
        this.pos = source.start.getCharPositionInLine();
        this.filename = filename;
        this.message = message;
        this.type = type;
    }

    public Suggestion(int type, String message, String filename, int line, int pos) {
        this.line = line;
        this.pos = pos;
        this.filename = filename;
        this.message = message;
        this.type = type;
    }

    public int getLine() {
        return line;
    }

    public int getPos() {
        return pos;
    }

    public String getFilename() {
        return filename;
    }

    public String getMessage() {
        return message;
    }

    public int getType() {
        return type;
    }

    public Suggestion scan(Scanner in) {
        final int type = in.nextInt();
        final String filename = in.nextLine();
        final String message = in.nextLine();
        final int line = in.nextInt();
        final int pos = in.nextInt();
        return new Suggestion(type, message, filename, line, pos);
    }

    public void serialize(PrintStream out) {
        out.print(type);
        out.println(filename);
        out.println(message);
        out.print(line);
        out.print(pos);
    }
}
