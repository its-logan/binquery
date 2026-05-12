package binquery;
import binquery.BinQueryBaseVisitor;
import binquery.BinQueryParser.*;
import binquery.handlers.FindCallsHandler;
import binquery.handlers.FindFunctionsHandler;
import binquery.handlers.FindSymbolsHandler;
import binquery.handlers.FindBytesHandler;
import binquery.handlers.FindStringsHandler;
import binquery.error.SemanticException;

import java.util.ArrayDeque;
import java.util.Deque;

/**
 * Each query type is dispatched to its own handler which owns both
 * validation and code generation for that query.
 *
 * scopeBlock support: the visitor maintains a stack of scope variable
 * names. When inside one or more `in <selector> { ... }` blocks, queries
 * see the innermost scope's Java variable name and emit scope-aware code.
 */
public class CodeGenVisitor extends BinQueryBaseVisitor<String> {

    private final FindCallsHandler     callsHandler     = new FindCallsHandler();
    private final FindFunctionsHandler functionsHandler = new FindFunctionsHandler();
    private final FindSymbolsHandler   symbolsHandler   = new FindSymbolsHandler();
    private final FindBytesHandler     bytesHandler     = new FindBytesHandler();
    private final FindStringsHandler   stringsHandler   = new FindStringsHandler();

    private String scriptName;
    private final StringBuilder body = new StringBuilder();

    private final Deque<String> scopeStack = new ArrayDeque<>();
    private int scopeCounter = 0;

    private String currentScope() {
        return scopeStack.peek();
    }

    @Override
    public String visitProgram(ProgramContext ctx) {
        visit(ctx.scriptDecl());
        for (TopLevelContext tl : ctx.topLevel()) {
            visit(tl);
        }
        return buildOutput();
    }

    @Override
    public String visitScriptDecl(ScriptDeclContext ctx) {
        scriptName = ctx.IDENTIFIER().getText();
        return null;
    }

    @Override
    public String visitScopeBlock(ScopeBlockContext ctx) {
        int n = ++scopeCounter;
        String scopeName = "_scope_" + n;
        String parent = currentScope();
        emitScopeOpen(ctx.scopeSelector(), n, scopeName, parent);

        scopeStack.push(scopeName);
        for (TopLevelContext tl : ctx.topLevel()) {
            visit(tl);
        }
        scopeStack.pop();

        body.append("      }\n");
        return null;
    }

    private void emitScopeOpen(ScopeSelectorContext sel, int n, String scopeName, String parent) {
        body.append("      {\n");
        if (sel.BLOCK() != null) {
            String name = stripQuotes(sel.STRING().getText());
            if (name.isBlank()) {
                throw new SemanticException(sel.getStart().getLine(),
                    "block name cannot be empty");
            }
            body.append("        MemoryBlock _blk_").append(n)
                .append(" = currentProgram.getMemory().getBlock(\"").append(name).append("\");\n");
            body.append("        AddressSet _scope_inner_").append(n).append(" = new AddressSet();\n");
            body.append("        if (_blk_").append(n).append(" == null) {\n");
            body.append("            printf(\"block not found: ").append(name).append("\\n\");\n");
            body.append("        } else {\n");
            body.append("            _scope_inner_").append(n)
                .append(".add(_blk_").append(n).append(".getStart(), _blk_")
                .append(n).append(".getEnd());\n");
            body.append("        }\n");
        } else {
            String name = stripQuotes(sel.STRING().getText());
            if (name.isBlank()) {
                throw new SemanticException(sel.getStart().getLine(),
                    "function name cannot be empty");
            }
            body.append("        AddressSet _scope_inner_").append(n).append(" = new AddressSet();\n");
            body.append("        {\n");
            body.append("            java.util.List<Symbol> _cands_").append(n)
                .append(" = new java.util.ArrayList<>();\n");
            body.append("            SymbolIterator _si_").append(n)
                .append(" = currentProgram.getSymbolTable().getSymbols(\"").append(name).append("\");\n");
            body.append("            while (_si_").append(n).append(".hasNext()) _cands_")
                .append(n).append(".add(_si_").append(n).append(".next());\n");
            body.append("            if (_cands_").append(n).append(".size() == 0) {\n");
            body.append("                printf(\"function not found: ").append(name).append("\\n\");\n");
            body.append("            } else if (_cands_").append(n).append(".size() > 1) {\n");
            body.append("                printf(\"ambiguous: %d functions named ").append(name)
                .append("\\n\", _cands_").append(n).append(".size());\n");
            body.append("            } else {\n");
            body.append("                Function _fn_").append(n)
                .append(" = getFunctionContaining(_cands_").append(n).append(".get(0).getAddress());\n");
            body.append("                if (_fn_").append(n).append(" == null) {\n");
            body.append("                    printf(\"symbol ").append(name)
                .append(" not inside a function\\n\");\n");
            body.append("                } else {\n");
            body.append("                    _scope_inner_").append(n)
                .append(".add(_fn_").append(n).append(".getBody());\n");
            body.append("                }\n");
            body.append("            }\n");
            body.append("        }\n");
        }
        if (parent != null) {
            body.append("        AddressSetView ").append(scopeName)
                .append(" = _scope_inner_").append(n).append(".intersect(").append(parent).append(");\n");
        } else {
            body.append("        AddressSetView ").append(scopeName)
                .append(" = _scope_inner_").append(n).append(";\n");
        }
    }

    @Override
    public String visitFindCalls(FindCallsContext ctx) {
        callsHandler.validate(ctx);
        body.append(callsHandler.emit(ctx, currentScope()));
        return null;
    }

    @Override
    public String visitFindFunctions(FindFunctionsContext ctx) {
        functionsHandler.validate(ctx);
        body.append(functionsHandler.emit(ctx, currentScope()));
        return null;
    }

    @Override
    public String visitFindSymbols(FindSymbolsContext ctx) {
        symbolsHandler.validate(ctx);
        body.append(symbolsHandler.emit(ctx, currentScope()));
        return null;
    }

    @Override
    public String visitFindBytes(FindBytesContext ctx) {
        bytesHandler.validate(ctx, currentScope() != null);
        body.append(bytesHandler.emit(ctx, currentScope()));
        return null;
    }

    @Override
    public String visitFindStrings(FindStringsContext ctx) {
        stringsHandler.validate(ctx, currentScope() != null);
        body.append(stringsHandler.emit(ctx, currentScope()));
        return null;
    }

    private static String stripQuotes(String s) {
        return s.substring(1, s.length() - 1);
    }

    private String buildOutput() {
        return "import ghidra.app.script.GhidraScript;\n"
             + "import ghidra.program.model.symbol.*;\n"
             + "import ghidra.program.model.listing.*;\n"
             + "import ghidra.program.model.address.*;\n"
             + "import ghidra.program.model.mem.MemoryBlock;\n"
             + "import ghidra.program.util.string.FoundString;\n"
             + "import java.util.HashMap;\n"
             + "import java.util.Map;\n"
             + "\n"
             + "public class " + scriptName + " extends GhidraScript {\n"
             + "    public void run() throws Exception {\n"
             + "      Map<String, Address[]> _byteCache = new HashMap<>();\n"
             + "      Map<String, java.util.List<FoundString>> _stringCache = new HashMap<>();\n"
             + body.toString()
             + "    }\n"
             + "\n"
             + "    private String _renderStr(String s) {\n"
             + "        StringBuilder sb = new StringBuilder();\n"
             + "        int limit = Math.min(s.length(), 80);\n"
             + "        for (int i = 0; i < limit; i++) {\n"
             + "            char c = s.charAt(i);\n"
             + "            switch (c) {\n"
             + "                case '\\n': sb.append(\"\\\\n\"); break;\n"
             + "                case '\\r': sb.append(\"\\\\r\"); break;\n"
             + "                case '\\t': sb.append(\"\\\\t\"); break;\n"
             + "                case '\\\\': sb.append(\"\\\\\\\\\"); break;\n"
             + "                case '\"':  sb.append(\"\\\\\\\"\"); break;\n"
             + "                default:    sb.append(c);\n"
             + "            }\n"
             + "        }\n"
             + "        if (s.length() > 80) sb.append(\"...\");\n"
             + "        return sb.toString();\n"
             + "    }\n"
             + "}\n";
    }
}
