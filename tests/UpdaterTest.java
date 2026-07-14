import java.util.*;

public class UpdaterTest {

    public static List<Runnable> suite() {
        return Arrays.asList(
            UpdaterTest::testCompareVersionsEqual,
            UpdaterTest::testCompareVersionsNewer,
            UpdaterTest::testCompareVersionsOlder,
            UpdaterTest::testCompareVersionsDifferentLength,
            UpdaterTest::testCompareVersionsWithVPrefix,
            UpdaterTest::testParseTagName,
            UpdaterTest::testParseTagNameNoMatch,
            UpdaterTest::testParseTagNameNull,
            UpdaterTest::testCurrentVersionFallback
        );
    }

    static void testCompareVersionsEqual() {
        int r = Updater.compareVersions("1.0.0", "1.0.0");
        TestRunner.assertEquals(0, r, "equal versions → 0");
    }

    static void testCompareVersionsNewer() {
        int r = Updater.compareVersions("1.0.0", "1.2.3");
        TestRunner.assertTrue(r > 0, "latest > current → positive (got " + r + ")");
    }

    static void testCompareVersionsOlder() {
        int r = Updater.compareVersions("2.0.0", "1.9.9");
        TestRunner.assertTrue(r < 0, "latest < current → negative (got " + r + ")");
    }

    static void testCompareVersionsDifferentLength() {
        int r = Updater.compareVersions("1.0", "1.0.1");
        TestRunner.assertTrue(r > 0, "1.0.1 > 1.0 → positive (got " + r + ")");
    }

    static void testCompareVersionsWithVPrefix() {
        int r = Updater.compareVersions("v1.0.0", "1.0.0");
        TestRunner.assertEquals(0, r, "v prefix stripped for comparison");
    }

    static void testParseTagName() {
        String json = "{\"tag_name\":\"v1.2.3\",\"name\":\"Release 1.2.3\"}";
        String tag = Updater.parseTagName(json);
        TestRunner.assertEquals("v1.2.3", tag, "parse tag_name from JSON");
    }

    static void testParseTagNameNoMatch() {
        String json = "{\"name\":\"some release\",\"id\":123}";
        String tag = Updater.parseTagName(json);
        TestRunner.assertTrue(tag == null, "no tag_name → null");
    }

    static void testParseTagNameNull() {
        String tag = Updater.parseTagName(null);
        TestRunner.assertTrue(tag == null, "null JSON → null");
    }

    static void testCurrentVersionFallback() {
        // When no manifest Implementation-Version, should fallback to "0.0.0"
        Updater u = new Updater();
        TestRunner.assertEquals("0.0.0", u.getCurrentVersion(),
            "fallback version when no manifest");
    }
}
