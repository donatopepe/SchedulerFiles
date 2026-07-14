
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.dnd.DnDConstants;
import java.awt.dnd.DropTargetDragEvent;
import java.awt.dnd.DropTargetDropEvent;
import java.awt.dnd.DropTargetEvent;
import java.awt.dnd.DropTargetListener;
import java.io.File;
import java.util.List;
import javax.swing.JOptionPane;
import javax.swing.JTextField;

/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

/**
 *
 * @author Donato
 */
public class DragListener implements DropTargetListener {

    JTextField pathLabel = new JTextField();
    
    public DragListener(JTextField pathLabel )
    {
        this.pathLabel=pathLabel;
    }
    
    @Override
    public void dragEnter(DropTargetDragEvent dtde) {
        
    }

    @Override
    public void dragOver(DropTargetDragEvent dtde) {
        
    }

    @Override
    public void dropActionChanged(DropTargetDragEvent dtde) {
        
    }

    @Override
    public void dragExit(DropTargetEvent dte) {
       
    }

    @Override
    public void drop(DropTargetDropEvent dtde) {
       dtde.acceptDrop(DnDConstants.ACTION_COPY);
       Transferable t= dtde.getTransferable(); //dropped items
       DataFlavor[] df = t.getTransferDataFlavors();
       
       for (DataFlavor f:df)
       {
           try
           {
               if(f.isFlavorJavaFileListType())
               {
                  List<File> files = (List<File>) t.getTransferData(f);
                  for(File file:files)
                  {
                      pathLabel.setText(file.getPath());
                  }
                  
               }
           } 
           catch( Exception ex)
                   {
                       JOptionPane.showMessageDialog(null, ex);
                   }
       }
       
       
       
    }
    
}
