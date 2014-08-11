package be.tarsos.ui.link.layers;

import java.awt.Dimension;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.List;

import javax.swing.Box;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;

public class LayerProperty<T> implements ItemListener {
	
	private String propertyName;
	private List<T> values;
	private T selectedValue;
	private JComboBox cb;
	
	private final Dimension dropDownDimension = new Dimension(110,20);
	private final Dimension labelDimension = new Dimension(80,20);
	
	public LayerProperty(String propertyName, List<T> values){
		this.propertyName = propertyName;
		this.values = values;
	}
	
	public LayerProperty(String propertyName, T value){
		this.propertyName = propertyName;
		this.selectedValue = value;
	}
	
	public JPanel getGUI(){
		JPanel p = new JPanel();
		JLabel label = new JLabel(propertyName, SwingConstants.RIGHT);
		label.setPreferredSize(labelDimension);
		cb = new JComboBox((T[])values.toArray());
		cb.setPreferredSize(dropDownDimension);
		cb.addItemListener(this);
		this.selectedValue = values.get(0);
		p.add(Box.createHorizontalGlue());
		p.add(label);
		p.add(cb);
		return p;
	}
	
	public T getSelectedValue(){
		return selectedValue;
	}

	public String getPropertyName() {
		return propertyName;
	}

	public void itemStateChanged(ItemEvent e) {
		selectedValue = values.get(cb.getSelectedIndex());
	}
}
