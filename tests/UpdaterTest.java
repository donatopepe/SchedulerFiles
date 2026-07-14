import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpExchange;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URI;
import java.util.*;

public class UpdaterTest {

    public static List<Runnable> suite() {
        return Arrays.asList(
            // Version comparison
            UpdaterTest::testCompareVersionsEqual,
            UpdaterTest::testCompareVersionsNewer,
            UpdaterTest::testCompareVersionsOlder,
            UpdaterTest::testCompareVersionsDifferentLength,
            UpdaterTest::testCompareVersionsWithVPrefix,
            // JSON parsing
            UpdaterTest::testParseTagName,
            UpdaterTest::testParseTagNameNoMatch,
            UpdaterTest::testParseTagNameNull,
            // Fallback
            UpdaterTest::testCurrentVersionFallback,
            // HTTP fetch (with local test server)
            UpdaterTest::testFetchLatestVersionSuccess,
            UpdaterTest::testFetchLatestVersion404,
            UpdaterTest::testFetchLatestVersionMalformedJson,
            UpdaterTest::testFetchLatestVersionEmptyBody,
            UpdaterTest::testFetchLatestVersionTimeout,
            UpdaterTest::testFetchLatestVersionNullUrl,
            // Check for updates (full dialog flow mocked)
            UpdaterTest::testAlreadyLatestNoDialog,
            UpdaterTest::testNewVersionDetected
        );
    }

    // ===== Version comparison =====

    static void testCompareVersionsEqual() {
        int r = Updater.compareVersions("1.0.0", "1.0.0");
        TestRunner.assertEquals(0, r, "equal versions -> 0");
    }

    static void testCompareVersionsNewer() {
        int r = Updater.compareVersions("1.0.0", "1.2.3");
        TestRunner.assertTrue(r > 0, "latest > current -> positive (got " + r + ")");
    }

    static void testCompareVersionsOlder() {
        int r = Updater.compareVersions("2.0.0", "1.9.9");
        TestRunner.assertTrue(r < 0, "latest < current -> negative (got " + r + ")");
    }

    static void testCompareVersionsDifferentLength() {
        int r = Updater.compareVersions("1.0", "1.0.1");
        TestRunner.assertTrue(r > 0, "1.0.1 > 1.0 -> positive (got " + r + ")");
    }

    static void testCompareVersionsWithVPrefix() {
        int r = Updater.compareVersions("v1.0.0", "1.0.0");
        TestRunner.assertEquals(0, r, "v prefix stripped for comparison");
    }

    // ===== JSON parsing =====

    static void testParseTagName() {
        String json = "{\"tag_name\":\"v1.2.3\",\"name\":\"Release 1.2.3\"}";
        TestRunner.assertEquals("v1.2.3", Updater.parseTagName(json), "parse tag_name from JSON");
    }

    static void testParseTagNameNoMatch() {
        String json = "{\"name\":\"some release\",\"id\":123}";
        TestRunner.assertTrue(Updater.parseTagName(json) == null, "no tag_name -> null");
    }

    static void testParseTagNameNull() {
        TestRunner.assertTrue(Updater.parseTagName(null) == null, "null JSON -> null");
    }

    // ===== Fallback =====

    static void testCurrentVersionFallback() {
        Updater u = new Updater();
        TestRunner.assertEquals("0.0.0", u.getCurrentVersion(),
            "fallback version when no manifest");
    }

    // ===== HTTP fetch with local test server =====

    static void testFetchLatestVersionSuccess() {
        try {
            HttpServer server = startMockServer(200,
                "{\"tag_name\":\"v2.0.0\",\"name\":\"Release 2.0.0\"}");
            try {
                Updater u = new Updater("http://localhost:" + server.getAddress().getPort() + "/");
                String v = u.fetchLatestVersion();
                TestRunner.assertEquals("v2.0.0", v, "fetch returns tag from mock server");
            } finally {
                server.stop(0);
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    static void testFetchLatestVersion404() {
        try {
            HttpServer server = startMockServer(404, "Not Found");
            try {
                Updater u = new Updater("http://localhost:" + server.getAddress().getPort() + "/");
                String v = u.fetchLatestVersion();
                TestRunner.assertTrue(v == null,
                    "HTTP 404 returns null (got: " + v + ")");
            } finally {
                server.stop(0);
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    static void testFetchLatestVersionMalformedJson() {
        try {
            HttpServer server = startMockServer(200, "{bad json}");
            try {
                Updater u = new Updater("http://localhost:" + server.getAddress().getPort() + "/");
                String v = u.fetchLatestVersion();
                TestRunner.assertTrue(v == null,
                    "malformed JSON returns null (got: " + v + ")");
            } finally {
                server.stop(0);
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    static void testFetchLatestVersionEmptyBody() {
        try {
            HttpServer server = startMockServer(200, "");
            try {
                Updater u = new Updater("http://localhost:" + server.getAddress().getPort() + "/");
                String v = u.fetchLatestVersion();
                TestRunner.assertTrue(v == null,
                    "empty body returns null (got: " + v + ")");
            } finally {
                server.stop(0);
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    static void testFetchLatestVersionTimeout() {
        // Point to a non-routable IP to trigger timeout / connection refused
        Updater u = new Updater("http://10.255.255.1:1/");
        String v = u.fetchLatestVersion();
        TestRunner.assertTrue(v == null,
            "unreachable host returns null (got: " + v + ")");
    }

    static void testFetchLatestVersionNullUrl() {
        // URL with unresolvable host
        Updater u = new Updater("http://thishostdoesnotexist99999.local/");
        String v = u.fetchLatestVersion();
        TestRunner.assertTrue(v == null,
            "unresolvable host returns null (got: " + v + ")");
    }

    // ===== Full check flow =====

    static void testAlreadyLatestNoDialog() {
        try {
            HttpServer server = startMockServer(200,
                "{\"tag_name\":\"v0.0.0\"}");
            try {
                Updater u = new Updater("http://localhost:" + server.getAddress().getPort() + "/");
                String latest = u.fetchLatestVersion();
                int cmp = Updater.compareVersions(u.getCurrentVersion(), latest);
                TestRunner.assertTrue(cmp <= 0,
                    "same version: compareVersions <= 0 (got " + cmp + ")");
            } finally {
                server.stop(0);
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    static void testNewVersionDetected() {
        try {
            HttpServer server = startMockServer(200,
                "{\"tag_name\":\"v9.9.9\"}");
            try {
                Updater u = new Updater("http://localhost:" + server.getAddress().getPort() + "/");
                String latest = u.fetchLatestVersion();
                int cmp = Updater.compareVersions(u.getCurrentVersion(), latest);
                TestRunner.assertTrue(cmp > 0,
                    "newer version detected: compareVersions > 0 (got " + cmp + ")");
                TestRunner.assertEquals("v9.9.9", latest, "correct tag returned");
            } finally {
                server.stop(0);
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    // ===== Mock HTTP server helper =====

    private static HttpServer startMockServer(int statusCode, String body) throws IOException {
        HttpServer server = HttpServer.create(new InetSocketAddress(0), 0);
        server.createContext("/", (HttpExchange e) -> {
            byte[] bytes = body.getBytes("UTF-8");
            e.sendResponseHeaders(statusCode, bytes.length);
            try (OutputStream os = e.getResponseBody()) {
                os.write(bytes);
            }
        });
        server.setExecutor(null);
        server.start();
        return server;
    }
}
