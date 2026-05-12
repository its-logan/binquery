package binquery;

import org.antlr.v4.runtime.*;
import org.antlr.v4.runtime.tree.*;
import java.nio.file.*;
import binquery.BinQueryParser;
import binquery.BinQueryLexer;
import binquery.error.BinQueryErrorListener;
import binquery.error.SemanticException;
import binquery.error.SyntaxException;

public class Main {
    public static void main(String[] args) throws Exception {
        if (args.length < 1) {
            System.err.println("usage: bq <script.bq>");
            System.exit(1);
        }

        CharStream input         = CharStreams.fromFileName(args[0]);
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
                    System.err.println(ex.getMessage());
                }
                System.exit(1);
            }

            CodeGenVisitor visitor = new CodeGenVisitor();
            String output = visitor.visit(tree);

            String outPath = args[0].replace(".bq", ".java");
            Files.writeString(Path.of(outPath), output);
            System.out.println("wrote " + outPath);
        } catch (SemanticException e) {
            System.err.println(e.getMessage());
            System.exit(1);
        }
    }
}
