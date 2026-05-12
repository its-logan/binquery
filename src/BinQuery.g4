grammar BinQuery;

program
    : scriptDecl topLevel+ EOF
    ;

scriptDecl
    : SCRIPT IDENTIFIER
    ;

topLevel
    : query
    | scopeBlock
    ;

scopeBlock
    : IN scopeSelector LBRACE topLevel+ RBRACE
    ;

scopeSelector
    : BLOCK STRING
    | FUNCTION STRING
    ;

query
    : findCalls
    | findFunctions
    | findSymbols
    | findBytes
    | findStrings
    ;

findCalls
    : FIND CALLS TO STRING thunkClause? locationFilter?
    ;

thunkClause
    : THROUGH THUNKS
    ;

findFunctions
    : FIND FUNCTIONS functionPredicate
    ;

findSymbols
    : FIND SYMBOLS STRING locationFilter?
    ;

findBytes
    : FIND BYTES STRING (FROM HEX_ADDR)? (IN FUNCTION STRING)?
    ;

findStrings
    : FIND STRINGS MINLEN INT encodingClause? stringFilter? scopeClause?
    ;

encodingClause
    : ASCII
    | UNICODE
    ;

stringFilter
    : CONTAINING STRING
    | MATCHING STRING
    ;

scopeClause
    : IN FUNCTION STRING
    ;

functionPredicate
    : WHERE XREFS compareOp INT locationFilter?    # predXrefs
    | NAMED STRING locationFilter?                  # predNamed
    | locationFilter                                # predLocationOnly
    ;

locationFilter
    : INTERNAL
    | EXTERNAL
    ;

compareOp
    : GE
    | LE
    | EQ
    | GT
    | LT
    ;


// Keywords
SCRIPT      : 'script' ;
FIND        : 'find' ;
CALLS       : 'calls' ;
TO          : 'to' ;
FUNCTIONS   : 'functions' ;
WHERE       : 'where' ;
XREFS       : 'xrefs' ;
BYTES       : 'bytes' ;
FROM        : 'from' ;
SYMBOLS     : 'symbols' ;
NAMED       : 'named' ;
IN          : 'in' ;
FUNCTION    : 'function' ;
INTERNAL    : 'internal' ;
EXTERNAL    : 'external' ;
THROUGH     : 'through' ;
THUNKS      : 'thunks' ;
STRINGS     : 'strings' ;
MINLEN      : 'minlen' ;
CONTAINING  : 'containing' ;
MATCHING    : 'matching' ;
ASCII       : 'ascii' ;
UNICODE     : 'unicode' ;
BLOCK       : 'block' ;
LBRACE      : '{' ;
RBRACE      : '}' ;

// Comparison operators --- multi-char first to satisfy lexer max-munch ordering
GE          : '>=' ;
LE          : '<=' ;
EQ          : '==' ;
GT          : '>' ;
LT          : '<' ;

STRING      : '"' (~["\r\n])* '"' ;
HEX_ADDR    : '0x' [0-9A-Fa-f]+ ;
INT         : [0-9]+ ;
IDENTIFIER  : [A-Za-z_][A-Za-z0-9_]* ;

WS          : [ \t\r\n]+ -> skip ;
COMMENT     : '//' ~[\r\n]* -> skip ;
