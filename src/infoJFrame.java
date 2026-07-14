import java.awt.Color;
import java.awt.Cursor;
import java.awt.Desktop;
import java.awt.Font;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.IOException;
import java.net.URISyntaxException;
import javax.swing.BorderFactory;
import javax.swing.JEditorPane;
import javax.swing.JLabel;
import javax.swing.JScrollPane;
import javax.swing.event.HyperlinkEvent;

public class infoJFrame extends javax.swing.JFrame {

    public infoJFrame() {
        initComponents();
    }

    @SuppressWarnings("unchecked")
    private void initComponents() {
        setTitle("About SchedulerFiles");
        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
        setResizable(true);
        setSize(520, 520);
        setLocationRelativeTo(null);

        JLabel header = new JLabel("Files Scheduler", JLabel.CENTER);
        header.setFont(new Font("Arial", Font.BOLD, 20));

        JLabel subtitle = new JLabel("Copy / Move files with scheduling and integrity check", JLabel.CENTER);
        subtitle.setFont(new Font("Arial", Font.PLAIN, 11));
        subtitle.setForeground(Color.GRAY);

        JEditorPane content = new JEditorPane("text/html", buildHtmlContent());
        content.setEditable(false);
        content.setCaretPosition(0);
        content.addHyperlinkListener(e -> {
            if (e.getEventType() == HyperlinkEvent.EventType.ACTIVATED) {
                try {
                    Desktop.getDesktop().browse(e.getURL().toURI());
                } catch (IOException | URISyntaxException ex) {
                    // ignore
                }
            }
        });

        JScrollPane scroll = new JScrollPane(content);
        scroll.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));

        JLabel footer = new JLabel(
            "<html><a href='https://github.com/donatopepe/SchedulerFiles'>"
            + "github.com/donatopepe/SchedulerFiles</a></html>", JLabel.CENTER);
        footer.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        footer.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                try {
                    Desktop.getDesktop().browse(
                        new java.net.URI("https://github.com/donatopepe/SchedulerFiles"));
                } catch (Exception ex) {}
            }
        });

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(layout.createParallelGroup()
            .addComponent(header)
            .addComponent(subtitle)
            .addComponent(scroll)
            .addComponent(footer)
        );
        layout.setVerticalGroup(layout.createSequentialGroup()
            .addGap(12)
            .addComponent(header)
            .addGap(2)
            .addComponent(subtitle)
            .addGap(8)
            .addComponent(scroll)
            .addGap(6)
            .addComponent(footer)
            .addGap(8)
        );
    }

    private String buildHtmlContent() {
        return "<html><body style='font-family:Arial;font-size:12px;padding:4px;'>"

            + "<h3>How it works</h3>"

            + "<ol>"
            + "<li><b>Select source & destination</b> — type paths, use <b>Browse</b>, "
            + "or <b>drag & drop</b> folders onto the fields.</li>"
            + "<li><b>Choose operation</b> — check <b>Copy</b> to duplicate files, "
            + "uncheck to <b>move</b> (files are deleted from source).</li>"
            + "<li><b>Tree structure</b>:<ul>"
            + "<li><b>Original Tree</b> — preserves the exact folder hierarchy of the source.</li>"
            + "<li><b>Scheduled</b> — reorganizes files as "
            + "<code>year/month/extension/</code> using last-modified date.</li>"
            + "</ul></li>"
            + "<li><b>Compare options</b> prevent duplicate copies:<ul>"
            + "<li><b>Compare by name</b> — skip files with the same filename.</li>"
            + "<li><b>Compare by content</b> — compare bytes (or SHA-256 if Verify is on).</li>"
            + "<li><b>Verify SHA-256</b> — compute hash before &amp; after, "
            + "log integrity result. When combined with content compare, "
            + "uses hash comparison (faster for large files).</li>"
            + "</ul></li>"
            + "<li>Click <b>Start</b> to begin. Progress shows percentage and file count.</li>"
            + "<li>Click <b>Cancel</b> to stop gracefully mid-operation.</li>"
            + "</ol>"

            + "<h3>Transaction log</h3>"
            + "<p>After each operation a file named <code>_files_scheduler.log</code> "
            + "is created in the <b>destination</b> directory. It records every "
            + "operation (COPY / MOVE / SKIP), timestamps, file sizes, and "
            + "SHA-256 hashes when verification is enabled.</p>"

            + "<h3>Automatic updates</h3>"
            + "<p>On startup the app checks GitHub for a newer release. "
            + "If found, a dialog offers to open the download page.</p>"

            + "<h3>Requirements</h3>"
            + "<p>Java Runtime Environment (JRE) 8 or later. "
            + "No other dependencies — the JAR is self-contained.</p>"

            + "<hr>"
            + "<p><b>License:</b> Free software. Do what you want. "
            + "No warranty, no liability — use at your own risk.</p>"
            + "<p style='color:gray;font-size:10px;'>Copyright (C) 2020 Donato Pepe</p>"

            + "</body></html>";
    }
}
