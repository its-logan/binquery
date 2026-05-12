import ghidra.app.script.GhidraScript;
import ghidra.program.model.symbol.*;
import ghidra.program.model.listing.*;
import ghidra.program.model.address.*;
import ghidra.program.model.mem.MemoryBlock;
import ghidra.program.util.string.FoundString;
import java.util.HashMap;
import java.util.Map;

public class ScopeBlocksDemo extends GhidraScript {
    public void run() throws Exception {
      Map<String, Address[]> _byteCache = new HashMap<>();
      Map<String, java.util.List<FoundString>> _stringCache = new HashMap<>();
      {
        // --- find symbols "_stdout" ---
        SymbolIterator _syms = currentProgram.getSymbolTable().getSymbols("_stdout");
        int _hitCount = 0;
        while (_syms.hasNext()) {
            Symbol _sym = _syms.next();
            printf("SYM  %s  at %s\n", _sym.getName(), _sym.getAddress().toString());
            _hitCount++;
        }
        if (_hitCount == 0) {
            printf("[findSymbols] no symbols matching: _stdout\n");
        }
      }
      {
        MemoryBlock _blk_1 = currentProgram.getMemory().getBlock(".text");
        AddressSet _scope_inner_1 = new AddressSet();
        if (_blk_1 == null) {
            printf("block not found: .text\n");
        } else {
            _scope_inner_1.add(_blk_1.getStart(), _blk_1.getEnd());
        }
        AddressSetView _scope_1 = _scope_inner_1;
      {
        // --- find bytes "FF 25" (ambient _scope_1) ---
        String _key = "FF 25@inscope=_scope_1";
        Address[] _matches = _byteCache.get(_key);
        if (_matches == null) {
            java.util.List<Address> _acc = new java.util.ArrayList<>();
            AddressRangeIterator _ranges = _scope_1.getAddressRanges();
            while (_ranges.hasNext()) {
                AddressRange _range = _ranges.next();
                Address _cur = _range.getMinAddress();
                while (_cur != null && _cur.compareTo(_range.getMaxAddress()) <= 0) {
                    Address _hit = findBytes(_cur, "FF 25");
                    if (_hit == null) break;
                    if (_hit.compareTo(_range.getMaxAddress()) > 0) break;
                    _acc.add(_hit);
                    _cur = _hit.add(2);
                }
            }
            _matches = _acc.toArray(new Address[0]);
            _byteCache.put(_key, _matches);
        }
        if (_matches.length == 0) {
            printf("[findBytes] no matches for pattern: FF 25\n");
        } else {
            for (Address _m : _matches) {
                printf("MATCH  FF 25  at %s\n", _m);
            }
        }
      }
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
                if (!_scope_1.contains(_from)) continue;
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
        // --- find functions where xrefs > 5 (ambient _scope_1) ---
        FunctionIterator _fns = currentProgram.getFunctionManager().getFunctions(true);
        int _hitCount = 0;
        while (_fns.hasNext()) {
            Function _fn = _fns.next();
            if (!_scope_1.contains(_fn.getEntryPoint())) continue;
            int _count = getReferencesTo(_fn.getEntryPoint()).length;
            if (_count > 5) {
                printf("FN  %s  xrefs=%d  at %s\n", _fn.getName(), _count, _fn.getEntryPoint().toString());
                _hitCount++;
            }
        }
        if (_hitCount == 0) {
            printf("[findFunctions] no matches\n");
        }
      }
      }
      {
        AddressSet _scope_inner_2 = new AddressSet();
        {
            java.util.List<Symbol> _cands_2 = new java.util.ArrayList<>();
            SymbolIterator _si_2 = currentProgram.getSymbolTable().getSymbols("main");
            while (_si_2.hasNext()) _cands_2.add(_si_2.next());
            if (_cands_2.size() == 0) {
                printf("function not found: main\n");
            } else if (_cands_2.size() > 1) {
                printf("ambiguous: %d functions named main\n", _cands_2.size());
            } else {
                Function _fn_2 = getFunctionContaining(_cands_2.get(0).getAddress());
                if (_fn_2 == null) {
                    printf("symbol main not inside a function\n");
                } else {
                    _scope_inner_2.add(_fn_2.getBody());
                }
            }
        }
        AddressSetView _scope_2 = _scope_inner_2;
      {
        // --- find bytes "C3" (ambient _scope_2) ---
        String _key = "C3@inscope=_scope_2";
        Address[] _matches = _byteCache.get(_key);
        if (_matches == null) {
            java.util.List<Address> _acc = new java.util.ArrayList<>();
            AddressRangeIterator _ranges = _scope_2.getAddressRanges();
            while (_ranges.hasNext()) {
                AddressRange _range = _ranges.next();
                Address _cur = _range.getMinAddress();
                while (_cur != null && _cur.compareTo(_range.getMaxAddress()) <= 0) {
                    Address _hit = findBytes(_cur, "C3");
                    if (_hit == null) break;
                    if (_hit.compareTo(_range.getMaxAddress()) > 0) break;
                    _acc.add(_hit);
                    _cur = _hit.add(1);
                }
            }
            _matches = _acc.toArray(new Address[0]);
            _byteCache.put(_key, _matches);
        }
        if (_matches.length == 0) {
            printf("[findBytes] no matches for pattern: C3\n");
        } else {
            for (Address _m : _matches) {
                printf("MATCH  C3  at %s\n", _m);
            }
        }
      }
      {
        // --- find strings minlen 8 (ambient _scope_2) ---
        String _key = "inscope=_scope_2@8";
        java.util.List<FoundString> _strs = _stringCache.get(_key);
        if (_strs == null) {
            _strs = findStrings(_scope_2, 8, 1, true, true);
            if (_strs == null) _strs = new java.util.ArrayList<>();
            _stringCache.put(_key, _strs);
        }
        int _hitCount = 0;
        for (FoundString _s : _strs) {
            String _val = _s.getString(currentProgram.getMemory());
            if (_val == null) continue;
            String _enc = _s.getDataType().getName().equals("unicode") ? "utf16" : "ascii";
            String _disp = _renderStr(_val);
            printf("STR[enc=%s]  \"%s\"  len=%d  at %s\n", _enc, _disp, _s.getLength(), _s.getAddress().toString());
            _hitCount++;
        }
        if (_hitCount == 0) {
            printf("[findStrings] no matches\n");
        }
      }
      }
      {
        MemoryBlock _blk_3 = currentProgram.getMemory().getBlock(".text");
        AddressSet _scope_inner_3 = new AddressSet();
        if (_blk_3 == null) {
            printf("block not found: .text\n");
        } else {
            _scope_inner_3.add(_blk_3.getStart(), _blk_3.getEnd());
        }
        AddressSetView _scope_3 = _scope_inner_3;
      {
        AddressSet _scope_inner_4 = new AddressSet();
        {
            java.util.List<Symbol> _cands_4 = new java.util.ArrayList<>();
            SymbolIterator _si_4 = currentProgram.getSymbolTable().getSymbols("main");
            while (_si_4.hasNext()) _cands_4.add(_si_4.next());
            if (_cands_4.size() == 0) {
                printf("function not found: main\n");
            } else if (_cands_4.size() > 1) {
                printf("ambiguous: %d functions named main\n", _cands_4.size());
            } else {
                Function _fn_4 = getFunctionContaining(_cands_4.get(0).getAddress());
                if (_fn_4 == null) {
                    printf("symbol main not inside a function\n");
                } else {
                    _scope_inner_4.add(_fn_4.getBody());
                }
            }
        }
        AddressSetView _scope_4 = _scope_inner_4.intersect(_scope_3);
      {
        // --- find strings minlen 4 containing "http" (ambient _scope_4) ---
        String _key = "inscope=_scope_4@4";
        java.util.List<FoundString> _strs = _stringCache.get(_key);
        if (_strs == null) {
            _strs = findStrings(_scope_4, 4, 1, true, true);
            if (_strs == null) _strs = new java.util.ArrayList<>();
            _stringCache.put(_key, _strs);
        }
        int _hitCount = 0;
        for (FoundString _s : _strs) {
            String _val = _s.getString(currentProgram.getMemory());
            if (_val == null) continue;
            String _enc = _s.getDataType().getName().equals("unicode") ? "utf16" : "ascii";
            if (!_val.contains("http")) continue;
            String _disp = _renderStr(_val);
            printf("STR[contains,enc=%s]  \"%s\"  len=%d  at %s\n", _enc, _disp, _s.getLength(), _s.getAddress().toString());
            _hitCount++;
        }
        if (_hitCount == 0) {
            printf("[findStrings] no matches\n");
        }
      }
      {
        // --- find calls to "printf" ---
        int _hitCount = 0;
        SymbolIterator _syms = currentProgram.getSymbolTable().getSymbols("printf");
        while (_syms.hasNext()) {
            Symbol _sym = _syms.next();
            Reference[] _refs = getReferencesTo(_sym.getAddress());
            for (Reference _ref : _refs) {
                if (!_ref.getReferenceType().isCall()) continue;
                Address _from = _ref.getFromAddress();
                if (!_scope_4.contains(_from)) continue;
                Function _fn = getFunctionContaining(_from);
                String _fnName = (_fn != null) ? _fn.getName() : "<no-fn>";
                printf("CALL  to printf  at %s  in %s\n", _from.toString(), _fnName);
                _hitCount++;
            }
        }
        if (_hitCount == 0) {
            printf("[findCalls] no calls to: printf\n");
        }
      }
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
