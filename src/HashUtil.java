import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class HashUtil {

    private static final String ALGORITHM = "SHA-256";
    private static final int BUFFER_SIZE = 1024 * 1024;
    private static final char[] HEX = "0123456789abcdef".toCharArray();

    /**
     * Computes SHA-256 using a 1 MiB streaming buffer. SHA-256 is the default
     * because it is standardized, JDK-provided, and cryptographically strong.
     */
    public static String sha256(Path filePath) throws IOException {
        try {
            MessageDigest md = MessageDigest.getInstance(ALGORITHM);
            try (InputStream is = Files.newInputStream(filePath);
                 DigestInputStream dis = new DigestInputStream(is, md)) {
                byte[] buffer = new byte[BUFFER_SIZE];
                while (dis.read(buffer) != -1) {
                    // digest accumulates
                }
            }
            return bytesToHex(md.digest());
        } catch (NoSuchAlgorithmException e) {
            // SHA-256 is guaranteed by the JDK; this should never happen.
            throw new RuntimeException("SHA-256 not available on this JVM", e);
        }
    }

    private static String bytesToHex(byte[] bytes) {
        char[] hex = new char[bytes.length * 2];
        for (int i = 0; i < bytes.length; i++) {
            int v = bytes[i] & 0xFF;
            hex[i * 2] = HEX[v >>> 4];
            hex[i * 2 + 1] = HEX[v & 0x0F];
        }
        return new String(hex);
    }
}
