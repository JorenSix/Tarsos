package be.hogent.tarsos.ui.pitch;

import java.util.HashMap;

import javax.swing.JComponent;

import be.hogent.tarsos.sampled.pitch.Annotation;
import be.hogent.tarsos.sampled.pitch.AnnotationListener;
import be.hogent.tarsos.sampled.pitch.PitchDetectionMode;
import be.hogent.tarsos.sampled.pitch.PitchUnit;
import be.hogent.tarsos.util.AudioFile;
import be.hogent.tarsos.util.ConfKey;
import be.hogent.tarsos.util.Configuration;
import be.hogent.tarsos.util.histogram.Histogram;
import be.hogent.tarsos.util.histogram.PitchClassHistogram;
import be.hogent.tarsos.util.histogram.PitchHistogram;

public class HistogramData  implements AudioFileChangedListener, AnnotationListener{

	private static HistogramData pitchClassHistogramInstance;
	private static HistogramData pitchHistogramInstance;
	
	private static final int AMBITUS_STOP = Configuration.getInt(ConfKey.pitch_histogram_stop);
	private static final int AMBITUS_START = Configuration.getInt(ConfKey.pitch_histogram_start);
	
	
	public static synchronized HistogramData getPitchHistogramInstance(){
		if(pitchHistogramInstance == null){
			pitchHistogramInstance = new HistogramData(false);
		}
		return pitchHistogramInstance;
	}
	
	public static synchronized HistogramData getPitchClassHistogramInstance(){
		if(pitchClassHistogramInstance == null){
			pitchClassHistogramInstance = new HistogramData(true);
		}
		return pitchClassHistogramInstance;
	}
	

	private final HashMap<PitchDetectionMode, Histogram> histos;
	private final boolean containsPitchClassHistogramData;
	
	private HistogramData(boolean containsPCH){
		histos = new HashMap<PitchDetectionMode, Histogram>();
		containsPitchClassHistogramData = containsPCH;
	}
	
	public void clearHistograms(){
		for (Histogram histogram : histos.values()) {
			histogram.clear();
		}
	}
	
	public boolean isEmpty(){
		return histos.isEmpty();
	}
	
	public Histogram getFirst() {
		return histos.values().iterator().next();
	}
	
	private JComponent componentToRepaint;
	public void setComponentToRepaint(final JComponent component){
		componentToRepaint = component;
	}

	@Override
	public void audioFileChanged(AudioFile newAudioFile) {
		if (Configuration.getBoolean(ConfKey.reset_on_import)) {
			clearHistograms();
			histos.clear();
			repaint();
		}
	}
	
	void repaint(){
		componentToRepaint.repaint();
	}
	
	public Histogram getHistogram(PitchDetectionMode mode){
		return histos.get(mode);
	}

	@Override
	public void addAnnotation(Annotation annotation) {
		double pitchInAbsCents = annotation.getPitch(PitchUnit.ABSOLUTE_CENTS);
		if (pitchInAbsCents > AMBITUS_START && pitchInAbsCents <= AMBITUS_STOP) {
			final Histogram histo;
			if (!histos.containsKey(annotation.getSource())) {
				if (!containsPitchClassHistogramData) {
					histo = new PitchHistogram();
				} else {
					histo = new PitchClassHistogram();
				}
				synchronized (this) {
					histos.put(annotation.getSource(), histo);
			    }
				
			} else {
				histo = histos.get(annotation.getSource());
			}
			histo.add(pitchInAbsCents);
			repaint();
		}
	}
	
	public boolean containsKey(PitchDetectionMode source) {
		return histos.containsKey(source);
	}

	@Override
	public void clearAnnotations() {
		clearHistograms();
		
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
