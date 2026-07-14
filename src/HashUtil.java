import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class HashUtil {

    private static final String ALGORITHM = "SHA-256";

    public static String sha256(Path filePath) throws IOException {
        try {
            MessageDigest md = MessageDigest.getInstance(ALGORITHM);
            try (InputStream is = Files.newInputStream(filePath);
                 DigestInputStream dis = new DigestInputStream(is, md)) {
                byte[] buffer = new byte[65536];
                while (dis.read(buffer) != -1) {
                    // digest accumulates
                }
            }
            return bytesToHex(md.digest());
        } catch (NoSuchAlgorithmException e) {
            throw new IOException("Hash algorithm " + ALGORITHM + " not available", e);
        }
    }

    private static String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }
}
