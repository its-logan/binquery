package binquery.error;

public class SyntaxException extends RuntimeException {

    private final int line;
    private final int column;

    public SyntaxException(int line, int column, String message) {
        super("Error [line " + line + ":" + column + "] " + message);
        this.line = line;
        this.column = column;
    }

    public int getLine()   { return line; }
    public int getColumn() { return column; }
}
