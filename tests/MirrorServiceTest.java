import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;

public class MirrorServiceTest {

    public static List<Runnable> suite() {
        return Arrays.asList(
            MirrorServiceTest::testCopiesMissingFiles,
            MirrorServiceTest::testCopiesNestedFiles,
            MirrorServiceTest::testRemovesStaleFiles,
            MirrorServiceTest::testKeepsStaleFilesWhenRequested,
            MirrorServiceTest::testImportsReplicaExtras,
            MirrorServiceTest::testImportThenParity,
            MirrorServiceTest::testRemovesStaleDirectories,
            MirrorServiceTest::testUpdatesChangedFiles,
            MirrorServiceTest::testParityDetectsMissingFile,
            MirrorServiceTest::testParityDetectsChangedFile,
            MirrorServiceTest::testParityAcceptsIdenticalTrees,
            MirrorServiceTest::testDetectsStaleEntries,
            MirrorServiceTest::testEmptyTreesHaveParity,
            MirrorServiceTest::testSkipsSymbolicLinks
        );
    }

    static void testCopiesMissingFiles() {
        withDirs((primary, replica) -> {
            Files.write(primary.resolve("a.txt"), "alpha".getBytes("UTF-8"));
            new MirrorService().synchronize(primary, replica);
            TestRunner.assertTrue(Files.exists(replica.resolve("a.txt")), "mirror copies missing file");
        });
    }

    static void testCopiesNestedFiles() {
        withDirs((primary, replica) -> {
            Files.createDirectories(primary.resolve("one/two"));
            Files.write(primary.resolve("one/two/a.txt"), "nested".getBytes("UTF-8"));
            new MirrorService().synchronize(primary, replica);
            TestRunner.assertTrue(Files.exists(replica.resolve("one/two/a.txt")), "mirror creates nested path");
        });
    }

    static void testRemovesStaleFiles() {
        withDirs((primary, replica) -> {
            Files.write(replica.resolve("stale.txt"), "stale".getBytes("UTF-8"));
            new MirrorService().synchronize(primary, replica);
            TestRunner.assertFalse(Files.exists(replica.resolve("stale.txt")), "mirror removes stale file");
        });
    }

    static void testRemovesStaleDirectories() {
        withDirs((primary, replica) -> {
            Files.createDirectories(replica.resolve("stale/dir"));
            Files.write(replica.resolve("stale/dir/file.txt"), "stale".getBytes("UTF-8"));
            new MirrorService().synchronize(primary, replica);
            TestRunner.assertFalse(Files.exists(replica.resolve("stale")), "mirror removes stale directory");
        });
    }

    static void testKeepsStaleFilesWhenRequested() {
        withDirs((primary, replica) -> {
            Files.write(replica.resolve("extra.txt"), "extra".getBytes("UTF-8"));
            new MirrorService().synchronize(primary, replica, false);
            TestRunner.assertTrue(Files.exists(replica.resolve("extra.txt")),
                "mirror keeps stale file when deletion disabled");
        });
    }

    static void testImportsReplicaExtras() {
        withDirs((primary, replica) -> {
            Files.createDirectories(replica.resolve("imported"));
            Files.write(replica.resolve("imported/file.txt"), "import".getBytes("UTF-8"));
            new MirrorService().synchronize(primary, replica, false, true);
            TestRunner.assertEquals("import", new String(
                Files.readAllBytes(primary.resolve("imported/file.txt")), "UTF-8"),
                "mirror imports replica extra into primary");
        });
    }

    static void testImportThenParity() {
        withDirs((primary, replica) -> {
            Files.write(replica.resolve("extra.txt"), "extra".getBytes("UTF-8"));
            new MirrorService().synchronize(primary, replica, true, true);
            TestRunner.assertTrue(new MirrorService().isSynchronized(primary, replica),
                "mirror parity holds after importing extra");
            TestRunner.assertTrue(Files.exists(primary.resolve("extra.txt")),
                "imported extra remains in primary");
        });
    }

    static void testUpdatesChangedFiles() {
        withDirs((primary, replica) -> {
            Files.write(primary.resolve("a.txt"), "new".getBytes("UTF-8"));
            Files.write(replica.resolve("a.txt"), "old".getBytes("UTF-8"));
            new MirrorService().synchronize(primary, replica);
            TestRunner.assertEquals("new", new String(Files.readAllBytes(replica.resolve("a.txt")), "UTF-8"),
                "mirror updates changed file");
        });
    }

    static void testParityDetectsMissingFile() {
        withDirs((primary, replica) -> {
            Files.write(primary.resolve("a.txt"), "a".getBytes("UTF-8"));
            TestRunner.assertFalse(new MirrorService().isSynchronized(primary, replica), "parity detects missing file");
        });
    }

    static void testParityDetectsChangedFile() {
        withDirs((primary, replica) -> {
            Files.write(primary.resolve("a.txt"), "new".getBytes("UTF-8"));
            Files.write(replica.resolve("a.txt"), "old".getBytes("UTF-8"));
            TestRunner.assertFalse(new MirrorService().isSynchronized(primary, replica), "parity detects changed file");
        });
    }

    static void testParityAcceptsIdenticalTrees() {
        withDirs((primary, replica) -> {
            Files.write(primary.resolve("a.txt"), "same".getBytes("UTF-8"));
            Files.write(replica.resolve("a.txt"), "same".getBytes("UTF-8"));
            TestRunner.assertTrue(new MirrorService().isSynchronized(primary, replica), "parity accepts identical trees");
        });
    }

    static void testDetectsStaleEntries() {
        withDirs((primary, replica) -> {
            Files.write(replica.resolve("extra.txt"), "extra".getBytes("UTF-8"));
            TestRunner.assertTrue(new MirrorService().hasStaleEntries(primary, replica),
                "mirror detects stale entry");
        });
    }

    static void testEmptyTreesHaveParity() {
        withDirs((primary, replica) -> TestRunner.assertTrue(
            new MirrorService().isSynchronized(primary, replica),
            "empty trees have parity"));
    }

    static void testSkipsSymbolicLinks() {
        withDirs((primary, replica) -> {
            Files.write(primary.resolve("real.txt"), "real".getBytes("UTF-8"));
            try {
                Files.createSymbolicLink(primary.resolve("link.txt"), primary.resolve("real.txt"));
            } catch (UnsupportedOperationException | IOException | SecurityException e) {
                TestRunner.assertTrue(true, "symbolic-link test skipped on unsupported platform");
                return;
            }
            new MirrorService().synchronize(primary, replica);
            TestRunner.assertFalse(Files.exists(replica.resolve("link.txt")), "mirror skips symbolic link");
        });
    }

    private static void withDirs(DirectoryAction action) {
        Path primary = null;
        Path replica = null;
        try {
            primary = TestRunner.createTempDir("mirrorPrimary_");
            replica = TestRunner.createTempDir("mirrorReplica_");
            action.run(primary, replica);
        } catch (Exception e) {
            throw new RuntimeException(e);
        } finally {
            try { TestRunner.deleteRecursive(primary); } catch (Exception ignored) { }
            try { TestRunner.deleteRecursive(replica); } catch (Exception ignored) { }
        }
    }

    private interface DirectoryAction {
        void run(Path primary, Path replica) throws Exception;
    }
}
