package sophie.tools.textfile.sort;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.nio.charset.Charset;
import java.text.Collator;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Locale;
import java.util.SortedMap;
import java.util.Vector;

import javax.swing.Box;
import javax.swing.DefaultCellEditor;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingConstants;
import javax.swing.UIManager;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;

import sophie.io.CharsetTeller;
import sophie.widget.ErrorMessageDialog;
import sophie.widget.JFileTextField;


public class SortUI extends JPanel {
	private static final String AUTOMATIC = "Automatic";
	private static final String DEFAULT = "Default";
	private static final String NONE = "None";
	private static final String BOL = "BOL"; // Beginning of line
	private static final String EOL = "EOL"; // End of line
	private JFileTextField inputFileTextField;
	private JFileTextField outputFileTextField;
	private JComboBox<Object> inputEncodingComboBox;
	private JComboBox<Object> outputEncodingComboBox;
	private JComboBox<Object> textLocaleComboBox;
	private JComboBox<Object> numberLocaleComboBox;
	private JComboBox<String> fieldSeparatorComboBox;
	private JCheckBox stableCheckBox;
	private JCheckBox uniqueCheckBox;
	private JSpinner headerField;
	private JTextField bufferSizeTextField;
	private JComboBox<String> bufferSizeComboBox;
	SortKeyTableModel sortKeyTableModel;
	SortKeyTableView sortKeyTableView;
    
	static class Key {
		int startFieldNumber = 0;
		int startCharNumber = 0;
		boolean startSkipBlanks = false;
		int endFieldNumber = Integer.MAX_VALUE;
		int endCharNumber = 0;
		boolean endSkipBlanks = false;
		SortKind sortKind = SortKind.Text;
		IgnoreKind ignoreKind = null;
		TranslateKind translateKind = null;
		boolean reverse = false;
	}
	
	static class SortKeyTableModel extends AbstractTableModel {
		Vector<Key> keyList = new Vector<Key>();

		void addNewKey() {
			keyList.add(new Key());
			fireTableDataChanged();
		}
		
		void removeKey(int rowIndex) {
			if(rowIndex >= 0 && rowIndex < keyList.size()) {
				keyList.remove(rowIndex);
				fireTableDataChanged();
			}
		}
		
		void moveUpKey(int rowIndex) {
			if(rowIndex > 0 && rowIndex < keyList.size()) {
				Key key = keyList.get(rowIndex);
				keyList.remove(rowIndex);
				keyList.add(rowIndex - 1, key);
				fireTableDataChanged();
			}
		}
		
		void moveDownKey(int rowIndex) {
			if(rowIndex >= 0 && rowIndex < keyList.size() - 1) {
				Key key = keyList.get(rowIndex);
				keyList.remove(rowIndex);
				keyList.add(rowIndex + 1, key);
				fireTableDataChanged();
			}
		}
		
		@Override
		public int getColumnCount() {
			return 11;
		}

		@Override
		public int getRowCount() {
			return keyList.size();
		}

		@Override
		public Object getValueAt(int rowIndex,  int columnIndex) {
			Key key = keyList.get(rowIndex);
			switch(columnIndex) {
			case 0: return rowIndex + 1;
			case 1: return (key.startFieldNumber == 0)? BOL: Integer.toString(key.startFieldNumber);
			case 2: return (key.startCharNumber == 0)? "": Integer.toString(key.startCharNumber);
			case 3: return key.startSkipBlanks;
			case 4: return (key.endFieldNumber == Integer.MAX_VALUE)? EOL: Integer.toString(key.endFieldNumber);
			case 5: return (key.endCharNumber == 0)? "": Integer.toString(key.endCharNumber);
			case 6: return key.endSkipBlanks;
			case 7: return key.sortKind;
			case 8: return (key.ignoreKind != null)? key.ignoreKind.toString(): "";
			case 9: return (key.translateKind != null)? key.translateKind.toString(): "";
			case 10: return key.reverse;
			default:  throw new IllegalStateException("columnIndex: Out of range");
			}
		}
		
		public void setValueAt(Object value, int rowIndex, int columnIndex) {
			Key key = keyList.get(rowIndex);
			switch(columnIndex) {
			case 0:
				break;
			case 1:
				String startFieldValue = (String) value;
				if(startFieldValue.matches("\\d+")) {
					key.startFieldNumber = Integer.valueOf(startFieldValue);
				} else {
					key.startFieldNumber = 0;
				}
				break;
			case 2:
				String startCharValue = (String) value;
				if(startCharValue.matches("\\d+")) {
					key.startCharNumber = Integer.valueOf(startCharValue);
				} else {
					key.startCharNumber = 0;
				}
				break;
			case 3:
				key.startSkipBlanks = (Boolean) value;
				break;
			case 4:
				String endFieldValue = (String) value;
				if(endFieldValue.matches("\\d+")) {
					key.endFieldNumber = Integer.valueOf(endFieldValue);
				} else {
					key.endFieldNumber = Integer.MAX_VALUE;
				}
				break;
			case 5:
				String endCharValue = (String) value;
				if(endCharValue.matches("\\d+")) {
					key.endCharNumber = Integer.valueOf(endCharValue);
				} else {
					key.endCharNumber = 0;
				}
				break;
			case 6:
				key.endSkipBlanks = (Boolean) value;
				break;
			case 7:
				key.sortKind = (SortKind) value;
				break;
			case 8:
				String ignoreValue = (String) value;
				if(ignoreValue.equals("")) {
					key.ignoreKind = null;
				} else {
					key.ignoreKind = IgnoreKind.valueOf(ignoreValue);
				}
				break;
			case 9:
				String translateValue = (String) value;
				if(translateValue.equals("")) {
					key.translateKind = null;
				} else {
					key.translateKind = TranslateKind.valueOf(translateValue);
				}
				break;
			case 10:
				key.reverse = (Boolean) value;
				break;
			default:
				throw new IllegalStateException();
			}
			fireTableCellUpdated(rowIndex, columnIndex);
		}
		
	    public String getColumnName(int columnIndex) {
	    	switch(columnIndex) {
	    	case 0: return "No";
	    	case 1: return "Start\nField #";
	    	case 2: return " \nChar #";
	    	case 3: return " \nSkip Blanks";
	    	case 4: return "End\nField #";
	    	case 5: return " \nChar #";
	    	case 6: return " \nSkip Blanks";
	    	case 7: return "Sort Kind";
	    	case 8: return "Ignore";
	    	case 9: return "Translate";
	    	case 10: return "Reverse";
	    	default: throw new IllegalStateException("columnIndex: Out of range");
	    	}
	    }
		
	}
	
	static class MultiLineHeaderCellRenderer implements TableCellRenderer {

		@Override
		public Component getTableCellRendererComponent(JTable table,
				Object value,
				boolean isSelected,
				boolean hasFocus,
				int row,
				int column) {
			String[] sa = value.toString().split("\n");
			Box box = Box.createVerticalBox();
			for(String s: sa) {
				JLabel label = new JLabel(s);
				if (table != null) {
					JTableHeader header = table.getTableHeader();
					if (header != null) {
						label.setForeground(header.getForeground());
						label.setBackground(header.getBackground());
						label.setFont(header.getFont());
					}
				}
				box.add(label);
			}
			box.setBorder(UIManager.getBorder("TableHeader.cellBorder"));
			return box;
		}

	}

	static class CellRenderer extends DefaultTableCellRenderer {
        CellRenderer() {
            setFont(new Font("Dialog", Font.PLAIN, 12));
            setHorizontalAlignment(DefaultTableCellRenderer.CENTER);
        }
        public Component getTableCellRendererComponent(JTable table,
                                               Object value,
                                               boolean isSelected,
                                               boolean hasFocus,
                                               int row,
                                               int column) {
            return super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
        }
    }
    
	static class BooleanCellRenderer  extends DefaultTableCellRenderer {
		JCheckBox checkBox;
        public Component getTableCellRendererComponent(JTable table,
                Object value,
                boolean isSelected,
                boolean hasFocus,
                int row,
                int column) {
        	checkBox.setSelected((Boolean) value);
        	checkBox.setBackground(UIManager.getColor(isSelected? "Table.selectionBackground": "Table.background"));
        	return checkBox;
        }
        
        BooleanCellRenderer() {
        	checkBox = new JCheckBox();
        	checkBox.setHorizontalAlignment(CENTER);
        	//checkBox.setBackground(Color.white);
        }
	}
	
	static class BooleanCellEditor extends DefaultCellEditor {
        JCheckBox checkBox;

        BooleanCellEditor() {
            super(new JCheckBox());
            checkBox = (JCheckBox)getComponent();
            checkBox.setHorizontalAlignment(DefaultTableCellRenderer.CENTER);
        }
        
        public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int row, int column) {
            checkBox.setSelected((Boolean)value);
            return checkBox;
        }
    }

	static class ComboBoxCellRenderer<T>  extends DefaultTableCellRenderer {
		JComboBox<String> comboBox;

        public Component getTableCellRendererComponent(JTable table,
                Object value,
                boolean isSelected,
                boolean hasFocus,
                int row,
                int column) {
        	comboBox.removeAllItems();
        	comboBox.addItem((value != null)? value.toString(): "");
        	comboBox.setBackground(UIManager.getColor(isSelected? "Table.selectionBackground": "Table.background"));
        	return comboBox;
        }
        
        ComboBoxCellRenderer() {
        	this.comboBox = new JComboBox<String>();
        }
	}

	static class ComboBoxCellEditor<T>  extends DefaultCellEditor {
		JComboBox<T> comboBox;

        public Component getTableCellRendererComponent(JTable table,
                Object value,
                boolean isSelected,
                boolean hasFocus,
                int row,
                int column) {
        	comboBox.setSelectedItem(value);
        	//comboBox.setBackground(UIManager.getColor(isSelected? "Table.selectionBackground": "Table.background"));
        	comboBox.getEditor().getEditorComponent().setBackground(UIManager.getColor("Table.background"));
        	return comboBox;
        }

        ComboBoxCellEditor(JComboBox<T> comboBox) {
        	super(comboBox);
        	this.comboBox = comboBox;
        }
	}

	static class SortKeyTableView  extends JTable {
		SortKeyTableModel model;
		
		void addNewKey() {
			((SortKeyTableModel)getModel()).addNewKey();
		}
		
		void removeKey() {
			int rowIndex = getSelectedRow();
			((SortKeyTableModel)getModel()).removeKey(rowIndex);
		}
		
		void moveUpKey() {
			int rowIndex = getSelectedRow();
			if(rowIndex > 0) {
				((SortKeyTableModel)getModel()).moveUpKey(rowIndex);
				addRowSelectionInterval(rowIndex - 1, rowIndex - 1);
			}
		}
		
		void moveDownKey() {
			int rowIndex = getSelectedRow();
			if(rowIndex >= 0 &&  rowIndex < getRowCount() - 1) {
				((SortKeyTableModel)getModel()).moveDownKey(rowIndex);
				addRowSelectionInterval(rowIndex + 1, rowIndex + 1);
			}
		}
		
	    public boolean isCellEditable(int row, int column) {
	        return true;
	    }

		SortKeyTableView(SortKeyTableModel model) {
			super(model);
	        setFont(new Font(Font.DIALOG, Font.BOLD, 12));
	        setRowHeight(18);
	        setFillsViewportHeight(true);
	        setAutoResizeMode(AUTO_RESIZE_ALL_COLUMNS);
	        setPreferredScrollableViewportSize(new Dimension(800,  Math.max(Math.min(model.getRowCount(), 20), 4) * getRowHeight()));
	        getTableHeader().setReorderingAllowed(false);
	        getTableHeader().setResizingAllowed(true);
	        ((DefaultTableCellRenderer)getTableHeader().getDefaultRenderer()).setHorizontalAlignment(SwingConstants.LEFT);
	        setDefaultRenderer(Object.class, new CellRenderer());
	        TableColumnModel columnModel = getColumnModel();
	        MultiLineHeaderCellRenderer multiLineHeaderCellRenderer = new MultiLineHeaderCellRenderer();
	        BooleanCellEditor booleanCellEditor = new BooleanCellEditor();
	        BooleanCellRenderer booleanCellRenderer = new BooleanCellRenderer();
	        JComboBox<SortKind> sortKindComboBox = new JComboBox<SortKind>();
        	for(SortKind item: SortKind.values()) {
        		sortKindComboBox.addItem(item);
        	}
	        ComboBoxCellRenderer<SortKind> sortKindCellRenderer = new ComboBoxCellRenderer<SortKind>();
	        ComboBoxCellEditor<SortKind> sortKindCellEditor = new ComboBoxCellEditor<SortKind>(sortKindComboBox);
        	JComboBox<String> ignoreKindComboBox = new JComboBox<String>();
        	ignoreKindComboBox.addItem("");
        	for(IgnoreKind item: IgnoreKind.values()) {
        		ignoreKindComboBox.addItem(item.toString());
        	}
	        ComboBoxCellRenderer<String> ignoreKindCellRenderer = new ComboBoxCellRenderer<String>();
	        ComboBoxCellEditor<String> ignoreKindCellEditor = new ComboBoxCellEditor<String>(ignoreKindComboBox);
        	JComboBox<String> translateKindComboBox = new JComboBox<String>();
        	translateKindComboBox.addItem("");
        	for(TranslateKind item: TranslateKind.values()) {
        		translateKindComboBox.addItem(item.toString());
        	}
	        ComboBoxCellRenderer<String> translateKindCellRenderer = new ComboBoxCellRenderer<String>();
	        ComboBoxCellEditor<String> translateKindCellEditor = new ComboBoxCellEditor<String>(translateKindComboBox);
        	JComboBox<String> startFieldComboBox = new JComboBox<String>();
        	startFieldComboBox.setEditable(true);
        	startFieldComboBox.addItem(BOL);
        	for(int i = 1; i < 100; i++) {
        		startFieldComboBox.addItem(Integer.toString(i));
        	}
	        ComboBoxCellRenderer<String> startFieldCellRenderer = new ComboBoxCellRenderer<String>();
	        ComboBoxCellEditor<String> startFieldCellEditor = new ComboBoxCellEditor<String>(startFieldComboBox);
        	JComboBox<String> endFieldComboBox = new JComboBox<String>();
        	endFieldComboBox.setEditable(true);
        	endFieldComboBox.addItem(EOL);
        	for(int i = 1; i < 100; i++) {
        		endFieldComboBox.addItem(Integer.toString(i));
        	}
	        ComboBoxCellRenderer<String> endFieldCellRenderer = new ComboBoxCellRenderer<String>();
	        ComboBoxCellEditor<String> endFieldCellEditor = new ComboBoxCellEditor<String>(endFieldComboBox);
        	JComboBox<String> charNumberComboBox = new JComboBox<String>();
        	charNumberComboBox.setEditable(true);
        	charNumberComboBox.addItem("");
        	for(int i = 1; i < 100; i++) {
        		charNumberComboBox.addItem(Integer.toString(i));
        	}
	        ComboBoxCellRenderer<String> charNumberCellRenderer = new ComboBoxCellRenderer<String>();
	        ComboBoxCellEditor<String> charNumberCellEditor = new ComboBoxCellEditor<String>(charNumberComboBox);
	        TableColumn column = columnModel.getColumn(0);
	        column.setPreferredWidth(30);
	        column.setMaxWidth(30);
	        column.setResizable(false);
	        column = columnModel.getColumn(1);
	        column.setPreferredWidth(50);
	        column.setMinWidth(50);
	        column.setResizable(false);
	        column.setHeaderRenderer(multiLineHeaderCellRenderer);
	        column.setCellRenderer(startFieldCellRenderer);
	        column.setCellEditor(startFieldCellEditor);
	        column = columnModel.getColumn(2);
	        column.setPreferredWidth(50);
	        column.setMinWidth(50);
	        column.setResizable(false);
	        column.setHeaderRenderer(multiLineHeaderCellRenderer);
	        column.setCellRenderer(charNumberCellRenderer);
	        column.setCellEditor(charNumberCellEditor);
	        column = columnModel.getColumn(3);
	        column.setPreferredWidth(60);
	        column.setMinWidth(60);
	        column.setResizable(false);
	        column.setHeaderRenderer(multiLineHeaderCellRenderer);
	        column.setCellRenderer(booleanCellRenderer);
	        column.setCellEditor(booleanCellEditor);
	        column = columnModel.getColumn(4);
	        column.setPreferredWidth(50);
	        column.setMinWidth(50);
	        column.setResizable(false);
	        column.setHeaderRenderer(multiLineHeaderCellRenderer);
	        column.setCellRenderer(endFieldCellRenderer);
	        column.setCellEditor(endFieldCellEditor);
	        column = columnModel.getColumn(5);
	        column.setPreferredWidth(50);
	        column.setMinWidth(50);
	        column.setResizable(false);
	        column.setHeaderRenderer(multiLineHeaderCellRenderer);
	        column.setCellRenderer(charNumberCellRenderer);
	        column.setCellEditor(charNumberCellEditor);
	        column = columnModel.getColumn(6);
	        column.setPreferredWidth(60);
	        column.setMinWidth(60);
	        column.setResizable(false);
	        column.setHeaderRenderer(multiLineHeaderCellRenderer);
	        column.setCellRenderer(booleanCellRenderer);
	        column.setCellEditor(booleanCellEditor);
	        column = columnModel.getColumn(7);
	        column.setPreferredWidth(120);
	        column.setMinWidth(120);
	        column.setResizable(false);
	        column.setHeaderRenderer(multiLineHeaderCellRenderer);
	        column.setCellRenderer(sortKindCellRenderer);
	        column.setCellEditor(sortKindCellEditor);
	        column = columnModel.getColumn(8);
	        column.setPreferredWidth(100);
	        column.setMinWidth(100);
	        column.setResizable(false);
	        column.setHeaderRenderer(multiLineHeaderCellRenderer);
	        column.setCellRenderer(ignoreKindCellRenderer);
	        column.setCellEditor(ignoreKindCellEditor);
	        column = columnModel.getColumn(9);
	        column.setPreferredWidth(80);
	        column.setMinWidth(80);
	        column.setResizable(false);
	        column.setHeaderRenderer(multiLineHeaderCellRenderer);
	        column.setCellRenderer(translateKindCellRenderer);
	        column.setCellEditor(translateKindCellEditor);
	        column = columnModel.getColumn(10);
	        column.setPreferredWidth(40);
	        column.setMinWidth(40);
	        column.setResizable(false);
	        column.setHeaderRenderer(multiLineHeaderCellRenderer);
	        column.setCellRenderer(booleanCellRenderer);
	        column.setCellEditor(booleanCellEditor);
		}
	}
	
    public SortUI() {
        setLayout(new BorderLayout());
        add(northPanel(), BorderLayout.NORTH);
    }
    
    Locale[] availableLocales() {
    	Locale[] locales = Locale.getAvailableLocales();
    	Arrays.sort(locales, new Comparator<Locale>() {
    		Collator collator = Collator.getInstance(Locale.US);

			@Override
			public int compare(Locale thisLocale, Locale thatLocale) {
				return collator.compare(thisLocale.toString(), thatLocale.toString());
			}});
    	return locales;
    }
    
    JPanel northPanel() {
        JPanel northPanel = new JPanel();
        northPanel.setLayout(new GridBagLayout());
		GridBagConstraints gbc = new GridBagConstraints();
		gbc.insets = new Insets(2, 4, 0, 4);
		gbc.fill = GridBagConstraints.HORIZONTAL;
		gbc.gridx = 0; gbc.gridy = 0; gbc.gridwidth = 1;
		JButton inputFileButton = new JButton("In file");
		northPanel.add(inputFileButton, gbc);
		gbc.gridx++; gbc.gridwidth = 5;
		gbc.weightx = 1.0;
		inputFileTextField = new JFileTextField(40);
		northPanel.add(inputFileTextField, gbc);
		gbc.weightx = 0;
		//
		gbc.gridx = 0; gbc.gridy++; gbc.gridwidth = 1;
		JButton outputFileButton = new JButton("Out file");
		northPanel.add(outputFileButton, gbc);
		gbc.gridx++; gbc.gridwidth = 5; 
		outputFileTextField = new JFileTextField(40);
		northPanel.add(outputFileTextField, gbc);
		//
		gbc.fill = GridBagConstraints.NONE;
		gbc.anchor = GridBagConstraints.EAST;
        gbc.gridx = 0; gbc.gridy++; gbc.gridwidth = 1;
        northPanel.add(new JLabel("Encoding", JLabel.RIGHT), gbc);
        gbc.gridx++;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        northPanel.add(new JLabel("Input", JLabel.RIGHT), gbc);
        gbc.gridx++;
    	SortedMap<String, Charset> charsetMap = Charset.availableCharsets();
        ArrayList<Object> inCharsetList = new ArrayList<Object>();
        inCharsetList.add(AUTOMATIC);
        inCharsetList.add(DEFAULT);
        for(Charset charset: charsetMap.values()) {
        	inCharsetList.add(charset);
        }
        inputEncodingComboBox = new JComboBox<Object>(inCharsetList.toArray());
        northPanel.add(inputEncodingComboBox, gbc);
        gbc.gridx++;
        northPanel.add(new JLabel("Output", JLabel.RIGHT), gbc);
        gbc.gridx++;
        ArrayList<Object> outCharsetList = new ArrayList<Object>();
        outCharsetList.add(AUTOMATIC);
        outCharsetList.add(DEFAULT);
        for(Charset charset: charsetMap.values()) {
        	outCharsetList.add(charset);
        }
        outputEncodingComboBox = new JComboBox<Object>(outCharsetList.toArray());
        northPanel.add( outputEncodingComboBox, gbc);
        //
        gbc.gridx = 0; gbc.gridy++;
        gbc.fill = GridBagConstraints.NONE;
        gbc.anchor = GridBagConstraints.EAST;
        northPanel.add(new JLabel("Locale", JLabel.RIGHT), gbc);
        gbc.gridx++;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        northPanel.add(new JLabel("Text", JLabel.RIGHT), gbc);
        gbc.gridx++;
        ArrayList<Object> textLocaleList = new ArrayList<Object>();
        textLocaleList.add(DEFAULT);
        textLocaleList.add(NONE);
        Locale[] availableLocales = availableLocales();
        for(Locale locale: availableLocales) {
        	textLocaleList.add(locale);
        }
        textLocaleComboBox = new JComboBox<Object>(textLocaleList.toArray());
        northPanel.add(textLocaleComboBox, gbc);
        gbc.gridx++;
        northPanel.add(new JLabel("Number", JLabel.RIGHT), gbc);
        gbc.gridx++;
        ArrayList<Object> numberLocaleList = new ArrayList<Object>();
        numberLocaleList.add(DEFAULT);
        for(Locale locale: availableLocales) {
        	numberLocaleList.add(locale);
        }
        numberLocaleComboBox = new JComboBox<Object>(numberLocaleList.toArray());
        northPanel.add(numberLocaleComboBox, gbc);
        //
        gbc.gridx = 0; gbc.gridy++; gbc.gridwidth = 1;
        northPanel.add(new JLabel("Options", JLabel.RIGHT), gbc);
        gbc.gridx++; gbc.gridwidth = 5;
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.NONE;
        JPanel innerPanel = new JPanel();
    	innerPanel.add(new JLabel("Field Separator"));
    	fieldSeparatorComboBox = new JComboBox<String>(new String[] {DEFAULT, "\\t", ":", ",", "|", " ", "\\0"});
    	fieldSeparatorComboBox.setEditable(true);
    	innerPanel.add(fieldSeparatorComboBox);
        innerPanel.add(Box.createHorizontalStrut(4));
    	stableCheckBox = new JCheckBox("Stable");
        innerPanel.add(stableCheckBox);
        uniqueCheckBox = new JCheckBox("Unique");
        innerPanel.add(uniqueCheckBox);
        innerPanel.add(Box.createHorizontalStrut(4));
        innerPanel.add(new JLabel("Header"));
    	SpinnerNumberModel spinnerNumberModel = new SpinnerNumberModel(0, 0, 20, 1);
    	headerField = new JSpinner(spinnerNumberModel);
    	innerPanel.add(headerField);
    	innerPanel.add(Box.createHorizontalStrut(4));
    	innerPanel.add(new JLabel("Buffer size"));
    	bufferSizeTextField = new JTextField(4);
    	innerPanel.add(bufferSizeTextField);
    	bufferSizeComboBox = new JComboBox<String>(new String[] {"%", "b", "K", "M", "G", " "});
    	innerPanel.add(bufferSizeComboBox);
        northPanel.add(innerPanel, gbc);
    	//
        gbc.gridx = 0; gbc.gridy++; gbc.gridwidth = 1;
        gbc.fill = GridBagConstraints.NONE;
        gbc.anchor = GridBagConstraints.EAST;
        northPanel.add(new JLabel("Keys", JLabel.RIGHT), gbc);
        gbc.gridx++; gbc.gridheight = 4; gbc.gridwidth = 5;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        sortKeyTableModel = new SortKeyTableModel();
        sortKeyTableView = new SortKeyTableView(sortKeyTableModel);
        JScrollPane scrollPane = new JScrollPane(sortKeyTableView, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        scrollPane.setMaximumSize(scrollPane.getPreferredSize());
        scrollPane.setMinimumSize(scrollPane.getPreferredSize());
        northPanel.add(scrollPane, gbc);
        //
        gbc.gridx += 5; gbc.gridheight = 1; gbc.gridwidth = 1;
        JButton addButton = new JButton("Add");
        northPanel.add(addButton, gbc);
        gbc.gridy++;
        JButton removeButton = new JButton("Remove");
        northPanel.add(removeButton, gbc);
        gbc.gridy++;
        JButton upButton = new JButton("Up");
        northPanel.add(upButton, gbc);
        gbc.gridy++; gbc.anchor = GridBagConstraints.NORTH;
        JButton downButton = new JButton("Down");
        northPanel.add(downButton, gbc);
        
        //
		gbc.gridx = 0; gbc.gridy++; gbc.gridwidth = 7;
		gbc.fill = GridBagConstraints.NONE; gbc.anchor = GridBagConstraints.CENTER;
        JPanel buttonPanel = new JPanel();
        JButton sortButton = new JButton("Sort");
        buttonPanel.add(sortButton);
        northPanel.add(buttonPanel, gbc);
        inputFileButton.addActionListener(new ActionListener() {
		    public void actionPerformed(ActionEvent e) {
		        browseInputFile();
		    }
		});

		outputFileButton.addActionListener(new ActionListener() {
		    public void actionPerformed(ActionEvent e) {
		        browseOutputFile();
		    }
		});
		
        addButton.addActionListener(new ActionListener() {
		    public void actionPerformed(ActionEvent e) {
		    	sortKeyTableView.addNewKey();
		    }
		});
        
        removeButton.addActionListener(new ActionListener() {
		    public void actionPerformed(ActionEvent e) {
		        sortKeyTableView.removeKey();
		    }
		});
        
        upButton.addActionListener(new ActionListener() {
		    public void actionPerformed(ActionEvent e) {
		        sortKeyTableView.moveUpKey();
		    }
		});
        
        downButton.addActionListener(new ActionListener() {
		    public void actionPerformed(ActionEvent e) {
		        sortKeyTableView.moveDownKey();
		    }
		});
        
       sortButton.addActionListener(new ActionListener() {
		    public void actionPerformed(ActionEvent e) {
		        sort();
		    }
		});
        return northPanel;
    }
    
	void browseInputFile() {
        File directory = null;
        String inputFileName = inputFileTextField.getText().trim();
        if(inputFileName.length() != 0) {
            File f = new File(inputFileName);
            if(f.exists()) { // && f.isDirectory()) {
                directory = f;
            } 
        }
        JFileChooser chooser = new JFileChooser(directory);
        chooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
        chooser.setDialogTitle("Choose Input file");
        int returnVal = chooser.showOpenDialog(this);
        if(returnVal == JFileChooser.APPROVE_OPTION) {
           inputFileTextField.setText(chooser.getSelectedFile().getPath().replace('\\', '/'));
        }
	}
    
	void browseOutputFile() {
        File directory = null;
        String outputFileName = outputFileTextField.getText().trim();
        if(outputFileName.length() != 0) {
            File f = new File(outputFileName);
            if(f.exists()) { // && f.isDirectory()) {
                directory = f;
            } 
        }
        JFileChooser chooser = new JFileChooser(directory);
        chooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
        chooser.setDialogType(JFileChooser.SAVE_DIALOG);
        chooser.setDialogTitle("Choose Output file");
        int returnVal = chooser.showOpenDialog(this);
        if(returnVal == JFileChooser.APPROVE_OPTION) {
           outputFileTextField.setText(chooser.getSelectedFile().getPath().replace('\\', '/'));
        }
	}

    void sort() {
        try {
        	String input = inputFileTextField.getText().trim();
        	String output = outputFileTextField.getText().trim();
            File inputFile = new File(input);
            File outputFile = new File(output);
            if(inputFile.isFile()) {
            	if(!outputFile.getCanonicalFile().equals(inputFile.getCanonicalFile())) {
            		if(!outputFile.exists() || JOptionPane.showConfirmDialog(SortUI.this, output + " exists.  Can overwrite?", Sort.TITLE, JOptionPane.OK_CANCEL_OPTION) == JOptionPane.OK_OPTION) {
            			Configuration configuration = new Configuration();
            			configuration.inputFileNames = new String[] {input};
            			configuration.outputFileName = output;
            			final Charset inputEncoding;
            			if(inputEncodingComboBox.getSelectedItem() == AUTOMATIC) {
            				inputEncoding = CharsetTeller.getCharset(inputFile);
            			} else if(inputEncodingComboBox.getSelectedItem() == DEFAULT) {
            				inputEncoding = Charset.defaultCharset();
            			} else {
            				inputEncoding = (Charset) inputEncodingComboBox.getSelectedItem();
            			}
            			configuration.inputEncoding = inputEncoding;
            			final Charset outputEncoding;
            			if(outputEncodingComboBox.getSelectedItem() == AUTOMATIC) {
            				outputEncoding = inputEncoding;
            			} else if(outputEncodingComboBox.getSelectedItem() == DEFAULT) {
            				outputEncoding = Charset.defaultCharset();
            			} else {
            				outputEncoding = (Charset) outputEncodingComboBox.getSelectedItem();
            			}
            			configuration.outputEncoding = outputEncoding;
            			final Locale textLocale;
            			if(textLocaleComboBox.getSelectedItem() == NONE) {
            				textLocale = null;
            			} else if(textLocaleComboBox.getSelectedItem() == DEFAULT) {
            				textLocale = Locale.getDefault();
            			} else {
            				textLocale = (Locale)textLocaleComboBox.getSelectedItem();
            			}
            			configuration.textLocale = textLocale;
            			final Locale numberLocale;
            			if(numberLocaleComboBox.getSelectedItem() == DEFAULT) {
            				numberLocale = Locale.getDefault();
            			} else {
            				numberLocale = (Locale)numberLocaleComboBox.getSelectedItem();
            			}
            			configuration.numberLocale = numberLocale;
            			boolean defaultFieldSeparator = false;
            			char fieldSeparator = 0;
            			String fs = (String)fieldSeparatorComboBox.getSelectedItem();
            			if(fs == null || fs == DEFAULT || fs.length() == 0) {
            				defaultFieldSeparator = true;
            			} else if(fs.length() == 1) {
            				fieldSeparator = fs.charAt(0);
            			} else if(fs.length() == 2 && (fs.startsWith("/") || fs.startsWith("\\"))) {
            				char c = fs.charAt(1);
            				switch(c) {
            				case 't': c = '\t'; break;
            				case 'r': c = '\r'; break;
            				case 'n': c = '\n'; break;
            				case 'f': c = '\f'; break;
            				case '0': c = '\0'; break;
            				}
            				fieldSeparator = c;
            			} else {
            				throw new IllegalAccessError("Invalid field separator: " + fs);
            			}
            			configuration.defaultFieldSeparator = defaultFieldSeparator;
            			configuration.fieldSeparator = fieldSeparator;
            			configuration.stable = stableCheckBox.isSelected();
            			configuration.unique = uniqueCheckBox.isSelected();
            			configuration.headerLines = (Integer)headerField.getValue();
            			configuration.headerEveryFile = true;
            			String bufferSize = bufferSizeTextField.getText().trim();
            			if(bufferSize.length() > 0) {
            				configuration.bufferSize = Integer.valueOf(bufferSize);
            				configuration.bufferSizeSuffix = (String)bufferSizeComboBox.getSelectedItem();
            			}
            			Vector<Key> keyList = sortKeyTableModel.keyList;
            			KeyField[] keyFields = new KeyField[keyList.size()];
            			for(int i = 0; i < keyFields.length; i++) {
            				KeyField keyField = keyFields[i] = new KeyField();
            				Key key = keyList.get(i);
            				if(key.startFieldNumber == 0) {
            					keyField.startField = 1;
            					keyField.startChar = 1;
            				} else {
            					keyField.startField = key.startFieldNumber;
            					keyField.startChar = key.startCharNumber;
            				}
            				keyField.skipStartBlanks = key.startSkipBlanks;
            				if(key.endFieldNumber == 0 || key.endFieldNumber == Integer.MAX_VALUE) {
            					keyField.endField = Integer.MAX_VALUE;
            					keyField.endChar = 0;
            				} else {
            					keyField.endField = key.endFieldNumber;
            					keyField.endChar = key.endCharNumber;
            				}
            				keyField.skipEndBlanks = key.endSkipBlanks;
            				keyField.sortKind = key.sortKind;
            				if(key.ignoreKind != null) {
            					keyField.ignore = true;
            					keyField.ignoreKind = key.ignoreKind;
            				}
            				if(key.translateKind != null) {
            					keyField.translate = true;
            					keyField.translateKind = key.translateKind;
            				}
            				keyField.reverse = key.reverse;
            			}
            			configuration.keyFields = keyFields;
            			//configuration.debug = true;
            			//configuration.print(new IndentedReporter(System.out));
            			Sort.sort(configuration);
            		}
            	} else {
            		JOptionPane.showMessageDialog(SortUI.this, "Input and Output are the same.", Sort.TITLE, JOptionPane.ERROR_MESSAGE);
            	}
            } else {
            	if(inputFile.exists()) {
            		JOptionPane.showMessageDialog(SortUI.this, input + " is not a file.", Sort.TITLE, JOptionPane.ERROR_MESSAGE);
            	} else {
            		JOptionPane.showMessageDialog(SortUI.this, (input.equals("")? "Input": input) + " doesn't exist.", Sort.TITLE, JOptionPane.ERROR_MESSAGE);
            	}
            }
        } catch(Throwable e) {
        	ErrorMessageDialog.showMessageDialog(this, e, Sort.TITLE);
        }
    }
     
	public static void main(String args[]) {
	    JFrame f = new JFrame();
	    f.getContentPane().add(new SortUI());
	    f.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        f.setTitle(Sort.TITLE);
        f.pack();
        f.setVisible(true);
    }
}
