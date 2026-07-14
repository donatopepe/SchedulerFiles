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

    public void synchronize(Path primary, Path replica) throws IOException {
        removeStale(primary, replica);
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
                Path target = replica.resolve(primary.relativize(file));
                if (!Files.exists(target) || !HashUtil.sha256(file).equals(HashUtil.sha256(target))) {
                    Files.createDirectories(target.getParent());
                    Files.copy(file, target, StandardCopyOption.REPLACE_EXISTING,
                        StandardCopyOption.COPY_ATTRIBUTES);
                }
                return FileVisitResult.CONTINUE;
            }
        });
        if (!isSynchronized(primary, replica)) {
            throw new IOException("mirror parity check failed");
        }
    }

    public boolean isSynchronized(Path primary, Path replica) throws IOException {
        Set<Path> primaryFiles = files(primary);
        Set<Path> replicaFiles = files(replica);
        if (!primaryFiles.equals(replicaFiles)) return false;
        for (Path relative : primaryFiles) {
            if (!HashUtil.sha256(primary.resolve(relative)).equals(
                    HashUtil.sha256(replica.resolve(relative)))) return false;
        }
        return true;
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
