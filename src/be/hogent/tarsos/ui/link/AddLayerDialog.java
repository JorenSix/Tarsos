package be.hogent.tarsos.ui.link;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;

import be.hogent.tarsos.tarsossegmenter.model.AASModel;
import be.hogent.tarsos.ui.link.layers.featurelayers.ConstantQLayer;
import be.hogent.tarsos.ui.link.layers.featurelayers.FFTLayer;
import be.hogent.tarsos.ui.link.layers.featurelayers.FeatureLayer;
import be.hogent.tarsos.ui.link.layers.featurelayers.PitchContourLayer;
import be.hogent.tarsos.ui.link.layers.featurelayers.WaveFormLayer;
import be.hogent.tarsos.ui.link.layers.segmentationlayers.SegmentationLayer;

public class AddLayerDialog extends JDialog implements ItemListener, ActionListener {

	/**
	 * 
	 */
	private static final long serialVersionUID = 4659122917576332161L;

	private final String LAYER_FEATURE_FFT = "FFT";
	
	private final String LAYER_FEATURE_WAVEFORM = "WaveForm";
//	private final String LAYER_FEATURE_MFCC = "MFCC";
	private final String LAYER_FEATURE_CQT = "CQT";
	private final String LAYER_FEATURE_PITCH = "Pitch contour";
	private final String LAYER_SEGMENTATION = "Segmentation";
	
	private Integer[] frameSizes = {512, 1024, 2048, 4096, 8192, 16384, 32768, 65536};
	private Float[] overlapPercent = {0.0f, 0.1f, 0.2f, 0.3f, 0.4f, 0.5f, 0.6f, 0.7f, 0.8f, 0.9f};
	
	private Color color;
	
	private JComboBox featureTypeList;
	private JComboBox frameSizeList;
	private JComboBox overlapList;
	private JComboBox colorList;
	
	private JButton createButton;
	private JButton cancelButton;
	
	private String featureType;
	private int frameSize;
	private float overlap;
	
	private LinkedPanel parent;

	private boolean answer = false;

	public boolean getAnswer() {
		return answer;
	}
	
	public AddLayerDialog(JFrame frame, LinkedPanel parent, boolean setModel, String myMessage) {
		super(frame, myMessage, setModel);
		this.parent = parent;
		this.setLayout(new BoxLayout(this.getContentPane(), BoxLayout.Y_AXIS));
		initialise();
		
		
		this.setMinimumSize(new Dimension(270,170));
		this.setMaximumSize(new Dimension(270,170));
//		this.setResizable(false);
//		this.setVisible(true);
		setLocationRelativeTo(frame);
		setVisible(true);
	}
	
	public void initialise(){
		
		Dimension dropDownDimension = new Dimension(110,20);
		Dimension buttonDimension = new Dimension(75,20);
		Dimension labelDimension = new Dimension(80,20);
		
		JPanel row1Panel = new JPanel();
		JPanel row2Panel = new JPanel();
		JPanel row3Panel = new JPanel();
		JPanel row4Panel = new JPanel();
		
		String[] featureItems = { this.LAYER_FEATURE_WAVEFORM, this.LAYER_FEATURE_CQT, this.LAYER_FEATURE_PITCH, this.LAYER_SEGMENTATION, this.LAYER_FEATURE_FFT };
		featureTypeList = new JComboBox(featureItems);
		featureTypeList.addItemListener(this);
		featureTypeList.setPreferredSize(dropDownDimension);
		
		
		String[] kleuren = { "Grijs", "Rood", "Blauw", "Groen", "Geel"};
		colorList = new JComboBox(kleuren);
		colorList.setPreferredSize(dropDownDimension);
		
		frameSizeList = new JComboBox(frameSizes);
		frameSizeList.addItemListener(this);
		frameSizeList.setPreferredSize(dropDownDimension);
		
		overlapList = new JComboBox(overlapPercent);
		overlapList.addItemListener(this);
		overlapList.setPreferredSize(dropDownDimension);
		
		
		frameSizeList.setSelectedIndex(5);
		overlapList.setSelectedIndex(5);
		featureTypeList.setSelectedIndex(0);
		
		frameSize = frameSizes[5];
		overlap = overlapPercent[5];
		featureType = LAYER_FEATURE_WAVEFORM;
		frameSizeList.setEnabled(false);
		overlapList.setEnabled(false);
		
		
		JLabel lblTypeFeature = new JLabel("Type feature: ", SwingConstants.RIGHT);
		lblTypeFeature.setPreferredSize(labelDimension);
//		lblTypeFeature.setAlignmentX(RIGHT_ALIGNMENT);
		
		JLabel lblColor = new JLabel("Kleur: ", SwingConstants.RIGHT);
		lblColor.setPreferredSize(labelDimension);
//		lblColor.setAlignmentX(RIGHT_ALIGNMENT);
		
		JLabel lblFrameSize = new JLabel("Framesize: ", SwingConstants.RIGHT);
		lblFrameSize.setPreferredSize(labelDimension);
//		lblFrameSize.setAlignmentX(RIGHT_ALIGNMENT);
		
		JLabel lblOverlapping = new JLabel("Overlapping: ", SwingConstants.RIGHT);
		lblOverlapping.setPreferredSize(labelDimension);
//		lblOverlapping.setAlignmentX(RIGHT_ALIGNMENT);

		createButton = new JButton("Create");
		createButton.addActionListener(this);
		createButton.setPreferredSize(buttonDimension);
		cancelButton = new JButton("Cancel");
		cancelButton.addActionListener(this);
		cancelButton.setPreferredSize(buttonDimension);
		
		row1Panel.add(Box.createHorizontalGlue());
		row1Panel.add(lblTypeFeature);
		row1Panel.add(featureTypeList);
		
		row2Panel.add(Box.createHorizontalGlue());
		row2Panel.add(lblFrameSize);
		row2Panel.add(frameSizeList);
		
		row3Panel.add(Box.createHorizontalGlue());
		row3Panel.add(lblOverlapping);
		row3Panel.add(overlapList);
		
		row4Panel.add(createButton);
		row4Panel.add(cancelButton);

		this.getContentPane().add(row1Panel);
		this.getContentPane().add(row2Panel);
		this.getContentPane().add(row3Panel);
		this.getContentPane().add(row4Panel);
	}

//	@Override
	public void itemStateChanged(ItemEvent arg0) {
		if (arg0.getSource().equals(featureTypeList)) {
			featureType = arg0.getItemSelectable().getSelectedObjects()[0].toString();
			if (featureType.equals(this.LAYER_FEATURE_WAVEFORM) || featureType.equals(this.LAYER_SEGMENTATION)){
				this.frameSizeList.setEnabled(false);
				this.overlapList.setEnabled(false);
			} else {
				this.frameSizeList.setEnabled(true);
				this.overlapList.setEnabled(true);
			}
		} else if (arg0.getSource().equals(frameSizeList)) {
			frameSize = (Integer)arg0.getItemSelectable().getSelectedObjects()[0];
		} else if (arg0.getSource().equals(overlapList)){
			overlap = (Float)arg0.getItemSelectable().getSelectedObjects()[0];
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
	
	public FeatureLayer getLayer(){
		if (featureType == LAYER_FEATURE_CQT){
			return new ConstantQLayer(parent, frameSize, (int)(overlap*frameSize));
		} else if (featureType == LAYER_FEATURE_FFT){
				return new FFTLayer(parent, frameSize, (int)(overlap*frameSize));
		} else if (featureType == LAYER_FEATURE_PITCH){
			return new PitchContourLayer(parent, frameSize, (int)(overlap*frameSize));
		} else if (featureType == LAYER_FEATURE_WAVEFORM){
			return new WaveFormLayer(parent);
		} else if (featureType == LAYER_SEGMENTATION){
			return new SegmentationLayer(parent, AASModel.MACRO_LEVEL, 100, 4000);
		}
		return null;
	}
	
}
