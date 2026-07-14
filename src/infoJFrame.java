import java.awt.Desktop;
import java.awt.Font;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.BorderFactory;
import javax.swing.JEditorPane;
import javax.swing.JLabel;
import javax.swing.JScrollPane;
import javax.swing.event.HyperlinkEvent;

public class infoJFrame extends javax.swing.JFrame {

    private static final long serialVersionUID = 1L;
    private static final Logger LOG = Logger.getLogger(infoJFrame.class.getName());

    public infoJFrame() {
        initComponents();
    }

    private void initComponents() {
        setTitle("About SchedulerFiles");
        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
        setResizable(true);
        setSize(580, 580);
        setLocationRelativeTo(null);

        JLabel header = new JLabel("Files Scheduler", JLabel.CENTER);
        header.setFont(new Font("Arial", Font.BOLD, 22));
        header.getAccessibleContext().setAccessibleName("Files Scheduler information");

        JEditorPane content = new JEditorPane("text/html", loadHtmlContent());
        content.setEditable(false);
        content.setCaretPosition(0);
        content.getAccessibleContext().setAccessibleName("SchedulerFiles information");
        content.getAccessibleContext().setAccessibleDescription(
            "Features, safety notice, license, and project link");
        content.addHyperlinkListener(event -> {
            if (event.getEventType() == HyperlinkEvent.EventType.ACTIVATED
                    && event.getURL() != null) {
                try {
                    openUrl(event.getURL().toURI());
                } catch (URISyntaxException e) {
                    LOG.log(Level.FINE, "Invalid hyperlink", e);
                }
            }
        });

        JScrollPane scroll = new JScrollPane(content);
        scroll.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createEmptyBorder(4, 16, 4, 16), scroll.getBorder()));

        javax.swing.JPanel panel = new javax.swing.JPanel(new java.awt.BorderLayout(0, 8));
        panel.setBorder(BorderFactory.createEmptyBorder(12, 10, 10, 10));
        panel.add(header, java.awt.BorderLayout.NORTH);
        panel.add(scroll, java.awt.BorderLayout.CENTER);
        setContentPane(panel);
    }

    private String loadHtmlContent() {
        try (InputStream input = infoJFrame.class.getResourceAsStream("/about.html")) {
            if (input == null) {
                LOG.warning("About page resource not found");
                return "<html><body><h2>SchedulerFiles</h2><p>Information unavailable.</p></body></html>";
            }
            java.io.ByteArrayOutputStream output = new java.io.ByteArrayOutputStream();
            byte[] buffer = new byte[1024];
            int read;
            while ((read = input.read(buffer)) != -1) output.write(buffer, 0, read);
            return new String(output.toByteArray(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            LOG.log(Level.WARNING, "Could not load about page", e);
            return "<html><body><h2>SchedulerFiles</h2><p>Information unavailable.</p></body></html>";
        }
    }

    private void openUrl(java.net.URI uri) {
        if (!Desktop.isDesktopSupported()) {
            LOG.fine("Desktop browser is not supported");
            return;
        }
        try {
            Desktop.getDesktop().browse(uri);
        } catch (IOException | SecurityException e) {
            LOG.log(Level.WARNING, "Could not open browser URL", e);
        }
    }
}
