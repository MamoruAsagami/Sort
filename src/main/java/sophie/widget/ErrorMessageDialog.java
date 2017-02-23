package sophie.widget;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;

import sophie.util.ThrowableUtilities;

public class ErrorMessageDialog {
	private static String SHOW_DETAILS = "Show details";
	private static String HIDE_DETAILS = "Hide details";
	private JButton showHideButton;
	private JScrollPane scrollPane;
	private JOptionPane optionPane;
	private JDialog dialog;
	
	
	private ErrorMessageDialog(Component parentComponent, Throwable e, String title, int messageType) {
    	JPanel panel = new JPanel();
    	panel.setLayout(new BorderLayout());
    	panel.add(new JLabel(e.toString()), BorderLayout.NORTH);
    	scrollPane = new JScrollPane(new JTextArea(ThrowableUtilities.toStackTraceString(e)));
    	scrollPane.setPreferredSize(new Dimension(640, 200));
    	panel.add(scrollPane);
    	scrollPane.setVisible(false);
    	showHideButton = new JButton(SHOW_DETAILS);
    	String OK = "OK";
    	optionPane = new JOptionPane(panel, messageType, JOptionPane.DEFAULT_OPTION, null, new Object[] {OK, showHideButton}, OK);
    	//optionPane = new JOptionPane(panel, JOptionPane.DEFAULT_OPTION, messageType);
    	
    	dialog = optionPane.createDialog(parentComponent, title);
    	showHideButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				showHidePressed();
			}});
	}
	
	void showHidePressed() {
		if(scrollPane.isVisible()) {
			scrollPane.setVisible(false);
			showHideButton.setText(SHOW_DETAILS);
		} else {
			scrollPane.setVisible(true);
			showHideButton.setText(HIDE_DETAILS);
		}
		dialog.pack();
	}
	
	private void show() {
		dialog.setVisible(true);
		// Object option = optionPane.getValue();
	}
	
	public static void showMessageDialog(Component parentComponent, Throwable e, String title, int messageType) {
		ErrorMessageDialog dialog = new ErrorMessageDialog(parentComponent, e, title, messageType);
		dialog.show();
	}

	public static void showMessageDialog(Component parentComponent, Throwable e, String title) {
		showMessageDialog(parentComponent, e, title, JOptionPane.ERROR_MESSAGE);
	}
}
