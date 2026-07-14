import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

/** Per-operation hash cache invalidated by size and modification time. */
public final class HashCache {
    private final Map<Path, Entry> entries = new HashMap<>();

    public String sha256(Path file) throws IOException {
        Path key = file.toAbsolutePath().normalize();
        long size = Files.size(key);
        long modified = Files.getLastModifiedTime(key).toMillis();
        Entry cached = entries.get(key);
        if (cached != null && cached.size == size && cached.modified == modified) {
            return cached.hash;
        }
        String hash = HashUtil.sha256(key);
        entries.put(key, new Entry(size, modified, hash));
        return hash;
    }

    public void invalidate(Path file) {
        entries.remove(file.toAbsolutePath().normalize());
    }

    private static final class Entry {
        final long size;
        final long modified;
        final String hash;
        Entry(long size, long modified, String hash) {
            this.size = size;
            this.modified = modified;
            this.hash = hash;
        }
    }
}
