package be.hogent.tarsos.ui.link;

import java.awt.Color;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JPanel;

import be.hogent.tarsos.ui.link.coordinatessystems.Units;

public class AddPanelDialog extends JDialog implements ItemListener,
		ActionListener {

	// TODO:
	// X_Axis
	// Y_Axis
	// BGColor

	private final String AXIS_TIME = "Time[s]";
	private final String AXIS_CENT = "Frequency[cents]";
	private final String AXIS_AMPL = "Amplitude";
	private final String AXIS_NONE = "None";
	private final String[] AXIS_X = {AXIS_TIME};
	private final String[] AXIS_Y = {AXIS_AMPL, AXIS_CENT, AXIS_NONE};

	private Units xUnits;
	private Units yUnits;
	private Color bgColor;

	private JComboBox xUnitsList;
	private JComboBox yUnitsList;
	private JComboBox bgColorList;

	private JButton createButton = null;
	private JButton cancelButton = null;
	private boolean answer = false;

	public boolean getAnswer() {
		return answer;
	}

	public AddPanelDialog(JFrame frame, boolean setModel, String myMessage) {
		super(frame, myMessage, setModel);
		initialise();
		pack();
		setLocationRelativeTo(frame);
		setVisible(true);
	}

	public void initialise() {
		JPanel contentPanel = new JPanel();
		xUnitsList = new JComboBox(AXIS_X);
		yUnitsList = new JComboBox(AXIS_Y);
		xUnitsList.addItemListener(this);
		yUnitsList.addItemListener(this);
		contentPanel.add(xUnitsList);
		contentPanel.add(yUnitsList);
		createButton = new JButton("Create");
		createButton.addActionListener(this);
		contentPanel.add(createButton);
		cancelButton = new JButton("Cancel");
		cancelButton.addActionListener(this);
		contentPanel.add(cancelButton);
		
		xUnits = Units.TIME_SSS;
		yUnits = Units.FREQUENCY_CENTS;

		this.getContentPane().add(contentPanel);

	}

	public void itemStateChanged(ItemEvent arg0) {
		if (arg0.getSource().equals(xUnitsList)) {
			if (arg0.getItemSelectable().getSelectedObjects()[0].toString().equals(AXIS_TIME)) {
				xUnits = Units.TIME_SSS;
			} else {
				xUnits = null;
			}
		} else if (arg0.getSource().equals(yUnitsList)) {
			if (arg0.getItemSelectable().getSelectedObjects()[0].toString().equals(AXIS_CENT)) {
				yUnits = Units.FREQUENCY_CENTS;
			} else if (arg0.getItemSelectable().getSelectedObjects()[0].toString().equals(AXIS_AMPL)) {
				yUnits = Units.AMPLITUDE;
			} else {
				yUnits = null;
			}
		}
	}
	
	

	public void actionPerformed(ActionEvent e) {
		if (createButton == e.getSource()) {
			answer = true;
			this.setVisible(false);
		} else if (cancelButton == e.getSource()) {
			answer = false;
			this.setVisible(false);
		}
	}
	
	public Units getXUnits(){
		return xUnits;
	}
	
	public Units getYUnits(){
		return yUnits;
	}
}