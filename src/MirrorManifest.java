import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Set;

/** SHA-256 manifest written after successful mirror parity. */
public final class MirrorManifest {
    public static final String FILE_NAME = ".schedulerfiles-manifest";
    private final HashCache hashCache = new HashCache();

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
                out.write(hashCache.sha256(file));
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

    /** Returns true when manifest exists and every listed file still matches. */
    public boolean isValid(Path root) throws IOException {
        Path manifest = root.resolve(FILE_NAME);
        if (!Files.isRegularFile(manifest)) return false;
        java.util.List<String> lines = Files.readAllLines(manifest,
            java.nio.charset.StandardCharsets.UTF_8);
        for (String line : lines) {
            String[] fields = line.split("\\t", -1);
            if (fields.length != 3) return false;
            Path file = root.resolve(fields[0]).normalize();
            if (!file.startsWith(root.toAbsolutePath().normalize()) || !Files.isRegularFile(file)) return false;
            if (Files.size(file) != Long.parseLong(fields[1]) || !hashCache.sha256(file).equals(fields[2])) return false;
        }
        return true;
    }
}
