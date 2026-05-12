import ghidra.app.script.GhidraScript;
import ghidra.program.model.symbol.*;
import ghidra.program.model.listing.*;
import ghidra.program.model.address.*;
import ghidra.program.util.string.FoundString;
import java.util.HashMap;
import java.util.Map;

public class FindCallsDemo extends GhidraScript {
    public void run() throws Exception {
      Map<String, Address[]> _byteCache = new HashMap<>();
      Map<String, java.util.List<FoundString>> _stringCache = new HashMap<>();
      {
        // --- find calls to "malloc" ---
        int _hitCount = 0;
        SymbolIterator _syms = currentProgram.getSymbolTable().getSymbols("malloc");
        while (_syms.hasNext()) {
            Symbol _sym = _syms.next();
            Reference[] _refs = getReferencesTo(_sym.getAddress());
            for (Reference _ref : _refs) {
                if (!_ref.getReferenceType().isCall()) continue;
                Address _from = _ref.getFromAddress();
                Function _fn = getFunctionContaining(_from);
                String _fnName = (_fn != null) ? _fn.getName() : "<no-fn>";
                printf("CALL  to malloc  at %s  in %s\n", _from.toString(), _fnName);
                _hitCount++;
            }
        }
        if (_hitCount == 0) {
            printf("[findCalls] no calls to: malloc\n");
        }
      }
      {
        // --- find calls to "CreateFileA" external ---
        int _hitCount = 0;
        SymbolIterator _syms = currentProgram.getSymbolTable().getSymbols("CreateFileA");
        while (_syms.hasNext()) {
            Symbol _sym = _syms.next();
            if (!_sym.isExternal()) continue;
            Reference[] _refs = getReferencesTo(_sym.getAddress());
            for (Reference _ref : _refs) {
                if (!_ref.getReferenceType().isCall()) continue;
                Address _from = _ref.getFromAddress();
                Function _fn = getFunctionContaining(_from);
                String _fnName = (_fn != null) ? _fn.getName() : "<no-fn>";
                printf("CALL  to CreateFileA  at %s  in %s [EXT]\n", _from.toString(), _fnName);
                _hitCount++;
            }
        }
        if (_hitCount == 0) {
            printf("[findCalls] no calls to: CreateFileA\n");
        }
      }
    }

    private String _renderStr(String s) {
        StringBuilder sb = new StringBuilder();
        int limit = Math.min(s.length(), 80);
        for (int i = 0; i < limit; i++) {
            char c = s.charAt(i);
            switch (c) {
                case '\n': sb.append("\\n"); break;
                case '\r': sb.append("\\r"); break;
                case '\t': sb.append("\\t"); break;
                case '\\': sb.append("\\\\"); break;
                case '"':  sb.append("\\\""); break;
                default:    sb.append(c);
            }
        }
        if (s.length() > 80) sb.append("...");
        return sb.toString();
    }
}
