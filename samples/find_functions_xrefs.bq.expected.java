import ghidra.app.script.GhidraScript;
import ghidra.program.model.symbol.*;
import ghidra.program.model.listing.*;
import ghidra.program.model.address.*;
import ghidra.program.util.string.FoundString;
import java.util.HashMap;
import java.util.Map;

public class FindFunctionsXrefsDemo extends GhidraScript {
    public void run() throws Exception {
      Map<String, Address[]> _byteCache = new HashMap<>();
      Map<String, java.util.List<FoundString>> _stringCache = new HashMap<>();
      {
        // --- find functions where xrefs > 5 ---
        FunctionIterator _fns = currentProgram.getFunctionManager().getFunctions(true);
        int _hitCount = 0;
        while (_fns.hasNext()) {
            Function _fn = _fns.next();
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
      {
        // --- find functions where xrefs >= 10 ---
        FunctionIterator _fns = currentProgram.getFunctionManager().getFunctions(true);
        int _hitCount = 0;
        while (_fns.hasNext()) {
            Function _fn = _fns.next();
            int _count = getReferencesTo(_fn.getEntryPoint()).length;
            if (_count >= 10) {
                printf("FN  %s  xrefs=%d  at %s\n", _fn.getName(), _count, _fn.getEntryPoint().toString());
                _hitCount++;
            }
        }
        if (_hitCount == 0) {
            printf("[findFunctions] no matches\n");
        }
      }
      {
        // --- find functions where xrefs == 0 ---
        FunctionIterator _fns = currentProgram.getFunctionManager().getFunctions(true);
        int _hitCount = 0;
        while (_fns.hasNext()) {
            Function _fn = _fns.next();
            int _count = getReferencesTo(_fn.getEntryPoint()).length;
            if (_count == 0) {
                printf("FN  %s  xrefs=%d  at %s\n", _fn.getName(), _count, _fn.getEntryPoint().toString());
                _hitCount++;
            }
        }
        if (_hitCount == 0) {
            printf("[findFunctions] no matches\n");
        }
      }
      {
        // --- find functions where xrefs <= 1 ---
        FunctionIterator _fns = currentProgram.getFunctionManager().getFunctions(true);
        int _hitCount = 0;
        while (_fns.hasNext()) {
            Function _fn = _fns.next();
            int _count = getReferencesTo(_fn.getEntryPoint()).length;
            if (_count <= 1) {
                printf("FN  %s  xrefs=%d  at %s\n", _fn.getName(), _count, _fn.getEntryPoint().toString());
                _hitCount++;
            }
        }
        if (_hitCount == 0) {
            printf("[findFunctions] no matches\n");
        }
      }
      {
        // --- find functions where xrefs < 2 ---
        FunctionIterator _fns = currentProgram.getFunctionManager().getFunctions(true);
        int _hitCount = 0;
        while (_fns.hasNext()) {
            Function _fn = _fns.next();
            int _count = getReferencesTo(_fn.getEntryPoint()).length;
            if (_count < 2) {
                printf("FN  %s  xrefs=%d  at %s\n", _fn.getName(), _count, _fn.getEntryPoint().toString());
                _hitCount++;
            }
        }
        if (_hitCount == 0) {
            printf("[findFunctions] no matches\n");
        }
      }
      {
        // --- find functions where xrefs > 5 external ---
        FunctionIterator _fns = currentProgram.getFunctionManager().getFunctions(true);
        int _hitCount = 0;
        while (_fns.hasNext()) {
            Function _fn = _fns.next();
            if (!_fn.isExternal()) continue;
            int _count = getReferencesTo(_fn.getEntryPoint()).length;
            if (_count > 5) {
                printf("FN  %s  xrefs=%d  at %s [EXT]\n", _fn.getName(), _count, _fn.getEntryPoint().toString());
                _hitCount++;
            }
        }
        if (_hitCount == 0) {
            printf("[findFunctions] no matches\n");
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
