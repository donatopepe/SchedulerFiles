import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.FileVisitResult;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
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

        if (opts.showVersion) {
            System.out.println("SchedulerFiles  v" + new Updater().getCurrentVersion());
            return EXIT_OK;
        }

        // Validate paths
        Path source = Paths.get(opts.source);
        if (!Files.isDirectory(source)) {
            System.err.println("Error: source is not a valid directory: " + opts.source);
            return EXIT_ERROR;
        }
        Path dest = Paths.get(opts.dest);
        Path dest2 = opts.dest2 == null ? null : Paths.get(opts.dest2);
        if (dest2 != null && !opts.copyMode) {
            System.err.println("Error: --dest2 requires --copy; move cannot replicate source twice safely");
            return EXIT_ERROR;
        }
        if (!Files.isDirectory(dest)) {
            // Create destination directory if it doesn't exist (consistent with GUI)
            try {
                Files.createDirectories(dest);
                System.out.println("Created destination directory: " + dest);
            } catch (IOException e) {
                System.err.println("Error: could not create destination directory: " + dest);
                return EXIT_ERROR;
            }
        }
        if (source.toAbsolutePath().equals(dest.toAbsolutePath())
                || (dest2 != null && source.toAbsolutePath().equals(dest2.toAbsolutePath()))
                || (dest2 != null && dest.toAbsolutePath().equals(dest2.toAbsolutePath()))) {
            System.err.println("Error: source and destinations must be different directories");
            return EXIT_ERROR;
        }
        if (dest2 != null && !Files.isDirectory(dest2)) {
            try {
                Files.createDirectories(dest2);
                System.out.println("Created second destination directory: " + dest2);
            } catch (IOException e) {
                System.err.println("Error: could not create second destination directory: " + dest2);
                return EXIT_ERROR;
            }
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
        boolean failed = mc.hasErrors();
        if (dest2 != null && !mc.isCancelled() && !failed) {
            dummyLog.append("===== SECOND DESTINATION =====\n");
            try {
                new MirrorService().synchronize(dest, dest2);
                dummyLog.append("RAID-1 parity verified: destinations synchronized\n");
            } catch (IOException e) {
                failed = true;
                dummyLog.append("RAID-1 parity failed: " + e.getMessage() + "\n");
            }
            if (failed) dummyLog.append("RAID-1 replication incomplete: destinations differ\n");
            else dummyLog.append("RAID-1 replication complete: destinations synchronized\n");
        }

        // Print log to stdout
        System.out.print(dummyLog.getText());
        if (mc.isCancelled()) return EXIT_CANCELLED;
        return failed ? EXIT_ERROR : EXIT_OK;
    }

    /* Legacy helper retained for source compatibility. MirrorService handles parity. */
    private static void removeStaleEntries(final Path primary, Path replica) throws IOException {
        Files.walkFileTree(replica, new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                Path relative = replica.relativize(file);
                if (!Files.exists(primary.resolve(relative))) Files.deleteIfExists(file);
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult postVisitDirectory(Path dir, IOException error) throws IOException {
                if (error != null) throw error;
                if (!dir.equals(replica) && !Files.exists(primary.resolve(replica.relativize(dir)))) {
                    Files.deleteIfExists(dir);
                }
                return FileVisitResult.CONTINUE;
            }
        });
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
        out.println("  -d, --dest <path>          Primary destination directory");
        out.println("      --dest2 <path>         Second destination (RAID-1 copy mode)");
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
        out.println("  --verify-hash              Compute & verify SHA-256 hash (default)");
        out.println();
        out.println("Other:");
        out.println("  -h, --help                 Show this help and exit");
        out.println("  -v, --version              Show version and exit");
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
        String dest2;
        boolean copyMode = true;
        boolean scheduledTree;
        boolean compareName;
        boolean compareContent;
        boolean verifyHash = true;
        boolean help;
        boolean showVersion;

        /** Parse args. Returns false on error. */
        boolean parse(String[] args) {
            for (int i = 0; i < args.length; i++) {
                switch (args[i]) {
                    case "-h":
                    case "--help":
                        help = true;
                        return true;
                    case "-v":
                    case "--version":
                        showVersion = true;
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
                    case "--dest2":
                        if (++i >= args.length) { err("--dest2 requires a path"); return false; }
                        dest2 = args[i];
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
