import ghidra.app.script.GhidraScript;
import ghidra.program.model.symbol.*;
import ghidra.program.model.listing.*;
import ghidra.program.model.address.*;
import ghidra.program.util.string.FoundString;
import java.util.HashMap;
import java.util.Map;

public class FindInFunctionDemo extends GhidraScript {
    public void run() throws Exception {
      Map<String, Address[]> _byteCache = new HashMap<>();
      Map<String, java.util.List<FoundString>> _stringCache = new HashMap<>();
      {
        // --- find bytes "4D 5A" in function "main" ---
        String _key = "4D 5A@infn=main";
        Address[] _matches = _byteCache.get(_key);
        if (_matches == null) {
            java.util.List<Address> _acc = new java.util.ArrayList<>();
            java.util.List<Symbol> _candidates = new java.util.ArrayList<>();
            SymbolIterator _symIter = currentProgram.getSymbolTable().getSymbols("main");
            while (_symIter.hasNext()) _candidates.add(_symIter.next());
            if (_candidates.size() == 0) {
                printf("function not found: main\n");
            } else if (_candidates.size() > 1) {
                printf("ambiguous: %d functions named main\n", _candidates.size());
            } else {
                Function _fn = getFunctionContaining(_candidates.get(0).getAddress());
                if (_fn == null) {
                    printf("symbol main not inside a function\n");
                } else {
                    AddressRangeIterator _ranges = _fn.getBody().getAddressRanges();
                    while (_ranges.hasNext()) {
                        AddressRange _range = _ranges.next();
                        Address _cur = _range.getMinAddress();
                        while (_cur != null && _cur.compareTo(_range.getMaxAddress()) <= 0) {
                            Address _hit = findBytes(_cur, "4D 5A");
                            if (_hit == null) break;
                            if (_hit.compareTo(_range.getMaxAddress()) > 0) break;
                            _acc.add(_hit);
                            _cur = _hit.add(2);
                        }
                    }
                }
            }
            _matches = _acc.toArray(new Address[0]);
            _byteCache.put(_key, _matches);
        }
        if (_matches.length == 0) {
            printf("[findBytes] no matches for pattern: 4D 5A\n");
        } else {
            for (Address _m : _matches) {
                printf("MATCH  4D 5A  at %s\n", _m);
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
