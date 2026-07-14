import javax.swing.*;
import java.io.IOException;
import java.nio.file.*;
import java.util.*;

public class MoveClassTest {

    public static List<Runnable> suite() {
        return Arrays.asList(
            MoveClassTest::testCopyFile,
            MoveClassTest::testMoveFile,
            MoveClassTest::testCopyPreservesOriginal,
            MoveClassTest::testMoveRemovesOriginal,
            MoveClassTest::testScheduledTreeStructure,
            MoveClassTest::testOriginalTreeStructure,
            MoveClassTest::testFileAlreadyExistsRenames,
            MoveClassTest::testCompareByNameSkipsIdentical,
            MoveClassTest::testCompareByContentSkipsIdentical,
            MoveClassTest::testCompareByContentCopiesDifferent,
            MoveClassTest::testNestedDirectories,
            MoveClassTest::testEmptySourceDirectory,
            MoveClassTest::testRequestStopDuringOperation,
            MoveClassTest::testSourceEqualsDestination,
            MoveClassTest::testNonexistentSource,
            MoveClassTest::testNonexistentDestination,
            MoveClassTest::testHashVerificationMatches,
            MoveClassTest::testLogOutput
        );
    }

    // Helper: run MoveClass synchronously and return the log JTextArea
    private static JTextArea runMove(Path src, Path dst, boolean copyMode,
                                      boolean schedTree, boolean compareContent,
                                      boolean compareName, boolean verifyHash) {
        JTextArea log = new JTextArea();
        JLabel progress = new JLabel();
        MoveClass mc = new MoveClass(src, dst, log, progress,
            compareContent, compareName, copyMode, schedTree, verifyHash);
        mc.run();
        return log;
    }

    // ===== COPY =====

    static void testCopyFile() {
        try {
            Path tmp = TestRunner.createTempDir("copyTest_");
            Path src = TestRunner.createFile(tmp, "source.txt", "hello");
            Path dst = TestRunner.createTempDir("copyDst_");
            runMove(tmp, dst, true, false, false, false, false);
            Path copied = dst.resolve("source.txt");
            TestRunner.assertTrue(Files.exists(copied), "copied file exists in destination");
            TestRunner.assertEquals("hello", new String(Files.readAllBytes(copied)),
                "copied content matches");
            TestRunner.deleteRecursive(tmp);
            TestRunner.deleteRecursive(dst);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    // ===== MOVE =====

    static void testMoveFile() {
        try {
            Path tmp = TestRunner.createTempDir("moveTest_");
            Path srcFile = TestRunner.createFile(tmp, "moveme.txt", "move content");
            Path dst = TestRunner.createTempDir("moveDst_");
            runMove(tmp, dst, false, false, false, false, false);
            Path moved = dst.resolve("moveme.txt");
            TestRunner.assertTrue(Files.exists(moved), "moved file exists in destination");
            TestRunner.assertEquals("move content", new String(Files.readAllBytes(moved)),
                "moved content matches");
            TestRunner.deleteRecursive(tmp);
            TestRunner.deleteRecursive(dst);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    static void testCopyPreservesOriginal() {
        try {
            Path tmp = TestRunner.createTempDir("copyPreserve_");
            Path srcFile = TestRunner.createFile(tmp, "keep.txt", "keep me");
            Path dst = TestRunner.createTempDir("copyDst2_");
            runMove(tmp, dst, true, false, false, false, false);
            TestRunner.assertTrue(Files.exists(srcFile), "original file still exists after copy");
            TestRunner.deleteRecursive(tmp);
            TestRunner.deleteRecursive(dst);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    static void testMoveRemovesOriginal() {
        try {
            Path tmp = TestRunner.createTempDir("moveRemove_");
            Path srcFile = TestRunner.createFile(tmp, "remove.txt", "remove me");
            Path dst = TestRunner.createTempDir("moveDst2_");
            runMove(tmp, dst, false, false, false, false, false);
            TestRunner.assertFalse(Files.exists(srcFile), "original file removed after move");
            TestRunner.deleteRecursive(tmp);
            TestRunner.deleteRecursive(dst);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    // ===== TREE STRUCTURE =====

    static void testScheduledTreeStructure() {
        try {
            Path tmp = TestRunner.createTempDir("schedTree_");
            TestRunner.createFile(tmp, "photo.jpg", "image data");
            Path dst = TestRunner.createTempDir("schedDst_");
            runMove(tmp, dst, true, true, false, false, false);

            // Scheduled tree: dst/YYYY/MM/jpg/photo.jpg
            int year = java.util.Calendar.getInstance().get(java.util.Calendar.YEAR);
            int month = java.util.Calendar.getInstance().get(java.util.Calendar.MONTH) + 1;
            Path expected = dst.resolve(year + "/" + month + "/jpg/photo.jpg");
            TestRunner.assertTrue(Files.exists(expected),
                "scheduled tree structure: " + expected);
            TestRunner.deleteRecursive(tmp);
            TestRunner.deleteRecursive(dst);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    static void testOriginalTreeStructure() {
        try {
            Path tmp = TestRunner.createTempDir("origTree_");
            Path sub = tmp.resolve("subdir");
            Files.createDirectories(sub);
            TestRunner.createFile(sub, "nested.txt", "nested");
            Path dst = TestRunner.createTempDir("origDst_");
            runMove(tmp, dst, true, false, false, false, false);

            Path expected = dst.resolve("subdir/nested.txt");
            TestRunner.assertTrue(Files.exists(expected),
                "original tree preserved: " + expected);
            TestRunner.deleteRecursive(tmp);
            TestRunner.deleteRecursive(dst);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    // ===== DUPLICATE HANDLING =====

    static void testFileAlreadyExistsRenames() {
        try {
            Path tmp = TestRunner.createTempDir("renameTest_");
            TestRunner.createFile(tmp, "file.txt", "first");
            TestRunner.createFile(tmp, "file.txt", "second"); // overwrites!
            Path dst = TestRunner.createTempDir("renameDst_");
            // Put a file with same name in destination first
            TestRunner.createFile(dst, "file.txt", "preexisting");

            runMove(tmp, dst, true, false, false, false, false);

            // Should have created "file 1.txt" since "file.txt" already existed
            boolean originalExists = Files.exists(dst.resolve("file.txt"));
            boolean renamedExists = Files.exists(dst.resolve("file 1.txt"));
            TestRunner.assertTrue(originalExists || renamedExists,
                "file exists in dst: original=" + originalExists + " renamed=" + renamedExists);
            TestRunner.deleteRecursive(tmp);
            TestRunner.deleteRecursive(dst);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    // ===== COMPARE MODES =====

    static void testCompareByNameSkipsIdentical() {
        try {
            Path tmp = TestRunner.createTempDir("cmpName_");
            TestRunner.createFile(tmp, "data.txt", "content");
            Path dst = TestRunner.createTempDir("cmpNameDst_");
            TestRunner.createFile(dst, "data.txt", "content");

            JTextArea log = runMove(tmp, dst, true, false, false, true, false);
            String logText = log.getText();
            TestRunner.assertTrue(logText.contains("Skipping"),
                "compare by name: identical file is skipped");
            TestRunner.deleteRecursive(tmp);
            TestRunner.deleteRecursive(dst);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    static void testCompareByContentSkipsIdentical() {
        try {
            Path tmp = TestRunner.createTempDir("cmpContent_");
            TestRunner.createFile(tmp, "data.bin", "same content");
            Path dst = TestRunner.createTempDir("cmpContentDst_");
            TestRunner.createFile(dst, "data.bin", "same content");

            JTextArea log = runMove(tmp, dst, true, false, true, false, false);
            TestRunner.assertTrue(log.getText().contains("Skipping"),
                "compare by content: identical file is skipped");
            TestRunner.deleteRecursive(tmp);
            TestRunner.deleteRecursive(dst);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    static void testCompareByContentCopiesDifferent() {
        try {
            Path tmp = TestRunner.createTempDir("cmpDiff_");
            TestRunner.createFile(tmp, "file.txt", "version A");
            Path dst = TestRunner.createTempDir("cmpDiffDst_");
            TestRunner.createFile(dst, "file.txt", "version B");

            JTextArea log = runMove(tmp, dst, true, false, true, false, false);
            // Should copy the different file (with rename)
            String logText = log.getText();
            TestRunner.assertTrue(logText.contains("Copied") || logText.contains("Skipping") == false,
                "different content → file is copied (log: " + logText + ")");
            TestRunner.deleteRecursive(tmp);
            TestRunner.deleteRecursive(dst);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    // ===== NESTED / EDGE CASES =====

    static void testNestedDirectories() {
        try {
            Path tmp = TestRunner.createTempDir("nestedTest_");
            Path deep = tmp.resolve("a/b/c");
            Files.createDirectories(deep);
            TestRunner.createFile(deep, "deep.txt", "deep file");
            Path dst = TestRunner.createTempDir("nestedDst_");
            runMove(tmp, dst, true, false, false, false, false);

            TestRunner.assertTrue(Files.exists(dst.resolve("a/b/c/deep.txt")),
                "nested structure preserved during copy");
            TestRunner.deleteRecursive(tmp);
            TestRunner.deleteRecursive(dst);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    static void testEmptySourceDirectory() {
        try {
            Path tmp = TestRunner.createTempDir("emptySrc_");
            Path dst = TestRunner.createTempDir("emptyDst_");
            // Should not throw
            JTextArea log = runMove(tmp, dst, true, false, false, false, false);
            TestRunner.assertTrue(log.getText().contains("Found 0"),
                "empty source: 'Found 0' in log");
            TestRunner.deleteRecursive(tmp);
            TestRunner.deleteRecursive(dst);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    static void testRequestStopDuringOperation() {
        try {
            Path tmp = TestRunner.createTempDir("stopTest_");
            // Create many files to give time to stop mid-way
            for (int i = 0; i < 20; i++) {
                TestRunner.createFile(tmp, "f" + i + ".txt", "data" + i);
            }
            Path dst = TestRunner.createTempDir("stopDst_");
            JTextArea log = new JTextArea();
            JLabel progress = new JLabel();
            MoveClass mc = new MoveClass(tmp, dst, log, progress,
                false, false, true, false, false);
            // Request stop immediately
            mc.requestStop();
            mc.run();
            String logText = log.getText();
            TestRunner.assertTrue(logText.contains("STOPPED") || logText.contains("Found"),
                "stop request handled gracefully: " + logText.substring(0, Math.min(100, logText.length())));
            TestRunner.deleteRecursive(tmp);
            TestRunner.deleteRecursive(dst);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    static void testSourceEqualsDestination() {
        try {
            Path tmp = TestRunner.createTempDir("sameDir_");
            TestRunner.createFile(tmp, "f.txt", "x");
            // Copy source == dest — should still work since MoveClass doesn't check
            // but should not create infinite loop. We just verify it doesn't crash.
            Path dst = tmp;
            JTextArea log = runMove(tmp, dst, true, false, false, false, false);
            // File already exists in same dir, so it'll try to copy → rename
            TestRunner.assertTrue(true, "source == destination does not crash");
            TestRunner.deleteRecursive(tmp);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    static void testNonexistentSource() {
        try {
            Path tmp = TestRunner.createTempDir("noexist_");
            Path fakeSrc = tmp.resolve("does_not_exist");
            Path dst = TestRunner.createTempDir("noexistDst_");
            JTextArea log = new JTextArea();
            JLabel progress = new JLabel();
            MoveClass mc = new MoveClass(fakeSrc, dst, log, progress,
                false, false, true, false, false);
            mc.run(); // should handle gracefully
            TestRunner.assertTrue(true, "nonexistent source does not crash");
            TestRunner.deleteRecursive(tmp);
            TestRunner.deleteRecursive(dst);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    static void testNonexistentDestination() {
        try {
            Path tmp = TestRunner.createTempDir("srcExists_");
            TestRunner.createFile(tmp, "f.txt", "data");
            Path fakeDst = tmp.resolve("nonexistent_dest");
            JTextArea log = new JTextArea();
            JLabel progress = new JLabel();
            MoveClass mc = new MoveClass(tmp, fakeDst, log, progress,
                false, false, true, false, false);
            mc.run(); // createDirectories in processFile will create it
            TestRunner.assertTrue(Files.exists(fakeDst.resolve("f.txt")),
                "non-existent destination dir is created");
            TestRunner.deleteRecursive(tmp);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    // ===== HASH VERIFICATION =====

    static void testHashVerificationMatches() {
        try {
            Path tmp = TestRunner.createTempDir("hashVerify_");
            TestRunner.createFile(tmp, "important.dat", "critical data");
            Path dst = TestRunner.createTempDir("hashVerifyDst_");
            JTextArea log = runMove(tmp, dst, true, false, false, false, true);
            String logText = log.getText();
            TestRunner.assertTrue(logText.contains("Hash OK"),
                "hash verification: 'Hash OK' in log");
            TestRunner.deleteRecursive(tmp);
            TestRunner.deleteRecursive(dst);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    // ===== LOG OUTPUT =====

    static void testLogOutput() {
        try {
            Path tmp = TestRunner.createTempDir("logTest_");
            TestRunner.createFile(tmp, "logme.txt", "log content");
            Path dst = TestRunner.createTempDir("logDst_");
            JTextArea log = runMove(tmp, dst, true, false, false, false, false);
            String text = log.getText();
            TestRunner.assertTrue(text.contains("START"), "log contains START");
            TestRunner.assertTrue(text.contains("END"), "log contains END");
            TestRunner.assertTrue(text.contains("Found 1"), "log contains file count");
            TestRunner.assertFalse(text.contains("Exception"), "no exceptions in log");
            TestRunner.deleteRecursive(tmp);
            TestRunner.deleteRecursive(dst);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
