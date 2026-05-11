package binquery.handlers;

import binquery.BinQueryParser.FindCallsContext;
import binquery.error.SemanticException;

// find calls to "<symbol>" --- Phase II
public class FindCallsHandler {

    private static final int DEFAULT_MATCH_LIMIT = 100;

    public void validate(Object ctx) {
    }

    public String emit(Object ctx) {
        return "";
    }
}
