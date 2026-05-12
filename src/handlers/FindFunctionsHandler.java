package binquery.handlers;

import binquery.BinQueryParser.FindFunctionsContext;
import binquery.BinQueryParser.FunctionPredicateContext;
import binquery.BinQueryParser.PredXrefsContext;
import binquery.BinQueryParser.PredNamedContext;
import binquery.BinQueryParser.PredLocationOnlyContext;
import binquery.BinQueryParser.LocationFilterContext;
import binquery.BinQueryParser.CompareOpContext;
import binquery.error.SemanticException;

// find functions <predicate>
//
//   predXrefs        : WHERE XREFS compareOp INT locationFilter?
//   predNamed        : NAMED STRING locationFilter?
//   predLocationOnly : locationFilter
public class FindFunctionsHandler {

    public void validate(FindFunctionsContext ctx) {
        FunctionPredicateContext pred = ctx.functionPredicate();
        if (pred instanceof PredNamedContext) {
            String pat = stripQuotes(((PredNamedContext) pred).STRING().getText());
            if (pat.isBlank()) {
                throw new SemanticException(
                    ctx.getStart().getLine(),
                    "function name pattern cannot be empty"
                );
            }
        }
    }

    public String emit(FindFunctionsContext ctx, String ambientScope) {
        FunctionPredicateContext pred = ctx.functionPredicate();
        if (pred instanceof PredXrefsContext) {
            return emitXrefs((PredXrefsContext) pred, ambientScope);
        }
        if (pred instanceof PredNamedContext) {
            return emitNamed((PredNamedContext) pred, ambientScope);
        }
        if (pred instanceof PredLocationOnlyContext) {
            return emitLocationOnly((PredLocationOnlyContext) pred, ambientScope);
        }
        return "";
    }

    private void appendScopeCheck(StringBuilder sb, String ambientScope) {
        if (ambientScope != null) {
            sb.append("            if (!").append(ambientScope)
              .append(".contains(_fn.getEntryPoint())) continue;\n");
        }
    }

    private String emitXrefs(PredXrefsContext px, String ambientScope) {
        String op = compareOpText(px.compareOp());
        String n = px.INT().getText();
        LocFlags lf = locFlags(px.locationFilter());

        StringBuilder sb = new StringBuilder();
        sb.append("      {\n");
        sb.append("        // --- find functions where xrefs ").append(op).append(" ").append(n);
        appendFilterComment(sb, lf);
        if (ambientScope != null) sb.append(" (ambient ").append(ambientScope).append(")");
        sb.append(" ---\n");

        sb.append("        FunctionIterator _fns = currentProgram.getFunctionManager().getFunctions(true);\n");
        sb.append("        int _hitCount = 0;\n");
        sb.append("        while (_fns.hasNext()) {\n");
        sb.append("            Function _fn = _fns.next();\n");
        appendFilterCheck(sb, lf);
        appendScopeCheck(sb, ambientScope);
        sb.append("            int _count = getReferencesTo(_fn.getEntryPoint()).length;\n");
        sb.append("            if (_count ").append(op).append(" ").append(n).append(") {\n");
        sb.append("                printf(\"FN  %s  xrefs=%d  at %s")
          .append(lf.suffix).append("\\n\", _fn.getName(), _count, _fn.getEntryPoint().toString());\n");
        sb.append("                _hitCount++;\n");
        sb.append("            }\n");
        sb.append("        }\n");
        sb.append("        if (_hitCount == 0) {\n");
        sb.append("            printf(\"[findFunctions] no matches\\n\");\n");
        sb.append("        }\n");
        sb.append("      }\n");
        return sb.toString();
    }

    private String emitNamed(PredNamedContext pn, String ambientScope) {
        String pat = stripQuotes(pn.STRING().getText());
        LocFlags lf = locFlags(pn.locationFilter());

        StringBuilder sb = new StringBuilder();
        sb.append("      {\n");
        sb.append("        // --- find functions named \"").append(pat).append("\"");
        appendFilterComment(sb, lf);
        if (ambientScope != null) sb.append(" (ambient ").append(ambientScope).append(")");
        sb.append(" ---\n");

        sb.append("        FunctionIterator _fns = currentProgram.getFunctionManager().getFunctions(true);\n");
        sb.append("        int _hitCount = 0;\n");
        sb.append("        while (_fns.hasNext()) {\n");
        sb.append("            Function _fn = _fns.next();\n");
        appendFilterCheck(sb, lf);
        appendScopeCheck(sb, ambientScope);
        sb.append("            if (_fn.getName().contains(\"").append(pat).append("\")) {\n");
        sb.append("                printf(\"FN  %s  at %s").append(lf.suffix)
          .append("\\n\", _fn.getName(), _fn.getEntryPoint().toString());\n");
        sb.append("                _hitCount++;\n");
        sb.append("            }\n");
        sb.append("        }\n");
        sb.append("        if (_hitCount == 0) {\n");
        sb.append("            printf(\"[findFunctions] no matches\\n\");\n");
        sb.append("        }\n");
        sb.append("      }\n");
        return sb.toString();
    }

    private String emitLocationOnly(PredLocationOnlyContext pl, String ambientScope) {
        LocFlags lf = locFlags(pl.locationFilter());

        StringBuilder sb = new StringBuilder();
        sb.append("      {\n");
        sb.append("        // --- find functions");
        appendFilterComment(sb, lf);
        if (ambientScope != null) sb.append(" (ambient ").append(ambientScope).append(")");
        sb.append(" ---\n");

        sb.append("        FunctionIterator _fns = currentProgram.getFunctionManager().getFunctions(true);\n");
        sb.append("        int _hitCount = 0;\n");
        sb.append("        while (_fns.hasNext()) {\n");
        sb.append("            Function _fn = _fns.next();\n");
        appendFilterCheck(sb, lf);
        appendScopeCheck(sb, ambientScope);
        sb.append("            printf(\"FN  %s  at %s").append(lf.suffix)
          .append("\\n\", _fn.getName(), _fn.getEntryPoint().toString());\n");
        sb.append("            _hitCount++;\n");
        sb.append("        }\n");
        sb.append("        if (_hitCount == 0) {\n");
        sb.append("            printf(\"[findFunctions] no matches\\n\");\n");
        sb.append("        }\n");
        sb.append("      }\n");
        return sb.toString();
    }

    // ---------- helpers ----------

    private static class LocFlags {
        boolean external;
        boolean internal;
        String suffix = "";   // " [EXT]" when external filter active
    }

    private LocFlags locFlags(LocationFilterContext loc) {
        LocFlags lf = new LocFlags();
        if (loc != null) {
            lf.external = loc.EXTERNAL() != null;
            lf.internal = loc.INTERNAL() != null;
            if (lf.external) lf.suffix = " [EXT]";
        }
        return lf;
    }

    private void appendFilterComment(StringBuilder sb, LocFlags lf) {
        if (lf.external) sb.append(" external");
        if (lf.internal) sb.append(" internal");
    }

    private void appendFilterCheck(StringBuilder sb, LocFlags lf) {
        if (lf.external) {
            sb.append("            if (!_fn.isExternal()) continue;\n");
        } else if (lf.internal) {
            sb.append("            if (_fn.isExternal()) continue;\n");
        }
    }

    private String compareOpText(CompareOpContext op) {
        if (op.GE() != null) return ">=";
        if (op.LE() != null) return "<=";
        if (op.EQ() != null) return "==";
        if (op.GT() != null) return ">";
        if (op.LT() != null) return "<";
        return ">";
    }

    private String stripQuotes(String s) {
        return s.substring(1, s.length() - 1);
    }
}
