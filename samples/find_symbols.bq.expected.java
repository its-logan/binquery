import ghidra.app.script.GhidraScript;
import ghidra.program.model.symbol.*;
import ghidra.program.model.listing.*;
import ghidra.program.model.address.*;
import java.util.HashMap;
import java.util.Map;

public class FindSymbolsDemo extends GhidraScript {
    public void run() throws Exception {
      Map<String, Address[]> _byteCache = new HashMap<>();
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
        // --- find symbols "printf" external ---
        SymbolIterator _syms = currentProgram.getSymbolTable().getSymbols("printf");
        int _hitCount = 0;
        while (_syms.hasNext()) {
            Symbol _sym = _syms.next();
            if (!_sym.isExternal()) continue;
            printf("SYM  %s  at %s [EXT]\n", _sym.getName(), _sym.getAddress().toString());
            _hitCount++;
        }
        if (_hitCount == 0) {
            printf("[findSymbols] no symbols matching: printf\n");
        }
      }
      {
        // --- find functions external ---
        FunctionIterator _fns = currentProgram.getFunctionManager().getFunctions(true);
        int _hitCount = 0;
        while (_fns.hasNext()) {
            Function _fn = _fns.next();
            if (!_fn.isExternal()) continue;
            printf("FN  %s  at %s [EXT]\n", _fn.getName(), _fn.getEntryPoint().toString());
            _hitCount++;
        }
        if (_hitCount == 0) {
            printf("[findFunctions] no matches\n");
        }
      }
    }
}
