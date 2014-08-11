package be.tarsos.ui.link;

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

import be.tarsos.ui.link.coordinatessystems.Quantity;

public class AddPanelDialog extends JDialog implements ItemListener,
		ActionListener {

	private static final long serialVersionUID = 1L;
	
	private final Quantity[] AXIS_X = {Quantity.TIME};
	private final Quantity[] AXIS_Y = {Quantity.FREQUENCY, Quantity.AMPLITUDE, Quantity.NONE};
	
	private Quantity xQuantity;
	private Quantity yQuantity;

	private JComboBox xQuantityList;
	private JComboBox yQuantityList;

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
		
		xQuantityList = new JComboBox(AXIS_X);
		xQuantityList.setEnabled(false);
		xQuantityList.addItemListener(this);
		xQuantityList.setPreferredSize(dropDownDimension);
		row1Panel.add(new JLabel("X Axis: "));
		row1Panel.add(xQuantityList);
		
		yQuantityList = new JComboBox(AXIS_Y);
		yQuantityList.addItemListener(this);
		yQuantityList.setPreferredSize(dropDownDimension);
		row2Panel.add(new JLabel("Y Axis: "));
		row2Panel.add(yQuantityList);
		
		createButton = new JButton("Create");
		createButton.addActionListener(this);
		createButton.setPreferredSize(buttonDimension);
		cancelButton = new JButton("Cancel");
		cancelButton.addActionListener(this);
		cancelButton.setPreferredSize(buttonDimension);
		row3Panel.add(createButton);
		row3Panel.add(cancelButton);
		
		xQuantity = Quantity.TIME;
		yQuantity = Quantity.FREQUENCY;

		this.getContentPane().add(row1Panel);
		this.getContentPane().add(row2Panel);
		this.getContentPane().add(row3Panel);

	}

	public void itemStateChanged(ItemEvent arg0) {
		if (arg0.getSource().equals(xQuantityList)) {
			xQuantity = (Quantity) arg0.getItemSelectable().getSelectedObjects()[0];
		} else if (arg0.getSource().equals(yQuantityList)) {
			yQuantity = (Quantity) arg0.getItemSelectable().getSelectedObjects()[0];
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
	
	public Quantity getXUnits(){
		return xQuantity;
	}
	
	public Quantity getYUnits(){
		return yQuantity;
	}
}