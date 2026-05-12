package binquery.handlers;

import binquery.BinQueryParser.FindStringsContext;
import binquery.BinQueryParser.EncodingClauseContext;
import binquery.BinQueryParser.StringFilterContext;
import binquery.BinQueryParser.ScopeClauseContext;
import binquery.error.SemanticException;

// find strings minlen N (ascii|unicode)? (containing "L" | matching "R")? (in function "F")?
public class FindStringsHandler {

    public void validate(FindStringsContext ctx, boolean inScopeBlock) {
        int line = ctx.getStart().getLine();
        int minlen = Integer.parseInt(ctx.INT().getText());
        if (minlen < 1) {
            throw new SemanticException(line, "minlen must be >= 1, got " + minlen);
        }

        StringFilterContext sf = ctx.stringFilter();
        if (sf != null) {
            String lit = stripQuotes(sf.STRING().getText());
            if (lit.isEmpty()) {
                throw new SemanticException(line,
                    sf.CONTAINING() != null
                        ? "containing literal cannot be empty"
                        : "matching pattern cannot be empty");
            }
            if (sf.MATCHING() != null) {
                try {
                    java.util.regex.Pattern.compile(lit);
                } catch (java.util.regex.PatternSyntaxException e) {
                    throw new SemanticException(line,
                        "invalid regex in matching clause: " + e.getDescription());
                }
            }
        }

        ScopeClauseContext sc = ctx.scopeClause();
        if (inScopeBlock && sc != null) {
            throw new SemanticException(line,
                "trailing scope clause forbidden inside scope block");
        }
        if (sc != null && stripQuotes(sc.STRING().getText()).isEmpty()) {
            throw new SemanticException(line,
                "function name in IN FUNCTION cannot be empty");
        }
    }

    public String emit(FindStringsContext ctx, String ambientScope) {
        int minlen = Integer.parseInt(ctx.INT().getText());

        EncodingClauseContext ec = ctx.encodingClause();
        boolean asciiOnly   = ec != null && ec.ASCII() != null;
        boolean unicodeOnly = ec != null && ec.UNICODE() != null;

        StringFilterContext sf = ctx.stringFilter();
        boolean isContains = sf != null && sf.CONTAINING() != null;
        boolean isMatching = sf != null && sf.MATCHING() != null;
        String filterLit = sf != null ? stripQuotes(sf.STRING().getText()) : null;

        ScopeClauseContext sc = ctx.scopeClause();
        boolean hasFnScope = sc != null;
        String fnName = hasFnScope ? stripQuotes(sc.STRING().getText()) : null;

        String scopeMarker;
        if (ambientScope != null) scopeMarker = "inscope=" + ambientScope;
        else if (hasFnScope)      scopeMarker = "infn=" + fnName;
        else                      scopeMarker = "all";
        String cacheKey = scopeMarker + "@" + minlen + (asciiOnly ? "@ascii" : "");

        String filterTag;
        if (isContains)      filterTag = "contains,";
        else if (isMatching) filterTag = "regex,";
        else                 filterTag = "";
        String headerFmt = "STR[" + filterTag + "enc=%s]";

        StringBuilder sb = new StringBuilder();
        sb.append("      {\n");
        sb.append("        // --- find strings minlen ").append(minlen);
        if (asciiOnly)   sb.append(" ascii");
        if (unicodeOnly) sb.append(" unicode");
        if (isContains)  sb.append(" containing \"").append(filterLit).append("\"");
        if (isMatching)  sb.append(" matching \"").append(filterLit).append("\"");
        if (hasFnScope)  sb.append(" in function \"").append(fnName).append("\"");
        if (ambientScope != null) sb.append(" (ambient ").append(ambientScope).append(")");
        sb.append(" ---\n");

        sb.append("        String _key = \"").append(cacheKey).append("\";\n");
        sb.append("        java.util.List<FoundString> _strs = _stringCache.get(_key);\n");
        sb.append("        if (_strs == null) {\n");

        if (ambientScope != null) {
            sb.append("            _strs = findStrings(").append(ambientScope).append(", ")
              .append(minlen).append(", 1, true, ")
              .append(asciiOnly ? "false" : "true").append(");\n");
        } else if (hasFnScope) {
            emitFunctionScopeResolution(sb, fnName, minlen, asciiOnly);
        } else {
            sb.append("            _strs = findStrings(currentProgram.getMemory(), ")
              .append(minlen).append(", 1, true, ")
              .append(asciiOnly ? "false" : "true").append(");\n");
        }

        sb.append("            if (_strs == null) _strs = new java.util.ArrayList<>();\n");
        sb.append("            _stringCache.put(_key, _strs);\n");
        sb.append("        }\n");

        sb.append("        int _hitCount = 0;\n");
        if (isMatching) {
            sb.append("        java.util.regex.Pattern _re = java.util.regex.Pattern.compile(\"")
              .append(escapeJavaStringLiteral(filterLit)).append("\");\n");
        }
        sb.append("        for (FoundString _s : _strs) {\n");
        sb.append("            String _val = _s.getString(currentProgram.getMemory());\n");
        sb.append("            if (_val == null) continue;\n");
        sb.append("            String _enc = _s.getDataType().getName().equals(\"unicode\") ? \"utf16\" : \"ascii\";\n");
        if (unicodeOnly) {
            sb.append("            if (!_enc.equals(\"utf16\")) continue;\n");
        }
        if (isContains) {
            sb.append("            if (!_val.contains(\"")
              .append(escapeJavaStringLiteral(filterLit)).append("\")) continue;\n");
        } else if (isMatching) {
            sb.append("            if (!_re.matcher(_val).find()) continue;\n");
        }
        sb.append("            String _disp = _renderStr(_val);\n");
        sb.append("            printf(\"").append(headerFmt)
          .append("  \\\"%s\\\"  len=%d  at %s\\n\", _enc, _disp, _s.getLength(), _s.getAddress().toString());\n");
        sb.append("            _hitCount++;\n");
        sb.append("        }\n");
        sb.append("        if (_hitCount == 0) {\n");
        sb.append("            printf(\"[findStrings] no matches\\n\");\n");
        sb.append("        }\n");
        sb.append("      }\n");

        return sb.toString();
    }

    private void emitFunctionScopeResolution(StringBuilder sb, String fnName,
                                             int minlen, boolean asciiOnly) {
        sb.append("            java.util.List<Symbol> _candidates = new java.util.ArrayList<>();\n");
        sb.append("            SymbolIterator _symIter = currentProgram.getSymbolTable().getSymbols(\"")
          .append(fnName).append("\");\n");
        sb.append("            while (_symIter.hasNext()) _candidates.add(_symIter.next());\n");
        sb.append("            if (_candidates.size() == 0) {\n");
        sb.append("                printf(\"function not found: ").append(fnName).append("\\n\");\n");
        sb.append("                _strs = new java.util.ArrayList<>();\n");
        sb.append("            } else if (_candidates.size() > 1) {\n");
        sb.append("                printf(\"ambiguous: %d functions named ")
          .append(fnName).append("\\n\", _candidates.size());\n");
        sb.append("                _strs = new java.util.ArrayList<>();\n");
        sb.append("            } else {\n");
        sb.append("                Function _fn = getFunctionContaining(_candidates.get(0).getAddress());\n");
        sb.append("                if (_fn == null) {\n");
        sb.append("                    printf(\"symbol ").append(fnName)
          .append(" not inside a function\\n\");\n");
        sb.append("                    _strs = new java.util.ArrayList<>();\n");
        sb.append("                } else {\n");
        sb.append("                    _strs = findStrings(_fn.getBody(), ")
          .append(minlen).append(", 1, true, ")
          .append(asciiOnly ? "false" : "true").append(");\n");
        sb.append("                }\n");
        sb.append("            }\n");
    }

    // Escape only characters significant to a Java string literal (quotes + backslash).
    // Embedded newlines in DSL strings are already prohibited by the lexer rule.
    private String escapeJavaStringLiteral(String s) {
        StringBuilder out = new StringBuilder(s.length());
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == '\\') out.append("\\\\");
            else if (c == '"') out.append("\\\"");
            else out.append(c);
        }
        return out.toString();
    }

    private String stripQuotes(String s) {
        return s.substring(1, s.length() - 1);
    }
}
