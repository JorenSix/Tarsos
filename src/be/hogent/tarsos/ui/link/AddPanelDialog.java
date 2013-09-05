package be.hogent.tarsos.ui.link;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;

import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
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
//	private Color bgColor;

	private JComboBox xUnitsList;
	private JComboBox yUnitsList;
//	private JComboBox<Color> bgColorList;

	private JButton createButton = null;
	private JButton cancelButton = null;
	private boolean answer = false;

	public boolean getAnswer() {
		return answer;
	}

	public AddPanelDialog(JFrame frame, boolean setModel, String myMessage) {
		super(frame, myMessage, setModel);
		this.setLayout(new BoxLayout(this.getContentPane(), BoxLayout.Y_AXIS));
		
		initialise();
		pack();
		setLocationRelativeTo(frame);
		setVisible(true);
	}

	public void initialise() {
		JPanel row1Panel = new JPanel();
		JPanel row2Panel = new JPanel();
		JPanel row3Panel = new JPanel();
		
		Dimension dropDownDimension = new Dimension(130,20);
		Dimension buttonDimension = new Dimension(75,20);
		
		xUnitsList = new JComboBox(AXIS_X);
		xUnitsList.setEnabled(false);
		xUnitsList.addItemListener(this);
		xUnitsList.setPreferredSize(dropDownDimension);
		row1Panel.add(new JLabel("X Axis: "));
		row1Panel.add(xUnitsList);
		
		yUnitsList = new JComboBox(AXIS_Y);
		yUnitsList.addItemListener(this);
		yUnitsList.setPreferredSize(dropDownDimension);
		row2Panel.add(new JLabel("Y Axis: "));
		row2Panel.add(yUnitsList);
		
		createButton = new JButton("Create");
		createButton.addActionListener(this);
		createButton.setPreferredSize(buttonDimension);
		cancelButton = new JButton("Cancel");
		cancelButton.addActionListener(this);
		cancelButton.setPreferredSize(buttonDimension);
		row3Panel.add(createButton);
		row3Panel.add(cancelButton);
		
		xUnits = Units.TIME;
		yUnits = Units.AMPLITUDE;

		this.getContentPane().add(row1Panel);
		this.getContentPane().add(row2Panel);
		this.getContentPane().add(row3Panel);

	}

	public void itemStateChanged(ItemEvent arg0) {
		if (arg0.getSource().equals(xUnitsList)) {
			if (arg0.getItemSelectable().getSelectedObjects()[0].toString().equals(AXIS_TIME)) {
				xUnits = Units.TIME;
			} else {
				xUnits = null;
			}
		} else if (arg0.getSource().equals(yUnitsList)) {
			if (arg0.getItemSelectable().getSelectedObjects()[0].toString().equals(AXIS_CENT)) {
				yUnits = Units.FREQUENCY;
			} else if (arg0.getItemSelectable().getSelectedObjects()[0].toString().equals(AXIS_AMPL)) {
				yUnits = Units.AMPLITUDE;
			} else  if (arg0.getItemSelectable().getSelectedObjects()[0].toString().equals(AXIS_NONE)) {
				yUnits = Units.NONE;
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