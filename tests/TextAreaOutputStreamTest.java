import javax.swing.JTextArea;
import java.util.*;

public class TextAreaOutputStreamTest {

    public static List<Runnable> suite() {
        return Arrays.asList(
            TextAreaOutputStreamTest::testWriteAndFlush,
            TextAreaOutputStreamTest::testMultipleWrites,
            TextAreaOutputStreamTest::testEmptyWrite
        );
    }

    static void testWriteAndFlush() {
        JTextArea ta = new JTextArea();
        TextAreaOutputStream os = new TextAreaOutputStream(ta);
        try {
            os.write("Hello".getBytes("UTF-8"));
            os.flush();
            TestRunner.assertEquals("Hello", ta.getText(), "TextArea contains 'Hello' after write+flush");
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    static void testMultipleWrites() {
        JTextArea ta = new JTextArea();
        TextAreaOutputStream os = new TextAreaOutputStream(ta);
        try {
            os.write("A".getBytes("UTF-8"));
            os.flush();
            os.write("B".getBytes("UTF-8"));
            os.flush();
            TestRunner.assertEquals("AB", ta.getText(), "TextArea contains 'AB' after two writes");
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    static void testEmptyWrite() {
        JTextArea ta = new JTextArea();
        TextAreaOutputStream os = new TextAreaOutputStream(ta);
        try {
            os.flush();
            TestRunner.assertEquals("", ta.getText(), "TextArea is empty after flush with no data");
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
