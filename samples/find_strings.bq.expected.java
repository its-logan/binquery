import ghidra.app.script.GhidraScript;
import ghidra.program.model.symbol.*;
import ghidra.program.model.listing.*;
import ghidra.program.model.address.*;
import ghidra.program.util.string.FoundString;
import java.util.HashMap;
import java.util.Map;

public class FindStringsDemo extends GhidraScript {
    public void run() throws Exception {
      Map<String, Address[]> _byteCache = new HashMap<>();
      Map<String, java.util.List<FoundString>> _stringCache = new HashMap<>();
      {
        // --- find strings minlen 8 ---
        String _key = "all@8";
        java.util.List<FoundString> _strs = _stringCache.get(_key);
        if (_strs == null) {
            _strs = findStrings(currentProgram.getMemory(), 8, 1, true, true);
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
      {
        // --- find strings minlen 8 ascii ---
        String _key = "all@8@ascii";
        java.util.List<FoundString> _strs = _stringCache.get(_key);
        if (_strs == null) {
            _strs = findStrings(currentProgram.getMemory(), 8, 1, true, false);
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
      {
        // --- find strings minlen 8 unicode ---
        String _key = "all@8";
        java.util.List<FoundString> _strs = _stringCache.get(_key);
        if (_strs == null) {
            _strs = findStrings(currentProgram.getMemory(), 8, 1, true, true);
            if (_strs == null) _strs = new java.util.ArrayList<>();
            _stringCache.put(_key, _strs);
        }
        int _hitCount = 0;
        for (FoundString _s : _strs) {
            String _val = _s.getString(currentProgram.getMemory());
            if (_val == null) continue;
            String _enc = _s.getDataType().getName().equals("unicode") ? "utf16" : "ascii";
            if (!_enc.equals("utf16")) continue;
            String _disp = _renderStr(_val);
            printf("STR[enc=%s]  \"%s\"  len=%d  at %s\n", _enc, _disp, _s.getLength(), _s.getAddress().toString());
            _hitCount++;
        }
        if (_hitCount == 0) {
            printf("[findStrings] no matches\n");
        }
      }
      {
        // --- find strings minlen 8 containing "http" ---
        String _key = "all@8";
        java.util.List<FoundString> _strs = _stringCache.get(_key);
        if (_strs == null) {
            _strs = findStrings(currentProgram.getMemory(), 8, 1, true, true);
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
        // --- find strings minlen 4 matching "[A-Z]{4,}" ---
        String _key = "all@4";
        java.util.List<FoundString> _strs = _stringCache.get(_key);
        if (_strs == null) {
            _strs = findStrings(currentProgram.getMemory(), 4, 1, true, true);
            if (_strs == null) _strs = new java.util.ArrayList<>();
            _stringCache.put(_key, _strs);
        }
        int _hitCount = 0;
        java.util.regex.Pattern _re = java.util.regex.Pattern.compile("[A-Z]{4,}");
        for (FoundString _s : _strs) {
            String _val = _s.getString(currentProgram.getMemory());
            if (_val == null) continue;
            String _enc = _s.getDataType().getName().equals("unicode") ? "utf16" : "ascii";
            if (!_re.matcher(_val).find()) continue;
            String _disp = _renderStr(_val);
            printf("STR[regex,enc=%s]  \"%s\"  len=%d  at %s\n", _enc, _disp, _s.getLength(), _s.getAddress().toString());
            _hitCount++;
        }
        if (_hitCount == 0) {
            printf("[findStrings] no matches\n");
        }
      }
      {
        // --- find strings minlen 8 in function "main" ---
        String _key = "infn=main@8";
        java.util.List<FoundString> _strs = _stringCache.get(_key);
        if (_strs == null) {
            java.util.List<Symbol> _candidates = new java.util.ArrayList<>();
            SymbolIterator _symIter = currentProgram.getSymbolTable().getSymbols("main");
            while (_symIter.hasNext()) _candidates.add(_symIter.next());
            if (_candidates.size() == 0) {
                printf("function not found: main\n");
                _strs = new java.util.ArrayList<>();
            } else if (_candidates.size() > 1) {
                printf("ambiguous: %d functions named main\n", _candidates.size());
                _strs = new java.util.ArrayList<>();
            } else {
                Function _fn = getFunctionContaining(_candidates.get(0).getAddress());
                if (_fn == null) {
                    printf("symbol main not inside a function\n");
                    _strs = new java.util.ArrayList<>();
                } else {
                    _strs = findStrings(_fn.getBody(), 8, 1, true, true);
                }
            }
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
      {
        // --- find strings minlen 8 ---
        String _key = "all@8";
        java.util.List<FoundString> _strs = _stringCache.get(_key);
        if (_strs == null) {
            _strs = findStrings(currentProgram.getMemory(), 8, 1, true, true);
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
