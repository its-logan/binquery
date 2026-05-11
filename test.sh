#!/usr/bin/env bash
#
# test.sh --- Build and test BinQuery Phase I end-to-end.
#
# Usage:
#   ./test.sh            Build + run the three sample tests.
#   ./test.sh --ghidra   Also type-check the generated audit.java against a
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
            sed -n '2,15p' "$0"
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
    src/*.java src/handlers/*.java src/error/*.java generated/*.java
echo "  build OK"

echo
echo "=== Test 1: audit.bq --- happy path (find bytes) ========="
java -cp "bin:$ANTLR_JAR" binquery.Main samples/audit.bq > /dev/null
if diff -q samples/audit.java samples/audit.bq.expected.java > /dev/null; then
    echo "  PASS: output matches samples/audit.bq.expected.java"
else
    echo "  FAIL: output differs from expected"
    diff samples/audit.java samples/audit.bq.expected.java
    exit 1
fi

echo
echo "=== Test 2: multi_query.bq --- stubs parse, bytes emits =="
java -cp "bin:$ANTLR_JAR" binquery.Main samples/multi_query.bq > /dev/null
echo "  PASS: all three query types parsed"

echo
echo "=== Test 3: error_demo.bq --- semantic validation ========"
if java -cp "bin:$ANTLR_JAR" binquery.Main samples/error_demo.bq 2>&1 \
        | grep -q "SemanticException"; then
    echo "  PASS: SemanticException raised on malformed hex"
else
    echo "  FAIL: expected SemanticException, did not see one"
    exit 1
fi

if [ "$CHECK_GHIDRA" -eq 1 ]; then
    echo
    echo "=== Test 4: type-check against Ghidra API ================"
    if [ ! -d "$GHIDRA" ]; then
        echo "  FAIL: Ghidra not found at $GHIDRA"
        echo "  Set \$GHIDRA to your install path, or drop --ghidra."
        exit 1
    fi
    CP=$(find "$GHIDRA" -name '*.jar' 2>/dev/null | tr '\n' ':')
    cp samples/audit.java samples/AuditScript.java
    mkdir -p /tmp/bq-check
    if javac -proc:none -cp "$CP" -d /tmp/bq-check samples/AuditScript.java 2>/dev/null; then
        echo "  PASS: generated code resolves against Ghidra jars"
    else
        echo "  FAIL: javac against Ghidra jars did not succeed"
        rm -f samples/AuditScript.java
        exit 1
    fi
    rm -f samples/AuditScript.java
fi

echo
echo "=== All tests passed ====================================="
