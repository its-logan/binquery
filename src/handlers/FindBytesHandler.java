package binquery.handlers;

import binquery.BinQueryParser.FindBytesContext;
import binquery.error.SemanticException;

// find bytes "<hex>" (from <addr>)? (in function "<name>")?
//
// Emits a script-level result cache lookup + dynamic-scan loop. No
// fixed match limit; the scan runs until findBytes returns null.
public class FindBytesHandler {

    public void validate(FindBytesContext ctx) {
        String raw = stripQuotes(ctx.STRING(0).getText());

        if (raw.isBlank()) {
            throw new SemanticException(
                ctx.getStart().getLine(),
                "byte pattern cannot be empty"
            );
        }

        for (String token : raw.strip().split("\\s+")) {
            if (!token.matches("[0-9A-Fa-f]{2}")) {
                throw new SemanticException(
                    ctx.getStart().getLine(),
                    "invalid byte pattern token \"" + token
                    + "\": expected hex pair like \"4D\", got \"" + token + "\""
                );
            }
        }

        boolean hasFrom = ctx.HEX_ADDR() != null;
        boolean hasInFn = ctx.IN() != null;
        if (hasFrom && hasInFn) {
            throw new SemanticException(
                ctx.getStart().getLine(),
                "FROM and IN FUNCTION are mutually exclusive"
            );
        }

        if (hasInFn) {
            String fnName = stripQuotes(ctx.STRING(1).getText());
            if (fnName.isBlank()) {
                throw new SemanticException(
                    ctx.getStart().getLine(),
                    "function name in IN FUNCTION cannot be empty"
                );
            }
        }
    }

    public String emit(FindBytesContext ctx) {
        String pattern = stripQuotes(ctx.STRING(0).getText());
        int patternByteLen = pattern.strip().split("\\s+").length;
        boolean hasFrom = ctx.HEX_ADDR() != null;
        boolean hasInFn = ctx.IN() != null;
        String fromAddr = hasFrom ? ctx.HEX_ADDR().getText() : null;
        String fnName = hasInFn ? stripQuotes(ctx.STRING(1).getText()) : null;

        String scopeMarker;
        if (hasFrom)       scopeMarker = "from=" + fromAddr;
        else if (hasInFn)  scopeMarker = "infn=" + fnName;
        else               scopeMarker = "all";

        String key = pattern + "@" + scopeMarker;

        StringBuilder sb = new StringBuilder();
        sb.append("      {\n");
        sb.append("        // --- find bytes \"").append(pattern).append("\"");
        if (hasFrom) sb.append(" from ").append(fromAddr);
        if (hasInFn) sb.append(" in function \"").append(fnName).append("\"");
        sb.append(" ---\n");

        sb.append("        String _key = \"").append(key).append("\";\n");
        sb.append("        Address[] _matches = _byteCache.get(_key);\n");
        sb.append("        if (_matches == null) {\n");
        sb.append("            java.util.List<Address> _acc = new java.util.ArrayList<>();\n");

        if (hasInFn) {
            emitInFunctionBody(sb, pattern, patternByteLen, fnName);
        } else {
            String startExpr = hasFrom
                ? "toAddr(\"" + fromAddr + "\")"
                : "currentProgram.getMinAddress()";
            emitSimpleScanBody(sb, pattern, patternByteLen, startExpr);
        }

        sb.append("            _matches = _acc.toArray(new Address[0]);\n");
        sb.append("            _byteCache.put(_key, _matches);\n");
        sb.append("        }\n");

        sb.append("        if (_matches.length == 0) {\n");
        sb.append("            printf(\"[findBytes] no matches for pattern: ")
          .append(pattern).append("\\n\");\n");
        sb.append("        } else {\n");
        sb.append("            for (Address _m : _matches) {\n");
        sb.append("                printf(\"MATCH  ").append(pattern)
          .append("  at %s\\n\", _m);\n");
        sb.append("            }\n");
        sb.append("        }\n");
        sb.append("      }\n");

        return sb.toString();
    }

    private void emitSimpleScanBody(StringBuilder sb, String pattern,
                                    int patternByteLen, String startExpr) {
        sb.append("            Address _cur = ").append(startExpr).append(";\n");
        sb.append("            while (_cur != null) {\n");
        sb.append("                Address _hit = findBytes(_cur, \"")
          .append(pattern).append("\");\n");
        sb.append("                if (_hit == null) break;\n");
        sb.append("                _acc.add(_hit);\n");
        sb.append("                _cur = _hit.add(").append(patternByteLen).append(");\n");
        sb.append("            }\n");
    }

    private void emitInFunctionBody(StringBuilder sb, String pattern,
                                    int patternByteLen, String fnName) {
        sb.append("            java.util.List<Symbol> _candidates = new java.util.ArrayList<>();\n");
        sb.append("            SymbolIterator _symIter = currentProgram.getSymbolTable().getSymbols(\"")
          .append(fnName).append("\");\n");
        sb.append("            while (_symIter.hasNext()) _candidates.add(_symIter.next());\n");
        sb.append("            if (_candidates.size() == 0) {\n");
        sb.append("                printf(\"function not found: ").append(fnName).append("\\n\");\n");
        sb.append("            } else if (_candidates.size() > 1) {\n");
        sb.append("                printf(\"ambiguous: %d functions named ")
          .append(fnName).append("\\n\", _candidates.size());\n");
        sb.append("            } else {\n");
        sb.append("                Function _fn = getFunctionContaining(_candidates.get(0).getAddress());\n");
        sb.append("                if (_fn == null) {\n");
        sb.append("                    printf(\"symbol ").append(fnName)
          .append(" not inside a function\\n\");\n");
        sb.append("                } else {\n");
        sb.append("                    AddressRangeIterator _ranges = _fn.getBody().getAddressRanges();\n");
        sb.append("                    while (_ranges.hasNext()) {\n");
        sb.append("                        AddressRange _range = _ranges.next();\n");
        sb.append("                        Address _cur = _range.getMinAddress();\n");
        sb.append("                        while (_cur != null && _cur.compareTo(_range.getMaxAddress()) <= 0) {\n");
        sb.append("                            Address _hit = findBytes(_cur, \"")
          .append(pattern).append("\");\n");
        sb.append("                            if (_hit == null) break;\n");
        sb.append("                            if (_hit.compareTo(_range.getMaxAddress()) > 0) break;\n");
        sb.append("                            _acc.add(_hit);\n");
        sb.append("                            _cur = _hit.add(").append(patternByteLen).append(");\n");
        sb.append("                        }\n");
        sb.append("                    }\n");
        sb.append("                }\n");
        sb.append("            }\n");
    }

    private String stripQuotes(String s) {
        return s.substring(1, s.length() - 1);
    }
}
