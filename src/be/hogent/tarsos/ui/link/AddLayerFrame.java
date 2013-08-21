package be.hogent.tarsos.ui.link;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;

import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;

public class AddLayerFrame extends JFrame implements ItemListener {

	/**
	 * 
	 */
	private static final long serialVersionUID = 4659122917576332161L;
	
	private final String TYPE_BASIC_LAYER = "Basic layer";
	private final String TYPE_FEATURE_LAYER = "Feature layer";
	
	private final String FEATURE_MFCC = "MFCC";
	private final String FEATURE_CQT = "CQT";
	private final String FEATURE_PITCH = "Pitch contour";
	private final String FEATURE_AC = "Autocorrelation";
	
	private Color color;
	
	private JComboBox typeList;
	private JComboBox featureTypeList;
	private JComboBox colorList;

	
	public AddLayerFrame(){
		super();
		this.setLayout(new FlowLayout());
		String[] types = { this.TYPE_BASIC_LAYER, this.TYPE_FEATURE_LAYER};
		typeList = new JComboBox(types);
		typeList.addItemListener(this);
		
		String[] featureItems = { this.FEATURE_MFCC, this.FEATURE_CQT, this.FEATURE_PITCH, this.FEATURE_AC };
		featureTypeList = new JComboBox(featureItems);
		featureTypeList.setEnabled(false);
		
		String[] kleuren = { "Grijs", "Rood", "Blauw", "Groen", "Geel"};
		colorList = new JComboBox(kleuren);
		
		JLabel lblTypeLayer = new JLabel("Type layer: ");
		JLabel lblTypeFeature = new JLabel("Type feature: ");
		JLabel lblColor = new JLabel("Kleur: ");
		JLabel lblFrameSize = new JLabel("Framesize: ");
		JLabel lblOverlapping = new JLabel("Overlapping: ");	
		this.add(lblTypeLayer);
		this.add(typeList);
		this.add(lblTypeFeature);
		this.add(featureTypeList);
		this.add(lblColor);
		this.add(colorList);
		
		
		this.setResizable(false);
		this.setMinimumSize(new Dimension(300,150));
		this.setVisible(true);
	}


//	public void itemStateChanged(ItemEvent e) {
//		
//		
//	}

//	@Override
	public void itemStateChanged(ItemEvent arg0) {
		if (typeList.getSelectedItem().equals(this.TYPE_FEATURE_LAYER)){
			this.featureTypeList.setEnabled(true);
		} else {
			this.featureTypeList.setEnabled(false);
		}
		
	}
	
}
