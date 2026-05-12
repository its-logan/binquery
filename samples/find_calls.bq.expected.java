import ghidra.app.script.GhidraScript;
import ghidra.program.model.symbol.*;
import ghidra.program.model.listing.*;
import ghidra.program.model.address.*;
import java.util.HashMap;
import java.util.Map;

public class FindCallsDemo extends GhidraScript {
    public void run() throws Exception {
      Map<String, Address[]> _byteCache = new HashMap<>();
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
}
