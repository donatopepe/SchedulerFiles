import java.io.Closeable;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

/** Exclusive process lock for mirror destination. */
public final class DestinationLock implements Closeable {
    private final FileChannel channel;
    private final FileLock lock;

    private DestinationLock(FileChannel channel, FileLock lock) {
        this.channel = channel;
        this.lock = lock;
    }

    public static DestinationLock acquire(Path destination) throws IOException {
        Files.createDirectories(destination);
        Path lockPath = destination.resolve(".schedulerfiles.lock");
        FileChannel channel = FileChannel.open(lockPath, StandardOpenOption.CREATE,
            StandardOpenOption.WRITE);
        try {
            FileLock acquired = channel.tryLock();
            if (acquired == null) {
                channel.close();
                throw new IOException("Destination is already in use: " + destination);
            }
            return new DestinationLock(channel, acquired);
        } catch (java.nio.channels.OverlappingFileLockException e) {
            channel.close();
            throw new IOException("Destination is already in use: " + destination, e);
        } catch (IOException e) {
            channel.close();
            throw e;
        }
    }

    @Override
    public void close() throws IOException {
        try { if (lock != null) lock.release(); }
        finally { channel.close(); }
    }
}
