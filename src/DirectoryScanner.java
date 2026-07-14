import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** Scans regular files without following symbolic links or revisiting directories. */
public final class DirectoryScanner {

    private DirectoryScanner() {
    }

    public static List<Path> scan(Path root, int maxDepth, final ScanListener listener)
            throws IOException {
        final List<Path> files = new ArrayList<>();
        final Set<Object> visitedDirectories = new HashSet<>();

        Files.walkFileTree(root, java.util.EnumSet.noneOf(java.nio.file.FileVisitOption.class),
            maxDepth, new SimpleFileVisitor<Path>() {
                @Override
                public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) {
                    if (Files.isSymbolicLink(dir)) {
                        listener.skipped(dir, "symbolic link");
                        return FileVisitResult.SKIP_SUBTREE;
                    }
                    Object identity = attrs.fileKey();
                    if (identity == null) {
                        try {
                            identity = dir.toRealPath(LinkOption.NOFOLLOW_LINKS).normalize();
                        } catch (IOException e) {
                            listener.skipped(dir, e.getMessage());
                            return FileVisitResult.SKIP_SUBTREE;
                        }
                    }
                    if (!visitedDirectories.add(identity)) {
                        listener.skipped(dir, "directory cycle");
                        return FileVisitResult.SKIP_SUBTREE;
                    }
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
                    if (attrs.isRegularFile() && !Files.isSymbolicLink(file)) {
                        files.add(file);
                    } else {
                        listener.skipped(file, "link or non-regular file");
                    }
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult visitFileFailed(Path file, IOException error) {
                    listener.skipped(file, error.getMessage());
                    return FileVisitResult.CONTINUE;
                }
            });
        return files;
    }

    public interface ScanListener {
        void skipped(Path path, String reason);
    }
}
