import java.nio.file.*;
import java.util.*;

public class HashUtilTest {

    public static List<Runnable> suite() {
        return Arrays.asList(
            HashUtilTest::testKnownHash,
            HashUtilTest::testSameContentSameHash,
            HashUtilTest::testDifferentContentDifferentHash,
            HashUtilTest::testEmptyFile,
            HashUtilTest::testLargeFile
        );
    }

    static void testKnownHash() {
        try {
            Path tmp = TestRunner.createTempDir("hashTest_");
            Path f = TestRunner.createFile(tmp, "hello.txt", "Hello World");
            String hash = HashUtil.sha256(f);
            // SHA-256 of "Hello World" (no newline)
            String expected = "a591a6d40bf420404a011733cfb7b190d62c65bf0bcda32b57b277d9ad9f146e";
            TestRunner.assertEquals(expected, hash, "SHA-256 of 'Hello World'");
            TestRunner.deleteRecursive(tmp);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    static void testSameContentSameHash() {
        try {
            Path tmp = TestRunner.createTempDir("hashSame_");
            Path a = TestRunner.createFile(tmp, "a.txt", "content");
            Path b = TestRunner.createFile(tmp, "b.txt", "content");
            String ha = HashUtil.sha256(a);
            String hb = HashUtil.sha256(b);
            TestRunner.assertEquals(ha, hb, "same content → same hash");
            TestRunner.deleteRecursive(tmp);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    static void testDifferentContentDifferentHash() {
        try {
            Path tmp = TestRunner.createTempDir("hashDiff_");
            Path a = TestRunner.createFile(tmp, "a.txt", "content A");
            Path b = TestRunner.createFile(tmp, "b.txt", "content B");
            String ha = HashUtil.sha256(a);
            String hb = HashUtil.sha256(b);
            TestRunner.assertFalse(ha.equals(hb), "different content → different hash");
            TestRunner.deleteRecursive(tmp);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    static void testEmptyFile() {
        try {
            Path tmp = TestRunner.createTempDir("hashEmpty_");
            Path f = TestRunner.createFile(tmp, "empty.txt", "");
            String hash = HashUtil.sha256(f);
            String expected = "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855";
            TestRunner.assertEquals(expected, hash, "SHA-256 of empty file");
            TestRunner.deleteRecursive(tmp);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    static void testLargeFile() {
        try {
            Path tmp = TestRunner.createTempDir("hashLarge_");
            byte[] big = new byte[1024 * 1024]; // 1 MB
            new java.util.Random(42).nextBytes(big);
            Path f = TestRunner.createFile(tmp, "large.bin", big);
            String hash = HashUtil.sha256(f);
            TestRunner.assertTrue(hash.length() == 64, "SHA-256 is 64 hex chars (got " + hash.length() + ")");
            TestRunner.assertTrue(hash.matches("[0-9a-f]{64}"), "SHA-256 is hex only");
            TestRunner.deleteRecursive(tmp);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
