import java.io.BufferedWriter;
import java.io.Closeable;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.SimpleDateFormat;
import java.util.Date;

/** Append-only transfer transaction log. */
public final class TransactionLog implements Closeable {

    public static final String FILE_NAME = "_files_scheduler.log";
    private final BufferedWriter writer;

    public TransactionLog(Path destination) throws IOException {
        Path path = destination.resolve(FILE_NAME);
        boolean exists = Files.exists(path);
        writer = new BufferedWriter(new FileWriter(path.toFile(), true));
        if (!exists) {
            write("# SchedulerFiles transaction log");
            write("# Op=COPY|MOVE|SKIP|HASH_MISMATCH  Time  Source  Dest  Size  Hash");
            write("# ===========================================================");
        }
    }

    public void startSession(Path source, Path destination) throws IOException {
        write("");
        write("# Session start: " + timestamp());
        write("# Source: " + source);
        write("# Destination: " + destination);
    }

    public void write(String line) throws IOException {
        writer.write(line);
        writer.newLine();
        writer.flush();
    }

    public static String timestamp() {
        return new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date());
    }

    @Override
    public void close() throws IOException {
        IOException failure = null;
        try {
            write("# ----- END -----");
        } catch (IOException e) {
            failure = e;
        }
        try {
            writer.close();
        } catch (IOException e) {
            if (failure == null) failure = e;
            else failure.addSuppressed(e);
        }
        if (failure != null) throw failure;
    }
}
