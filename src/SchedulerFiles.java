import java.awt.Cursor;
import java.awt.Desktop;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.io.IOException;
import java.io.BufferedWriter;
import java.io.FileWriter;
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
    private javax.swing.JTextField secondDestinationPath;
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
        DestinationPath.setToolTipText("Primary destination directory path - type, drag & drop, or Browse");
        secondDestinationPath.setToolTipText("Optional second destination; copy mode keeps both destinations synchronized");
        jButtonScheda.setToolTipText("Start the copy/move operation");
        jButtonCancel.setToolTipText("Cancel the running operation");
        jCheckBoxCopia.setToolTipText("Uncheck to move files (delete from source)");
        OriginalTree.setToolTipText("Preserve the original folder structure");
        ScheduledTree.setToolTipText("Organize by year / month / file extension");
        comparefile.setToolTipText("Skip files with identical content");
        comparename.setToolTipText("Skip files with the same filename");
        verifyHash.setToolTipText("Compute SHA-256 hash before and after transfer (default enabled)");
        SourcePath.getAccessibleContext().setAccessibleName("Source directory");
        DestinationPath.getAccessibleContext().setAccessibleName("Primary destination directory");
        secondDestinationPath.getAccessibleContext().setAccessibleName("Optional second destination directory");
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
        verifyHash.setSelected(true);
        jPanel1.add(verifyHash, new AbsoluteConstraints(10, 30, -1, -1));

        addBrowseButtons();
        javax.swing.JButton btnDst2 = new javax.swing.JButton("Browse");
        btnDst2.setFont(new java.awt.Font("Arial", 0, 12));
        btnDst2.getAccessibleContext().setAccessibleName("Browse second destination directory");
        btnDst2.addActionListener(e -> chooseDirectory(secondDestinationPath));
        getContentPane().add(btnDst2, new AbsoluteConstraints(625, 74, 80, 25));
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
        secondDestinationPath = new javax.swing.JTextField();
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
        java.net.URL appIcon = SchedulerFiles.class.getResource("/icon/schedulerfiles.png");
        if (appIcon != null) setIconImage(new javax.swing.ImageIcon(appIcon).getImage());
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

        //--- Optional twin destination ---
        javax.swing.JLabel secondDestinationLabel = new javax.swing.JLabel("Dest 2");
        secondDestinationLabel.setFont(new java.awt.Font("Arial", 0, 14));
        secondDestinationLabel.setLabelFor(secondDestinationPath);
        getContentPane().add(secondDestinationLabel, new AbsoluteConstraints(10, 76, -1, -1));
        getContentPane().add(secondDestinationPath, new AbsoluteConstraints(85, 74, 530, 25));

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
        String dst2 = secondDestinationPath.getText().trim();
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
        if (!dst2.isEmpty() && !jCheckBoxCopia.isSelected()) {
            jTextLog.setText("Error: second destination requires Copy mode");
            return false;
        }
        if (!Files.isDirectory(destPath)) {
            jTextLog.setText("Error: destination is not a valid directory\n" + dst);
            return false;
        }
        Path dest2Path = dst2.isEmpty() ? null : Paths.get(dst2);
        if (dest2Path != null && (!Files.isDirectory(dest2Path)
                || destPath.toAbsolutePath().equals(dest2Path.toAbsolutePath())
                || sourcePath.toAbsolutePath().equals(dest2Path.toAbsolutePath()))) {
            jTextLog.setText("Error: second destination must be different and valid");
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

        final String secondPath = secondDestinationPath.getText().trim();
        final boolean[] mirrorDelete = {false};
        final boolean[] mirrorCopyToPrimary = {false};
        if (!secondPath.isEmpty()) {
            try {
                Path primary = Paths.get(DestinationPath.getText());
                Path replica = Paths.get(secondPath);
                if (new MirrorService().hasStaleEntries(primary, replica)) {
                    Object[] choices = {"Delete extras", "Copy to Dest", "Keep extras", "Cancel"};
                    int choice = JOptionPane.showOptionDialog(this,
                        "Dest 2 contains files not present in Dest.\nWhat should synchronization do?",
                        "Mirror synchronization", JOptionPane.DEFAULT_OPTION,
                        JOptionPane.WARNING_MESSAGE, null, choices, choices[1]);
                    if (choice == 3 || choice == JOptionPane.CLOSED_OPTION) return;
                    mirrorDelete[0] = choice == 0;
                    mirrorCopyToPrimary[0] = choice == 1;
                }
            } catch (IOException e) {
                jTextLog.setText("Error checking Dest 2: " + e.getMessage());
                return;
            }
        }

        // Confirm before starting
        int confirm = JOptionPane.showConfirmDialog(this,
            "Start " + (jCheckBoxCopia.isSelected() ? "copy" : "move")
            + " from:\n  " + SourcePath.getText()
            + "\nto:\n  " + DestinationPath.getText()
            + (secondDestinationPath.getText().trim().isEmpty() ? "" :
                "\nand mirror:\n  " + secondDestinationPath.getText().trim())
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

        workerThread = new Thread(() -> {
            currentTask.run();
            if (!currentTask.hasErrors() && !currentTask.isCancelled() && !secondPath.isEmpty()) {
                try {
                    new MirrorService().synchronize(Paths.get(DestinationPath.getText()),
                        Paths.get(secondPath), mirrorDelete[0], mirrorCopyToPrimary[0]);
                    jTextLog.append("Twin destinations synchronized and parity verified.\n");
                } catch (IOException e) {
                    jTextLog.append("Twin destination parity failed: " + e.getMessage() + "\n");
                }
            }
        });
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

    private static void offerDesktopShortcut() {
        if (!System.getProperty("os.name", "").toLowerCase().contains("win")) return;

        File desktop = new File(System.getProperty("user.home"), "Desktop");
        if (!desktop.isDirectory()) return;
        File shortcut = new File(desktop, "SchedulerFiles.lnk");
        if (shortcut.exists()) return;

        int answer = JOptionPane.showConfirmDialog(null,
            "Create SchedulerFiles shortcut on user desktop?",
            "SchedulerFiles", JOptionPane.YES_NO_OPTION,
            JOptionPane.QUESTION_MESSAGE);
        if (answer != JOptionPane.YES_OPTION) return;

        try {
            java.net.URL location = SchedulerFiles.class.getProtectionDomain()
                .getCodeSource().getLocation();
            File jar = new File(location.toURI());
            if (!jar.isFile() || !jar.getName().toLowerCase().endsWith(".jar")) {
                JOptionPane.showMessageDialog(null,
                    "Shortcut can be created only when running from a JAR.",
                    "SchedulerFiles", JOptionPane.INFORMATION_MESSAGE);
                return;
            }

            File iconFile = new File(System.getenv("LOCALAPPDATA"),
                "SchedulerFiles\\schedulerfiles.ico");
            File iconDirectory = iconFile.getParentFile();
            if (!iconDirectory.isDirectory() && !iconDirectory.mkdirs()) {
                throw new IOException("Could not create icon directory");
            }
            try (java.io.InputStream icon = SchedulerFiles.class
                    .getResourceAsStream("/icon/schedulerfiles.ico")) {
                if (icon == null) throw new IOException("Application icon is missing");
                Files.copy(icon, iconFile.toPath(),
                    java.nio.file.StandardCopyOption.REPLACE_EXISTING);
            }

            File script = File.createTempFile("schedulerfiles-shortcut-", ".vbs");
            try (BufferedWriter writer = new BufferedWriter(new FileWriter(script))) {
                writer.write("Set shell = CreateObject(\"WScript.Shell\")\r\n");
                writer.write("Set link = shell.CreateShortcut(\"" + vbs(shortcut.getAbsolutePath()) + "\")\r\n");
                writer.write("link.TargetPath = \"javaw.exe\"\r\n");
                writer.write("link.Arguments = \"-jar \"\"" + vbs(jar.getAbsolutePath()) + "\"\"\"\r\n");
                writer.write("link.WorkingDirectory = \"" + vbs(jar.getParentFile().getAbsolutePath()) + "\"\r\n");
                writer.write("link.Description = \"SchedulerFiles\"\r\n");
                writer.write("link.IconLocation = \"" + vbs(iconFile.getAbsolutePath()) + ",0\"\r\n");
                writer.write("link.Save\r\n");
            }
            Process process = new ProcessBuilder("cscript.exe", "//nologo", script.getAbsolutePath())
                .redirectErrorStream(true).start();
            if (process.waitFor() != 0 || !shortcut.isFile()) {
                throw new IOException("Windows shortcut creation failed");
            }
        } catch (Exception e) {
            Logger.getLogger(SchedulerFiles.class.getName()).log(Level.WARNING,
                "Could not create desktop shortcut", e);
            JOptionPane.showMessageDialog(null,
                "Could not create desktop shortcut:\n" + e.getMessage(),
                "SchedulerFiles", JOptionPane.WARNING_MESSAGE);
        }
    }

    private static String vbs(String value) {
        return value.replace("\\", "\\\\").replace("\"", "\"\"");
    }

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
        java.awt.EventQueue.invokeLater(() -> {
            offerDesktopShortcut();
            new SchedulerFiles().setVisible(true);
        });
    }
}
