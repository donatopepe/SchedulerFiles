
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Desktop;
import java.awt.dnd.DropTarget;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import static java.lang.Thread.sleep;
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
import java.util.logging.LogRecord;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;
import java.util.logging.StreamHandler;
import javax.swing.JOptionPane;
import javax.swing.JTextArea;
import javax.swing.Timer;
import javax.swing.text.DefaultCaret;


/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
/**
 *
 * @author donato
 */
public class SchedulerFiles extends javax.swing.JFrame {

    /**
     * Creates new form SchedulerFiles
     */
    //public static JTextArea textlog;
    String address = "dnt.ppe@gmail.com"; // global
    String paypalme = "https://www.paypal.me/DonatoPepe";

    class TextAreaOutputStream extends OutputStream {

        private final ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        private final JTextArea textArea;

        protected TextAreaOutputStream(JTextArea textArea) {
            super();
            this.textArea = textArea;
        }

        @Override
        public void flush() throws IOException {
            textArea.append(buffer.toString("UTF-8"));
            buffer.reset();
        }

        @Override
        public void write(int b) throws IOException {
            buffer.write(b);
        }
    }

    class TextAreaHandler extends StreamHandler {

        private void configure() {
            setFormatter(new SimpleFormatter());
            try {
                setEncoding("UTF-8");
            } catch (IOException ex) {
                try {
                    setEncoding(null);
                } catch (IOException ex2) {
                    // doing a setEncoding with null should always work.
                    // assert false;
                    ex2.printStackTrace();
                }
            }
        }

        protected TextAreaHandler(OutputStream os) {
            super();
            configure();
            setOutputStream(os);
        }

        // [UnsynchronizedOverridesSynchronized] Unsynchronized method publish overrides synchronized method in StreamHandler
        @SuppressWarnings("PMD.AvoidSynchronizedAtMethodLevel")
        @Override
        public synchronized void publish(LogRecord record) {
            super.publish(record);
            flush();
        }

        // [UnsynchronizedOverridesSynchronized] Unsynchronized method close overrides synchronized method in StreamHandler
        @SuppressWarnings("PMD.AvoidSynchronizedAtMethodLevel")
        @Override
        public synchronized void close() {
            flush();
        }
    }

    public SchedulerFiles() {
        initComponents();
        Logger logger = Logger.getLogger(SchedulerFiles.class.getName());
        FileHandler fh;
        OutputStream os = new TextAreaOutputStream(jTextLog);
        logger.addHandler(new TextAreaHandler(os));
        try {

            // This block configure the logger with handler and formatter  
            Date date = new Date();
            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH-mm-ss");
            fh = new FileHandler("SchedulerFiles " + dateFormat.format(date) + ".log");
            logger.addHandler(fh);
            SimpleFormatter formatter = new SimpleFormatter();
            fh.setFormatter(formatter);

            // the following statement is used to log any messages  
            logger.info("Start");

        } catch (SecurityException | IOException ex) {

            logger.log(Level.SEVERE, null, ex);
        }
        if (jCheckBoxCopia.isSelected()) {
            jButtonScheda.setText("Copy");
        } else {
            jButtonScheda.setText("Move");
        }
        DefaultCaret caret = (DefaultCaret) jTextLog.getCaret();
        caret.setUpdatePolicy(DefaultCaret.ALWAYS_UPDATE);
        developer.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        developer.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                try {
                    //Desktop.getDesktop().mail(new URI("mailto:" + address + "?subject=Hello"));
                    Desktop.getDesktop().browse(new URI(paypalme));
                } catch (URISyntaxException | IOException ex) {
                    JOptionPane.showMessageDialog(null, ex);
                }
            }
        });
        //setBackground(new Color(0,0,0,0));
        info.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        info.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                //System.out.println("page info");
                infoJFrame newFrame = new infoJFrame();
                newFrame.setVisible(true);
            }
        });
        connectToDragDrop();
        Timer timer = new Timer(1000, new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent evt) {

                if (t1 == null || (!t1.isAlive())) {
                    t1 = null;

                    jButtonScheda.setEnabled(true);
                    jButtonCancel.setEnabled(false);
                } else {
                    jButtonScheda.setEnabled(false);
                    jButtonCancel.setEnabled(true);
                }

            }
        });
        timer.start();

    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
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
        jTextLog.setLineWrap(true);
        jTextLog.setRows(5);
        jScrollPane1.setViewportView(jTextLog);

        getContentPane().add(jScrollPane1, new org.netbeans.lib.awtextra.AbsoluteConstraints(0, 229, 820, 180));

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
        getContentPane().add(SourcePath, new org.netbeans.lib.awtextra.AbsoluteConstraints(130, 20, 690, 30));

        DestinationPath.setFont(new java.awt.Font("Arial", 0, 12)); // NOI18N
        DestinationPath.setText("Destination path");
        getContentPane().add(DestinationPath, new org.netbeans.lib.awtextra.AbsoluteConstraints(130, 50, 690, 30));

        developer.setFont(new java.awt.Font("Arial", 0, 12)); // NOI18N
        developer.setText("If you like the program, the author accepts donations through PayPal.");
        getContentPane().add(developer, new org.netbeans.lib.awtextra.AbsoluteConstraints(400, 170, -1, -1));

        info.setText("?");
        info.setAlignmentX(0.5F);
        info.setPreferredSize(new java.awt.Dimension(10, 10));
        getContentPane().add(info, new org.netbeans.lib.awtextra.AbsoluteConstraints(820, 0, 10, 20));

        avanzamento.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        avanzamento.setText("0%");
        getContentPane().add(avanzamento, new org.netbeans.lib.awtextra.AbsoluteConstraints(360, 200, 90, 20));

        jPanelGerarchia.setBackground(new java.awt.Color(204, 204, 204));
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

    private Thread t1;

    private void connectToDragDrop() {
        DragListener dsource = new DragListener(SourcePath);
        new DropTarget(SourcePath, dsource);
        DragListener ddestination = new DragListener(DestinationPath);
        new DropTarget(DestinationPath, ddestination);
    }


    private void jButtonSchedaActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButtonSchedaActionPerformed

        System.out.println("Source Dir: " + SourcePath.getText());

        if (SourcePath.getText() == null) {
            System.out.println("Error <source directory>");
            jTextLog.setText("Error <starting directory>");
            //System.exit(-1);
        } else {
            System.out.println("Destination Dir: " + DestinationPath.getText());
            if (DestinationPath.getText() == null) {
                System.out.println("Error <destination directory>");
                jTextLog.setText("Error <destination directory>");
                //System.exit(-1);

            } else {
                Path source = Paths.get(SourcePath.getText());//.getAbsolutePath()
                if (!Files.isDirectory(source)) {
                    System.out.println(source.toString() + " must be a directory!");
                    //System.exit(-1);
                    jTextLog.setText(source.toString() + " must be a directory!");
                } else {
                    Path destination = Paths.get(DestinationPath.getText());//.getAbsolutePath()
                    if (!Files.isDirectory(destination)) {
                        System.out.println(destination.toString() + " must be a directory!");
                        //System.exit(-1);
                        jTextLog.setText(destination.toString() + " must be a directory!");

                    } else {
                        if (destination.toString().equals(source.toString())) {
                            System.out.println(destination.toString() + " must be different directory!");
                            //System.exit(-1);
                            jTextLog.setText(destination.toString() + " must be different directory!");

                        } else {
                            if ((t1 == null))// || (!t1.isAlive())) 
                            {
                                MoveClass moveClass = null;
                                try {
                                    moveClass = new MoveClass(source, destination, jTextLog, avanzamento, comparefile.isSelected(), comparename.isSelected(), jCheckBoxCopia.isSelected(), this.ScheduledTree.isSelected());
                                } catch (IOException ex) {
                                    Logger.getLogger(SchedulerFiles.class.getName()).log(Level.SEVERE, null, ex);
                                }

                                //t1.start();
                                t1 = new Thread(moveClass);
                                t1.start();

                                jButtonScheda.setEnabled(false);
                                jButtonCancel.setEnabled(true);
                            } else {
                                System.out.println("Wait, please ");
                                jTextLog.setText("Wait, please ");
                            }

                        }
                    }
                }
            }
        }

    }//GEN-LAST:event_jButtonSchedaActionPerformed

    private void jButtonCancelActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButtonCancelActionPerformed
        if ((t1 == null) || (!t1.isAlive())) {
            System.out.println("No threads to stop ");
            jTextLog.setText("No threads to stop");
        } else {
            System.out.println("Stop thread");
            jTextLog.setText("Stop thread");

            t1.interrupt();
        }
    }//GEN-LAST:event_jButtonCancelActionPerformed

    private void jCheckBoxCopiaActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jCheckBoxCopiaActionPerformed
        // TODO add your handling code here:

        if (jCheckBoxCopia.isSelected()) {
            jButtonScheda.setText("Copy");
        } else {
            jButtonScheda.setText("Move");
        }
    }//GEN-LAST:event_jCheckBoxCopiaActionPerformed

    private void SourcePathActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_SourcePathActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_SourcePathActionPerformed

    private void ScheduledTreeActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_ScheduledTreeActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_ScheduledTreeActionPerformed

    private void comparenameActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_comparenameActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_comparenameActionPerformed

    private void formWindowClosing(java.awt.event.WindowEvent evt) {//GEN-FIRST:event_formWindowClosing
        Logger log = Logger.getLogger(SchedulerFiles.class.getName());
        log.info("End!!!");
        Handler[] hs = log.getHandlers();
        for (Handler h : hs) {
            h.close();
            log.removeHandler(h);
        }
    }//GEN-LAST:event_formWindowClosing

    /**
     * @param args the command line arguments
     */
    public static void main(String args[]) {


        /* Set the Nimbus look and feel */
        //<editor-fold defaultstate="collapsed" desc=" Look and feel setting code (optional) ">
        /* If Nimbus (introduced in Java SE 6) is not available, stay with the default look and feel.
         * For details see http://download.oracle.com/javase/tutorial/uiswing/lookandfeel/plaf.html 
         */
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
        //</editor-fold>
        //</editor-fold>

        //</editor-fold>
        //</editor-fold>

        /* Create and display the form */
        java.awt.EventQueue.invokeLater(() -> {
            new SchedulerFiles().setVisible(true);
        });

    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
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
}
