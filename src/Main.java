package binquery;

import org.antlr.v4.runtime.*;
import org.antlr.v4.runtime.tree.*;
import java.nio.file.*;
import binquery.BinQueryParser;
import binquery.BinQueryLexer;

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

        // TODO(phase-II): wire up a custom error listener
        // parser.removeErrorListeners();
        // parser.addErrorListener(new BinQueryErrorListener());

        BinQueryParser.ProgramContext tree = parser.program();
        CodeGenVisitor visitor = new CodeGenVisitor();
        String output = visitor.visit(tree);

        String outPath = args[0].replace(".bq", ".java");
        Files.writeString(Path.of(outPath), output);
        System.out.println("wrote " + outPath);
    }
}
