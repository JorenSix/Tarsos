package be.tarsos.ui.pitch;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.io.File;
import java.util.HashMap;

import javax.swing.JPanel;
import javax.swing.JSplitPane;

import be.tarsos.dsp.ui.AxisUnit;
import be.tarsos.dsp.ui.CoordinateSystem;
import be.tarsos.dsp.ui.LinkedPanel;
import be.tarsos.dsp.ui.ViewPort;
import be.tarsos.dsp.ui.ViewPort.ViewPortChangedListener;
import be.tarsos.dsp.ui.layers.AmplitudeAxisLayer;
import be.tarsos.dsp.ui.layers.BackgroundLayer;
import be.tarsos.dsp.ui.layers.BeatLayer;
import be.tarsos.dsp.ui.layers.LegendLayer;
import be.tarsos.dsp.ui.layers.PitchContourLayer;
import be.tarsos.dsp.ui.layers.SelectionLayer;
import be.tarsos.dsp.ui.layers.TimeAxisLayer;
import be.tarsos.dsp.ui.layers.VerticalFrequencyAxisLayer;
import be.tarsos.dsp.ui.layers.WaveFormLayer;
import be.tarsos.sampled.pitch.Annotation;
import be.tarsos.sampled.pitch.AnnotationListener;
import be.tarsos.util.AudioFile;

public class LinkedFeaturePanel extends JPanel implements ScaleChangedListener, AudioFileChangedListener, AnnotationListener, ViewPortChangedListener {
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 8176344804704683193L;
	
	JSplitPane split;
	private static HashMap<String, LinkedPanel> panels;

	private boolean drawing = false;
	
	public LinkedFeaturePanel(){
		this.setLayout(new BorderLayout());
		panels = new HashMap<String, LinkedPanel>();
		split = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
		split.setPreferredSize(new Dimension(500,500));
		this.add(split,BorderLayout.CENTER);
	}

	private CoordinateSystem getCoordinateSystem(AxisUnit yUnits) {
		float minValue = -1000;
		float maxValue = 1000;
		if(yUnits == AxisUnit.FREQUENCY){
			minValue = 200;
			maxValue = 8000;
		}
		return new CoordinateSystem(yUnits, minValue, maxValue);
	}	

	@Override
	public void audioFileChanged(AudioFile newAudioFile) {
		CoordinateSystem cs = getCoordinateSystem(AxisUnit.AMPLITUDE);
		be.tarsos.dsp.ui.LinkedPanel panel = new be.tarsos.dsp.ui.LinkedPanel(cs);
		panel.addLayer(new BackgroundLayer(cs));
		panel.addLayer(new AmplitudeAxisLayer(cs));
		panel.addLayer(new WaveFormLayer(cs, new File(newAudioFile.transcodedPath())));
		panel.addLayer(new BeatLayer(cs,new File(newAudioFile.transcodedPath()),true,true));
		panel.addLayer(new TimeAxisLayer(cs));
		panel.addLayer(new SelectionLayer(cs));
		LegendLayer legend = new LegendLayer(cs,50);
		panel.addLayer(legend);
		legend.addEntry("Onsets",Color.BLUE);
		legend.addEntry("Beats", Color.RED);
		
		panel.getViewPort().addViewPortChangedListener(this);
		
		panels.put("Waveform", panel);

		this.split.add(panel, JSplitPane.TOP);
		
		cs = getCoordinateSystem(AxisUnit.FREQUENCY);
		panel = new LinkedPanel(cs);
		panel.addLayer(new BackgroundLayer(cs));
		//panel.addLayer(new ConstantQLayer(cs,new File(newAudioFile.transcodedPath()),2048,3600,10800,12));
	
		panel.addLayer(new PitchContourLayer(cs,new File(newAudioFile.transcodedPath()),Color.red,2048,1024));
		panel.addLayer(new SelectionLayer(cs));
		panel.addLayer(new VerticalFrequencyAxisLayer(cs));
		panel.addLayer(new TimeAxisLayer(cs));
		panel.getViewPort().addViewPortChangedListener(this);
		
		panels.put("Spectral info", panel);
		
		this.split.add(panel, JSplitPane.BOTTOM);
	}

	@Override
	public void scaleChanged(double[] newScale, boolean isChanging,
			boolean shiftHisto) {
		// TODO Auto-generated method stub
		
	}

	public void viewPortChanged(ViewPort newViewPort) {
		if (!drawing) {
			drawing = true;
			for (LinkedPanel panel : panels.values()) {
				panel.repaint();
			}
			drawing = false;
		}
	}
	
	@Override
	public void addAnnotation(Annotation annotation) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void clearAnnotations() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void annotationsAdded() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void extractionStarted() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void extractionFinished() {
		// TODO Auto-generated method stub
	}

}
