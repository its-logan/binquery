package binquery.handlers;

import binquery.BinQueryParser.FindBytesContext;
import binquery.error.SemanticException;

// find bytes "<hex_pattern>" (from <addr>)?
public class FindBytesHandler {

    private static final int DEFAULT_MATCH_LIMIT = 100;

    public void validate(FindBytesContext ctx) {
        String raw = stripQuotes(ctx.STRING().getText());

        if (raw.isBlank()) {
            throw new SemanticException(
                ctx.getStart().getLine(),
                "byte pattern cannot be empty"
            );
        }

        String[] tokens = raw.strip().split("\\s+");
        for (String token : tokens) {
            if (!token.matches("[0-9A-Fa-f]{2}")) {
                throw new SemanticException(
                    ctx.getStart().getLine(),
                    "invalid byte pattern token \"" + token
                    + "\": expected hex pair like \"4D\", got \"" + token + "\""
                );
            }
        }
    }

    // Wrapped in a block scope so multiple find-bytes queries in one
    // script don't collide on _startAddr / _matches.
    public String emit(FindBytesContext ctx) {
        String pattern = stripQuotes(ctx.STRING().getText());
        boolean hasFrom = ctx.HEX_ADDR() != null;
        String startAddr = hasFrom ? ctx.HEX_ADDR().getText() : null;

        StringBuilder sb = new StringBuilder();
        sb.append("      {\n");
        sb.append("        // --- find bytes \"").append(pattern).append("\"");
        if (hasFrom) sb.append(" from ").append(startAddr);
        sb.append(" ---\n");

        if (hasFrom) {
            sb.append("        Address _startAddr = toAddr(\"")
              .append(startAddr).append("\");\n");
        } else {
            sb.append("        Address _startAddr = currentProgram.getMinAddress();\n");
        }

        sb.append("        Address[] _matches = findBytes(_startAddr, \"")
          .append(pattern).append("\", ").append(DEFAULT_MATCH_LIMIT).append(");\n");

        sb.append("        if (_matches == null || _matches.length == 0) {\n");
        sb.append("            printf(\"[findBytes] no matches for pattern: ")
          .append(pattern).append("\\n\");\n");
        sb.append("        } else {\n");
        sb.append("            for (Address _match : _matches) {\n");
        sb.append("                printf(\"MATCH  ").append(pattern)
          .append("  at %s\\n\", _match);\n");
        sb.append("            }\n");
        sb.append("        }\n");
        sb.append("      }\n");

        return sb.toString();
    }

    private String stripQuotes(String s) {
        return s.substring(1, s.length() - 1);
    }
}
