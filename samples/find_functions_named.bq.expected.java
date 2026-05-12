import ghidra.app.script.GhidraScript;
import ghidra.program.model.symbol.*;
import ghidra.program.model.listing.*;
import ghidra.program.model.address.*;
import java.util.HashMap;
import java.util.Map;

public class FindFunctionsNamedDemo extends GhidraScript {
    public void run() throws Exception {
      Map<String, Address[]> _byteCache = new HashMap<>();
      {
        // --- find functions named "sub_" ---
        FunctionIterator _fns = currentProgram.getFunctionManager().getFunctions(true);
        int _hitCount = 0;
        while (_fns.hasNext()) {
            Function _fn = _fns.next();
            if (_fn.getName().contains("sub_")) {
                printf("FN  %s  at %s\n", _fn.getName(), _fn.getEntryPoint().toString());
                _hitCount++;
            }
        }
        if (_hitCount == 0) {
            printf("[findFunctions] no matches\n");
        }
      }
      {
        // --- find functions named "Create" external ---
        FunctionIterator _fns = currentProgram.getFunctionManager().getFunctions(true);
        int _hitCount = 0;
        while (_fns.hasNext()) {
            Function _fn = _fns.next();
            if (!_fn.isExternal()) continue;
            if (_fn.getName().contains("Create")) {
                printf("FN  %s  at %s [EXT]\n", _fn.getName(), _fn.getEntryPoint().toString());
                _hitCount++;
            }
        }
        if (_hitCount == 0) {
            printf("[findFunctions] no matches\n");
        }
      }
    }
}
