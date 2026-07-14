import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import javax.swing.JTextArea;

public class TextAreaOutputStream extends OutputStream {

    private final ByteArrayOutputStream buffer = new ByteArrayOutputStream();
    private final JTextArea textArea;

    public TextAreaOutputStream(JTextArea textArea) {
        super();
        this.textArea = textArea;
    }

    @Override
    public synchronized void flush() throws IOException {
        textArea.append(buffer.toString("UTF-8"));
        buffer.reset();
    }

    @Override
    public synchronized void write(int b) throws IOException {
        buffer.write(b);
    }
}
