package binquery.cli;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

// Parsed CLI options. Created via CliOptions.parse(argv).
//
// Parse() returns a ParseResult: either a fully-populated CliOptions
// (continue running) or an early-exit code (help/version printed, or
// usage error).
public final class CliOptions {

    public static final String VERSION = "BinQuery 1.7";

    public static final String USAGE = String.join("\n",
        "usage: bq [options] <script.bq> [more.bq ...]",
        "",
        "Compiles a BinQuery DSL file into a GhidraScript Java source.",
        "With --run-in, also runs the compiled script against the binary",
        "via Ghidra's analyzeHeadless.",
        "",
        "Options:",
        "  -h, --help              show this message and exit",
        "      --version           print version and exit",
        "  -o, --output <path>     write generated code to <path>; use - for stdout",
        "                          (only valid with a single input)",
        "  -d, --outdir <dir>      write outputs to <dir>/<basename>.java",
        "                          (default: alongside each input)",
        "      --check             validate inputs; do not write any output",
        "  -q, --quiet             suppress per-file 'wrote <path>' messages",
        "",
        "Headless Ghidra execution:",
        "      --run-in <binary>   after compiling, run the script against <binary>",
        "      --ghidra <path>     Ghidra install root (overrides $GHIDRA)",
        "      --project <dir>     project dir (default: ~/.cache/bq/projects/<sha256>)",
        "      --keep-project      retain project dir on exit (currently a no-op:",
        "                          projects are persistent by default)",
        "      --dry-run           with --run-in: compile, print analyzeHeadless argv,",
        "                          do not invoke",
        "",
        "When called with multiple inputs, each is compiled independently;",
        "a compile error on one file does not stop the others. Exit code is",
        "1 if any input failed, 0 otherwise. 2 indicates a CLI usage error."
    );

    public final List<String> inputs;
    public final String output;
    public final String outdir;
    public final boolean check;
    public final boolean quiet;
    public final String runIn;
    public final String ghidraOverride;
    public final String projectOverride;
    public final boolean keepProject;
    public final boolean dryRun;

    private CliOptions(Builder b) {
        this.inputs          = Collections.unmodifiableList(b.inputs);
        this.output          = b.output;
        this.outdir          = b.outdir;
        this.check           = b.check;
        this.quiet           = b.quiet;
        this.runIn           = b.runIn;
        this.ghidraOverride  = b.ghidraOverride;
        this.projectOverride = b.projectOverride;
        this.keepProject     = b.keepProject;
        this.dryRun          = b.dryRun;
    }

    public static ParseResult parse(String[] argv) {
        Builder b = new Builder();
        for (int i = 0; i < argv.length; i++) {
            String a = argv[i];
            switch (a) {
                case "-h": case "--help":
                    System.out.println(USAGE);
                    return ParseResult.exit(0);
                case "--version":
                    System.out.println(VERSION);
                    return ParseResult.exit(0);
                case "-o": case "--output":
                    if (++i >= argv.length) return usageError(a + " requires an argument");
                    b.output = argv[i];
                    break;
                case "-d": case "--outdir":
                    if (++i >= argv.length) return usageError(a + " requires an argument");
                    b.outdir = argv[i];
                    break;
                case "--check":         b.check = true; break;
                case "-q": case "--quiet": b.quiet = true; break;
                case "--run-in":
                    if (++i >= argv.length) return usageError(a + " requires an argument");
                    b.runIn = argv[i];
                    break;
                case "--ghidra":
                    if (++i >= argv.length) return usageError(a + " requires an argument");
                    b.ghidraOverride = argv[i];
                    break;
                case "--project":
                    if (++i >= argv.length) return usageError(a + " requires an argument");
                    b.projectOverride = argv[i];
                    break;
                case "--keep-project":  b.keepProject = true; break;
                case "--dry-run":       b.dryRun = true; break;
                default:
                    if (a.startsWith("-") && !a.equals("-")) {
                        return usageError("unknown option: " + a);
                    }
                    b.inputs.add(a);
            }
        }

        if (b.inputs.isEmpty()) {
            return usageError("no input files");
        }
        if (b.output != null && b.inputs.size() > 1) {
            return usageError("-o/--output is only valid with a single input");
        }
        if (b.output != null && b.outdir != null) {
            return usageError("-o and -d are mutually exclusive");
        }
        if (b.output != null && b.check) {
            return usageError("-o and --check are mutually exclusive");
        }
        if (b.runIn != null && b.check) {
            return usageError("--run-in and --check are mutually exclusive");
        }
        if (b.runIn != null && "-".equals(b.output)) {
            return usageError("--run-in requires a real file path; cannot use -o -");
        }
        if (b.dryRun && b.runIn == null) {
            return usageError("--dry-run requires --run-in");
        }

        return ParseResult.ok(new CliOptions(b));
    }

    private static ParseResult usageError(String msg) {
        System.err.println("bq: " + msg);
        System.err.println("Try 'bq --help' for more information.");
        return ParseResult.exit(2);
    }

    public static final class ParseResult {
        public final CliOptions options;  // null when exitCode != null
        public final Integer exitCode;     // null when options != null

        private ParseResult(CliOptions o, Integer rc) {
            this.options = o; this.exitCode = rc;
        }
        static ParseResult ok(CliOptions o)   { return new ParseResult(o, null); }
        static ParseResult exit(int rc)       { return new ParseResult(null, rc); }
    }

    private static final class Builder {
        final List<String> inputs = new ArrayList<>();
        String output, outdir, runIn, ghidraOverride, projectOverride;
        boolean check, quiet, keepProject, dryRun;
    }
}
