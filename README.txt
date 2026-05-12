BinQuery --- v1.3
=================

BinQuery compiles a small DSL (.bq files) into GhidraScript Java source.
Each input produces a class extending ghidra.app.script.GhidraScript that
uses Ghidra's FlatProgramAPI to run binary-analysis queries.

The grammar (src/BinQuery.g4) currently supports five query verbs:

    find bytes     "<hex>" (from <addr>)? (in function "<name>")?
    find calls     to "<symbol>" (through thunks)? (internal|external)?
    find functions <predicate>
                       where xrefs (>|<|>=|<=|==) <int> (internal|external)?
                     | named "<pattern>" (internal|external)?
                     | (internal|external)
    find symbols   "<name>" (internal|external)?
    find strings   minlen <int> (ascii|unicode)?
                       (containing "<lit>" | matching "<regex>")?
                       (in function "<name>")?

    in block "<name>"    { <queries> ... }     // shared memory-block scope
    in function "<name>" { <queries> ... }     // shared function-body scope

Every query verb is fully implemented; grammar coverage is demonstrated
by samples/multi_query.bq.

The generated GhidraScript declares two script-level caches:
   Map<String, Address[]>             _byteCache
   Map<String, List<FoundString>>     _stringCache
so repeated queries with the same scope+pattern share one memory scan.


Requirements
------------
  * JDK 17+ (javac, java on PATH)
  * ANTLR 4.13.2 complete jar
      https://www.antlr.org/download/antlr-4.13.2-complete.jar
      Point $ANTLR_JAR at it (test.sh defaults to ~/antlr-4.13.2-complete.jar).

Ghidra is NOT required for the default test run. If you pass --ghidra to
test.sh, it will additionally type-check every generated .java against a
local Ghidra install (default $HOME/ghidra_11.3_PUBLIC, override with
$GHIDRA). See "Running against Ghidra" below.


Build
-----
    export ANTLR_JAR=$HOME/antlr-4.13.2-complete.jar
    mkdir -p generated bin
    ( cd src && java -jar "$ANTLR_JAR" -o ../generated -package binquery -visitor BinQuery.g4 )
    javac -cp "$ANTLR_JAR" -d bin \
        src/*.java src/handlers/*.java src/error/*.java generated/*.java

The Bash one-liner equivalent of all the above lives in test.sh's Build
section --- run ./test.sh once and everything compiles clean.


Command-line usage
------------------
    bq [options] <script.bq> [more.bq ...]

      -h, --help              show usage and exit
          --version           print version and exit
      -o, --output <path>     write generated code to <path>; use - for stdout
                              (only valid with a single input)
      -d, --outdir <dir>      write outputs to <dir>/<basename>.java
                              (default: alongside each input)
          --check             validate inputs; do not write any output
      -q, --quiet             suppress per-file 'wrote <path>' messages

    Headless Ghidra execution:
          --run-in <binary>   after compiling, run the script against <binary>
          --ghidra <path>     Ghidra install root (overrides $GHIDRA)
          --project <dir>     project dir (default: ~/.cache/bq/projects/<sha256>)
          --keep-project      retain project dir (currently no-op; projects are
                              persistent by default)
          --dry-run           with --run-in: compile, print analyzeHeadless argv,
                              do not invoke

`bq` is shorthand for `java -cp "bin:$ANTLR_JAR" binquery.Main`. With
multiple positional inputs each file is compiled independently;
a compile error on one file does not stop the others. Exit code is 1
if any input failed, 0 otherwise. Use exit code 2 to detect a CLI
usage error (bad flag, no inputs, conflicting options).

Examples:

    bq samples/audit.bq                       # writes samples/audit.java
    bq -o /tmp/Audit.java samples/audit.bq    # custom output path
    bq -o - samples/audit.bq | less           # output to stdout
    bq -d build samples/*.bq                  # batch into build/
    bq --check samples/*.bq                   # lint only, no files written

Headless Ghidra execution
-------------------------
With --run-in, bq compiles the script then invokes Ghidra's
$GHIDRA/support/analyzeHeadless against the target binary. Output
streams to the terminal as Ghidra runs.

    export GHIDRA=$HOME/ghidra_11.3_PUBLIC
    bq --run-in target.exe samples/audit.bq

Project directories are persistent and content-addressed by default:

    ~/.cache/bq/projects/<sha256-of-binary>/

The first run analyzes the binary (10-60s); subsequent runs against the
same binary reuse the cached project and start much faster. Override
with --project <dir> for a one-off location. Wipe the cache when
disk-pressured:

    rm -rf ~/.cache/bq/projects/

Use --dry-run to inspect (and capture) the analyzeHeadless argv without
running:

    bq --ghidra "$GHIDRA" --run-in target.exe --dry-run samples/audit.bq
    # prints one argv element per line; copy-paste into a shell to repeat.

The generated .java is a plain GhidraScript. Drop it into Ghidra's
ghidra_scripts/ directory (or any directory listed under
Window > Script Manager > Manage Script Directories) and run it from
the Script Manager. Output goes to the Console window via printf().


Tests
-----
    chmod +x test.sh
    ./test.sh              # build + sample diffs + error demos
    ./test.sh --ghidra     # also type-check against Ghidra's jars

test.sh runs four groups:

  1. Per-query goldens. Each samples/<name>.bq is compiled, then
     samples/<name>.java is diff'd against samples/<name>.bq.expected.java.
     Any drift fails the test.

  2. multi_query.bq parse smoke. Confirms every grammar production
     cohabits in one script without ambiguity.

  3. SemanticException demos. samples/error_demo*.bq each contain
     intentionally invalid input; the harness asserts each raises
     SemanticException with the expected message snippet.

  4. (--ghidra only) javac every generated .java against every jar under
     $GHIDRA, proving every emitted symbol (GhidraScript, Address,
     findBytes, findStrings, FoundString, Reference, Function,
     getThunkedFunction, java.util.regex.Pattern, ...) resolves against
     the real API.


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


Scope blocks
------------
Group queries under a shared scope with `in <selector> { ... }`:

    in block ".text" {
        find bytes "FF 25"
        find calls to "malloc"
        find functions where xrefs > 5
    }

    in function "main" {
        find bytes "C3"
        find strings minlen 8
    }

Selectors:
    block "<name>"      MemoryBlock by name (.text, .rdata, EXTERNAL, ...).
                        Backed by currentProgram.getMemory().getBlock(name).
    function "<name>"   Function body. Same multiplicity check as the
                        trailing `in function "X"` clause (0 -> not found,
                        >1 -> ambiguous, exactly-1 -> body used as scope).

Inside a block, every query's scope is the AddressSetView established by
the enclosing selector. find-bytes / find-strings restrict their scan;
find-calls / find-symbols filter by from-address / symbol-address; find-
functions filter by getEntryPoint().

Nesting is allowed; the inner scope is the intersection of the inner
selector's set with the enclosing scope:

    in block ".text" {
        in function "main" {       // scope = .text intersect main.body
            find strings minlen 4 containing "http"
        }
    }

Trailing per-query scope clauses (`in function "X"`, `from <addr>`) are
forbidden inside a scope block --- the block already owns the scope.


Error format
------------
Parse and semantic errors share one line shape on stderr:

    Error [line N:col] <message>     # parse errors (SyntaxException)
    Error [line N] <message>         # semantic errors (SemanticException)

Parse errors collect; running a script with multiple malformed queries
prints one line per error before exit code 1. Semantic errors halt on
the first failure inside FindXxxHandler.validate().


Output format reference
-----------------------
Each query emits a single grep-anchored line per hit:

    MATCH  <pattern>                  at <addr>            # find bytes
    CALL   to <name>                  at <addr>  in <fn>   # find calls
    FN     <name>  xrefs=<n>          at <entry>           # find functions
    SYM    <name>                     at <addr>            # find symbols
    STR[<filter?>,enc=<ascii|utf16>] "<val>"  len=<n>  at <addr>

Suffixes appended when applicable:
    [EXT]      target symbol/function is in EXTERNAL block
    [thunk]    call site reached via thunk-walking expansion
    ...        string content truncated at 80 chars (real len still in len=)
