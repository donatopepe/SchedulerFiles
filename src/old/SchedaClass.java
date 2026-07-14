
import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.FileVisitResult;
import static java.nio.file.FileVisitResult.CONTINUE;
import static java.nio.file.FileVisitResult.TERMINATE;
import java.nio.file.FileVisitor;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Comparator;
import java.util.GregorianCalendar;
import java.util.TreeSet;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JTextArea;

/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
/**
 *
 * @author donato
 */
public class SchedaClass implements FileVisitor<Path> {

    public ArrayList<String> log = new ArrayList<>();
    private final Path destination;
    private final Path source;
    private final Calendar cdr = new GregorianCalendar();
    private final JTextArea textlog;
    private final boolean interrupted;
    private final boolean copia;
    private final boolean schedula;

    SchedaClass(Path source, Path destination, JTextArea textlog, boolean interrupted, boolean copia, boolean schedula) {
        this.destination = destination;
        this.textlog = textlog;
        this.interrupted = interrupted;
        this.copia = copia;
        this.schedula = schedula;
        this.source = source;
    }

    private boolean IsHidden(Path file) throws IOException {
        boolean hidden = false;

        //if ((Files.isHidden(file)) || (file.getFileName().startsWith("."))) {
        if (Files.isHidden(file)) {
            hidden = true;
        }

        return hidden;
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

// Absolute paths should be sorted lexicograhically for files in folders
    //private TreeSet<File> accumulator = new TreeSet<>(Comparator.comparing(File::getAbsolutePath));
    private TreeSet<String> accumulator = new TreeSet<>(new LengthFirstComparator());

    @Override
    public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {

        if (interrupted) {
            return TERMINATE;
        }
        /*
        synchronized (accumulator) {
            accumulator.clear();  // Start fresh
        }
        */
        //System.out.print("preVisitDirectory: ");
/*
        if (IsHidden(dir)) {
            //System.out.print("Hidden ");
            outputtext("Hidden " + dir + "\n");
            //return SKIP_SUBTREE;
        }
        if (attrs.isDirectory()) {

            //System.out.format("Directory: %s ", dir);
            outputtext("Directory: " + dir + "\n");
        } else {
            //System.out.format("Other: %s ", dir);
            outputtext("Other: " + dir + "\n");
        }
         */
        //String name = dir.getFileName().toString();
        //String ext = name.substring(name.lastIndexOf(".") + 1);
        //cdr.setTimeInMillis(attrs.lastModifiedTime().toMillis());
        //int year = cdr.get(Calendar.YEAR);
        //int month = cdr.get(Calendar.MONTH) + 1;
        //String output = "(" + attrs.size() + " bytes, lastModifiedTime: " + "Years: " + year + " Month: " + month + " extension " + ext + " )";
        //System.out.println(output);
        //outputtext(output + "\n");

        //outputtext("preVisitDirectory: " + dir.toString() + "\n");
        return CONTINUE;
    }
    //private int sec_prec;

    private void outputtext(String test) {

        cdr.setTimeInMillis(System.currentTimeMillis());
        System.out.print(cdr.getTime() + ":" + test);
        /*
        log.add((cdr.getTime() + ":" + test));
        if (log.size() > 100) {
            log.remove(0);
        }
        //SchedaJFrame.textlog.setText(log);
        textlog.setText(log.toString());
        textlog.setCaretPosition(textlog.getText().length());
         */
        textlog.append(cdr.getTime() + ":" + test);
        textlog.setCaretPosition(textlog.getDocument().getLength());
        /*
        if (cdr.get(Calendar.SECOND) != sec_prec) {
            //SchedaJFrame.textlog.update(SchedaJFrame.textlog.getGraphics());
            //textlog.update(textlog.getGraphics());
            sec_prec = cdr.get(Calendar.SECOND);
        }
         */
    }

    private void createdir(String newDirectory) {
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

    private boolean comparefile(Path source, String dest) throws IOException {
        if (Files.size(Paths.get(dest)) != Files.size(source)) {
            return false;
        }

        if (Files.exists(Paths.get(dest))) {

            //outputtext("same size then compare file " + source.toString() + "\n");

            long start = System.nanoTime();
            FileChannel ch1 = new RandomAccessFile(source.toString(), "r").getChannel();
            FileChannel ch2 = new RandomAccessFile(Paths.get(dest).toString(), "r").getChannel();
            if (ch1.size() != ch2.size()) {
                outputtext("Files have different length" + "\n");
                return false;
            }
            long size = ch1.size();
            ByteBuffer m1 = ch1.map(FileChannel.MapMode.READ_ONLY, 0L, size);
            ByteBuffer m2 = ch2.map(FileChannel.MapMode.READ_ONLY, 0L, size);

            for (int pos = 0; pos < size; pos++) {
                if (m1.get(pos) != m2.get(pos)) {
                    //outputtext("Files differ at position "+ "\n");// + pos + "\n");

                    return false;
                }
            }

            long end = System.nanoTime();
            outputtext("Execution time: " + (end - start) / 1000000 + "ms" + "\n");
            //outputtext("Same files " + source.toString() + " == " + dest + "\n");
        }

        return true;
    }

    private void move(Path source, String dest) throws IOException {
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

            File folder = new File(dest);
            boolean samefile = false;
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
                outputtext("File already exists in the target directory regardless of filename then it will be bypassed " + source.toString() + "\n");
            }

        } catch (IOException e) {
            //moving file failed.
            //System.out.println("unable to move the file " + source.toString());
            outputtext(e + " unable to move the file " + source.toString() + "\n");
        }

    }

    private void FileMoveOrCopy(String file) {
        try {
            //String name = file.getFileName().toString();

            Path fileio = Paths.get(file);
            String name = fileio.getFileName().toString();
            String ext = "";
            BasicFileAttributes attrs = Files.readAttributes(fileio, BasicFileAttributes.class);
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

                String pathfilesource = file.toString().substring(0, file.toString().length() - name.length());
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

    @Override
    public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
        if (interrupted) {
            return TERMINATE;
        }
        /*
        //System.out.print("visitFile: ");
        //outputtext("visitFile: ");
        if (IsHidden(file)) {
            //System.out.print("Hidden ");
            outputtext("Hidden " + file + "\n");
            //return CONTINUE;
        }
        if (attrs.isSymbolicLink()) {
            //System.out.format("Symbolic link: %s ", file);
            outputtext("Symbolic link: " + file + "\n");
            //return CONTINUE;
        } else if (attrs.isRegularFile()) {

            //System.out.format("Regular file: %s ", file);
            outputtext("Regular file: " + file + "\n");

        } else {
            //System.out.format("Other: %s ", file);
            outputtext("Other:  " + file + "\n");

        }
         */
        //File f = file.toFile();
        synchronized (accumulator) {
            accumulator.add(file.toString());  // accumulate
            //outputtext("add treeset: " + file + "\n");
        }

        return CONTINUE;
    }

    @Override
    public FileVisitResult visitFileFailed(Path file, IOException exc) throws IOException {
        if (interrupted) {
            return TERMINATE;
        }
        //System.out.print("vistiFileFailed: ");
        //System.err.println(exc);
        outputtext("visit File Failed: " + exc + "\n");
        return CONTINUE;
    }

    @Override
    public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
        if (interrupted) {
            return TERMINATE;
        }
        //System.out.print("postVisitDirectory: ");
        //System.out.format("Directory: %s%n", dir);

        //TreeSet<File> intsReverse = (TreeSet<File>)accumulator.descendingSet(); 
        accumulator.forEach((f) -> {
            FileMoveOrCopy(f);
        });
        /*
        synchronized (accumulator) {
            accumulator.clear();  // Start fresh
        }
        */

        //outputtext("postVisitDirectory: " + dir + "\n");
        return CONTINUE;
    }

}
