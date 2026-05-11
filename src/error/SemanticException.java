package binquery.error;

public class SemanticException extends RuntimeException {

    private final int line;

    public SemanticException(int line, String message) {
        super("Error [line " + line + "] " + message);
        this.line = line;
    }

    public int getLine() {
        return line;
    }
}
