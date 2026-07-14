import java.awt.Desktop;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Updater {

    private static final String REPO = "donatopepe/SchedulerFiles";
    private static final String API_URL = "https://api.github.com/repos/" + REPO + "/releases/latest";
    private static final String RELEASE_URL = "https://github.com/" + REPO + "/releases/latest";
    private static final Logger LOG = Logger.getLogger(Updater.class.getName());

    private final String currentVersion;
    private final String apiUrl;  // overridable for testing

    public Updater() {
        this(API_URL);
    }

    /** Package-private: allows injecting a custom API URL (e.g. local test server). */
    Updater(String apiUrl) {
        String v = getClass().getPackage().getImplementationVersion();
        this.currentVersion = (v != null) ? v : "0.0.0";
        this.apiUrl = apiUrl;
    }

    public String getCurrentVersion() {
        return currentVersion;
    }

    public String fetchLatestVersion() {
        HttpURLConnection conn = null;
        try {
            URL url = new URI(apiUrl).toURL();
            conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setRequestProperty("Accept", "application/json");
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
                return parseTagName(json.toString());
            }
        } catch (IOException | URISyntaxException e) {
            LOG.log(Level.FINE, "Update check failed: " + e.getMessage(), e);
            return null;
        } finally {
            if (conn != null) conn.disconnect();
        }
    }

    static String parseTagName(String json) {
        if (json == null) return null;
        String key = "\"tag_name\":\"";
        int idx = json.indexOf(key);
        if (idx < 0) return null;
        idx += key.length();
        int end = json.indexOf("\"", idx);
        if (end < 0) return null;
        return json.substring(idx, end);
    }

    public static int compareVersions(String current, String latest) {
        String[] curParts = current.replaceAll("^v", "").split("\\.");
        String[] latParts = latest.replaceAll("^v", "").split("\\.");
        int len = Math.max(curParts.length, latParts.length);
        for (int i = 0; i < len; i++) {
            int c = (i < curParts.length) ? parseIntSafe(curParts[i], 0) : 0;
            int l = (i < latParts.length) ? parseIntSafe(latParts[i], 0) : 0;
            if (l != c) return l - c;
        }
        return 0;
    }

    private static int parseIntSafe(String s, int def) {
        try { return Integer.parseInt(s); } catch (NumberFormatException e) { return def; }
    }

    public static void openReleasesPage() {
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
