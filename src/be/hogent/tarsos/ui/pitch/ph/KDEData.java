/**
*
*  Tarsos is developed by Joren Six at 
*  The Royal Academy of Fine Arts & Royal Conservatory,
*  University College Ghent,
*  Hoogpoort 64, 9000 Ghent - Belgium
*
**/
package be.hogent.tarsos.ui.pitch.ph;

import java.util.HashMap;

import javax.swing.JComponent;

import be.hogent.tarsos.sampled.pitch.Annotation;
import be.hogent.tarsos.sampled.pitch.AnnotationListener;
import be.hogent.tarsos.sampled.pitch.PitchDetectionMode;
import be.hogent.tarsos.sampled.pitch.PitchUnit;
import be.hogent.tarsos.ui.pitch.AudioFileChangedListener;
import be.hogent.tarsos.util.AudioFile;
import be.hogent.tarsos.util.ConfKey;
import be.hogent.tarsos.util.Configuration;
import be.hogent.tarsos.util.KernelDensityEstimate;
import be.hogent.tarsos.util.histogram.Histogram;
import be.hogent.tarsos.util.histogram.PitchClassHistogram;
import be.hogent.tarsos.util.histogram.PitchHistogram;

public class KDEData  implements AudioFileChangedListener, AnnotationListener{

	private static final int AMBITUS_STOP = Configuration.getInt(ConfKey.pitch_histogram_stop);
	private static final int AMBITUS_START = Configuration.getInt(ConfKey.pitch_histogram_start);
	
	
	public static synchronized KDEData getInstance(){
		return null;
	}
	
		

	private final HashMap<PitchDetectionMode, KernelDensityEstimate> histos;
	private final boolean containsPitchClassHistogramData;
	
	private KDEData(boolean containsPCH){
		histos = new HashMap<PitchDetectionMode, KernelDensityEstimate>();
		containsPitchClassHistogramData = containsPCH;
	}
	

	
	public boolean isEmpty(){
		return histos.isEmpty();
	}

	
	private JComponent componentToRepaint;
	public void setComponentToRepaint(final JComponent component){
		componentToRepaint = component;
	}


	public void audioFileChanged(AudioFile newAudioFile) {
		if (Configuration.getBoolean(ConfKey.reset_on_import)) {
			histos.clear();
			repaint();
		}
	}
	
	public void repaint(){
		componentToRepaint.repaint();
	}


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
					
			    }
				
			} else {
			
			}

			repaint();
		}
	}
	
	public boolean containsKey(PitchDetectionMode source) {
		return histos.containsKey(source);
	}


	public void clearAnnotations() {

	}


	public void annotationsAdded() {
		// TODO Auto-generated method stub
	}


	public void extractionStarted() {
		// TODO Auto-generated method stub
	}


	public void extractionFinished() {
		// TODO Auto-generated method stub
	}
	
}
