import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Path;

/** File comparison operations used by transfers. */
public final class FileComparator {

    private FileComparator() {
    }

    public static boolean sameBytes(Path first, Path second) throws IOException {
        if (!Files.exists(second) || Files.size(first) != Files.size(second)) return false;

        try (RandomAccessFile firstFile = new RandomAccessFile(first.toFile(), "r");
             RandomAccessFile secondFile = new RandomAccessFile(second.toFile(), "r");
             FileChannel firstChannel = firstFile.getChannel();
             FileChannel secondChannel = secondFile.getChannel()) {
            long size = firstChannel.size();
            long position = 0;
            final long chunkSize = 16L * 1024 * 1024;
            while (position < size) {
                long remaining = Math.min(chunkSize, size - position);
                MappedByteBuffer firstBuffer = firstChannel.map(
                    FileChannel.MapMode.READ_ONLY, position, remaining);
                MappedByteBuffer secondBuffer = secondChannel.map(
                    FileChannel.MapMode.READ_ONLY, position, remaining);
                if (!firstBuffer.equals(secondBuffer)) return false;
                position += remaining;
            }
            return true;
        }
    }
}
