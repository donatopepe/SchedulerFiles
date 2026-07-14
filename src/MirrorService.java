import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.HashSet;
import java.util.Set;

/** Keeps two copy destinations identical and verifies mirror parity. */
public final class MirrorService {

    public boolean hasStaleEntries(Path primary, Path replica) throws IOException {
        Set<Path> primaryFiles = files(primary);
        Set<Path> replicaFiles = files(replica);
        for (Path relative : replicaFiles) {
            if (!primaryFiles.contains(relative)) return true;
        }
        return false;
    }

    public void synchronize(Path primary, Path replica) throws IOException {
        synchronize(primary, replica, true);
    }

    public void synchronize(Path primary, Path replica, boolean removeStaleEntries) throws IOException {
        synchronize(primary, replica, removeStaleEntries, false);
    }

    /** Synchronizes mirror; copyReplicaExtras copies Dest 2 extras into primary. */
    public void synchronize(Path primary, Path replica, boolean removeStaleEntries,
                            boolean copyReplicaExtras) throws IOException {
        if (copyReplicaExtras) copyExtrasToPrimary(primary, replica);
        if (removeStaleEntries) removeStale(primary, replica);
        Files.walkFileTree(primary, new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs)
                    throws IOException {
                Files.createDirectories(replica.resolve(primary.relativize(dir)));
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs)
                    throws IOException {
                if (Files.isSymbolicLink(file) || !attrs.isRegularFile()) {
                    return FileVisitResult.CONTINUE;
                }
                Path target = replica.resolve(primary.relativize(file));
                if (!Files.exists(target) || !HashUtil.sha256(file).equals(HashUtil.sha256(target))) {
                    Files.createDirectories(target.getParent());
                    Files.copy(file, target, StandardCopyOption.REPLACE_EXISTING,
                        StandardCopyOption.COPY_ATTRIBUTES);
                }
                return FileVisitResult.CONTINUE;
            }
        });
        if (!isSynchronized(primary, replica, removeStaleEntries || copyReplicaExtras)) {
            throw new IOException("mirror parity check failed");
        }
    }

    public boolean isSynchronized(Path primary, Path replica) throws IOException {
        return isSynchronized(primary, replica, true);
    }

    private boolean isSynchronized(Path primary, Path replica, boolean requireSameFiles)
            throws IOException {
        Set<Path> primaryFiles = files(primary);
        Set<Path> replicaFiles = files(replica);
        if (requireSameFiles && !primaryFiles.equals(replicaFiles)) return false;
        for (Path relative : primaryFiles) {
            Path replicaFile = replica.resolve(relative);
            if (!Files.exists(replicaFile)) return false;
            if (!HashUtil.sha256(primary.resolve(relative)).equals(
                    HashUtil.sha256(replicaFile))) return false;
        }
        return true;
    }

    private void copyExtrasToPrimary(Path primary, Path replica) throws IOException {
        Set<Path> primaryFiles = files(primary);
        Set<Path> replicaFiles = files(replica);
        for (Path relative : replicaFiles) {
            if (!primaryFiles.contains(relative)) {
                Path source = replica.resolve(relative);
                Path target = primary.resolve(relative);
                Files.createDirectories(target.getParent());
                Files.copy(source, target, StandardCopyOption.REPLACE_EXISTING,
                    StandardCopyOption.COPY_ATTRIBUTES);
            }
        }
    }

    private void removeStale(Path primary, Path replica) throws IOException {
        Files.walkFileTree(replica, new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                if (!Files.exists(primary.resolve(replica.relativize(file)))) Files.delete(file);
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult postVisitDirectory(Path dir, IOException error) throws IOException {
                if (error != null) throw error;
                if (!dir.equals(replica) && !Files.exists(primary.resolve(replica.relativize(dir)))) {
                    Files.deleteIfExists(dir);
                }
                return FileVisitResult.CONTINUE;
            }
        });
    }

    private Set<Path> files(Path root) throws IOException {
        Set<Path> result = new HashSet<>();
        Files.walkFileTree(root, new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
                if (attrs.isRegularFile() && !Files.isSymbolicLink(file)) {
                    result.add(root.relativize(file));
                }
                return FileVisitResult.CONTINUE;
            }
        });
        return result;
    }
}
