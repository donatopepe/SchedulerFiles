import java.awt.Desktop;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Updater {

    private static final String REPO = "donatopepe/SchedulerFiles";
    private static final String API_URL = "https://api.github.com/repos/" + REPO + "/releases/latest";
    private static final String RELEASE_URL = "https://github.com/" + REPO + "/releases/latest";
    private static final Logger LOG = Logger.getLogger(Updater.class.getName());

    // Cache: avoid repeated API calls within the same JVM session
    private static final Map<String, CachedVersion> CACHE = new HashMap<>();
    private static final long CACHE_TTL_MS = 60 * 60 * 1000; // 1 hour
    private static final Object CACHE_LOCK = new Object();

    private final String currentVersion;
    private final String apiUrl;  // overridable for testing

    public Updater() {
        this(API_URL);
    }

    /** Package-private: allows injecting a custom API URL (e.g. local test server). */
    Updater(String apiUrl) {
        this.currentVersion = detectVersion();
        this.apiUrl = apiUrl == null ? API_URL : apiUrl;
    }

    /** Read version from manifest, or return "0.0.0" fallback. */
    private static String detectVersion() {
        String v = Updater.class.getPackage().getImplementationVersion();
        if (v != null) return v;

        try (java.io.InputStream is = Updater.class.getResourceAsStream(
                "/META-INF/MANIFEST.MF")) {
            if (is == null) return "0.0.0";
            java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream(1024);
            byte[] buf = new byte[512];
            int n;
            while ((n = is.read(buf)) != -1) baos.write(buf, 0, n);
            String content = baos.toString("UTF-8");
            for (String line : content.split("\n")) {
                if (line.startsWith("Implementation-Version:")) {
                    return line.substring("Implementation-Version:".length()).trim();
                }
            }
        } catch (Exception ignored) {}
        return "0.0.0";
    }

    public String getCurrentVersion() {
        return currentVersion;
    }

    public String fetchLatestVersion() {
        synchronized (CACHE_LOCK) {
            return fetchLatestVersionLocked();
        }
    }

    private String fetchLatestVersionLocked() {
        // Return cached result if still fresh
        CachedVersion cached = CACHE.get(apiUrl);
        if (cached != null && System.currentTimeMillis() - cached.timestamp < CACHE_TTL_MS) {
            return cached.version;
        }

        HttpURLConnection conn = null;
        try {
            URL url = new URI(apiUrl).toURL();
            conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setRequestProperty("Accept", "application/json");
            conn.setRequestProperty("User-Agent", "SchedulerFiles/" + currentVersion);
            conn.setConnectTimeout(5000);
            conn.setReadTimeout(5000);

            int status = conn.getResponseCode();
            if (status != 200) {
                LOG.warning("Update check: HTTP " + status);
                return null;
            }

            try (BufferedReader br = new BufferedReader(
                    new InputStreamReader(conn.getInputStream(), "UTF-8"))) {

                StringBuilder json = new StringBuilder();
                String line;
                while ((line = br.readLine()) != null) {
                    json.append(line);
                }
                String latest = parseTagName(json.toString());
                if (latest != null) {
                    CACHE.put(apiUrl, new CachedVersion(latest, System.currentTimeMillis()));
                }
                return latest;
            }
        } catch (IOException | URISyntaxException e) {
            LOG.log(Level.FINE, "Update check failed: " + e.getMessage(), e);
            return null;
        } finally {
            if (conn != null) conn.disconnect();
        }
    }

    /** Clear the cached version (for testing). */
    static void clearCache() {
        synchronized (CACHE_LOCK) {
            CACHE.clear();
        }
    }

    private static final class CachedVersion {
        final String version;
        final long timestamp;

        CachedVersion(String version, long timestamp) {
            this.version = version;
            this.timestamp = timestamp;
        }
    }

    static String parseTagName(String json) {
        if (json == null) return null;
        int key = json.indexOf("\"tag_name\"");
        if (key < 0) return null;
        int colon = json.indexOf(':', key + 10);
        if (colon < 0) return null;
        int start = colon + 1;
        while (start < json.length() && Character.isWhitespace(json.charAt(start))) start++;
        if (start >= json.length() || json.charAt(start) != '\"') return null;
        start++;
        StringBuilder value = new StringBuilder();
        boolean escaped = false;
        for (int i = start; i < json.length(); i++) {
            char c = json.charAt(i);
            if (escaped) {
                value.append(c);
                escaped = false;
            } else if (c == '\\') {
                escaped = true;
            } else if (c == '\"') {
                return value.toString();
            } else {
                value.append(c);
            }
        }
        return null;
    }

    public static int compareVersions(String current, String latest) {
        if (current == null || latest == null) return 0;
        Version cur = Version.parse(current);
        Version lat = Version.parse(latest);
        int core = compareCore(cur.core, lat.core);
        if (core != 0) return core;
        if (cur.preRelease == null && lat.preRelease == null) return 0;
        if (cur.preRelease == null) return -1;
        if (lat.preRelease == null) return 1;

        String[] curIds = cur.preRelease.split("\\.");
        String[] latIds = lat.preRelease.split("\\.");
        for (int i = 0; i < Math.max(curIds.length, latIds.length); i++) {
            if (i >= curIds.length) return -1;
            if (i >= latIds.length) return 1;
            String c = curIds[i];
            String l = latIds[i];
            boolean cNum = c.matches("\\d+");
            boolean lNum = l.matches("\\d+");
            if (cNum && lNum) {
                int result = Integer.compare(parseIntSafe(l, Integer.MAX_VALUE),
                    parseIntSafe(c, Integer.MAX_VALUE));
                if (result != 0) return result;
            } else if (cNum != lNum) {
                return cNum ? -1 : 1;
            } else {
                int result = l.compareTo(c);
                if (result != 0) return result;
            }
        }
        return 0;
    }

    private static int compareCore(String[] current, String[] latest) {
        int len = Math.max(current.length, latest.length);
        for (int i = 0; i < len; i++) {
            int c = i < current.length ? parseIntSafe(current[i], 0) : 0;
            int l = i < latest.length ? parseIntSafe(latest[i], 0) : 0;
            if (l != c) return Integer.compare(l, c);
        }
        return 0;
    }

    private static final class Version {
        final String[] core;
        final String preRelease;

        private Version(String[] core, String preRelease) {
            this.core = core;
            this.preRelease = preRelease;
        }

        static Version parse(String value) {
            String normalized = value.replaceFirst("^[vV]", "");
            int build = normalized.indexOf('+');
            if (build >= 0) normalized = normalized.substring(0, build);
            int pre = normalized.indexOf('-');
            String core = pre >= 0 ? normalized.substring(0, pre) : normalized;
            String preRelease = pre >= 0 ? normalized.substring(pre + 1) : null;
            return new Version(core.split("\\."), preRelease);
        }
    }

    private static int parseIntSafe(String s, int def) {
        try { return Integer.parseInt(s); } catch (NumberFormatException e) { return def; }
    }

    public static void openReleasesPage() {
        if (!Desktop.isDesktopSupported()) {
            LOG.warning("Cannot open browser: Desktop not supported");
            return;
        }
        try {
            Desktop.getDesktop().browse(new URI(RELEASE_URL));
        } catch (IOException | URISyntaxException e) {
            LOG.log(Level.WARNING, "Could not open releases page", e);
        }
    }

    /** For testing: the default API URL constant. */
    static String getDefaultApiUrl() {
        return API_URL;
    }
}
