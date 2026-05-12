package binquery.cli;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

// Pure path math + class-name extraction. No I/O surprises beyond
// creating an outdir on demand.
public final class OutputResolver {

    private static final Pattern CLASS_LINE = Pattern.compile("public class (\\w+)");

    private OutputResolver() {}

    public static Path resolveOutputPath(String inPath, CliOptions opts) throws IOException {
        if (opts.output != null) {
            return Path.of(opts.output);
        }
        String name = inPath.endsWith(".bq")
            ? inPath.substring(0, inPath.length() - 3) + ".java"
            : inPath + ".java";
        if (opts.outdir != null) {
            Path dir = Path.of(opts.outdir);
            Files.createDirectories(dir);
            return dir.resolve(Path.of(name).getFileName());
        }
        return Path.of(name);
    }

    public static String extractClassName(String generated) {
        Matcher m = CLASS_LINE.matcher(generated);
        return m.find() ? m.group(1) : null;
    }
}
