package be.tarsos;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.GridLayout;
import java.io.File;
import java.util.HashMap;
import java.util.List;

import javax.swing.JFrame;
import javax.swing.JPanel;

import be.tarsos.dsp.ui.Axis;
import be.tarsos.dsp.ui.AxisUnit;
import be.tarsos.dsp.ui.CoordinateSystem;
import be.tarsos.dsp.ui.LinkedPanel;
import be.tarsos.dsp.ui.ViewPort;
import be.tarsos.dsp.ui.ViewPort.ViewPortChangedListener;
import be.tarsos.dsp.ui.layers.BackgroundLayer;
import be.tarsos.dsp.ui.layers.DragMouseListenerLayer;
import be.tarsos.dsp.ui.layers.SelectionLayer;
import be.tarsos.dsp.ui.layers.ZoomMouseListenerLayer;
import be.tarsos.dsp.ui.layers.pch.ScaleLayer;
import be.tarsos.sampled.pitch.Annotation;
import be.tarsos.sampled.pitch.PitchUnit;
import be.tarsos.util.FileDrop;
import be.tarsos.util.FileUtils;


public class VisFrame extends JFrame  {
		
	LinkedPanel heatMapsPanel;
	JPanel pchPanel;
	int index = 0;
	public VisFrame(){
		this.setLayout(new BorderLayout());
		this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		this.setTitle("NFFT Fingerprint Visualizer");
		
		pchPanel = new JPanel();
		pchPanel.setLayout(new GridLayout(0,1));
		pchPanel.add(emptyFeaturePanel());
				
		new FileDrop(null, pchPanel, /*dragBorder,*/ new FileDrop.Listener(){   
			public void filesDropped( java.io.File[] files ){   
				for( int i = 0; i < files.length; i++) {   
					final File fileToAdd = files[i];
					new Thread(new Runnable(){
						@Override
						public void run() {					
		                	addFile(fileToAdd.getAbsolutePath());
						}

						}).start();
	            }
			}
	    });
		
		this.add(pchPanel,BorderLayout.CENTER);
	}
	
	private void addFile(String absolutePath) {
		List<Annotation> annotations = FileUtils.readPitchAnnotations(absolutePath);
		HashMap<Integer,Double> heatmapRow = new HashMap<Integer,Double>();
		for(int i = 0 ; i < 60; i++){
			heatmapRow.put(i, 0.0);
		}
		for(Annotation annotation : annotations){
			int value = (int) Math.round(annotation.getPitch(PitchUnit.RELATIVE_CENTS)/20) % 20;
			heatmapRow.put(value, 1+ heatmapRow.get(value));
		}
		
		double maxValue = - 10;
		for(int i = 0 ; i < 60; i++){
			double value = heatmapRow.get(i) / annotations.size();
			maxValue = Math.max(value,maxValue);
			heatmapRow.put(i,value);
		}
		
		for(int i = 0 ; i < 60; i++){
			double value = heatmapRow.get(i) / maxValue;
			heatmapRow.put(i,value*value);
		}
		
		heatMapsPanel.addLayer(new HeatMapLayer(index, heatmapRow));
		index++;
	}
	
	private Component emptyFeaturePanel(){
		final CoordinateSystem cs = new CoordinateSystem(AxisUnit.FREQUENCY, 0, -2000);
		cs.setMax(Axis.X, 1200);
		cs.setMin(Axis.X, 0);
		heatMapsPanel = new LinkedPanel(cs);
		heatMapsPanel.getViewPort().addViewPortChangedListener(new ViewPortChangedListener() {
			@Override
			public void viewPortChanged(ViewPort newViewPort) {
				heatMapsPanel.repaint();
			
			}
		});	
		heatMapsPanel.addLayer(new ZoomMouseListenerLayer());
		heatMapsPanel.addLayer(new DragMouseListenerLayer(cs));
		heatMapsPanel.addLayer(new BackgroundLayer(cs));
		heatMapsPanel.addLayer(new ScaleLayer(cs,true));
		heatMapsPanel.addLayer(new SelectionLayer(cs));
		return heatMapsPanel;
	}
	
	public static void main(String[] args) {
		JFrame frame = new VisFrame();
		
		frame.pack();
		frame.setSize(800,550);
		frame.setLocationRelativeTo(null);
		frame.setVisible(true);

	}

}
