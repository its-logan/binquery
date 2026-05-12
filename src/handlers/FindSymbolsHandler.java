package binquery.handlers;

import binquery.BinQueryParser.FindSymbolsContext;
import binquery.BinQueryParser.LocationFilterContext;
import binquery.error.SemanticException;

// find symbols "<name>" (internal|external)?
public class FindSymbolsHandler {

    public void validate(FindSymbolsContext ctx) {
        String name = stripQuotes(ctx.STRING().getText());
        if (name.isBlank()) {
            throw new SemanticException(
                ctx.getStart().getLine(),
                "symbol name cannot be empty"
            );
        }
    }

    public String emit(FindSymbolsContext ctx, String ambientScope) {
        String name = stripQuotes(ctx.STRING().getText());
        LocationFilterContext loc = ctx.locationFilter();
        boolean filterExternal = loc != null && loc.EXTERNAL() != null;
        boolean filterInternal = loc != null && loc.INTERNAL() != null;
        String suffix = filterExternal ? " [EXT]" : "";

        StringBuilder sb = new StringBuilder();
        sb.append("      {\n");
        sb.append("        // --- find symbols \"").append(name).append("\"");
        if (filterExternal) sb.append(" external");
        if (filterInternal) sb.append(" internal");
        if (ambientScope != null) sb.append(" (ambient ").append(ambientScope).append(")");
        sb.append(" ---\n");

        sb.append("        SymbolIterator _syms = currentProgram.getSymbolTable().getSymbols(\"")
          .append(name).append("\");\n");
        sb.append("        int _hitCount = 0;\n");
        sb.append("        while (_syms.hasNext()) {\n");
        sb.append("            Symbol _sym = _syms.next();\n");
        if (filterExternal) {
            sb.append("            if (!_sym.isExternal()) continue;\n");
        } else if (filterInternal) {
            sb.append("            if (_sym.isExternal()) continue;\n");
        }
        if (ambientScope != null) {
            sb.append("            if (!").append(ambientScope).append(".contains(_sym.getAddress())) continue;\n");
        }
        sb.append("            printf(\"SYM  %s  at %s").append(suffix)
          .append("\\n\", _sym.getName(), _sym.getAddress().toString());\n");
        sb.append("            _hitCount++;\n");
        sb.append("        }\n");
        sb.append("        if (_hitCount == 0) {\n");
        sb.append("            printf(\"[findSymbols] no symbols matching: ")
          .append(name).append("\\n\");\n");
        sb.append("        }\n");
        sb.append("      }\n");

        return sb.toString();
    }

    private String stripQuotes(String s) {
        return s.substring(1, s.length() - 1);
    }
}
