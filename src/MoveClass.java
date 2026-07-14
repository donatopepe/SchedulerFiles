import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.TreeSet;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JLabel;
import javax.swing.JTextArea;

public class MoveClass implements Runnable {

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
        Logger.getLogger(MoveClass.class.getName()).info(message);
    }

    // ---- File comparison ----

    private boolean filesAreIdentical(Path sourceFile, String destPath) throws IOException, InterruptedException {
        Path destFile = Paths.get(destPath);
        if (!Files.exists(destFile)) {
            return false;
        }
        if (Files.size(destFile) != Files.size(sourceFile)) {
            return false;
        }

        try (FileChannel ch1 = new RandomAccessFile(sourceFile.toString(), "r").getChannel();
             FileChannel ch2 = new RandomAccessFile(destFile.toString(), "r").getChannel()) {

            long size = ch1.size();
            long offset = 0;
            long chunkSize = Math.min(size, Integer.MAX_VALUE);

            long start = System.nanoTime();

            while (chunkSize > 0) {
                MappedByteBuffer m1 = ch1.map(FileChannel.MapMode.READ_ONLY, offset, chunkSize);
                MappedByteBuffer m2 = ch2.map(FileChannel.MapMode.READ_ONLY, offset, chunkSize);

                for (int pos = 0; pos < chunkSize; pos++) {
                    if (m1.get(pos) != m2.get(pos)) {
                        return false;
                    }
                    if (isStopped()) {
                        return false;
                    }
                }

                offset += chunkSize;
                chunkSize = Math.min(ch1.size() - offset, Integer.MAX_VALUE);
            }

            long elapsed = System.nanoTime() - start;
            log("Compare time: " + (elapsed / 1000000) + "ms");
            return true;
        } catch (IOException e) {
            log("Compare error: " + e.getMessage());
            return false;
        }
    }

    // ---- File move/copy with hash verification ----

    private void transferFile(Path sourceFile, String destDir) throws IOException, InterruptedException {
        String name = sourceFile.getFileName().toString();
        String ext = "";
        String baseName = name;

        int dot = name.lastIndexOf(".");
        if (dot >= 0) {
            ext = name.substring(dot).toLowerCase();
            baseName = name.substring(0, dot);
        }

        // Check if identical file already exists
        boolean skip = false;
        if (compareByName || compareByContent) {
            File dir = new File(destDir);
            File[] existing = dir.listFiles();
            if (existing != null) {
                for (File f : existing) {
                    if (f.isFile()) {
                        boolean same = false;
                        if (!compareByContent && compareByName) {
                            same = f.getName().equals(name);
                        } else {
                            same = filesAreIdentical(sourceFile, f.getAbsolutePath());
                        }
                        if (same) {
                            log("Skipping identical: " + sourceFile + " == " + f.getAbsolutePath());
                            skip = true;
                            break;
                        }
                    }
                }
            }
        }

        if (skip) {
            return;
        }

        // Find unique name
        String suffix = "";
        int counter = 0;
        Path targetPath = Paths.get(destDir + baseName + suffix + ext);
        while (Files.exists(targetPath)) {
            counter++;
            suffix = " " + counter;
            targetPath = Paths.get(destDir + baseName + suffix + ext);
        }

        // Compute source hash before operation (if verifyHash enabled)
        String sourceHash = null;
        if (verifyHash) {
            try {
                sourceHash = HashUtil.sha256(sourceFile);
            } catch (Exception e) {
                log("Warning: could not hash source " + sourceFile + ": " + e.getMessage());
            }
        }

        // Perform copy or move
        if (copyMode) {
            try {
                Files.copy(sourceFile, targetPath, StandardCopyOption.COPY_ATTRIBUTES);
                log("Copied: " + sourceFile + " -> " + targetPath);
            } catch (IOException ex) {
                log("Copy failed: " + ex.getMessage());
                return;
            }
        } else {
            try {
                Files.move(sourceFile, targetPath);
                log("Moved: " + sourceFile + " -> " + targetPath);
            } catch (IOException ex) {
                log("Move failed: " + ex.getMessage());
                return;
            }
        }

        // Verify hash on destination
        if (verifyHash && sourceHash != null) {
            try {
                String destHash = HashUtil.sha256(targetPath);
                if (sourceHash.equals(destHash)) {
                    log("Hash OK: " + sourceHash.substring(0, 16) + "...  " + targetPath.getFileName());
                } else {
                    log("HASH MISMATCH! " + targetPath.getFileName()
                        + "\n  source: " + sourceHash
                        + "\n  dest:   " + destHash);
                }
            } catch (Exception e) {
                log("Warning: could not hash destination " + targetPath + ": " + e.getMessage());
            }
        }
    }

    private void ensureDirExists(String dirPath) throws IOException {
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
                dest = destination.toString() + "/" + year + "/" + month;
                int dot = name.lastIndexOf(".");
                if (dot >= 0) {
                    dest += "/" + name.substring(dot + 1).toLowerCase();
                }
                dest += "/";
                log("(" + attrs.size() + " bytes, " + year + "-" + month + ")");
            } else {
                // Keep original tree structure: dest + relative directory (without filename)
                String relative = filePath.substring(source.toString().length());
                int lastSep = relative.lastIndexOf("/");
                if (lastSep < 0) lastSep = relative.lastIndexOf("\\");
                String dirPart = (lastSep >= 0) ? relative.substring(0, lastSep + 1) : "/";
                dest = destination.toString() + dirPart;
            }

            ensureDirExists(dest);
            transferFile(fileio, dest);
        } catch (IOException | InterruptedException ex) {
            Logger.getLogger(MoveClass.class.getName()).log(Level.SEVERE, "Error processing " + filePath, ex);
        }
    }

    // ---- File listing ----

    private void listFiles(File dir) {
        if (isStopped()) return;

        File[] entries = dir.listFiles();
        if (entries == null) return;

        for (File entry : entries) {
            if (isStopped()) return;

            if (entry.isDirectory()) {
                listFiles(entry);
            } else {
                synchronized (fileAccumulator) {
                    fileAccumulator.add(entry.getPath());
                }
            }
        }
    }

    // ---- Main run ----

    @Override
    public void run() {
        try {
            textlog.setText("");
            log("===== START =====");

            synchronized (fileAccumulator) {
                fileAccumulator.clear();
            }
            listFiles(source.toFile());
            int totalFiles;
            synchronized (fileAccumulator) {
                totalFiles = fileAccumulator.size();
            }
            log("Found " + totalFiles + " files to process");

            int processed = 0;
            synchronized (fileAccumulator) {
                for (String f : fileAccumulator) {
                    if (isStopped()) {
                        log("===== STOPPED =====");
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
            Logger log = Logger.getLogger(MoveClass.class.getName());
            for (Handler h : log.getHandlers()) {
                h.close();
                log.removeHandler(h);
            }
        }
    }
}
