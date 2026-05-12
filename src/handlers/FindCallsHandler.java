package binquery.handlers;

import binquery.BinQueryParser.FindCallsContext;
import binquery.BinQueryParser.LocationFilterContext;
import binquery.error.SemanticException;

// find calls to "<name>" (through thunks)? (internal|external)?
public class FindCallsHandler {

    public void validate(FindCallsContext ctx) {
        String name = stripQuotes(ctx.STRING().getText());
        if (name.isBlank()) {
            throw new SemanticException(
                ctx.getStart().getLine(),
                "call target name cannot be empty"
            );
        }
    }

    public String emit(FindCallsContext ctx, String ambientScope) {
        String name = stripQuotes(ctx.STRING().getText());
        LocationFilterContext loc = ctx.locationFilter();
        boolean filterExternal = loc != null && loc.EXTERNAL() != null;
        boolean filterInternal = loc != null && loc.INTERNAL() != null;
        boolean walkThunks = ctx.thunkClause() != null;
        String extSuffix = filterExternal ? " [EXT]" : "";

        if (walkThunks) {
            return emitThunkWalking(name, filterExternal, filterInternal, extSuffix, ambientScope);
        }
        return emitDirect(name, filterExternal, filterInternal, extSuffix, ambientScope);
    }

    private String emitDirect(String name, boolean filterExternal,
                              boolean filterInternal, String extSuffix,
                              String ambientScope) {
        StringBuilder sb = new StringBuilder();
        sb.append("      {\n");
        sb.append("        // --- find calls to \"").append(name).append("\"");
        if (filterExternal) sb.append(" external");
        if (filterInternal) sb.append(" internal");
        sb.append(" ---\n");

        sb.append("        int _hitCount = 0;\n");
        sb.append("        SymbolIterator _syms = currentProgram.getSymbolTable().getSymbols(\"")
          .append(name).append("\");\n");
        sb.append("        while (_syms.hasNext()) {\n");
        sb.append("            Symbol _sym = _syms.next();\n");
        appendFilterSkip(sb, filterExternal, filterInternal);
        sb.append("            Reference[] _refs = getReferencesTo(_sym.getAddress());\n");
        sb.append("            for (Reference _ref : _refs) {\n");
        sb.append("                if (!_ref.getReferenceType().isCall()) continue;\n");
        sb.append("                Address _from = _ref.getFromAddress();\n");
        if (ambientScope != null) {
            sb.append("                if (!").append(ambientScope).append(".contains(_from)) continue;\n");
        }
        sb.append("                Function _fn = getFunctionContaining(_from);\n");
        sb.append("                String _fnName = (_fn != null) ? _fn.getName() : \"<no-fn>\";\n");
        sb.append("                printf(\"CALL  to ").append(name)
          .append("  at %s  in %s").append(extSuffix).append("\\n\", _from.toString(), _fnName);\n");
        sb.append("                _hitCount++;\n");
        sb.append("            }\n");
        sb.append("        }\n");
        sb.append("        if (_hitCount == 0) {\n");
        sb.append("            printf(\"[findCalls] no calls to: ").append(name).append("\\n\");\n");
        sb.append("        }\n");
        sb.append("      }\n");
        return sb.toString();
    }

    private String emitThunkWalking(String name, boolean filterExternal,
                                    boolean filterInternal, String extSuffix,
                                    String ambientScope) {
        StringBuilder sb = new StringBuilder();
        sb.append("      {\n");
        sb.append("        // --- find calls to \"").append(name).append("\" through thunks");
        if (filterExternal) sb.append(" external");
        if (filterInternal) sb.append(" internal");
        sb.append(" ---\n");

        sb.append("        java.util.LinkedHashSet<Address> _direct = new java.util.LinkedHashSet<>();\n");
        sb.append("        java.util.LinkedHashSet<Address> _thunked = new java.util.LinkedHashSet<>();\n");
        sb.append("        java.util.HashSet<Address> _seenFrom = new java.util.HashSet<>();\n");

        sb.append("        SymbolIterator _syms = currentProgram.getSymbolTable().getSymbols(\"")
          .append(name).append("\");\n");
        sb.append("        while (_syms.hasNext()) {\n");
        sb.append("            Symbol _sym = _syms.next();\n");
        appendFilterSkip(sb, filterExternal, filterInternal);
        sb.append("            _direct.add(_sym.getAddress());\n");
        sb.append("            Function _candidate = getFunctionContaining(_sym.getAddress());\n");
        sb.append("            if (_candidate == null) continue;\n");
        sb.append("            Function _underlying = _candidate.isThunk()\n");
        sb.append("                ? _candidate.getThunkedFunction(true)\n");
        sb.append("                : _candidate;\n");
        sb.append("            if (_underlying == null) continue;\n");
        sb.append("            _thunked.add(_underlying.getEntryPoint());\n");
        sb.append("            Address[] _thunkAddrs = _underlying.getFunctionThunkAddresses(true);\n");
        sb.append("            if (_thunkAddrs != null) {\n");
        sb.append("                for (Address _ta : _thunkAddrs) _thunked.add(_ta);\n");
        sb.append("            }\n");
        sb.append("        }\n");
        sb.append("        _thunked.removeAll(_direct);\n");

        sb.append("        int _hitCount = 0;\n");

        String scopeCheck = ambientScope != null
            ? "                if (!" + ambientScope + ".contains(_from)) continue;\n"
            : "";

        // Pass 1: direct
        sb.append("        for (Address _t : _direct) {\n");
        sb.append("            for (Reference _ref : getReferencesTo(_t)) {\n");
        sb.append("                if (!_ref.getReferenceType().isCall()) continue;\n");
        sb.append("                Address _from = _ref.getFromAddress();\n");
        sb.append(scopeCheck);
        sb.append("                if (!_seenFrom.add(_from)) continue;\n");
        sb.append("                Function _fn = getFunctionContaining(_from);\n");
        sb.append("                String _fnName = (_fn != null) ? _fn.getName() : \"<no-fn>\";\n");
        sb.append("                printf(\"CALL  to ").append(name)
          .append("  at %s  in %s").append(extSuffix).append("\\n\", _from.toString(), _fnName);\n");
        sb.append("                _hitCount++;\n");
        sb.append("            }\n");
        sb.append("        }\n");

        // Pass 2: thunked
        sb.append("        for (Address _t : _thunked) {\n");
        sb.append("            for (Reference _ref : getReferencesTo(_t)) {\n");
        sb.append("                if (!_ref.getReferenceType().isCall()) continue;\n");
        sb.append("                Address _from = _ref.getFromAddress();\n");
        sb.append(scopeCheck);
        sb.append("                if (!_seenFrom.add(_from)) continue;\n");
        sb.append("                Function _fn = getFunctionContaining(_from);\n");
        sb.append("                String _fnName = (_fn != null) ? _fn.getName() : \"<no-fn>\";\n");
        sb.append("                printf(\"CALL  to ").append(name)
          .append("  at %s  in %s").append(extSuffix).append(" [thunk]\\n\", _from.toString(), _fnName);\n");
        sb.append("                _hitCount++;\n");
        sb.append("            }\n");
        sb.append("        }\n");

        sb.append("        if (_hitCount == 0) {\n");
        sb.append("            printf(\"[findCalls] no calls to: ").append(name).append("\\n\");\n");
        sb.append("        }\n");
        sb.append("      }\n");
        return sb.toString();
    }

    private void appendFilterSkip(StringBuilder sb, boolean ext, boolean intl) {
        if (ext) {
            sb.append("            if (!_sym.isExternal()) continue;\n");
        } else if (intl) {
            sb.append("            if (_sym.isExternal()) continue;\n");
        }
    }

    private String stripQuotes(String s) {
        return s.substring(1, s.length() - 1);
    }
}
