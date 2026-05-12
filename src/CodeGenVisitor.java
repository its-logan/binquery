package binquery;
import binquery.BinQueryBaseVisitor;
import binquery.BinQueryParser.*;
import binquery.handlers.FindCallsHandler;
import binquery.handlers.FindFunctionsHandler;
import binquery.handlers.FindSymbolsHandler;
import binquery.handlers.FindBytesHandler;
import binquery.error.SemanticException;

/**
 * Each query type is dispatched to its own handler which owns both
 * validation and code generation for that query.
 */
public class CodeGenVisitor extends BinQueryBaseVisitor<String> {

    private final FindCallsHandler     callsHandler     = new FindCallsHandler();
    private final FindFunctionsHandler functionsHandler = new FindFunctionsHandler();
    private final FindSymbolsHandler   symbolsHandler   = new FindSymbolsHandler();
    private final FindBytesHandler     bytesHandler     = new FindBytesHandler();

    private String scriptName;
    private final StringBuilder body = new StringBuilder();

    @Override
    public String visitProgram(ProgramContext ctx) {
        visit(ctx.scriptDecl());
        for (QueryContext q : ctx.query()) {
            visit(q);
        }
        return buildOutput();
    }

    @Override
    public String visitScriptDecl(ScriptDeclContext ctx) {
        scriptName = ctx.IDENTIFIER().getText();
        return null;
    }

    @Override
    public String visitFindCalls(FindCallsContext ctx) {
        callsHandler.validate(ctx);
        body.append(callsHandler.emit(ctx));
        return null;
    }

    @Override
    public String visitFindFunctions(FindFunctionsContext ctx) {
        functionsHandler.validate(ctx);
        body.append(functionsHandler.emit(ctx));
        return null;
    }

    @Override
    public String visitFindSymbols(FindSymbolsContext ctx) {
        symbolsHandler.validate(ctx);
        body.append(symbolsHandler.emit(ctx));
        return null;
    }

    @Override
    public String visitFindBytes(FindBytesContext ctx) {
        bytesHandler.validate(ctx);
        body.append(bytesHandler.emit(ctx));
        return null;
    }


    private String buildOutput() {
        return "import ghidra.app.script.GhidraScript;\n"
             + "import ghidra.program.model.symbol.*;\n"
             + "import ghidra.program.model.listing.*;\n"
             + "import ghidra.program.model.address.*;\n"
             + "import java.util.HashMap;\n"
             + "import java.util.Map;\n"
             + "\n"
             + "public class " + scriptName + " extends GhidraScript {\n"
             + "    public void run() throws Exception {\n"
             + "      Map<String, Address[]> _byteCache = new HashMap<>();\n"
             + body.toString()
             + "    }\n"
             + "}\n";
    }
}
