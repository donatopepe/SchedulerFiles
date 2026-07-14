import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Set;

/** SHA-256 manifest written after successful mirror parity. */
public final class MirrorManifest {
    public static final String FILE_NAME = ".schedulerfiles-manifest";

    public void write(Path root, Set<Path> files) throws IOException {
        Path manifest = root.resolve(FILE_NAME);
        Path temp = root.resolve(FILE_NAME + ".tmp");
        try (BufferedWriter out = Files.newBufferedWriter(temp, java.nio.charset.StandardCharsets.UTF_8,
                StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING)) {
            for (Path relative : files) {
                Path file = root.resolve(relative);
                out.write(relative.toString().replace('\\', '/'));
                out.write("\t");
                out.write(Long.toString(Files.size(file)));
                out.write("\t");
                out.write(HashUtil.sha256(file));
                out.newLine();
            }
        }
        try {
            Files.move(temp, manifest, java.nio.file.StandardCopyOption.ATOMIC_MOVE,
                java.nio.file.StandardCopyOption.REPLACE_EXISTING);
        } catch (java.nio.file.AtomicMoveNotSupportedException e) {
            Files.move(temp, manifest, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
        }
    }
}
