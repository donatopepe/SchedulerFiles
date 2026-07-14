import java.awt.Cursor;
import java.awt.Desktop;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
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
import javax.swing.Timer;
import javax.swing.text.DefaultCaret;

public class SchedulerFiles extends javax.swing.JFrame {

    private final String address = "dnt.ppe@gmail.com";
    private final String paypalme = "https://www.paypal.me/DonatoPepe";
    private Thread workerThread;
    private MoveClass currentTask;

    public SchedulerFiles() {
        initComponents();
        postInit();
    }

    private void postInit() {
        // Logger setup
        Logger logger = Logger.getLogger(SchedulerFiles.class.getName());
        OutputStream os = new TextAreaOutputStream(jTextLog);
        logger.addHandler(new TextAreaHandler(os));
        try {
            Date date = new Date();
            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH-mm-ss");
            FileHandler fh = new FileHandler("SchedulerFiles " + dateFormat.format(date) + ".log");
            logger.addHandler(fh);
            fh.setFormatter(new SimpleFormatter());
            logger.info("Start");
        } catch (SecurityException | IOException ex) {
            Logger.getLogger(SchedulerFiles.class.getName()).log(Level.SEVERE, null, ex);
        }

        // Sync button text
        updateButtonLabel();

        // Auto-scroll log
        DefaultCaret caret = (DefaultCaret) jTextLog.getCaret();
        caret.setUpdatePolicy(DefaultCaret.ALWAYS_UPDATE);

        // PayPal link
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

        // Info link
        info.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        info.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                new infoJFrame().setVisible(true);
            }
        });

        // Drag & drop
        connectToDragDrop();

        // Browse buttons
        addBrowseButtons();

        // Periodic UI refresh
        Timer timer = new Timer(500, e -> {
            if (currentTask != null && workerThread != null && workerThread.isAlive()) {
                // progress updated by MoveClass directly via avanzamento label
            }
        });
        timer.start();
    }

    private void addBrowseButtons() {
        javax.swing.JButton btnSrc = new javax.swing.JButton("Browse");
        btnSrc.setFont(new java.awt.Font("Arial", 0, 12));
        btnSrc.setBounds(630, 20, 80, 25);
        btnSrc.addActionListener(e -> chooseDirectory(SourcePath));
        getContentPane().add(btnSrc, 0);

        javax.swing.JButton btnDst = new javax.swing.JButton("Browse");
        btnDst.setFont(new java.awt.Font("Arial", 0, 12));
        btnDst.setBounds(630, 50, 80, 25);
        btnDst.addActionListener(e -> chooseDirectory(DestinationPath));
        getContentPane().add(btnDst, 0);
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

    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
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
        setFont(new java.awt.Font("Arial", 0, 10)); // NOI18N
        setResizable(false);
        addWindowListener(new java.awt.event.WindowAdapter() {
            public void windowClosing(java.awt.event.WindowEvent evt) {
                formWindowClosing(evt);
            }
        });
        getContentPane().setLayout(new org.netbeans.lib.awtextra.AbsoluteLayout());

        jTextLog.setEditable(false);
        jTextLog.setColumns(20);
        jTextLog.setFont(new java.awt.Font("Arial", 0, 12)); // NOI18N
        jTextLog.setRows(5);
        jScrollPane1.setViewportView(jTextLog);

        getContentPane().add(jScrollPane1, new org.netbeans.lib.awtextra.AbsoluteConstraints(10, 210, 710, 190));

        jLabel1.setFont(new java.awt.Font("Arial", 0, 18)); // NOI18N
        jLabel1.setText("Source Dir");
        getContentPane().add(jLabel1, new org.netbeans.lib.awtextra.AbsoluteConstraints(10, 20, -1, -1));

        jButtonScheda.setFont(new java.awt.Font("Arial", 0, 12)); // NOI18N
        jButtonScheda.setText("Move");
        jButtonScheda.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButtonSchedaActionPerformed(evt);
            }
        });
        getContentPane().add(jButtonScheda, new org.netbeans.lib.awtextra.AbsoluteConstraints(470, 120, 160, -1));

        jLabel3.setFont(new java.awt.Font("Arial", 0, 18)); // NOI18N
        jLabel3.setText("Destination Dir");
        getContentPane().add(jLabel3, new org.netbeans.lib.awtextra.AbsoluteConstraints(10, 50, -1, -1));

        jButtonCancel.setFont(new java.awt.Font("Arial", 0, 12)); // NOI18N
        jButtonCancel.setText("Cancel");
        jButtonCancel.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButtonCancelActionPerformed(evt);
            }
        });
        getContentPane().add(jButtonCancel, new org.netbeans.lib.awtextra.AbsoluteConstraints(630, 120, 160, -1));

        jCheckBoxCopia.setFont(new java.awt.Font("Arial", 0, 12)); // NOI18N
        jCheckBoxCopia.setSelected(true);
        jCheckBoxCopia.setText("Copy");
        jCheckBoxCopia.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jCheckBoxCopiaActionPerformed(evt);
            }
        });
        getContentPane().add(jCheckBoxCopia, new org.netbeans.lib.awtextra.AbsoluteConstraints(400, 120, 70, -1));

        SourcePath.setFont(new java.awt.Font("Arial", 0, 12)); // NOI18N
        SourcePath.setText("Source path");
        SourcePath.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                SourcePathActionPerformed(evt);
            }
        });
        getContentPane().add(SourcePath, new org.netbeans.lib.awtextra.AbsoluteConstraints(100, 20, 520, 25));

        DestinationPath.setFont(new java.awt.Font("Arial", 0, 12)); // NOI18N
        DestinationPath.setText("Destination path");
        DestinationPath.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                DestinationPathActionPerformed(evt);
            }
        });
        getContentPane().add(DestinationPath, new org.netbeans.lib.awtextra.AbsoluteConstraints(100, 50, 520, 25));

        developer.setFont(new java.awt.Font("Arial", 0, 12)); // NOI18N
        developer.setForeground(new java.awt.Color(0, 0, 204));
        developer.setText("Donate to developer");
        getContentPane().add(developer, new org.netbeans.lib.awtextra.AbsoluteConstraints(400, 420, -1, -1));

        info.setFont(new java.awt.Font("Arial", 0, 12)); // NOI18N
        info.setForeground(new java.awt.Color(0, 0, 204));
        info.setText("Info");
        getContentPane().add(info, new org.netbeans.lib.awtextra.AbsoluteConstraints(10, 420, -1, -1));

        avanzamento.setFont(new java.awt.Font("Arial", 0, 12)); // NOI18N
        avanzamento.setText(" ");
        getContentPane().add(avanzamento, new org.netbeans.lib.awtextra.AbsoluteConstraints(400, 80, 330, 20));

        jPanelGerarchia.setBorder(javax.swing.BorderFactory.createTitledBorder("Tree structure"));
        jPanelGerarchia.setLayout(new org.netbeans.lib.awtextra.AbsoluteLayout());

        GroupTree.add(ScheduledTree);
        ScheduledTree.setFont(new java.awt.Font("Arial", 0, 12)); // NOI18N
        ScheduledTree.setText("Tree scheduled: divided by year, month and extension");
        ScheduledTree.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                ScheduledTreeActionPerformed(evt);
            }
        });
        jPanelGerarchia.add(ScheduledTree, new org.netbeans.lib.awtextra.AbsoluteConstraints(10, 30, 330, -1));

        GroupTree.add(OriginalTree);
        OriginalTree.setFont(new java.awt.Font("Arial", 0, 12)); // NOI18N
        OriginalTree.setSelected(true);
        OriginalTree.setText("Original Tree");
        jPanelGerarchia.add(OriginalTree, new org.netbeans.lib.awtextra.AbsoluteConstraints(10, 10, -1, -1));

        getContentPane().add(jPanelGerarchia, new org.netbeans.lib.awtextra.AbsoluteConstraints(10, 90, 350, 60));

        jPanel1.setBackground(new java.awt.Color(204, 204, 204));
        jPanel1.setLayout(new org.netbeans.lib.awtextra.AbsoluteLayout());

        comparefile.setSelected(true);
        comparefile.setText("Compare file content");
        jPanel1.add(comparefile, new org.netbeans.lib.awtextra.AbsoluteConstraints(10, 10, -1, -1));

        comparename.setText("Compare file name");
        comparename.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                comparenameActionPerformed(evt);
            }
        });
        jPanel1.add(comparename, new org.netbeans.lib.awtextra.AbsoluteConstraints(190, 10, -1, -1));

        getContentPane().add(jPanel1, new org.netbeans.lib.awtextra.AbsoluteConstraints(10, 160, 350, 40));

        pack();
        setLocationRelativeTo(null);
    }// </editor-fold>//GEN-END:initComponents

    private void connectToDragDrop() {
        new java.awt.dnd.DropTarget(SourcePath, new DragListener(SourcePath));
        new java.awt.dnd.DropTarget(DestinationPath, new DragListener(DestinationPath));
    }

    private void updateButtonLabel() {
        if (jCheckBoxCopia.isSelected()) {
            jButtonScheda.setText("Copy");
        } else {
            jButtonScheda.setText("Move");
        }
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

    private void jButtonSchedaActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButtonSchedaActionPerformed
        if (workerThread != null && workerThread.isAlive()) {
            jTextLog.setText("Operation already in progress. Click Cancel to stop it.");
            return;
        }

        if (!validateInput()) return;

        Path source = Paths.get(SourcePath.getText());
        Path destination = Paths.get(DestinationPath.getText());

        currentTask = new MoveClass(
            source, destination, jTextLog, avanzamento,
            comparefile.isSelected(), comparename.isSelected(),
            jCheckBoxCopia.isSelected(), ScheduledTree.isSelected()
        );

        workerThread = new Thread(currentTask);
        workerThread.setDaemon(true);
        workerThread.start();

        jButtonScheda.setEnabled(false);
        jTextLog.setText("Starting...\n");
    }//GEN-LAST:event_jButtonSchedaActionPerformed

    private void jButtonCancelActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButtonCancelActionPerformed
        if (currentTask != null) {
            currentTask.requestStop();
        }
        if (workerThread != null && workerThread.isAlive()) {
            workerThread.interrupt();
            jTextLog.append("\nCancellation requested...\n");
        } else {
            jTextLog.setText("No active operation to cancel");
        }
    }//GEN-LAST:event_jButtonCancelActionPerformed

    private void jCheckBoxCopiaActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jCheckBoxCopiaActionPerformed
        updateButtonLabel();
    }//GEN-LAST:event_jCheckBoxCopiaActionPerformed

    private void SourcePathActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_SourcePathActionPerformed
        chooseDirectory(SourcePath);
    }//GEN-LAST:event_SourcePathActionPerformed

    private void DestinationPathActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_DestinationPathActionPerformed
        chooseDirectory(DestinationPath);
    }//GEN-LAST:event_DestinationPathActionPerformed

    private void ScheduledTreeActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_ScheduledTreeActionPerformed
        // Radio button selected state is automatically handled by ButtonGroup
    }//GEN-LAST:event_ScheduledTreeActionPerformed

    private void comparenameActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_comparenameActionPerformed
        // When comparing by name, content comparison becomes redundant
        // But we let the user choose any combination
    }//GEN-LAST:event_comparenameActionPerformed

    private void formWindowClosing(java.awt.event.WindowEvent evt) {//GEN-FIRST:event_formWindowClosing
        // Cancel running task
        if (currentTask != null) {
            currentTask.requestStop();
        }
        if (workerThread != null && workerThread.isAlive()) {
            workerThread.interrupt();
        }

        Logger log = Logger.getLogger(SchedulerFiles.class.getName());
        log.info("End");
        for (Handler h : log.getHandlers()) {
            h.close();
            log.removeHandler(h);
        }
    }//GEN-LAST:event_formWindowClosing

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
        try {
            for (javax.swing.UIManager.LookAndFeelInfo info : javax.swing.UIManager.getInstalledLookAndFeels()) {
                if ("Nimbus".equals(info.getName())) {
                    javax.swing.UIManager.setLookAndFeel(info.getClassName());
                    break;
                }
            }
        } catch (ClassNotFoundException | InstantiationException | IllegalAccessException | javax.swing.UnsupportedLookAndFeelException ex) {
            java.util.logging.Logger.getLogger(SchedulerFiles.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        }

        java.awt.EventQueue.invokeLater(() -> new SchedulerFiles().setVisible(true));
    }
}
