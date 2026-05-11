import ghidra.app.script.GhidraScript;
import ghidra.program.model.symbol.*;
import ghidra.program.model.listing.*;
import ghidra.program.model.address.*;

public class AuditScript extends GhidraScript {
    public void run() throws Exception {
      {
        // --- find bytes "4D 5A 90 00" from 0x00400000 ---
        Address _startAddr = toAddr("0x00400000");
        Address[] _matches = findBytes(_startAddr, "4D 5A 90 00", 100);
        if (_matches == null || _matches.length == 0) {
            printf("[findBytes] no matches for pattern: 4D 5A 90 00\n");
        } else {
            for (Address _match : _matches) {
                printf("MATCH  4D 5A 90 00  at %s\n", _match);
            }
        }
      }
      {
        // --- find bytes "FF 25" ---
        Address _startAddr = currentProgram.getMinAddress();
        Address[] _matches = findBytes(_startAddr, "FF 25", 100);
        if (_matches == null || _matches.length == 0) {
            printf("[findBytes] no matches for pattern: FF 25\n");
        } else {
            for (Address _match : _matches) {
                printf("MATCH  FF 25  at %s\n", _match);
            }
        }
      }
    }
}
