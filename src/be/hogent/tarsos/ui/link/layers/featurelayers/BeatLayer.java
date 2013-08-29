package be.hogent.tarsos.ui.link.layers.featurelayers;

import java.awt.Color;
import java.awt.Graphics2D;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.sound.sampled.UnsupportedAudioFileException;

import be.hogent.tarsos.dsp.AudioDispatcher;
import be.hogent.tarsos.dsp.AudioEvent;
import be.hogent.tarsos.dsp.AudioProcessor;
import be.hogent.tarsos.dsp.beatroot.BeatRootOnsetEventHandler;
import be.hogent.tarsos.dsp.onsets.ComplexOnsetDetector;
import be.hogent.tarsos.dsp.onsets.OnsetHandler;
import be.hogent.tarsos.ui.link.LinkedFrame;
import be.hogent.tarsos.ui.link.LinkedPanel;
import be.hogent.tarsos.ui.link.coordinatessystems.CoordinateSystem;

public class BeatLayer extends FeatureLayer {

	final List<Double> onsets; // in seconds
	final List<Double> beats; //in seconds
	public BeatLayer(LinkedPanel parent) {
		this(parent,256,0);
	}
	
	public BeatLayer(final LinkedPanel parent, int frameSize, int overlap) {
		super(parent, frameSize, overlap);
		this.name = "Beat layer";
		this.onsets = new ArrayList<Double>();
		this.beats = new ArrayList<Double>();
	}
	
	public void draw(Graphics2D graphics){
		CoordinateSystem cs = parent.getCoordinateSystem();
		int maxY = Math.round(cs.getMax(CoordinateSystem.Y_AXIS));
		int minY = Math.round(cs.getMin(CoordinateSystem.Y_AXIS));
		if(!onsets.isEmpty()){
			graphics.setColor(Color.blue);
			for(Double onset : onsets){
				int onsetTime = (int) Math.round(onset*1000);//in ms
				graphics.drawLine(onsetTime,minY, onsetTime, maxY);
			}
		}
		if(!beats.isEmpty()){
			graphics.setColor(Color.red);
			for(Double beat : beats){
				int beatTime = (int) Math.round(beat*1000);//in ms
				graphics.drawLine(beatTime,minY, beatTime, maxY);
			}
		}
	}
	
	@Override
	public void initialise() {
		float sampleRate = LinkedFrame.getInstance().getAudioFile().fileFormat().getFormat().getSampleRate();
		
		final double lag =  this.getFrameSize() / sampleRate / 2.0;// in seconds
		try {
			adp = AudioDispatcher.fromFile(new File(LinkedFrame.getInstance()
					.getAudioFile().originalPath()), this.getFrameSize(),
					this.getOverlap());
		} catch (UnsupportedAudioFileException e) {
			e.printStackTrace();
		} catch (IOException e2){
			e2.printStackTrace();
		}		
		final ComplexOnsetDetector detector = new ComplexOnsetDetector(getFrameSize());
		final BeatRootOnsetEventHandler broeh = new BeatRootOnsetEventHandler();
		adp.addAudioProcessor(detector);
		adp.addAudioProcessor(new AudioProcessor() {
			public void processingFinished() {
				broeh.trackBeats(new OnsetHandler() {
					@Override
					public void handleOnset(double time, double salience) {
						beats.add(time-lag);
					}
				});
			}
			public boolean process(AudioEvent audioEvent) {
				return true;
			}
		});
		detector.setHandler(new OnsetHandler() {
			@Override
			public void handleOnset(double time, double salience) {
				onsets.add(time - lag);
				broeh.handleOnset(time - lag, salience);
			}
		});
		adp.run();
	}

}
