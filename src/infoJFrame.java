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
        setSize(580, 580);
        setLocationRelativeTo(null);

        JLabel header = new JLabel("Files Scheduler", JLabel.CENTER);
        header.setFont(new Font("Arial", Font.BOLD, 22));

        JLabel subtitle = new JLabel("Copy / Move files with scheduling and integrity check", JLabel.CENTER);
        subtitle.setFont(new Font("Arial", Font.PLAIN, 12));
        subtitle.setForeground(Color.GRAY);

        JEditorPane content = new JEditorPane("text/html", buildHtmlContent());
        content.setEditable(false);
        content.setCaretPosition(0);
        content.addHyperlinkListener(e -> {
            if (e.getEventType() == HyperlinkEvent.EventType.ACTIVATED) {
                try {
                    Desktop.getDesktop().browse(e.getURL().toURI());
                } catch (IOException | URISyntaxException ex) {
                }
            }
        });

        JScrollPane scroll = new JScrollPane(content);
        scroll.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createEmptyBorder(4, 16, 4, 16),
            scroll.getBorder()));

        JLabel footer = new JLabel(
            "<html><a href='https://github.com/donatopepe/SchedulerFiles'>"
            + "github.com/donatopepe/SchedulerFiles</a></html>", JLabel.CENTER);
        footer.setFont(new Font("Arial", Font.PLAIN, 12));
        footer.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        footer.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                try {
                    Desktop.getDesktop().browse(
                        new java.net.URI("https://github.com/donatopepe/SchedulerFiles"));
                } catch (Exception ex) {
                }
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
            .addGap(18)
            .addComponent(header)
            .addGap(4)
            .addComponent(subtitle)
            .addGap(14)
            .addComponent(scroll)
            .addGap(10)
            .addComponent(footer)
            .addGap(12)
        );
    }

    private String buildHtmlContent() {
        return "<html><meta charset='utf-8'><body style='"
            + "font-family:Arial;font-size:13px;padding:12px 16px;"
            + "line-height:1.5;'>"

            + "<h3 style='margin-top:6px;margin-bottom:10px;'>How it works</h3>"

            + "<ol style='margin-left:6px;padding-left:20px;'>"
            + "<li style='margin-bottom:8px;'><b>Select source &amp; destination</b> "
            + "- type paths, use <b>Browse</b>, "
            + "or <b>drag &amp; drop</b> folders onto the fields.</li>"
            + "<li style='margin-bottom:8px;'><b>Choose operation</b> "
            + "- check <b>Copy</b> to duplicate files, "
            + "uncheck to <b>move</b> (files removed from source).</li>"
            + "<li style='margin-bottom:8px;'><b>Tree structure</b>:<ul>"
            + "<li><b>Original Tree</b> - preserves source folder hierarchy.</li>"
            + "<li><b>Scheduled</b> - reorganizes as "
            + "<code>year/month/extension/</code> by last-modified date.</li>"
            + "</ul></li>"
            + "<li style='margin-bottom:8px;'><b>Compare options</b> prevent duplicates:<ul>"
            + "<li><b>Compare by name</b> - skip files with same filename.</li>"
            + "<li><b>Compare by content</b> - byte compare (or SHA-256 if Verify on).</li>"
            + "<li><b>Verify SHA-256</b> - hash before &amp; after, "
            + "log integrity. With content compare uses hash (faster).</li>"
            + "</ul></li>"
            + "<li style='margin-bottom:8px;'>Click <b>Start</b> to begin. "
            + "Progress = % + file count.</li>"
            + "<li style='margin-bottom:8px;'>Click <b>Cancel</b> to stop "
            + "mid-operation.</li>"
            + "</ol>"

            + "<h3 style='margin-top:18px;margin-bottom:10px;'>Transaction log</h3>"
            + "<p style='margin-bottom:12px;'>A file <code>_files_scheduler.log</code> "
            + "is created in the <b>destination</b> folder. It logs every operation "
            + "(COPY / MOVE / SKIP), timestamps, sizes, and hashes.</p>"

            + "<h3 style='margin-top:18px;margin-bottom:10px;'>Auto-update</h3>"
            + "<p style='margin-bottom:12px;'>On startup checks GitHub for a newer "
            + "release. Dialog offers download link if found.</p>"

            + "<h3 style='margin-top:18px;margin-bottom:10px;'>Requirements</h3>"
            + "<p style='margin-bottom:12px;'>JRE 8 or later. "
            + "Self-contained JAR (no dependencies).</p>"

            + "<hr style='margin:16px 0;'>"
            + "<p><b>License:</b> Free software. Do what you want. "
            + "No warranty, no liability - use at your own risk.</p>"
            + "<p style='color:gray;font-size:10px;margin-top:6px;'>"
            + "Copyright (C) 2020 Donato Pepe</p>"

            + "</body></html>";
    }
}
