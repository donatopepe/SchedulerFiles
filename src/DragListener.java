import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.dnd.DnDConstants;
import java.awt.dnd.DropTargetDragEvent;
import java.awt.dnd.DropTargetDropEvent;
import java.awt.dnd.DropTargetEvent;
import java.awt.dnd.DropTargetListener;
import java.io.File;
import java.util.List;
import javax.swing.JTextField;

public class DragListener implements DropTargetListener {

    private final JTextField pathLabel;

    public DragListener(JTextField pathLabel) {
        this.pathLabel = pathLabel;
    }

    @Override
    public void dragEnter(DropTargetDragEvent dtde) {
        dtde.acceptDrag(DnDConstants.ACTION_COPY);
        pathLabel.setBackground(new java.awt.Color(220, 240, 220));
    }

    @Override
    public void dragOver(DropTargetDragEvent dtde) {
        dtde.acceptDrag(DnDConstants.ACTION_COPY);
    }

    @Override
    public void dropActionChanged(DropTargetDragEvent dtde) {
    }

    @Override
    public void dragExit(DropTargetEvent dte) {
        pathLabel.setBackground(java.awt.Color.WHITE);
    }

    @Override
    public void drop(DropTargetDropEvent dtde) {
        dtde.acceptDrop(DnDConstants.ACTION_COPY);
        Transferable t = dtde.getTransferable();
        DataFlavor[] df = t.getTransferDataFlavors();

        for (DataFlavor f : df) {
            try {
                if (f.isFlavorJavaFileListType()) {
                    List<File> files = (List<File>) t.getTransferData(f);
                    for (File file : files) {
                        if (file.isDirectory()) {
                            pathLabel.setText(file.getAbsolutePath());
                            break; // first directory found
                        }
                    }
                }
            } catch (Exception ex) {
                pathLabel.setText("Error: " + ex.getMessage());
            }
        }
    }
}
