package sophie.widget;

import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.dnd.DnDConstants;
import java.awt.dnd.DropTarget;
import java.awt.dnd.DropTargetDragEvent;
import java.awt.dnd.DropTargetDropEvent;
import java.awt.dnd.DropTargetEvent;
import java.awt.dnd.DropTargetListener;
import java.io.File;
import java.io.IOException;
import java.util.List;

import javax.swing.JTextField;
import javax.swing.text.Document;

public class JFileTextField extends JTextField {
	static DataFlavor acceptFlavor = DataFlavor.javaFileListFlavor;
	public JFileTextField() {
		super();
		setupDnD();
	}
	
	public JFileTextField(Document doc, String text, int columns) {
		super(doc, text, columns);
		setupDnD();
	}
	
	public JFileTextField(int columns) {
		super(columns);
		setupDnD();
	}
	
	public JFileTextField(String text) {
		super(text);
		setupDnD();
	}

	public JFileTextField(String text, int columns) {
		super(text, columns);
		setupDnD();
	}

	void setupDnD() {
        DropTargetListener dropTargetListener = new DropTargetListener() {
            public void dragEnter(DropTargetDragEvent dtde) {
                DataFlavor[] flavors = dtde.getCurrentDataFlavors();
                int acceptableActions = 0;
                for(int i=0; i<flavors.length; i++) {
                    if(flavors[i].equals(acceptFlavor))
                        acceptableActions |= DnDConstants.ACTION_REFERENCE;
                }
                int newActions = dtde.getSourceActions() & acceptableActions;
                newActions |= DnDConstants.ACTION_COPY;
                if(dtde.getDropAction() != newActions) {
                    dtde.acceptDrag(newActions);
                }
            }
            public void dragOver(DropTargetDragEvent dtde) {
            }
            public void dropActionChanged(DropTargetDragEvent dtde) {
            }
            public void dragExit(DropTargetEvent dte) {
            }
            public void drop(DropTargetDropEvent dtde){
                boolean complete = false;
                DataFlavor[] flavors = dtde.getCurrentDataFlavors();
                for(int i=0; i<flavors.length; i++) {
                    if(flavors[i].equals(acceptFlavor) && ((dtde.getSourceActions() & DnDConstants.ACTION_REFERENCE) != 0)) {
                        dtde.acceptDrop(DnDConstants.ACTION_REFERENCE);
                        try {
                        	@SuppressWarnings("unchecked")
                        	List<File> fileList = (List<File>)dtde.getTransferable().getTransferData(acceptFlavor);
                        	if(fileList.size() == 1) {
                        		JFileTextField.this.setText(fileList.get(0).toString().replace('\\', '/'));
                        	}
                            complete = true;
                        } catch(UnsupportedFlavorException ex) {
                            System.out.println(ex);
                        } catch(IOException ex) {
                            System.out.println(ex);
                        }
                    }
                }
                if(complete) {
                    dtde.dropComplete(true);
                } else {
                    dtde.rejectDrop();
                }
            }
        };
        new DropTarget(this, dropTargetListener);
	}
}
