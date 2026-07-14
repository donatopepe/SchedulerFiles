import java.awt.Cursor;
import java.awt.Desktop;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.logging.FileHandler;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import javax.swing.text.DefaultCaret;
import org.netbeans.lib.awtextra.AbsoluteConstraints;

public class SchedulerFiles extends javax.swing.JFrame {

    private static final long serialVersionUID = 1L;
    private final String paypalme = "https://www.paypal.me/DonatoPepe";
    private final javax.swing.JCheckBox verifyHash = new javax.swing.JCheckBox("Verify SHA-256");
    private Thread workerThread;
    private MoveClass currentTask;

    public SchedulerFiles() {
        initComponents();
        postInit();
    }

    private void postInit() {
        Logger logger = Logger.getLogger(SchedulerFiles.class.getName());
        OutputStream os = new TextAreaOutputStream(jTextLog);
        logger.addHandler(new TextAreaHandler(os));
        // Create logs/ directory and log file there (keep CWD clean)
        try {
            Files.createDirectories(Paths.get("logs"));
            Date date = new Date();
            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH-mm-ss");
            FileHandler fh = new FileHandler("logs/SchedulerFiles " + dateFormat.format(date) + ".log");
            logger.addHandler(fh);
            fh.setFormatter(new SimpleFormatter());
            logger.info("Start");
        } catch (SecurityException | IOException ex) {
            Logger.getLogger(SchedulerFiles.class.getName()).log(Level.SEVERE, null, ex);
        }

        updateButtonLabel();

        DefaultCaret caret = (DefaultCaret) jTextLog.getCaret();
        caret.setUpdatePolicy(DefaultCaret.ALWAYS_UPDATE);

        developer.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        developer.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                try {
                    Desktop.getDesktop().browse(new URI(paypalme));
                } catch (URISyntaxException | IOException ex) {
                    JOptionPane.showMessageDialog(null, ex);
                }
            }
        });

        info.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        info.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                new infoJFrame().setVisible(true);
            }
        });

        connectToDragDrop();
        checkForUpdates();

        // Tooltips and accessible names
        SourcePath.setToolTipText("Source directory path - type, drag & drop, or Browse");
        DestinationPath.setToolTipText("Destination directory path - type, drag & drop, or Browse");
        jButtonScheda.setToolTipText("Start the copy/move operation");
        jButtonCancel.setToolTipText("Cancel the running operation");
        jCheckBoxCopia.setToolTipText("Uncheck to move files (delete from source)");
        OriginalTree.setToolTipText("Preserve the original folder structure");
        ScheduledTree.setToolTipText("Organize by year / month / file extension");
        comparefile.setToolTipText("Skip files with identical content");
        comparename.setToolTipText("Skip files with the same filename");
        verifyHash.setToolTipText("Compute SHA-256 hash before and after transfer");
        SourcePath.getAccessibleContext().setAccessibleName("Source directory");
        DestinationPath.getAccessibleContext().setAccessibleName("Destination directory");
        jButtonScheda.getAccessibleContext().setAccessibleName("Start file transfer");
        jButtonCancel.getAccessibleContext().setAccessibleName("Cancel file transfer");
        jCheckBoxCopia.getAccessibleContext().setAccessibleDescription(
            "Selected copies files; unselected moves and removes source files");
        OriginalTree.getAccessibleContext().setAccessibleDescription(
            "Preserve source folder hierarchy");
        ScheduledTree.getAccessibleContext().setAccessibleDescription(
            "Organize files by year, month, and extension");
        comparefile.getAccessibleContext().setAccessibleDescription(
            "Skip destination files with identical content");
        comparename.getAccessibleContext().setAccessibleDescription(
            "Skip destination files with identical names");
        avanzamento.getAccessibleContext().setAccessibleName("Transfer progress");
        jTextLog.getAccessibleContext().setAccessibleName("Transfer log");
        jTextLog.getAccessibleContext().setAccessibleDescription(
            "File transfer status and error messages");
        jLabel1.setLabelFor(SourcePath);
        jLabel3.setLabelFor(DestinationPath);
        getRootPane().setDefaultButton(jButtonScheda);

        // Verify hash inside compare panel (second row)
        verifyHash.setFont(new java.awt.Font("Arial", 0, 12));
        jPanel1.add(verifyHash, new AbsoluteConstraints(10, 30, -1, -1));

        addBrowseButtons();
    }

    private void addBrowseButtons() {
        javax.swing.JButton btnSrc = new javax.swing.JButton("Browse");
        btnSrc.setFont(new java.awt.Font("Arial", 0, 12));
        btnSrc.getAccessibleContext().setAccessibleName("Browse source directory");
        btnSrc.addActionListener(e -> chooseDirectory(SourcePath));
        getContentPane().add(btnSrc, new AbsoluteConstraints(625, 18, 80, 25));

        javax.swing.JButton btnDst = new javax.swing.JButton("Browse");
        btnDst.setFont(new java.awt.Font("Arial", 0, 12));
        btnDst.getAccessibleContext().setAccessibleName("Browse destination directory");
        btnDst.addActionListener(e -> chooseDirectory(DestinationPath));
        getContentPane().add(btnDst, new AbsoluteConstraints(625, 46, 80, 25));
    }

    private void chooseDirectory(javax.swing.JTextField targetField) {
        JFileChooser chooser = new JFileChooser();
        chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        chooser.setDialogTitle("Select Directory");
        String current = targetField.getText();
        if (current != null && !current.isEmpty()) {
            File curDir = new File(current);
            if (curDir.exists()) {
                chooser.setCurrentDirectory(curDir);
            }
        }
        int result = chooser.showOpenDialog(this);
        if (result == JFileChooser.APPROVE_OPTION) {
            targetField.setText(chooser.getSelectedFile().getAbsolutePath());
        }
    }

    // ---- NetBeans generated UI ----

    @SuppressWarnings("unchecked")
    private void initComponents() {

        GroupTree = new javax.swing.ButtonGroup();
        jScrollPane1 = new javax.swing.JScrollPane();
        jTextLog = new javax.swing.JTextArea();
        jLabel1 = new javax.swing.JLabel();
        jButtonScheda = new javax.swing.JButton();
        jLabel3 = new javax.swing.JLabel();
        jButtonCancel = new javax.swing.JButton();
        jCheckBoxCopia = new javax.swing.JCheckBox();
        SourcePath = new javax.swing.JTextField();
        DestinationPath = new javax.swing.JTextField();
        developer = new javax.swing.JLabel();
        info = new javax.swing.JLabel();
        avanzamento = new javax.swing.JLabel();
        jPanelGerarchia = new javax.swing.JPanel();
        ScheduledTree = new javax.swing.JRadioButton();
        OriginalTree = new javax.swing.JRadioButton();
        jPanel1 = new javax.swing.JPanel();
        comparefile = new javax.swing.JCheckBox();
        comparename = new javax.swing.JCheckBox();

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);
        setTitle("Files scheduler");
        setFont(new java.awt.Font("Arial", 0, 10));
        setResizable(true);
        addWindowListener(new java.awt.event.WindowAdapter() {
            public void windowClosing(java.awt.event.WindowEvent evt) {
                formWindowClosing(evt);
            }
        });
        getContentPane().setLayout(new org.netbeans.lib.awtextra.AbsoluteLayout());

        //--- Row 1: Source path ---
        jLabel1.setFont(new java.awt.Font("Arial", 0, 14));
        jLabel1.setText("Source");
        getContentPane().add(jLabel1, new AbsoluteConstraints(10, 20, -1, -1));

        SourcePath.setFont(new java.awt.Font("Arial", 0, 12));
        getContentPane().add(SourcePath, new AbsoluteConstraints(85, 18, 530, 25));

        //--- Row 2: Destination path ---
        jLabel3.setFont(new java.awt.Font("Arial", 0, 14));
        jLabel3.setText("Dest");
        getContentPane().add(jLabel3, new AbsoluteConstraints(10, 48, -1, -1));

        DestinationPath.setFont(new java.awt.Font("Arial", 0, 12));
        getContentPane().add(DestinationPath, new AbsoluteConstraints(85, 46, 530, 25));

        //--- Row 3: Progress bar (full width) ---
        avanzamento.setFont(new java.awt.Font("Arial", 0, 12));
        avanzamento.setText(" ");
        getContentPane().add(avanzamento, new AbsoluteConstraints(10, 80, 780, 20));

        //--- Row 4: Tree panel (left) + Mode/Start/Cancel (right) ---
        jPanelGerarchia.setBorder(javax.swing.BorderFactory.createTitledBorder("Tree structure"));
        jPanelGerarchia.setLayout(new org.netbeans.lib.awtextra.AbsoluteLayout());

        GroupTree.add(OriginalTree);
        OriginalTree.setFont(new java.awt.Font("Arial", 0, 12));
        OriginalTree.setSelected(true);
        OriginalTree.setText("Original Tree");
        jPanelGerarchia.add(OriginalTree, new AbsoluteConstraints(10, 18, -1, -1));

        GroupTree.add(ScheduledTree);
        ScheduledTree.setFont(new java.awt.Font("Arial", 0, 12));
        ScheduledTree.setText("Scheduled: by year/month/extension");
        ScheduledTree.addActionListener(e -> ScheduledTreeActionPerformed(e));
        jPanelGerarchia.add(ScheduledTree, new AbsoluteConstraints(10, 38, 310, -1));

        getContentPane().add(jPanelGerarchia, new AbsoluteConstraints(10, 108, 340, 60));

        // Right side: Mode + buttons
        jCheckBoxCopia.setFont(new java.awt.Font("Arial", 0, 12));
        jCheckBoxCopia.setSelected(true);
        jCheckBoxCopia.setText("Copy");
        jCheckBoxCopia.addActionListener(e -> jCheckBoxCopiaActionPerformed(e));
        getContentPane().add(jCheckBoxCopia, new AbsoluteConstraints(370, 110, 80, 26));

        jButtonScheda.setFont(new java.awt.Font("Arial", 0, 12));
        jButtonScheda.setText("Start");
        jButtonScheda.addActionListener(e -> jButtonSchedaActionPerformed(e));
        getContentPane().add(jButtonScheda, new AbsoluteConstraints(460, 110, 150, 26));

        jButtonCancel.setFont(new java.awt.Font("Arial", 0, 12));
        jButtonCancel.setText("Cancel");
        jButtonCancel.addActionListener(e -> jButtonCancelActionPerformed(e));
        getContentPane().add(jButtonCancel, new AbsoluteConstraints(620, 110, 140, 26));

        //--- Row 5: Compare options panel ---
        jPanel1.setBackground(new java.awt.Color(220, 220, 220));
        jPanel1.setLayout(new org.netbeans.lib.awtextra.AbsoluteLayout());

        comparefile.setSelected(true);
        comparefile.setText("Compare by content");
        jPanel1.add(comparefile, new AbsoluteConstraints(10, 10, -1, -1));

        comparename.setText("Compare by name");
        comparename.addActionListener(e -> comparenameActionPerformed(e));
        jPanel1.add(comparename, new AbsoluteConstraints(150, 10, -1, -1));

        getContentPane().add(jPanel1, new AbsoluteConstraints(10, 178, 340, 60));

        //--- Row 6: Log area ---
        jTextLog.setEditable(false);
        jTextLog.setColumns(20);
        jTextLog.setFont(new java.awt.Font("Monospaced", 0, 11));
        jTextLog.setRows(5);
        jScrollPane1.setViewportView(jTextLog);
        getContentPane().add(jScrollPane1, new AbsoluteConstraints(10, 248, 780, 190));

        //--- Row 7: Footer ---
        info.setFont(new java.awt.Font("Arial", 0, 12));
        info.setForeground(new java.awt.Color(0, 0, 204));
        info.setText("Info");
        getContentPane().add(info, new AbsoluteConstraints(10, 448, -1, -1));

        developer.setFont(new java.awt.Font("Arial", 0, 12));
        developer.setForeground(new java.awt.Color(0, 0, 204));
        developer.setText("Donate to developer");
        getContentPane().add(developer, new AbsoluteConstraints(640, 448, -1, -1));

        pack();
        setLocationRelativeTo(null);
    }

    private void connectToDragDrop() {
        new java.awt.dnd.DropTarget(SourcePath, new DragListener(SourcePath));
        new java.awt.dnd.DropTarget(DestinationPath, new DragListener(DestinationPath));
    }

    private void checkForUpdates() {
        new Thread(() -> {
            Updater updater = new Updater();
            String latest = updater.fetchLatestVersion();
            if (latest == null) return;

            String current = updater.getCurrentVersion();
            if (Updater.compareVersions(current, latest) > 0) {
                String[] options = {"Download", "Later"};
                // Show dialog on EDT
                javax.swing.SwingUtilities.invokeLater(() -> {
                    int choice = JOptionPane.showOptionDialog(this,
                        "<html>A new version is available!<br>"
                        + "Current: <b>" + current + "</b><br>"
                        + "Latest:  <b>" + latest + "</b><br><br>"
                        + "Open the download page?</html>",
                        "Update Available",
                        JOptionPane.YES_NO_OPTION,
                        JOptionPane.INFORMATION_MESSAGE,
                        null, options, "Download");
                    if (choice == JOptionPane.YES_OPTION) {
                        Updater.openReleasesPage();
                    }
                });
            }
        }, "update-checker").start();
    }

    private void updateButtonLabel() {
        jButtonScheda.setText(jCheckBoxCopia.isSelected() ? "Start Copy" : "Start Move");
    }

    /** Called by MoveClass when operation completes (success or cancel). */
    void onTaskFinished() {
        javax.swing.SwingUtilities.invokeLater(() -> {
            jButtonScheda.setEnabled(true);
            workerThread = null;
            currentTask = null;
        });
    }

    private boolean validateInput() {
        String src = SourcePath.getText();
        String dst = DestinationPath.getText();
        if (src == null || src.trim().isEmpty()) {
            jTextLog.setText("Error: source directory not specified");
            return false;
        }
        if (dst == null || dst.trim().isEmpty()) {
            jTextLog.setText("Error: destination directory not specified");
            return false;
        }
        Path sourcePath = Paths.get(src);
        if (!Files.isDirectory(sourcePath)) {
            jTextLog.setText("Error: source is not a valid directory\n" + src);
            return false;
        }
        Path destPath = Paths.get(dst);
        if (!Files.isDirectory(destPath)) {
            jTextLog.setText("Error: destination is not a valid directory\n" + dst);
            return false;
        }
        if (sourcePath.toAbsolutePath().equals(destPath.toAbsolutePath())) {
            jTextLog.setText("Error: source and destination must be different directories");
            return false;
        }
        return true;
    }

    // ---- Event handlers ----

    private void jButtonSchedaActionPerformed(java.awt.event.ActionEvent evt) {
        if (workerThread != null && workerThread.isAlive()) {
            jTextLog.setText("Operation already in progress. Click Cancel to stop.");
            return;
        }
        if (!validateInput()) return;

        // Confirm before starting
        int confirm = JOptionPane.showConfirmDialog(this,
            "Start " + (jCheckBoxCopia.isSelected() ? "copy" : "move")
            + " from:\n  " + SourcePath.getText()
            + "\nto:\n  " + DestinationPath.getText()
            + "\n\nThis operation cannot be undone.",
            "Confirm",
            JOptionPane.OK_CANCEL_OPTION,
            JOptionPane.WARNING_MESSAGE);
        if (confirm != JOptionPane.OK_OPTION) return;

        Path source = Paths.get(SourcePath.getText());
        Path destination = Paths.get(DestinationPath.getText());

        currentTask = new MoveClass(
            source, destination, jTextLog, avanzamento,
            comparefile.isSelected(), comparename.isSelected(),
            jCheckBoxCopia.isSelected(), ScheduledTree.isSelected(),
            verifyHash.isSelected()
        );

        workerThread = new Thread(currentTask);
        workerThread.setDaemon(true);
        // Monitor thread: wait for worker, then re-enable button
        Thread monitor = new Thread(() -> {
            try {
                workerThread.join();
            } catch (InterruptedException ignored) {}
            javax.swing.SwingUtilities.invokeLater(() -> {
                jButtonScheda.setEnabled(true);
            });
        }, "task-monitor");
        monitor.setDaemon(true);
        workerThread.start();
        monitor.start();

        jButtonScheda.setEnabled(false);
        jTextLog.setText("Starting...\n");
    }

    private void jButtonCancelActionPerformed(java.awt.event.ActionEvent evt) {
        if (currentTask != null) currentTask.requestStop();
        if (workerThread != null && workerThread.isAlive()) {
            workerThread.interrupt();
            jTextLog.append("\nCancellation requested...\n");
        } else {
            jTextLog.setText("No active operation to cancel");
        }
    }

    private void jCheckBoxCopiaActionPerformed(java.awt.event.ActionEvent evt) {
        updateButtonLabel();
    }

    private void SourcePathActionPerformed(java.awt.event.ActionEvent evt) {
        chooseDirectory(SourcePath);
    }

    private void DestinationPathActionPerformed(java.awt.event.ActionEvent evt) {
        chooseDirectory(DestinationPath);
    }

    private void ScheduledTreeActionPerformed(java.awt.event.ActionEvent evt) {
    }

    private void comparenameActionPerformed(java.awt.event.ActionEvent evt) {
    }

    private void formWindowClosing(java.awt.event.WindowEvent evt) {
        Thread t = workerThread;
        MoveClass task = currentTask;
        if (task != null) task.requestStop();
        if (t != null && t.isAlive()) {
            t.interrupt();
            // Do not join on EDT: worker may be waiting for invokeAndWait().
        }

        Logger log = Logger.getLogger(SchedulerFiles.class.getName());
        log.info("End");
        for (Handler h : log.getHandlers()) {
            h.close();
            log.removeHandler(h);
        }
    }

    // -- Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JTextField DestinationPath;
    private javax.swing.ButtonGroup GroupTree;
    private javax.swing.JRadioButton OriginalTree;
    private javax.swing.JRadioButton ScheduledTree;
    private javax.swing.JTextField SourcePath;
    private javax.swing.JLabel avanzamento;
    private javax.swing.JCheckBox comparefile;
    private javax.swing.JCheckBox comparename;
    private javax.swing.JLabel developer;
    private javax.swing.JLabel info;
    private javax.swing.JButton jButtonCancel;
    private javax.swing.JButton jButtonScheda;
    private javax.swing.JCheckBox jCheckBoxCopia;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JPanel jPanelGerarchia;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JTextArea jTextLog;
    // End of variables declaration//GEN-END:variables

    public static void main(String args[]) {
        // CLI mode: if arguments provided, run headless
        if (args.length > 0) {
            System.exit(Cli.run(args));
            return;
        }

        // GUI mode
        try {
            javax.swing.UIManager.setLookAndFeel(
                javax.swing.UIManager.getSystemLookAndFeelClassName());
        } catch (ClassNotFoundException | InstantiationException | IllegalAccessException
                 | javax.swing.UnsupportedLookAndFeelException ex) {
            Logger.getLogger(SchedulerFiles.class.getName()).log(Level.SEVERE, null, ex);
        }
        java.awt.EventQueue.invokeLater(() -> new SchedulerFiles().setVisible(true));
    }
}
