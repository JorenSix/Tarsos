package be.tarsos.ui.link;

import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.ArrayList;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;

import be.tarsos.ui.link.layers.Layer;
import be.tarsos.ui.link.layers.LayerBuilder;
import be.tarsos.ui.link.layers.LayerProperty;
import be.tarsos.ui.link.layers.LayerType;

public class AddLayerDialog extends JDialog implements ItemListener,
		ActionListener {

	private static final long serialVersionUID = 4659122917576332161L;

	private JComboBox layerTypeList;

	private JButton createButton;
	private JButton cancelButton;

	private LayerType layerType;

	private LinkedPanel parent;

	private JPanel layerTypePanel;
	private JPanel buttonPanel;

	private boolean answer = false;

	private final int MINHEIGHT = 100;
	private final int STEPHEIGHT = 30;

	private int height;

	private ArrayList<LayerProperty> properties;

	public boolean getAnswer() {
		return answer;
	}

	public AddLayerDialog(JFrame frame, LinkedPanel parent, boolean setModel,
			String myMessage) {
		super(frame, myMessage, setModel);
		this.parent = parent;
		this.setLayout(new BoxLayout(this.getContentPane(), BoxLayout.Y_AXIS));
		initialise();

		// this.setMinimumSize(new Dimension(270,MINHEIGHT));
		// this.setMaximumSize(new Dimension(270,MINHEIGHT));
		this.setResizable(false);
		setLocationRelativeTo(frame);
		setVisible(true);
	}

	public void initialise() {

		Dimension buttonDimension = new Dimension(75, 20);
		Dimension labelDimension = new Dimension(80, 20);
		Dimension dropDownDimension = new Dimension(110, 20);

		layerTypePanel = new JPanel();
		LayerType[] layerTypes = LayerType.getLayerTypes(parent
				.getCoordinateSystem());
		layerTypeList = new JComboBox(layerTypes);
		layerTypeList.addItemListener(this);
		layerTypeList.setPreferredSize(dropDownDimension);
		JLabel lblTypeLayer = new JLabel("Type layer: ", SwingConstants.RIGHT);
		lblTypeLayer.setPreferredSize(labelDimension);
		layerTypePanel.add(Box.createHorizontalGlue());
		layerTypePanel.add(lblTypeLayer);
		layerTypePanel.add(layerTypeList);
		layerType = (LayerType)layerTypeList.getItemAt(0);
		buttonPanel = new JPanel();
		createButton = new JButton("Create");
		createButton.addActionListener(this);
		createButton.setPreferredSize(buttonDimension);
		cancelButton = new JButton("Cancel");
		cancelButton.addActionListener(this);
		cancelButton.setPreferredSize(buttonDimension);
		buttonPanel.add(createButton);
		buttonPanel.add(cancelButton);
		this.height = MINHEIGHT;
		doCustomLayout();

	}

	private void doCustomLayout() {
		this.getContentPane().removeAll();
		this.getContentPane().add(layerTypePanel);
		LayerType lt = (LayerType) this.layerTypeList.getSelectedItem();
		properties = LayerType.getProperties(lt);
		this.height = MINHEIGHT;
		for (LayerProperty<?> lp : properties) {
			this.getContentPane().add(lp.getGUI());
			this.height += STEPHEIGHT;
		}
		this.getContentPane().add(buttonPanel);
		this.setMinimumSize(new Dimension(270, height));
		this.setMaximumSize(new Dimension(270, height));
		this.setPreferredSize(new Dimension(270, height));
		this.setSize(new Dimension(270, height));
//		this.revalidate();
		this.repaint();
	}

	// @Override
	public void itemStateChanged(ItemEvent arg0) {
		if (arg0.getSource().equals(layerTypeList)) {
			layerType = (LayerType) arg0.getItemSelectable()
					.getSelectedObjects()[0];
			doCustomLayout();
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

	public Layer getLayer() {
		return LayerBuilder.buildLayer(parent, layerType, properties);
	}
}
