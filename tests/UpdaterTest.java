import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpExchange;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URI;
import java.util.*;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;

public class UpdaterTest {

    public static List<Runnable> suite() {
        return Arrays.asList(
            // Version comparison
            UpdaterTest::testCompareVersionsEqual,
            UpdaterTest::testCompareVersionsNewer,
            UpdaterTest::testCompareVersionsOlder,
            UpdaterTest::testCompareVersionsDifferentLength,
            UpdaterTest::testCompareVersionsWithVPrefix,
            UpdaterTest::testComparePrereleaseVersions,
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
            UpdaterTest::testCacheSeparatesApiUrls,
            UpdaterTest::testConcurrentFetchUsesCache,
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

    static void testComparePrereleaseVersions() {
        TestRunner.assertTrue(Updater.compareVersions("1.2.0-beta", "1.2.0") > 0,
            "release is newer than prerelease");
        TestRunner.assertTrue(Updater.compareVersions("1.2.0-alpha", "1.2.0-beta") > 0,
            "beta is newer than alpha");
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
        Updater.clearCache();
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
        Updater.clearCache();
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
        Updater.clearCache();
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
        Updater.clearCache();
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
        Updater.clearCache();
        // Point to a non-routable IP to trigger timeout / connection refused
        Updater u = new Updater("http://10.255.255.1:1/");
        String v = u.fetchLatestVersion();
        TestRunner.assertTrue(v == null,
            "unreachable host returns null (got: " + v + ")");
    }

    static void testFetchLatestVersionNullUrl() {
        Updater.clearCache();
        // URL with unresolvable host
        Updater u = new Updater("http://thishostdoesnotexist99999.local/");
        String v = u.fetchLatestVersion();
        TestRunner.assertTrue(v == null,
            "unresolvable host returns null (got: " + v + ")");
    }

    static void testCacheSeparatesApiUrls() {
        Updater.clearCache();
        try {
            AtomicInteger firstRequests = new AtomicInteger();
            AtomicInteger secondRequests = new AtomicInteger();
            HttpServer first = startMockServer(200, "{\"tag_name\":\"v1.0.0\"}", firstRequests);
            HttpServer second = startMockServer(200, "{\"tag_name\":\"v2.0.0\"}", secondRequests);
            try {
                TestRunner.assertEquals("v1.0.0", new Updater(url(first)).fetchLatestVersion(),
                    "first API URL result");
                TestRunner.assertEquals("v2.0.0", new Updater(url(second)).fetchLatestVersion(),
                    "second API URL result");
                TestRunner.assertEquals("v1.0.0", new Updater(url(first)).fetchLatestVersion(),
                    "first API URL remains cached");
                TestRunner.assertEquals(1, firstRequests.get(), "first API called once");
                TestRunner.assertEquals(1, secondRequests.get(), "second API called once");
            } finally {
                first.stop(0);
                second.stop(0);
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    static void testConcurrentFetchUsesCache() {
        Updater.clearCache();
        try {
            AtomicInteger requests = new AtomicInteger();
            HttpServer server = startMockServer(200, "{\"tag_name\":\"v3.0.0\"}", requests);
            try {
                final String apiUrl = url(server);
                final CountDownLatch start = new CountDownLatch(1);
                List<Thread> threads = new ArrayList<>();
                for (int i = 0; i < 8; i++) {
                    Thread t = new Thread(() -> {
                        try {
                            start.await();
                            TestRunner.assertEquals("v3.0.0", new Updater(apiUrl).fetchLatestVersion(),
                                "concurrent fetch result");
                        } catch (Exception e) {
                            throw new RuntimeException(e);
                        }
                    });
                    threads.add(t);
                    t.start();
                }
                start.countDown();
                for (Thread t : threads) t.join();
                TestRunner.assertEquals(1, requests.get(), "concurrent fetch calls API once");
            } finally {
                server.stop(0);
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    // ===== Full check flow =====

    static void testAlreadyLatestNoDialog() {
        Updater.clearCache();
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
        Updater.clearCache();
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

    private static String url(HttpServer server) {
        return "http://localhost:" + server.getAddress().getPort() + "/";
    }

    private static HttpServer startMockServer(int statusCode, String body) throws IOException {
        return startMockServer(statusCode, body, null);
    }

    private static HttpServer startMockServer(int statusCode, String body,
                                               AtomicInteger requests) throws IOException {
        HttpServer server = HttpServer.create(new InetSocketAddress(0), 0);
        server.createContext("/", (HttpExchange e) -> {
            if (requests != null) requests.incrementAndGet();
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
