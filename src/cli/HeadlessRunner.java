package binquery.cli;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;

// Resolves Ghidra install / analyzeHeadless / project dir, then invokes
// analyzeHeadless once per compiled script. ProcessBuilder.inheritIO()
// streams stdout/stderr live.
//
// Project dirs are content-addressed by default
// (~/.cache/bq/projects/<sha256>) so repeat runs against the same binary
// reuse the cached project.
public final class HeadlessRunner {

    private final CliOptions opts;
    private Path headlessTool;
    private Path projectDir;

    public HeadlessRunner(CliOptions opts) {
        this.opts = opts;
    }

    // Returns 0 if Ghidra/target valid, 2 (usage error) otherwise.
    public int prepare() throws IOException {
        Path target = Path.of(opts.runIn);
        if (!Files.isRegularFile(target)) {
            return usageError("--run-in target not found: " + opts.runIn);
        }
        String ghidra = ghidraRoot();
        if (ghidra == null || ghidra.isBlank()) {
            return usageError("--run-in requires --ghidra <path> or $GHIDRA");
        }
        Path root = Path.of(ghidra);
        if (!Files.isDirectory(root)) {
            return usageError("Ghidra install not found: " + ghidra);
        }
        this.headlessTool = resolveHeadlessTool(root);
        this.projectDir   = resolveProjectDir(target);
        return 0;
    }

    public int run(Path scriptPath, String className) throws IOException {
        List<String> argv = new ArrayList<>();
        argv.add(headlessTool.toString());
        argv.add(projectDir.toString());
        argv.add("bqsession");
        argv.add("-import");     argv.add(opts.runIn);
        argv.add("-scriptPath"); argv.add(scriptPath.getParent().toString());
        argv.add("-postScript"); argv.add(className + ".java");

        if (opts.dryRun) {
            for (String s : argv) System.out.println(s);
            return 0;
        }
        if (!opts.quiet) {
            System.out.println("running " + className + " against " + opts.runIn);
        }
        ProcessBuilder pb = new ProcessBuilder(argv).inheritIO();
        try {
            return pb.start().waitFor();
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
            return 130;
        }
    }

    private String ghidraRoot() {
        return opts.ghidraOverride != null ? opts.ghidraOverride : System.getenv("GHIDRA");
    }

    private Path resolveHeadlessTool(Path root) {
        boolean win = System.getProperty("os.name", "").toLowerCase().startsWith("win");
        String relative = win ? "support/analyzeHeadless.bat" : "support/analyzeHeadless";
        return root.resolve(relative);
    }

    private Path resolveProjectDir(Path target) throws IOException {
        if (opts.projectOverride != null) {
            Path p = Path.of(opts.projectOverride);
            Files.createDirectories(p);
            return p;
        }
        String hash = sha256Hex(target);
        Path cache = Path.of(System.getProperty("user.home"), ".cache", "bq", "projects", hash);
        Files.createDirectories(cache);
        return cache;
    }

    private String sha256Hex(Path p) throws IOException {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                                         .digest(Files.readAllBytes(p));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }

    private int usageError(String msg) {
        System.err.println("bq: " + msg);
        System.err.println("Try 'bq --help' for more information.");
        return 2;
    }
}
