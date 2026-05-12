import ghidra.app.script.GhidraScript;
import ghidra.program.model.symbol.*;
import ghidra.program.model.listing.*;
import ghidra.program.model.address.*;
import java.util.HashMap;
import java.util.Map;

public class CacheReuseDemo extends GhidraScript {
    public void run() throws Exception {
      Map<String, Address[]> _byteCache = new HashMap<>();
      {
        // --- find bytes "4D 5A" from 0x00400000 ---
        String _key = "4D 5A@from=0x00400000";
        Address[] _matches = _byteCache.get(_key);
        if (_matches == null) {
            java.util.List<Address> _acc = new java.util.ArrayList<>();
            Address _cur = toAddr("0x00400000");
            while (_cur != null) {
                Address _hit = findBytes(_cur, "4D 5A");
                if (_hit == null) break;
                _acc.add(_hit);
                _cur = _hit.add(2);
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
      {
        // --- find bytes "4D 5A" from 0x00400000 ---
        String _key = "4D 5A@from=0x00400000";
        Address[] _matches = _byteCache.get(_key);
        if (_matches == null) {
            java.util.List<Address> _acc = new java.util.ArrayList<>();
            Address _cur = toAddr("0x00400000");
            while (_cur != null) {
                Address _hit = findBytes(_cur, "4D 5A");
                if (_hit == null) break;
                _acc.add(_hit);
                _cur = _hit.add(2);
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
}
