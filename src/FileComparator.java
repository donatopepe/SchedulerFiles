import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

/** File comparison operations used by transfers. */
public final class FileComparator {

    private FileComparator() {
    }

    public static boolean sameBytes(Path first, Path second) throws IOException {
        if (!Files.exists(second) || Files.size(first) != Files.size(second)) return false;
        byte[] firstBuffer = new byte[1024 * 1024];
        byte[] secondBuffer = new byte[firstBuffer.length];
        try (InputStream firstStream = Files.newInputStream(first);
             InputStream secondStream = Files.newInputStream(second)) {
            int firstRead;
            while ((firstRead = readChunk(firstStream, firstBuffer)) > 0) {
                int secondRead = readChunk(secondStream, secondBuffer);
                if (firstRead != secondRead) return false;
                for (int i = 0; i < firstRead; i++) {
                    if (firstBuffer[i] != secondBuffer[i]) return false;
                }
            }
            return secondStream.read() == -1;
        }
    }

    private static int readChunk(InputStream stream, byte[] buffer) throws IOException {
        int total = 0;
        int read;
        while (total < buffer.length && (read = stream.read(buffer, total,
                buffer.length - total)) != -1) {
            total += read;
        }
        return total;
    }
}
