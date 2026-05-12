package binquery.cli;

import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;

import binquery.BinQueryLexer;
import binquery.BinQueryParser;
import binquery.CodeGenVisitor;
import binquery.error.BinQueryErrorListener;
import binquery.error.SemanticException;
import binquery.error.SyntaxException;

import java.io.IOException;

// Owns the lex -> parse -> visit pipeline for a single .bq file.
//
// Errors (syntax or semantic) are printed to stderr prefixed with the
// input path; the returned CompileResult.ok is false in that case.
public final class Compiler {

    public static final class Result {
        public final boolean ok;
        public final String generated;  // null when !ok

        private Result(boolean ok, String generated) {
            this.ok = ok; this.generated = generated;
        }
        static Result fail()                   { return new Result(false, null); }
        static Result success(String gen)      { return new Result(true,  gen);  }
    }

    public Result compile(String inPath) throws IOException {
        CharStream input         = CharStreams.fromFileName(inPath);
        BinQueryLexer lexer      = new BinQueryLexer(input);
        CommonTokenStream tokens = new CommonTokenStream(lexer);
        BinQueryParser parser    = new BinQueryParser(tokens);

        BinQueryErrorListener listener = new BinQueryErrorListener();
        lexer.removeErrorListeners();
        lexer.addErrorListener(listener);
        parser.removeErrorListeners();
        parser.addErrorListener(listener);

        try {
            BinQueryParser.ProgramContext tree = parser.program();
            if (listener.hasErrors()) {
                for (SyntaxException ex : listener.getErrors()) {
                    System.err.println(inPath + ": " + ex.getMessage());
                }
                return Result.fail();
            }
            CodeGenVisitor visitor = new CodeGenVisitor();
            return Result.success(visitor.visit(tree));
        } catch (SemanticException e) {
            System.err.println(inPath + ": " + e.getMessage());
            return Result.fail();
        }
    }
}
