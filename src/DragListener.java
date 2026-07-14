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

        try {
            for (DataFlavor f : df) {
                if (f.isFlavorJavaFileListType()) {
                    Object data = t.getTransferData(f);
                    if (!(data instanceof List<?>)) continue;
                    for (Object item : (List<?>) data) {
                        if (item instanceof File && ((File) item).isDirectory()) {
                            pathLabel.setText(((File) item).getAbsolutePath());
                            break;
                        }
                    }
                }
            }
        } catch (Exception ex) {
            pathLabel.setText("Error: " + ex.getMessage());
        } finally {
            pathLabel.setBackground(java.awt.Color.WHITE);
        }
    }
}
