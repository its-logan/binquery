#!/usr/bin/env bash
#
# test.sh --- Build and test BinQuery end-to-end.
#
# Usage:
#   ./test.sh            Build + run sample tests.
#   ./test.sh --ghidra   Also type-check every generated *.java against a
#                        local Ghidra install (requires $GHIDRA or
#                        $HOME/ghidra_11.3_PUBLIC).
#
# Environment:
#   ANTLR_JAR   Path to antlr-4.13.2-complete.jar.
#               Default: $HOME/antlr-4.13.2-complete.jar
#   GHIDRA      Path to a Ghidra install (only used with --ghidra).
#               Default: $HOME/ghidra_11.3_PUBLIC

set -e
cd "$(dirname "$0")"

CHECK_GHIDRA=0
for arg in "$@"; do
    case "$arg" in
        --ghidra|-g) CHECK_GHIDRA=1 ;;
        -h|--help)
            sed -n '2,16p' "$0"
            exit 0
            ;;
        *)
            echo "unknown argument: $arg" >&2
            echo "usage: $0 [--ghidra]" >&2
            exit 2
            ;;
    esac
done

: "${ANTLR_JAR:=$HOME/antlr-4.13.2-complete.jar}"
: "${GHIDRA:=$HOME/ghidra_11.3_PUBLIC}"

if [ ! -f "$ANTLR_JAR" ]; then
    echo "ERROR: ANTLR jar not found at $ANTLR_JAR"
    echo "Set ANTLR_JAR or place the jar there. See README.txt."
    exit 1
fi

echo "=== Build ================================================"
rm -rf bin generated
mkdir -p bin generated
( cd src && java -jar "$ANTLR_JAR" -o ../generated -package binquery -visitor BinQuery.g4 )
javac -cp "$ANTLR_JAR" -d bin \
    src/*.java src/cli/*.java src/handlers/*.java src/error/*.java generated/*.java
echo "  build OK"

# ----- helpers ---------------------------------------------------------------

run_main() {
    java -cp "bin:$ANTLR_JAR" binquery.Main "$1" > /dev/null
}

assert_golden() {
    local sample="$1"
    local generated="samples/${sample}.java"
    local golden="samples/${sample}.bq.expected.java"
    if diff -q "$generated" "$golden" > /dev/null; then
        echo "  PASS: ${sample}"
    else
        echo "  FAIL: ${sample} differs from golden"
        diff "$generated" "$golden"
        exit 1
    fi
}

assert_semantic_exception() {
    local sample="$1"
    local expected_snippet="$2"
    local out rc
    if out=$(java -cp "bin:$ANTLR_JAR" binquery.Main "samples/${sample}.bq" 2>&1); then
        rc=0
    else
        rc=$?
    fi
    if [ "$rc" -ne 0 ] && printf '%s\n' "$out" | grep -q "Error \[line.*${expected_snippet}"; then
        echo "  PASS: ${sample} raised compile error"
    else
        echo "  FAIL: ${sample} did not raise expected error"
        printf '%s\n' "$out" | head -5
        exit 1
    fi
}

assert_parse_errors() {
    local sample="$1"
    local expected="$2"
    local out rc
    if out=$(java -cp "bin:$ANTLR_JAR" binquery.Main "samples/${sample}.bq" 2>&1); then
        rc=0
    else
        rc=$?
    fi
    local count
    count=$(printf '%s\n' "$out" | grep -c 'Error \[line' || true)
    if [ "$rc" -ne 0 ] && [ "$count" -eq "$expected" ]; then
        echo "  PASS: ${sample} (${count} parse errors, exit ${rc})"
    else
        echo "  FAIL: ${sample}: expected ${expected} parse errors + nonzero exit; got ${count} errors, exit ${rc}"
        printf '%s\n' "$out" | head -10
        exit 1
    fi
}

# ----- Test 1: per-query goldens --------------------------------------------

echo
echo "=== Test 1: each sample compiles to the saved expected Java ====="
for s in audit find_calls find_calls_thunks find_functions_xrefs \
         find_functions_named find_symbols find_in_function cache_reuse \
         find_strings scope_blocks; do
    run_main "samples/${s}.bq"
    assert_golden "${s}"
done

# ----- Test 2: multi_query parse smoke --------------------------------------

echo
echo "=== Test 2: every query type works in one script ==============="
run_main "samples/multi_query.bq"
echo "  PASS: every production cohabits"

# ----- Test 3: SemanticException demos ---------------------------------------

echo
echo "=== Test 3: bad inputs raise the right errors =================="
assert_semantic_exception error_demo          "invalid byte pattern"
assert_semantic_exception error_demo_empty    "cannot be empty"
assert_semantic_exception error_demo_conflict "mutually exclusive"
assert_semantic_exception error_demo_strings  "minlen must be"
assert_semantic_exception error_demo_scope_conflict "trailing scope clause forbidden"
assert_parse_errors       error_demo_syntax       1
assert_parse_errors       error_demo_syntax_multi 3

# ----- Test 4: Ghidra API type-check ----------------------------------------

# ----- Test 4: CLI ergonomics ----------------------------------------------

echo
echo "=== Test 4: command-line flags behave as documented ============"
BQ="java -cp bin:$ANTLR_JAR binquery.Main"

cli_check() {
    local label="$1"; shift
    local expected_rc="$1"; shift
    local cmd_rc
    if "$@" > /tmp/bq-cli.out 2> /tmp/bq-cli.err; then cmd_rc=0; else cmd_rc=$?; fi
    if [ "$cmd_rc" -eq "$expected_rc" ]; then
        echo "  PASS: ${label} (exit ${cmd_rc})"
    else
        echo "  FAIL: ${label}: expected exit ${expected_rc}, got ${cmd_rc}"
        cat /tmp/bq-cli.out /tmp/bq-cli.err
        exit 1
    fi
}

cli_check "--version"     0 $BQ --version
cli_check "--help"        0 $BQ --help
cli_check "no args"       2 $BQ
cli_check "unknown flag"  2 $BQ --bogus samples/audit.bq
cli_check "-o + multi"    2 $BQ -o /tmp/x.java samples/audit.bq samples/find_calls.bq
cli_check "--check ok"    0 $BQ --check samples/audit.bq
cli_check "--check fail"  1 $BQ --check samples/error_demo.bq
cli_check "-q success"    0 $BQ -q --check samples/audit.bq
cli_check "-o stdout"     0 $BQ -o - samples/audit.bq

# -d outdir creates directory and writes files there
rm -rf /tmp/bq-cli-outdir
cli_check "-d outdir"     0 $BQ -d /tmp/bq-cli-outdir samples/audit.bq samples/find_calls.bq
if [ -f /tmp/bq-cli-outdir/audit.java ] && [ -f /tmp/bq-cli-outdir/find_calls.java ]; then
    echo "  PASS: -d wrote both files into outdir"
else
    echo "  FAIL: -d did not produce expected files"
    ls /tmp/bq-cli-outdir/
    exit 1
fi

# Batch with one failure: rc=1, good files still written, bad file's .java NOT
rm -f samples/error_demo.java
cli_check "batch 1 fail"  1 $BQ -q -d /tmp/bq-cli-outdir samples/audit.bq samples/error_demo.bq samples/find_calls.bq
if [ ! -f /tmp/bq-cli-outdir/error_demo.java ]; then
    echo "  PASS: failed input did not produce output file"
else
    echo "  FAIL: failed input wrote a stale .java"
    exit 1
fi

# Headless flags (no real analyzeHeadless invocation -- slow + needs a binary).
# --run-in requires either --ghidra or $GHIDRA.
sub_rc=0
( unset GHIDRA && $BQ --run-in /bin/ls samples/audit.bq > /dev/null 2>&1 ) || sub_rc=$?
if [ "$sub_rc" -eq 2 ]; then
    echo "  PASS: --run-in without ghidra/GHIDRA fails rc=2"
else
    echo "  FAIL: --run-in without ghidra should rc=2 (got $sub_rc)"
    exit 1
fi

cli_check "--run-in bad target"  2 $BQ --ghidra "$GHIDRA" --run-in /no/such/file samples/audit.bq
cli_check "--run-in + --check"   2 $BQ --ghidra "$GHIDRA" --run-in /bin/ls --check samples/audit.bq
cli_check "--run-in + -o -"      2 $BQ --ghidra "$GHIDRA" --run-in /bin/ls -o - samples/audit.bq
cli_check "--dry-run alone"      2 $BQ --dry-run samples/audit.bq

# --dry-run with --run-in prints argv containing analyzeHeadless and the class name
$BQ --ghidra "$GHIDRA" --run-in /bin/ls --dry-run -q samples/audit.bq > /tmp/bq-dryrun.out 2>&1
if grep -q 'analyzeHeadless' /tmp/bq-dryrun.out && grep -q 'AuditScript.java' /tmp/bq-dryrun.out; then
    echo "  PASS: --dry-run prints analyzeHeadless argv with class name"
else
    echo "  FAIL: --dry-run argv missing expected content"
    cat /tmp/bq-dryrun.out
    exit 1
fi
rm -f /tmp/bq-dryrun.out

rm -rf /tmp/bq-cli-outdir /tmp/bq-cli.out /tmp/bq-cli.err

if [ "$CHECK_GHIDRA" -eq 1 ]; then
    echo
    echo "=== Test 5: generated Java compiles against the real Ghidra API "
    if [ ! -d "$GHIDRA" ]; then
        echo "  FAIL: Ghidra not found at $GHIDRA"
        echo "  Set \$GHIDRA to your install path, or drop --ghidra."
        exit 1
    fi
    CP=$(find "$GHIDRA" -name '*.jar' 2>/dev/null | tr '\n' ':')
    TMP=/tmp/bq-check
    rm -rf "$TMP"
    mkdir -p "$TMP/src"
    for s in audit find_calls find_calls_thunks find_functions_xrefs \
             find_functions_named find_symbols find_in_function \
             cache_reuse find_strings scope_blocks multi_query; do
        # Generated .java's class name is derived from the script's IDENTIFIER,
        # not the filename. Read first 'public class' line.
        cls=$(grep -m1 -oP 'public class \K\w+' "samples/${s}.java")
        cp "samples/${s}.java" "$TMP/src/${cls}.java"
    done
    if javac -proc:none -cp "$CP" -d "$TMP/bin" "$TMP/src"/*.java 2>/tmp/bq-check.err; then
        echo "  PASS: all generated scripts resolve against Ghidra jars"
    else
        echo "  FAIL: javac against Ghidra jars failed"
        cat /tmp/bq-check.err
        exit 1
    fi
fi

echo
echo "=== All tests passed ====================================="
