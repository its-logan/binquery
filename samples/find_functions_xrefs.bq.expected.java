import ghidra.app.script.GhidraScript;
import ghidra.program.model.symbol.*;
import ghidra.program.model.listing.*;
import ghidra.program.model.address.*;
import java.util.HashMap;
import java.util.Map;

public class FindFunctionsXrefsDemo extends GhidraScript {
    public void run() throws Exception {
      Map<String, Address[]> _byteCache = new HashMap<>();
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
}
