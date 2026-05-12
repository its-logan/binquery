package binquery;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import binquery.cli.CliOptions;
import binquery.cli.Compiler;
import binquery.cli.HeadlessRunner;
import binquery.cli.OutputResolver;

public class Main {

    public static void main(String[] argv) throws Exception {
        CliOptions.ParseResult pr = CliOptions.parse(argv);
        if (pr.exitCode != null) {
            System.exit(pr.exitCode);
        }
        System.exit(new Main(pr.options).run());
    }

    private final CliOptions opts;
    private final Compiler compiler = new Compiler();
    private HeadlessRunner headless;
    private String scriptDirOverride;   // tempdir injected when --run-in + no -o/-d

    private Main(CliOptions opts) {
        this.opts = opts;
    }

    private int run() throws IOException {
        if (opts.runIn != null) {
            headless = new HeadlessRunner(opts);
            int prep = headless.prepare();
            if (prep != 0) return prep;
            if (opts.output == null && opts.outdir == null) {
                scriptDirOverride = Files.createTempDirectory("bq-run-").toString();
            }
        }

        int compileFailed = 0;
        int headlessFailed = 0;

        for (String in : opts.inputs) {
            Compiler.Result cr = compiler.compile(in);
            if (!cr.ok) { compileFailed++; continue; }

            // --check: no output, no headless run.
            if (opts.check) {
                if (!opts.quiet) System.out.println("ok " + in);
                continue;
            }

            // -o - : stream generated code to stdout; not eligible for headless
            // (rejected during CliOptions.parse).
            if ("-".equals(opts.output)) {
                System.out.print(cr.generated);
                continue;
            }

            Path outPath = scriptDirOverride != null
                ? Path.of(scriptDirOverride).resolve(scriptName(in))
                : OutputResolver.resolveOutputPath(in, opts);
            Files.writeString(outPath, cr.generated);
            if (!opts.quiet) System.out.println("wrote " + outPath);

            if (headless != null) {
                String className = OutputResolver.extractClassName(cr.generated);
                if (className == null) {
                    System.err.println(in + ": could not determine class name from generated code");
                    compileFailed++;
                    continue;
                }
                int rc = headless.run(outPath, className);
                if (rc != 0) headlessFailed++;
            }
        }

        if (compileFailed > 0)  return 1;
        if (headlessFailed > 0) return 1;
        return 0;
    }

    private static String scriptName(String inPath) {
        String name = inPath.endsWith(".bq")
            ? inPath.substring(0, inPath.length() - 3) + ".java"
            : inPath + ".java";
        return Path.of(name).getFileName().toString();
    }
}
