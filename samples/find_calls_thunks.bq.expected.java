import ghidra.app.script.GhidraScript;
import ghidra.program.model.symbol.*;
import ghidra.program.model.listing.*;
import ghidra.program.model.address.*;
import ghidra.program.util.string.FoundString;
import java.util.HashMap;
import java.util.Map;

public class FindCallsThunksDemo extends GhidraScript {
    public void run() throws Exception {
      Map<String, Address[]> _byteCache = new HashMap<>();
      Map<String, java.util.List<FoundString>> _stringCache = new HashMap<>();
      {
        // --- find calls to "malloc" through thunks ---
        java.util.LinkedHashSet<Address> _direct = new java.util.LinkedHashSet<>();
        java.util.LinkedHashSet<Address> _thunked = new java.util.LinkedHashSet<>();
        java.util.HashSet<Address> _seenFrom = new java.util.HashSet<>();
        SymbolIterator _syms = currentProgram.getSymbolTable().getSymbols("malloc");
        while (_syms.hasNext()) {
            Symbol _sym = _syms.next();
            _direct.add(_sym.getAddress());
            Function _candidate = getFunctionContaining(_sym.getAddress());
            if (_candidate == null) continue;
            Function _underlying = _candidate.isThunk()
                ? _candidate.getThunkedFunction(true)
                : _candidate;
            if (_underlying == null) continue;
            _thunked.add(_underlying.getEntryPoint());
            Address[] _thunkAddrs = _underlying.getFunctionThunkAddresses(true);
            if (_thunkAddrs != null) {
                for (Address _ta : _thunkAddrs) _thunked.add(_ta);
            }
        }
        _thunked.removeAll(_direct);
        int _hitCount = 0;
        for (Address _t : _direct) {
            for (Reference _ref : getReferencesTo(_t)) {
                if (!_ref.getReferenceType().isCall()) continue;
                Address _from = _ref.getFromAddress();
                if (!_seenFrom.add(_from)) continue;
                Function _fn = getFunctionContaining(_from);
                String _fnName = (_fn != null) ? _fn.getName() : "<no-fn>";
                printf("CALL  to malloc  at %s  in %s\n", _from.toString(), _fnName);
                _hitCount++;
            }
        }
        for (Address _t : _thunked) {
            for (Reference _ref : getReferencesTo(_t)) {
                if (!_ref.getReferenceType().isCall()) continue;
                Address _from = _ref.getFromAddress();
                if (!_seenFrom.add(_from)) continue;
                Function _fn = getFunctionContaining(_from);
                String _fnName = (_fn != null) ? _fn.getName() : "<no-fn>";
                printf("CALL  to malloc  at %s  in %s [thunk]\n", _from.toString(), _fnName);
                _hitCount++;
            }
        }
        if (_hitCount == 0) {
            printf("[findCalls] no calls to: malloc\n");
        }
      }
      {
        // --- find calls to "CreateFileA" through thunks external ---
        java.util.LinkedHashSet<Address> _direct = new java.util.LinkedHashSet<>();
        java.util.LinkedHashSet<Address> _thunked = new java.util.LinkedHashSet<>();
        java.util.HashSet<Address> _seenFrom = new java.util.HashSet<>();
        SymbolIterator _syms = currentProgram.getSymbolTable().getSymbols("CreateFileA");
        while (_syms.hasNext()) {
            Symbol _sym = _syms.next();
            if (!_sym.isExternal()) continue;
            _direct.add(_sym.getAddress());
            Function _candidate = getFunctionContaining(_sym.getAddress());
            if (_candidate == null) continue;
            Function _underlying = _candidate.isThunk()
                ? _candidate.getThunkedFunction(true)
                : _candidate;
            if (_underlying == null) continue;
            _thunked.add(_underlying.getEntryPoint());
            Address[] _thunkAddrs = _underlying.getFunctionThunkAddresses(true);
            if (_thunkAddrs != null) {
                for (Address _ta : _thunkAddrs) _thunked.add(_ta);
            }
        }
        _thunked.removeAll(_direct);
        int _hitCount = 0;
        for (Address _t : _direct) {
            for (Reference _ref : getReferencesTo(_t)) {
                if (!_ref.getReferenceType().isCall()) continue;
                Address _from = _ref.getFromAddress();
                if (!_seenFrom.add(_from)) continue;
                Function _fn = getFunctionContaining(_from);
                String _fnName = (_fn != null) ? _fn.getName() : "<no-fn>";
                printf("CALL  to CreateFileA  at %s  in %s [EXT]\n", _from.toString(), _fnName);
                _hitCount++;
            }
        }
        for (Address _t : _thunked) {
            for (Reference _ref : getReferencesTo(_t)) {
                if (!_ref.getReferenceType().isCall()) continue;
                Address _from = _ref.getFromAddress();
                if (!_seenFrom.add(_from)) continue;
                Function _fn = getFunctionContaining(_from);
                String _fnName = (_fn != null) ? _fn.getName() : "<no-fn>";
                printf("CALL  to CreateFileA  at %s  in %s [EXT] [thunk]\n", _from.toString(), _fnName);
                _hitCount++;
            }
        }
        if (_hitCount == 0) {
            printf("[findCalls] no calls to: CreateFileA\n");
        }
      }
      {
        // --- find calls to "_strlen" through thunks internal ---
        java.util.LinkedHashSet<Address> _direct = new java.util.LinkedHashSet<>();
        java.util.LinkedHashSet<Address> _thunked = new java.util.LinkedHashSet<>();
        java.util.HashSet<Address> _seenFrom = new java.util.HashSet<>();
        SymbolIterator _syms = currentProgram.getSymbolTable().getSymbols("_strlen");
        while (_syms.hasNext()) {
            Symbol _sym = _syms.next();
            if (_sym.isExternal()) continue;
            _direct.add(_sym.getAddress());
            Function _candidate = getFunctionContaining(_sym.getAddress());
            if (_candidate == null) continue;
            Function _underlying = _candidate.isThunk()
                ? _candidate.getThunkedFunction(true)
                : _candidate;
            if (_underlying == null) continue;
            _thunked.add(_underlying.getEntryPoint());
            Address[] _thunkAddrs = _underlying.getFunctionThunkAddresses(true);
            if (_thunkAddrs != null) {
                for (Address _ta : _thunkAddrs) _thunked.add(_ta);
            }
        }
        _thunked.removeAll(_direct);
        int _hitCount = 0;
        for (Address _t : _direct) {
            for (Reference _ref : getReferencesTo(_t)) {
                if (!_ref.getReferenceType().isCall()) continue;
                Address _from = _ref.getFromAddress();
                if (!_seenFrom.add(_from)) continue;
                Function _fn = getFunctionContaining(_from);
                String _fnName = (_fn != null) ? _fn.getName() : "<no-fn>";
                printf("CALL  to _strlen  at %s  in %s\n", _from.toString(), _fnName);
                _hitCount++;
            }
        }
        for (Address _t : _thunked) {
            for (Reference _ref : getReferencesTo(_t)) {
                if (!_ref.getReferenceType().isCall()) continue;
                Address _from = _ref.getFromAddress();
                if (!_seenFrom.add(_from)) continue;
                Function _fn = getFunctionContaining(_from);
                String _fnName = (_fn != null) ? _fn.getName() : "<no-fn>";
                printf("CALL  to _strlen  at %s  in %s [thunk]\n", _from.toString(), _fnName);
                _hitCount++;
            }
        }
        if (_hitCount == 0) {
            printf("[findCalls] no calls to: _strlen\n");
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
