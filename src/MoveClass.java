
import java.io.BufferedInputStream;
import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import static java.lang.Thread.sleep;
import java.nio.ByteBuffer;
import java.nio.LongBuffer;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.FileVisitOption;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Comparator;
import java.util.Date;
import java.util.EnumSet;
import java.util.GregorianCalendar;
import java.util.TreeSet;
import java.util.logging.FileHandler;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;
import java.util.logging.StreamHandler;
import javax.swing.JLabel;
import javax.swing.JTextArea;
import java.nio.file.attribute.UserDefinedFileAttributeView;

import java.util.List;

/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
/**
 *
 * @author donato
 */
public class MoveClass implements Runnable {

    private final Path source;
    private final Path destination;
    private final JTextArea textlog;
    private final JLabel avanzamento;
    private final boolean comparefile;
    private final boolean comparename;
    private final boolean copia;
    private final boolean schedula;
    private final Calendar cdr = new GregorianCalendar();
    //private final FileWriter writer;
    //private final File file;
    //private final BufferedWriter writer;
    private String stravanzamento;
    private boolean stoptread;

    private boolean isstoptread() {
        if (Thread.interrupted()) {
            stoptread = true;
        }
        return stoptread;
    }

    private void outputtext(String test) throws IOException {
        /*
        cdr.setTimeInMillis(System.currentTimeMillis());
        System.out.print(cdr.getTime() + ":" + test);
        writer.append(cdr.getTime() + ":" + test);
        textlog.append(cdr.getTime() + ":" + test);
        textlog.setCaretPosition(textlog.getDocument().getLength());
         */
        Logger.getLogger(MoveClass.class.getName()).info(test);
    }

    public class LengthFirstComparator implements Comparator<String> {

        @Override
        public int compare(String o1, String o2) {
            if (o1.length() != o2.length()) {
                return o1.length() - o2.length(); //overflow impossible since lengths are non-negative
            }
            return o1.compareTo(o2);
        }
    }

    private void createdir(String newDirectory) throws IOException {
        Path newDirectoryPath = Paths.get(newDirectory);
        //if (!Files.exists(newDirectoryPath)) {
        try {
            Files.createDirectories(newDirectoryPath);
            //System.out.println("Cerate Dir " + newDirectoryPath.toString());
            //outputtext("Create Dir " + newDirectoryPath.toString() + "\n");
        } catch (IOException e) {
            //System.err.println(e);
            outputtext(e.toString() + "\n");
        }
        //}
    }

    private boolean comparefile(Path source, String dest) throws IOException, InterruptedException {
        if (!Files.exists(Paths.get(dest))) {
            return false;
        } else if (Files.size(Paths.get(dest)) != Files.size(source)) {
            return false;
        } else if (comparefile) {
            //outputtext("same size then compare file " + source.toString() + "\n");
            long start = System.nanoTime();
            //if (Files.size(source) <= Integer.MAX_VALUE) {
            FileChannel ch1, ch2;
            try {

                ch1 = new RandomAccessFile(source.toString(), "r").getChannel();
                ch2 = new RandomAccessFile(Paths.get(dest).toString(), "r").getChannel();
                if (ch1.size() != ch2.size()) {
                    outputtext("Files have different length" + "\n");
                    return false;
                }
                long size = ch1.size();
                long offset = 0;

                long chucksize = size > Integer.MAX_VALUE ? Integer.MAX_VALUE : size;

                while (chucksize > 0) {
                    //System.out.println("offset " + offset + " chucksize " + chucksize);
                    MappedByteBuffer m1 = ch1.map(FileChannel.MapMode.READ_ONLY, offset, chucksize);
                    MappedByteBuffer m2 = ch2.map(FileChannel.MapMode.READ_ONLY, offset, chucksize);

                    for (int pos = 0; pos < chucksize; pos++) {
                        double perctree = ((double) (offset + pos) / (double) size) * (double) 100;
                        if (perctree % 10 == 0) {
                            String strDouble = String.format("%2.0f", perctree);
                            avanzamento.setText(stravanzamento + "% <--" + strDouble + "%");
                        }

                        if (m1.get(pos) != m2.get(pos)) {
                            //outputtext("Files differ at position "+ "\n");// + pos + "\n");
                            ch1.close();
                            ch2.close();
                            return false;
                        }
                        //sleep(1);
                        if (isstoptread()) {
                            //System.out.println("Stop compare");
                            ch1.close();
                            ch2.close();
                            return false;
                        }
                    }

                    offset = offset + chucksize;

                    long chuckrim = ch1.size() - offset;

                    chucksize = chuckrim > Integer.MAX_VALUE ? Integer.MAX_VALUE : chuckrim;

                }

                /*
            } else {
                BufferedInputStream fis1 = new BufferedInputStream(new FileInputStream(source.toString()));
                BufferedInputStream fis2 = new BufferedInputStream(new FileInputStream(Paths.get(dest).toString()));
                int b1 = 0, b2 = 0, pos = 1;
                while (b1 != -1 && b2 != -1) {
                    if (b1 != b2) {
                        System.out.println("Files differ at position " + pos);
                        return false;
                    }
                    pos++;
                    b1 = fis1.read();
                    b2 = fis2.read();
                }
                if (b1 != b2) {
                    System.out.println("Files have different length");
                    return false;
                } else {
                    System.out.println("Files are identical, you can delete one of them.");
                }
                fis1.close();
                fis2.close();
            }
                 */
                long end = System.nanoTime();
                outputtext("Execution time compare files: " + (end - start) / 1000000 + "ms ");
                //outputtext("Same files " + source.toString() + " == " + dest + "\n");
                ch1.close();
                ch2.close();
                return true;
            } catch (IOException e) {
                //moving file failed.
                //System.out.println("unable to move the file " + source.toString());
                outputtext(e + " unable access the file for compare " + "\n");
            }
        }
        return false;
    }

    private void move(Path source, String dest) throws IOException, InterruptedException {
        String name = source.getFileName().toString();
        String ext = "";

        if (name.contains(".")) {
            int punto = name.lastIndexOf(".");
            ext = (name.substring(punto)).toLowerCase();
            name = name.substring(0, punto);
        }

        String ren = "";
        Integer i = 0;

        try {

            boolean samefile = false;
            if (comparename || comparefile) {
                if (!comparename) {
                    File folder = new File(dest);
                    for (final File fileEntry : folder.listFiles()) {
                        if (fileEntry.isDirectory()) {
                            //System.out.println("Directory: " + fileEntry.getName());
                        } else {
                            //System.out.println("File: " + fileEntry.getName());
                            samefile = comparefile(source, fileEntry.getAbsolutePath());
                            if (samefile) {
                                outputtext("Same files " + source.toString() + " == " + fileEntry.getAbsolutePath() + "\n");
                                break;
                            }
                        }
                    }
                } else {
                    samefile = comparefile(source, dest + name + ren + ext);
                }
            }
            if (isstoptread()) {
                //System.out.println("Stop copy or move");
                return;
            }
            if (!samefile) {

                while (Files.exists(Paths.get(dest + name + ren + ext))) {
                    outputtext("file exist " + Paths.get(dest + name + ren + ext) + "\n");
                    i = i + 1;
                    ren = " " + i.toString();
                }

                if (this.copia) {
                    try {
                        Path copy = Files.copy(source, Paths.get(dest + name + ren + ext), StandardCopyOption.COPY_ATTRIBUTES);
                        if (copy != null) {
                            outputtext("File copied successfully " + source.toString() + "->" + dest + name + ren + ext + "\n");
                        } else {
                            outputtext("Failed to copied the file" + "\n");
                        }
                    } catch (IOException ex) {
                        outputtext("Failed to copy the file " + ex + "\n");
                    }
                } else {

                    //boolean move = source.toFile().renameTo(Paths.get(dest + ren + ext).toFile());
                    try {
                        Path move = Files.move(source, Paths.get(dest + name + ren + ext));
                        //FileUtils.moveFile(source.toFile(), (Paths.get(dest + ren + ext)).toFile());
                        if (move != null) {
                            outputtext("File moved successfully " + source.toString() + "->" + dest + name + ren + ext + "\n");
                        } else {
                            outputtext("Failed to move the file" + "\n");
                        }
                    } catch (IOException ex) {
                        outputtext("Failed to move the file " + ex + "\n");
                    }

                }
            } else {
                outputtext("File bypassed " + source.toString() + "\n");
            }

        } catch (IOException e) {
            //moving file failed.
            //System.out.println("unable to move the file " + source.toString());
            outputtext(e + " unable access the file " + source.toString() + "\n");
        }

    }

    private void FileMoveOrCopy(String file) throws InterruptedException {
        try {
            //String name = file.getFileName().toString();

            Path fileio = Paths.get(file);
            String name = fileio.getFileName().toString();
            String ext = "";
            BasicFileAttributes attrs = Files.readAttributes(fileio, BasicFileAttributes.class);
            
            UserDefinedFileAttributeView fileAttributeView = Files.getFileAttributeView(fileio, UserDefinedFileAttributeView.class);
            List<String> allAttrs = fileAttributeView.list();

            allAttrs.forEach((att) -> {
                System.out.println("att = " + att);
            });
            cdr.setTimeInMillis(attrs.lastModifiedTime().toMillis());
            int year = cdr.get(Calendar.YEAR);
            int month = cdr.get(Calendar.MONTH) + 1;
            String dest = destination.toString();

            if (this.schedula) {
                dest = dest + "/" + year + "/" + month;

                if (name.contains(".")) {

                    ext = name.substring(name.lastIndexOf(".") + 1).toLowerCase();
                    dest = dest + "/" + ext;
                }
                dest = dest + "/";
                outputtext("(" + attrs.size() + " bytes, lastModifiedTime: " + "Years: " + year + " Month: " + month + " extension " + ext + " )" + "\n");
            } else {

                String pathfilesource = file.substring(0, file.length() - name.length());
                //outputtext("pathfilesource:  " + pathfilesource + "\n");
                String dirsource = source.toString();
                //outputtext("dirsource:  " + dirsource + "\n");
                dest = dest + pathfilesource.substring(dirsource.length());
                //outputtext("dest:  " + dest + "\n");
            }
            createdir(dest);
            move(fileio, dest);
        } catch (IOException ex) {
            Logger.getLogger(SchedaClass.class.getName()).log(Level.SEVERE, null, ex);
        }

    }

    // Absolute paths should be sorted lexicograhically for files in folders
    //private TreeSet<File> accumulator = new TreeSet<>(Comparator.comparing(File::getAbsolutePath));
    private final TreeSet<String> accumulator = new TreeSet<>(new LengthFirstComparator());

    public void listFiles(final File folder) throws InterruptedException {
        for (final File file : folder.listFiles()) {

            if (isstoptread()) {
                //System.out.println("Stop listing files");
                return;
            }

            if (file.isDirectory()) {
                listFiles(file);
                //outputtext("dir: " + file.getPath() + "\n");
            } else {
                //outputtext("file: " + file.getPath() + "\n");

                synchronized (accumulator) {
                    accumulator.add(file.getPath());  // accumulate
                    //outputtext("add treeset: " + file + "\n");
                    avanzamento.setText(accumulator.size() + "");
                }

                //FileMoveOrCopy(file.getPath());
            }
        }
    }

    @Override
    public void run() {

        try {
            textlog.setText("");

            outputtext("!!!!THE START!!!!!\n");
            synchronized (accumulator) {
                accumulator.clear();  // Start fresh
            }
            final File folder = new File(source.toUri());
            listFiles(folder);
            int sizetree = accumulator.size();
            int counttree = 0;
            //System.out.println("size tree:" + sizetree);
            for (String f : accumulator) {

                if (isstoptread()) {
                    //System.out.println("Stop loop files");
                    return;
                }

                FileMoveOrCopy(f);
                counttree++;
                double perctree = ((double) counttree / (double) sizetree) * (double) 100;
                stravanzamento = String.format("%.2f", perctree);
                avanzamento.setText(stravanzamento + "%");
                //System.out.println(counttree + " " + stravanzamento + "%");

            }
            synchronized (accumulator) {
                accumulator.clear();  // Start fresh
            }
            /* 
            try {
            Thread.sleep(100);
            // follow links when copying files
            EnumSet<FileVisitOption> opts = EnumSet.of(FileVisitOption.FOLLOW_LINKS);
            SchedaClass scheda = new SchedaClass(source, destination, textlog, Thread.interrupted(), this.copia, this.schedula);
            Path walkFileTree = Files.walkFileTree(source, opts, Integer.MAX_VALUE, scheda);
            Thread.sleep(100);
            
            //this.jTextLog.add(scheda.log, this);
            //textlog.setText("!!!!THE END!!!!!\n"+scheda.log);

            //System.out.println("!!!!THE END!!!!!");
            } catch (IOException e) {
            //System.out.println("Exception: " + e);
            outputtext("Exception: " + e);
            
            } catch (InterruptedException ex) {
            Logger.getLogger(MoveClass.class.getName()).log(Level.SEVERE, null, ex);
            }
             */
            //System.out.println("Exception: " + e);
            outputtext("!!!!THE END!!!!!\n");
            // writer.close();

        } catch (IOException | InterruptedException ex) {
            Logger.getLogger(MoveClass.class.getName()).log(Level.SEVERE, null, ex);
        } finally {
            /*
            try {
                writer.close();
            } catch (IOException ex) {
                Logger.getLogger(MoveClass.class.getName()).log(Level.SEVERE, null, ex);
            }
             */
            Logger log = Logger.getLogger(MoveClass.class.getName());
            Handler[] hs = log.getHandlers();
            for (Handler h : hs) {
                h.close();
                log.removeHandler(h);
            }
        }

    }

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

    public MoveClass(Path source, Path destination, JTextArea textlog, JLabel avanzamento, boolean comparefile, boolean comparename, boolean copia, boolean schedula) throws IOException {
        this.stoptread = false;
        this.comparefile = comparefile;
        this.comparename = comparename;
        this.stravanzamento = "0";
        this.source = source;
        this.destination = destination;
        this.textlog = textlog;
        this.copia = copia;
        this.schedula = schedula;
        this.avanzamento = avanzamento;
        //cdr.setTimeInMillis(System.currentTimeMillis());

        //writer = new FileWriter("SchedulerFiles "+cdr.getTime()+".log", true);
        //writer = new FileWriter("SchedulerFiles.log", true);
        // Date date = new Date();
        //SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH-mm-ss");
        //file = new File("CopyMove " + dateFormat.format(date) + ".log");
        // writer = new BufferedWriter(new FileWriter(file));
        Logger logger = Logger.getLogger(MoveClass.class.getName());
        FileHandler fh;
        OutputStream os = new TextAreaOutputStream(textlog);
        logger.addHandler(new TextAreaHandler(os));

        try {

            // This block configure the logger with handler and formatter  
            Date date = new Date();
            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH-mm-ss");
            fh = new FileHandler("CopyMove " + dateFormat.format(date) + ".log");
            logger.addHandler(fh);
            SimpleFormatter formatter = new SimpleFormatter();
            fh.setFormatter(formatter);

            // the following statement is used to log any messages  
            logger.info("Start");

        } catch (SecurityException | IOException ex) {

            logger.log(Level.SEVERE, null, ex);
        }

        //logger.info("End!!!");
    }

}
