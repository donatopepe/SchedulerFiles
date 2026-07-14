import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

/** Performs one file copy or move. */
public final class TransferService {

    public Path transfer(Path source, Path target, boolean copyMode) throws IOException {
        if (copyMode) {
            return AtomicFileTransfer.copy(source, target);
        }
        return Files.move(source, target);
    }
}
