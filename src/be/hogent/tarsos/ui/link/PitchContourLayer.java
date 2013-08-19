package be.hogent.tarsos.ui.link;

import java.awt.Color;
import java.awt.Graphics2D;
import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.TreeMap;

import javax.sound.sampled.UnsupportedAudioFileException;

import be.hogent.tarsos.dsp.AudioDispatcher;
import be.hogent.tarsos.dsp.AudioEvent;
import be.hogent.tarsos.dsp.example.PitchConverter;
import be.hogent.tarsos.dsp.pitch.PitchDetectionHandler;
import be.hogent.tarsos.dsp.pitch.PitchDetectionResult;
import be.hogent.tarsos.dsp.pitch.PitchProcessor;
import be.hogent.tarsos.dsp.pitch.PitchProcessor.PitchEstimationAlgorithm;

public class PitchContourLayer implements Layer{
	
	private final TreeMap<Double, Double> pitchContour;
	
	public PitchContourLayer(){
		pitchContour= new TreeMap<Double, Double>();
		extractPitchContour();		
	}
	
	private void extractPitchContour(){
		try {
			final double timeLag =  1024/44100.0;
			AudioDispatcher dispatcher;
			dispatcher = AudioDispatcher.fromFile(new File("/home/joren/Desktop/08._Ladrang_Kandamanyura_10s-20s_up.wav"), 2048, 0);
			dispatcher.addAudioProcessor(new PitchProcessor(PitchEstimationAlgorithm.FFT_YIN,44100,2048,new PitchDetectionHandler() {
				
			
				public void handlePitch(PitchDetectionResult pitchDetectionResult,
						AudioEvent audioEvent) {
					if(pitchDetectionResult.isPitched()){
						pitchContour.put(audioEvent.getTimeStamp()-timeLag, PitchConverter.hertzToAbsoluteCent(pitchDetectionResult.getPitch()));
					}
				}
			}));
			dispatcher.run();
		} catch (UnsupportedAudioFileException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	public void draw(Graphics2D graphics){		
		ViewPort viewport = ViewPort.getInstance();	
		
		graphics.setColor(Color.green);	
		int ovalWidth = Math.round(LayerUtilities.unitsToPixels(graphics,4, true));
		int ovalHeight = Math.round(LayerUtilities.unitsToPixels(graphics,4, false));
		//every second
		for(Map.Entry<Double, Double> entry : pitchContour.subMap(viewport.getMinTime()/1000.0, viewport.getMaxTime()/1000.0).entrySet()){
			double time = entry.getKey();// in seconds
			double pitch = entry.getValue();//in cents
			if(pitch > viewport.getMinFrequencyInCents() && pitch < viewport.getMaxFrequencyInCents()){
				graphics.drawOval((int)(time*1000), (int)pitch, ovalWidth, ovalHeight);
			}
		}
	}

}
