package binquery;
import binquery.BinQueryBaseVisitor;
import binquery.BinQueryParser.*;
import binquery.handlers.FindCallsHandler;
import binquery.handlers.FindFunctionsHandler;
import binquery.handlers.FindBytesHandler;
import binquery.error.SemanticException;

/**
 * Each query type is dispatched to its own handler which owns both
 * validation and code generation for that query.
 */
public class CodeGenVisitor extends BinQueryBaseVisitor<String> {

    private final FindCallsHandler     callsHandler     = new FindCallsHandler();
    private final FindFunctionsHandler functionsHandler = new FindFunctionsHandler();
    private final FindBytesHandler     bytesHandler     = new FindBytesHandler();

    private String scriptName;
    private final StringBuilder body = new StringBuilder();

    // ── program: scriptDecl query+ EOF ──────────────────────────────────────

    @Override
    public String visitProgram(ProgramContext ctx) {
        visit(ctx.scriptDecl());
        for (QueryContext q : ctx.query()) {
            visit(q);
        }
        return buildOutput();
    }

    // ── scriptDecl: SCRIPT IDENTIFIER ───────────────────────────────────────

    @Override
    public String visitScriptDecl(ScriptDeclContext ctx) {
        scriptName = ctx.IDENTIFIER().getText();
        return null;
    }

    // ── query dispatch ───────────────────────────────────────────────────────

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
             + "\n"
             + "public class " + scriptName + " extends GhidraScript {\n"
             + "    public void run() throws Exception {\n"
             + body.toString()
             + "    }\n"
             + "}\n";
    }
}
