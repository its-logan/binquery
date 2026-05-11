grammar BinQuery;

program
    : scriptDecl query+ EOF
    ;

scriptDecl
    : SCRIPT IDENTIFIER
    ;

query
    : findCalls
    | findFunctions
    | findBytes
    ;

findCalls
    : FIND CALLS TO STRING
    ;

findFunctions
    : FIND FUNCTIONS WHERE XREFS GT INT
    ;

findBytes
    : FIND BYTES STRING (FROM HEX_ADDR)?
    ;


SCRIPT      : 'script' ;
FIND        : 'find' ;
CALLS       : 'calls' ;
TO          : 'to' ;
FUNCTIONS   : 'functions' ;
WHERE       : 'where' ;
XREFS       : 'xrefs' ;
GT          : '>' ;
BYTES       : 'bytes' ;
FROM        : 'from' ;

STRING      : '"' (~["\r\n])* '"' ;
HEX_ADDR    : '0x' [0-9A-Fa-f]+ ;
INT         : [0-9]+ ;
IDENTIFIER  : [A-Za-z_][A-Za-z0-9_]* ;

WS          : [ \t\r\n]+ -> skip ;
COMMENT     : '//' ~[\r\n]* -> skip ;
