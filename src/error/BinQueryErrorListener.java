package binquery.error;

import org.antlr.v4.runtime.BaseErrorListener;
import org.antlr.v4.runtime.RecognitionException;
import org.antlr.v4.runtime.Recognizer;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

// Accumulating ANTLR error listener.
//
// Attached to both the lexer and parser. Each call to syntaxError() is
// converted into a SyntaxException and stored; the listener never throws.
// ANTLR's default error-recovery strategy continues parsing so subsequent
// independent errors are reported in the same pass. Main.java inspects
// hasErrors() after parser.program() returns.
public class BinQueryErrorListener extends BaseErrorListener {

    private final List<SyntaxException> errors = new ArrayList<>();

    @Override
    public void syntaxError(Recognizer<?, ?> recognizer,
                            Object offendingSymbol,
                            int line,
                            int charPositionInLine,
                            String msg,
                            RecognitionException e) {
        errors.add(new SyntaxException(line, charPositionInLine, msg));
    }

    public boolean hasErrors() {
        return !errors.isEmpty();
    }

    public List<SyntaxException> getErrors() {
        return Collections.unmodifiableList(errors);
    }
}
