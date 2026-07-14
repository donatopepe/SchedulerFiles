import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.FileVisitOption;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.EnumSet;
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

public class MoveClass implements Runnable {

    private static final String TX_LOG_NAME = "_files_scheduler.log";
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
    private BufferedWriter txWriter;
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

    private boolean isStopped() {
        return stopRequested || Thread.currentThread().isInterrupted();
    }

    private void log(String message) {
        textlog.append(message + "\n");
        textlog.setCaretPosition(textlog.getDocument().getLength());
        if (!suppressLogger) {
            Logger.getLogger(MoveClass.class.getName()).info(message);
        }
    }

    // ---- Transaction log ----

    private void openTxLog() throws IOException {
        Path txPath = destination.resolve(TX_LOG_NAME);
        boolean exists = Files.exists(txPath);
        txWriter = new BufferedWriter(new FileWriter(txPath.toFile(), true));
        if (!exists) {
            writeTx("# SchedulerFiles transaction log");
            writeTx("# Op=COPY|MOVE|SKIP|HASH_MISMATCH  Time  Source  Dest  Size  Hash");
            writeTx("# ===========================================================");
        }
    }

    private void writeTx(String line) throws IOException {
        if (txWriter != null) {
            txWriter.write(line);
            txWriter.newLine();
            txWriter.flush();
        }
    }

    private String txTimestamp() {
        return new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date());
    }

    private void closeTxLog() {
        try {
            if (txWriter != null) {
                writeTx("# ----- END -----");
                txWriter.close();
            }
        } catch (IOException e) {
            log("Warning: could not close transaction log: " + e.getMessage());
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

        try (FileChannel ch1 = new RandomAccessFile(sourceFile.toString(), "r").getChannel();
             FileChannel ch2 = new RandomAccessFile(destFile.toString(), "r").getChannel()) {
            long size = ch1.size();
            long offset = 0;
            long chunkSize = Math.min(size, Integer.MAX_VALUE);
            while (chunkSize > 0) {
                MappedByteBuffer m1 = ch1.map(FileChannel.MapMode.READ_ONLY, offset, chunkSize);
                MappedByteBuffer m2 = ch2.map(FileChannel.MapMode.READ_ONLY, offset, chunkSize);
                for (int pos = 0; pos < chunkSize; pos++) {
                    if (m1.get(pos) != m2.get(pos)) return false;
                    if (isStopped()) return false;
                }
                offset += chunkSize;
                chunkSize = Math.min(ch1.size() - offset, Integer.MAX_VALUE);
            }
            log("Byte compare: " + (System.nanoTime() - start) / 1000000 + "ms");
            return true;
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
            File dir = new File(destDir);
            File[] existing = dir.listFiles();
            HashSet<String> destNames = new HashSet<>();
            Map<String, String> destPaths = new HashMap<>();
            if (existing != null) {
                for (File f : existing) {
                    if (f.isFile()) {
                        destNames.add(f.getName());
                        destPaths.put(f.getName(), f.getAbsolutePath());
                    }
                }
            }

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
        if (copyMode) {
            try {
                Files.copy(sourceFile, targetPath, StandardCopyOption.COPY_ATTRIBUTES);
                log("Copied: " + sourceFile + " -> " + targetPath);
                success = true;
            } catch (IOException ex) { log("Copy failed: " + ex.getMessage()); }
        } else {
            try {
                Files.move(sourceFile, targetPath);
                log("Moved: " + sourceFile + " -> " + targetPath);
                success = true;
            } catch (IOException ex) { log("Move failed: " + ex.getMessage()); }
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
        writeTx(tx.toString());
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
                String relative = filePath.substring(source.toString().length());
                int lastSep = relative.lastIndexOf(File.separatorChar);
                if (lastSep < 0 && File.separatorChar != '/') lastSep = relative.lastIndexOf('/');
                String dirPart = (lastSep >= 0) ? relative.substring(0, lastSep + 1) : File.separator;
                dest = destination + dirPart;
            }

            createDestDir(dest);
            transferFile(fileio, dest);
        } catch (IOException | InterruptedException ex) {
            Logger.getLogger(MoveClass.class.getName()).log(Level.SEVERE, "Error processing " + filePath, ex);
        }
    }

    // ---- File listing (iterative walk, depth-limited, no recursive stack overflow) ----

    private void listFiles(File dir) {
        if (isStopped()) return;

        final Path dstAbs = destination.toAbsolutePath().normalize();
        final Path srcAbs = dir.toPath().toAbsolutePath().normalize();
        boolean skipDest = srcAbs.equals(dstAbs) || srcAbs.startsWith(dstAbs) || dstAbs.startsWith(srcAbs);

        synchronized (fileAccumulator) { fileAccumulator.clear(); }

        try {
            EnumSet<FileVisitOption> opts = EnumSet.of(FileVisitOption.FOLLOW_LINKS);
            Files.walkFileTree(dir.toPath(), opts, MAX_DEPTH, new SimpleFileVisitor<Path>() {
                @Override
                public FileVisitResult preVisitDirectory(Path dirPath, BasicFileAttributes attrs) {
                    if (isStopped()) return FileVisitResult.TERMINATE;
                    if (skipDest) {
                        Path abs = dirPath.toAbsolutePath().normalize();
                        if (abs.equals(dstAbs) || abs.startsWith(dstAbs))
                            return FileVisitResult.SKIP_SUBTREE;
                    }
                    return FileVisitResult.CONTINUE;
                }
                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
                    if (isStopped()) return FileVisitResult.TERMINATE;
                    // Skip symlinks that point back to already-visited dirs
                    if (attrs.isSymbolicLink()) {
                        try {
                            Path real = file.toRealPath();
                            if (real.startsWith(srcAbs) && !real.equals(file)) {
                                log("Skipping symlink cycle: " + file);
                                return FileVisitResult.CONTINUE;
                            }
                        } catch (IOException e) {
                            return FileVisitResult.CONTINUE;
                        }
                    }
                    synchronized (fileAccumulator) { fileAccumulator.add(file.toString()); }
                    return FileVisitResult.CONTINUE;
                }
                @Override
                public FileVisitResult visitFileFailed(Path file, IOException exc) {
                    return FileVisitResult.CONTINUE;
                }
            });
        } catch (IOException e) {
            log("Error walking files: " + e.getMessage());
        }
    }

    // ---- Main run ----

    @Override
    public void run() {
        try {
            log("===== START =====");

            try {
                openTxLog();
                writeTx("# " + txTimestamp() + "  Session start  " + (copyMode ? "COPY" : "MOVE")
                    + "  from=" + source + "  to=" + destination);
            } catch (IOException e) {
                log("Warning: could not create transaction log: " + e.getMessage());
            }

            listFiles(source.toFile());
            int totalFiles;
            synchronized (fileAccumulator) { totalFiles = fileAccumulator.size(); }
            log("Found " + totalFiles + " files to process");

            int processed = 0;
            synchronized (fileAccumulator) {
                for (String f : fileAccumulator) {
                    if (isStopped()) {
                        log("===== STOPPED =====");
                        writeTx("# " + txTimestamp() + "  *** STOPPED ***");
                        return;
                    }
                    processFile(f);
                    processed++;
                    double pct = ((double) processed / totalFiles) * 100;
                    progressLabel.setText(String.format("%.1f%% (%d/%d)", pct, processed, totalFiles));
                }
                fileAccumulator.clear();
            }

            log("===== END =====");
        } catch (Exception ex) {
            Logger.getLogger(MoveClass.class.getName()).log(Level.SEVERE, "Fatal error", ex);
        } finally {
            closeTxLog();
            Logger log = Logger.getLogger(MoveClass.class.getName());
            for (Handler h : log.getHandlers()) { h.close(); log.removeHandler(h); }
        }
    }
}
