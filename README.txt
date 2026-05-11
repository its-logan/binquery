BinQuery --- Phase I
====================

BinQuery compiles a small DSL (.bq files) into GhidraScript Java source.
Each input produces a class extending ghidra.app.script.GhidraScript that
uses Ghidra's FlatProgramAPI to run binary-analysis queries.

The grammar (src/BinQuery.g4) supports three query types:
    find calls to "<symbol>"
    find functions where xrefs > <int>
    find bytes "<hex>" (from <addr>)?

Phase I scope: only 'find bytes' generates full codegen. 'find calls' and
'find functions' parse cleanly but their handlers are stubs that emit
nothing. Grammar coverage is demonstrated by samples/multi_query.bq.


Requirements
------------
  * JDK 17+ (javac, java on PATH)
  * ANTLR 4.13.2 complete jar
      https://www.antlr.org/download/antlr-4.13.2-complete.jar
      Point $ANTLR_JAR at it (test.sh defaults to ~/antlr-4.13.2-complete.jar).

Ghidra is NOT required for the default test run. If you pass --ghidra
to test.sh, it will additionally type-check the generated code against
a local Ghidra install (default $HOME/ghidra_11.3_PUBLIC, override with
$GHIDRA). See "Running against Ghidra" below.


Build
-----
    export ANTLR_JAR=$HOME/antlr-4.13.2-complete.jar
    mkdir -p generated bin
    ( cd src && java -jar "$ANTLR_JAR" -o ../generated -package binquery -visitor BinQuery.g4 )
    javac -cp "$ANTLR_JAR" -d bin \
        src/*.java src/handlers/*.java src/error/*.java generated/*.java

Run a single script:
    java -cp "bin:$ANTLR_JAR" binquery.Main samples/audit.bq
    # --> writes samples/audit.java


Tests
-----
    chmod +x test.sh
    ./test.sh              # default: three sample tests
    ./test.sh --ghidra     # also type-check against Ghidra's jars

Clean-builds, then runs:

  1. audit.bq        Two find-bytes queries. Output is diffed against
                     samples/audit.bq.expected.java; must be identical.
  2. multi_query.bq  All three query types. Confirms the stub handlers
                     parse without error.
  3. error_demo.bq   Malformed hex pattern. Must raise SemanticException
                     from FindBytesHandler.validate() --- the required
                     semantic-level operation on the parse tree.
  4. (--ghidra only) javac samples/audit.java against every jar under
                     $GHIDRA, proving every emitted symbol (GhidraScript,
                     Address, findBytes, toAddr, printf, ...) resolves
                     against the real API.


Running against Ghidra (optional)
---------------------------------
If you want to run test 4:

  1. Grab a release from
     https://github.com/NationalSecurityAgency/ghidra/releases
     (tested with ghidra_11.3_PUBLIC).
  2. Unzip it anywhere, e.g.:
        unzip ghidra_11.3_PUBLIC_*.zip -d $HOME/
  3. Point $GHIDRA at the extracted directory and re-run:
        export GHIDRA=$HOME/ghidra_11.3_PUBLIC
        ./test.sh --ghidra
