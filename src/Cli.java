import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import javax.swing.JLabel;
import javax.swing.JTextArea;

/**
 * Command-line interface for SchedulerFiles.
 * Run with --help to see usage.
 */
public class Cli {

    static final int EXIT_OK = 0;
    static final int EXIT_ERROR = 1;
    static final int EXIT_CANCELLED = 2;

    public static void main(String[] args) {
        System.exit(run(args));
    }

    /** Parse args and execute. Returns exit code. */
    static int run(String[] args) {
        if (args.length == 0) {
            printUsage(System.out);
            return EXIT_ERROR;
        }

        CliOptions opts = new CliOptions();
        if (!opts.parse(args)) {
            printUsage(System.err);
            return EXIT_ERROR;
        }

        if (opts.help) {
            printUsage(System.out);
            return EXIT_OK;
        }

        // Validate paths
        Path source = Paths.get(opts.source);
        if (!Files.isDirectory(source)) {
            System.err.println("Error: source is not a valid directory: " + opts.source);
            return EXIT_ERROR;
        }
        Path dest = Paths.get(opts.dest);
        if (!Files.isDirectory(dest)) {
            System.err.println("Error: destination is not a valid directory: " + opts.dest);
            return EXIT_ERROR;
        }
        if (source.toAbsolutePath().equals(dest.toAbsolutePath())) {
            System.err.println("Error: source and destination must be different directories");
            return EXIT_ERROR;
        }

        // Run headless (suppress Logger console output)
        MoveClass.setSuppressLogger(true);
        JTextArea dummyLog = new JTextArea();
        JLabel dummyProgress = new JLabel();

        MoveClass mc = new MoveClass(
            source, dest, dummyLog, dummyProgress,
            opts.compareContent, opts.compareName,
            opts.copyMode, opts.scheduledTree, opts.verifyHash
        );
        mc.run();

        // Print log to stdout
        System.out.print(dummyLog.getText());
        return EXIT_OK;
    }

    static void printUsage(PrintStream out) {
        out.println("SchedulerFiles  v" + new Updater().getCurrentVersion());
        out.println("Copy / move files with scheduling and integrity check.");
        out.println();
        out.println("Usage:");
        out.println("  java -jar SchedulerFiles.jar [options]");
        out.println();
        out.println("Required:");
        out.println("  -s, --source <path>        Source directory");
        out.println("  -d, --dest <path>          Destination directory");
        out.println();
        out.println("Mode (default: copy):");
        out.println("  -c, --copy                 Copy files (preserve originals)");
        out.println("  -m, --move                 Move files (delete from source)");
        out.println();
        out.println("Tree structure (default: original):");
        out.println("  --original                 Preserve source folder hierarchy");
        out.println("  --scheduled                Organize by year/month/extension");
        out.println();
        out.println("Compare options:");
        out.println("  --compare-name             Skip files with same filename");
        out.println("  --compare-content          Skip files with identical content");
        out.println("  --verify-hash              Compute & verify SHA-256 hash");
        out.println();
        out.println("Other:");
        out.println("  -h, --help                 Show this help and exit");
        out.println();
        out.println("Examples:");
        out.println("  java -jar SchedulerFiles.jar -s /photos -d /backup --copy");
        out.println("  java -jar SchedulerFiles.jar -s /src -d /dst --move --scheduled --verify-hash");
        out.println();
        out.println("License: Free software. No warranty. Use at your own risk.");
    }

    /** Parsed CLI options. */
    static class CliOptions {
        String source;
        String dest;
        boolean copyMode = true;
        boolean scheduledTree;
        boolean compareName;
        boolean compareContent;
        boolean verifyHash;
        boolean help;

        /** Parse args. Returns false on error. */
        boolean parse(String[] args) {
            for (int i = 0; i < args.length; i++) {
                switch (args[i]) {
                    case "-h":
                    case "--help":
                        help = true;
                        return true;
                    case "-s":
                    case "--source":
                        if (++i >= args.length) { err("--source requires a path"); return false; }
                        source = args[i];
                        break;
                    case "-d":
                    case "--dest":
                        if (++i >= args.length) { err("--dest requires a path"); return false; }
                        dest = args[i];
                        break;
                    case "-c":
                    case "--copy":
                        copyMode = true;
                        break;
                    case "-m":
                    case "--move":
                        copyMode = false;
                        break;
                    case "--original":
                        scheduledTree = false;
                        break;
                    case "--scheduled":
                        scheduledTree = true;
                        break;
                    case "--compare-name":
                        compareName = true;
                        break;
                    case "--compare-content":
                        compareContent = true;
                        break;
                    case "--verify-hash":
                        verifyHash = true;
                        break;
                    default:
                        err("Unknown option: " + args[i]);
                        return false;
                }
            }
            if (!help && (source == null || dest == null)) {
                err("Both --source and --dest are required");
                return false;
            }
            return true;
        }

        private void err(String msg) {
            System.err.println("Error: " + msg);
        }
    }
}
