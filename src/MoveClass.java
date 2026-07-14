import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Calendar;
import java.util.List;
import java.util.GregorianCalendar;
import java.util.HashSet;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeSet;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JLabel;
import javax.swing.JTextArea;
import javax.swing.SwingUtilities;

public class MoveClass implements Runnable {

    private static final int MAX_DEPTH = 100;

    private static volatile boolean suppressLogger;

    public static void setSuppressLogger(boolean suppress) {
        suppressLogger = suppress;
        if (suppress) {
            Logger.getLogger(MoveClass.class.getName()).setUseParentHandlers(false);
        }
    }

    private final Path source;
    private final Path destination;
    private final JTextArea textlog;
    private final JLabel progressLabel;
    private final boolean compareByContent;
    private final boolean compareByName;
    private final boolean copyMode;
    private final boolean useScheduledTree;
    private final boolean verifyHash;
    private final Calendar calendar = new GregorianCalendar();
    private volatile boolean stopRequested;
    private volatile boolean operationFailed;
    private TransactionLog txLog;
    private boolean txLogFailed;
    private final TransferService transferService = new TransferService();
    private final Map<Path, DestinationIndex> destinationIndexes = new HashMap<>();
    private String cachedSourceHash;  // avoids double hash in skip+verify

    private final TreeSet<String> fileAccumulator = new TreeSet<>(new LengthFirstComparator());

    public MoveClass(Path source, Path destination, JTextArea textlog, JLabel progressLabel,
                     boolean compareByContent, boolean compareByName, boolean copyMode,
                     boolean useScheduledTree, boolean verifyHash) {
        this.source = source;
        this.destination = destination;
        this.textlog = textlog;
        this.progressLabel = progressLabel;
        this.compareByContent = compareByContent;
        this.compareByName = compareByName;
        this.copyMode = copyMode;
        this.useScheduledTree = useScheduledTree;
        this.verifyHash = verifyHash;
        this.stopRequested = false;
    }

    public void requestStop() {
        this.stopRequested = true;
    }

    public boolean isCancelled() {
        return stopRequested;
    }

    public boolean hasErrors() {
        return operationFailed;
    }

    private boolean isStopped() {
        return stopRequested || Thread.currentThread().isInterrupted();
    }

    private void log(String message) {
        updateSwing(() -> {
            textlog.append(message + "\n");
            textlog.setCaretPosition(textlog.getDocument().getLength());
        });
        if (!suppressLogger) {
            Logger.getLogger(MoveClass.class.getName()).info(message);
        }
    }

    private void setProgress(String text) {
        updateSwing(() -> progressLabel.setText(text));
    }

    private void updateSwing(Runnable update) {
        if (SwingUtilities.isEventDispatchThread()) {
            update.run();
            return;
        }
        try {
            SwingUtilities.invokeAndWait(update);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } catch (java.lang.reflect.InvocationTargetException e) {
            Logger.getLogger(MoveClass.class.getName()).log(
                Level.WARNING, "Swing update failed", e.getCause());
        }
    }

    // ---- Transaction log ----

    private void openTxLog() throws IOException {
        txLog = new TransactionLog(destination);
    }

    private void writeTx(String line) throws IOException {
        if (txLog != null) txLog.write(line);
    }

    private void writeTransferTx(String line, String operation, Path targetPath) {
        try {
            writeTx(line);
        } catch (IOException e) {
            operationFailed = true;
            txLogFailed = true;
            log("Transaction log failed after successful " + operation + ": "
                + targetPath + " (" + e.getMessage() + ")");
        }
    }

    private String txTimestamp() {
        return TransactionLog.timestamp();
    }

    private void closeTxLog() {
        if (txLog == null) return;
        try {
            txLog.close();
        } catch (IOException e) {
            operationFailed = true;
            txLogFailed = true;
            log("Transaction log close failed: " + e.getMessage());
        } finally {
            txLog = null;
        }
    }

    // ---- File comparison ----

    private String getSourceHash(Path file) throws IOException {
        if (cachedSourceHash == null) {
            cachedSourceHash = HashUtil.sha256(file);
        }
        return cachedSourceHash;
    }

    private boolean filesAreIdentical(Path sourceFile, String destPath) throws IOException, InterruptedException {
        Path destFile = Paths.get(destPath);
        if (!Files.exists(destFile)) return false;
        if (Files.size(destFile) != Files.size(sourceFile)) return false;

        long start = System.nanoTime();

        if (verifyHash) {
            try {
                String srcHash = getSourceHash(sourceFile);
                String dstHash = HashUtil.sha256(destFile);
                log("Hash compare: " + (System.nanoTime() - start) / 1000000 + "ms  " + srcHash.substring(0, 12) + "...");
                return srcHash.equals(dstHash);
            } catch (IOException e) {
                log("Hash compare error, fallback to byte compare: " + e.getMessage());
            }
        }

        try {
            boolean identical = FileComparator.sameBytes(sourceFile, destFile);
            log("Byte compare: " + (System.nanoTime() - start) / 1000000 + "ms");
            return identical;
        } catch (IOException e) {
            log("Compare error: " + e.getMessage());
            return false;
        }
    }

    // ---- File transfer with hash verification & transaction log ----

    private void transferFile(Path sourceFile, String destDir) throws IOException, InterruptedException {
        cachedSourceHash = null;  // reset per file

        String name = sourceFile.getFileName().toString();
        String ext = "";
        String baseName = name;
        int dot = name.lastIndexOf(".");
        if (dot >= 0) {
            ext = name.substring(dot).toLowerCase();
            baseName = name.substring(0, dot);
        }
        long fileSize = Files.size(sourceFile);

        // Check duplicates using HashSet + HashMap (O(1) name lookup)
        boolean skip = false;
        String skipDestPath = null;

        if (compareByName || compareByContent) {
            DestinationIndex index = getDestinationIndex(Paths.get(destDir));
            HashSet<String> destNames = index.names;
            Map<String, String> destPaths = index.paths;

            if (compareByName && !compareByContent) {
                if (destNames.contains(name)) {
                    skipDestPath = destPaths.get(name);
                    skip = true;
                }
            } else if (compareByContent) {
                for (Map.Entry<String, String> e : destPaths.entrySet()) {
                    if (filesAreIdentical(sourceFile, e.getValue())) {
                        skipDestPath = e.getValue();
                        skip = true;
                        break;
                    }
                }
            }
        }

        if (skip) {
            log("Skipping identical: " + sourceFile + " == " + skipDestPath);
            String hashInfo = "";
            if (verifyHash) {
                try { hashInfo = "  H=" + getSourceHash(sourceFile).substring(0, 16); }
                catch (Exception ignored) {}
            }
            writeTx(txTimestamp() + "  SKIP  " + sourceFile + "  " + skipDestPath + "  " + fileSize + hashInfo);
            return;
        }

        // Find unique name using Path.resolve (safe path concatenation)
        Path destDirPath = Paths.get(destDir);
        String suffix = "";
        int counter = 0;
        Path targetPath = destDirPath.resolve(baseName + suffix + ext);
        while (Files.exists(targetPath)) {
            counter++;
            suffix = " " + counter;
            targetPath = destDirPath.resolve(baseName + suffix + ext);
        }

        // Compute source hash before operation
        String sourceHash = null;
        if (verifyHash) {
            try { sourceHash = getSourceHash(sourceFile); }
            catch (Exception e) { log("Warning: could not hash source: " + e.getMessage()); }
        }

        String op = copyMode ? "COPY" : "MOVE";
        boolean success = false;
        try {
            transferService.transfer(sourceFile, targetPath, copyMode);
            log((copyMode ? "Copied: " : "Moved: ") + sourceFile + " -> " + targetPath);
            success = true;
        } catch (IOException ex) {
            operationFailed = true;
            log((copyMode ? "Copy failed: " : "Move failed: ") + ex.getMessage());
        }
        if (!success) return;

        // Verify hash on destination
        String destHash = null;
        boolean hashOk = true;
        if (verifyHash && sourceHash != null) {
            try {
                destHash = HashUtil.sha256(targetPath);
                if (sourceHash.equals(destHash)) {
                    log("Hash OK: " + sourceHash.substring(0, 16) + "...  " + targetPath.getFileName());
                } else {
                    hashOk = false;
                    operationFailed = true;
                    log("HASH MISMATCH! " + targetPath.getFileName()
                        + "\n  source: " + sourceHash + "\n  dest:   " + destHash);
                }
            } catch (Exception e) {
                log("Warning: could not hash destination: " + e.getMessage());
            }
        }

        StringBuilder tx = new StringBuilder()
            .append(txTimestamp()).append("  ").append(op)
            .append("  ").append(sourceFile)
            .append("  ").append(targetPath)
            .append("  ").append(fileSize);
        if (sourceHash != null) tx.append("  H=").append(sourceHash.substring(0, 16));
        if (destHash != null) tx.append("  D=").append(destHash.substring(0, 16));
        if (!hashOk) tx.append("  ** HASH MISMATCH **");
        writeTransferTx(tx.toString(), op, targetPath);
        DestinationIndex index = destinationIndexes.get(destDirPath.toAbsolutePath().normalize());
        if (index != null) index.add(targetPath);
    }

    private DestinationIndex getDestinationIndex(Path directory) {
        Path key = directory.toAbsolutePath().normalize();
        DestinationIndex index = destinationIndexes.get(key);
        if (index != null) return index;
        index = new DestinationIndex(key);
        destinationIndexes.put(key, index);
        return index;
    }

    private static final class DestinationIndex {
        final HashSet<String> names = new HashSet<>();
        final Map<String, String> paths = new HashMap<>();

        DestinationIndex(Path directory) {
            File[] existing = directory.toFile().listFiles();
            if (existing != null) {
                for (File file : existing) {
                    if (file.isFile()) add(file.toPath());
                }
            }
        }

        void add(Path file) {
            names.add(file.getFileName().toString());
            paths.put(file.getFileName().toString(), file.toAbsolutePath().toString());
        }
    }

    private void createDestDir(String dirPath) throws IOException {
        Files.createDirectories(Paths.get(dirPath));
    }

    // ---- File processing ----

    private void processFile(String filePath) {
        try {
            Path fileio = Paths.get(filePath);
            String name = fileio.getFileName().toString();
            BasicFileAttributes attrs = Files.readAttributes(fileio, BasicFileAttributes.class);
            calendar.setTimeInMillis(attrs.lastModifiedTime().toMillis());
            int year = calendar.get(Calendar.YEAR);
            int month = calendar.get(Calendar.MONTH) + 1;

            String dest;
            if (useScheduledTree) {
                dest = destination + "/" + year + "/" + month;
                int dot = name.lastIndexOf(".");
                if (dot >= 0) dest += "/" + name.substring(dot + 1).toLowerCase();
                dest += "/";
                log("(" + attrs.size() + " bytes, " + year + "-" + month + ")");
            } else {
                Path sourceRoot = source.toAbsolutePath().normalize();
                Path filePathAbs = fileio.toAbsolutePath().normalize();
                Path relative = sourceRoot.relativize(filePathAbs);
                Path parent = relative.getParent();
                dest = destination.resolve(parent == null ? Paths.get("") : parent).toString();
            }

            createDestDir(dest);
            transferFile(fileio, dest);
        } catch (IOException | InterruptedException ex) {
            operationFailed = true;
            Logger.getLogger(MoveClass.class.getName()).log(Level.SEVERE, "Error processing " + filePath, ex);
        }
    }

    // ---- File listing (depth-limited, links skipped) ----

    private void listFiles(File dir) {
        if (isStopped()) return;

        final Path dstAbs = destination.toAbsolutePath().normalize();
        synchronized (fileAccumulator) { fileAccumulator.clear(); }

        try {
            List<Path> scanned = DirectoryScanner.scan(dir.toPath(), MAX_DEPTH,
                (path, reason) -> log("Skipped: " + path + " (" + reason + ")"));
            synchronized (fileAccumulator) {
                for (Path file : scanned) {
                    if (isStopped()) break;
                    Path absolute = file.toAbsolutePath().normalize();
                    if (!absolute.equals(dstAbs) && !absolute.startsWith(dstAbs)) {
                        fileAccumulator.add(file.toString());
                    }
                }
            }
        } catch (IOException e) {
            operationFailed = true;
            log("Error walking files: " + e.getMessage());
        }
    }

    // ---- Main run ----

    @Override
    public void run() {
        try {
            log("===== START =====");
            if (isStopped()) {
                setProgress("Cancelled");
                log("===== STOPPED =====");
                return;
            }

            try {
                openTxLog();
                txLog.startSession(source, destination);
            } catch (IOException e) {
                operationFailed = true;
                log("Warning: could not create transaction log: " + e.getMessage());
            }

            listFiles(source.toFile());
            int totalFiles;
            synchronized (fileAccumulator) { totalFiles = fileAccumulator.size(); }
            log("Found " + totalFiles + " files to process");
            if (totalFiles == 0) {
                setProgress("100.0% (0/0)");
            }

            int processed = 0;
            synchronized (fileAccumulator) {
                for (String f : fileAccumulator) {
                    if (isStopped()) {
                        setProgress("Cancelled (" + processed + "/" + totalFiles + ")");
                        log("===== STOPPED =====");
                        if (txLog != null) writeTx("# " + txTimestamp() + "  *** STOPPED ***");
                        return;
                    }
                    processFile(f);
                    processed++;
                    double pct = totalFiles == 0 ? 100.0 : ((double) processed / totalFiles) * 100;
                    setProgress(String.format("%.1f%% (%d/%d)", pct, processed, totalFiles));
                }
                fileAccumulator.clear();
            }

            log("===== END =====");
        } catch (Exception ex) {
            operationFailed = true;
            Logger.getLogger(MoveClass.class.getName()).log(Level.SEVERE, "Fatal error", ex);
        } finally {
            closeTxLog();
            Logger log = Logger.getLogger(MoveClass.class.getName());
            for (Handler h : log.getHandlers()) { h.close(); log.removeHandler(h); }
        }
    }
}
