/**
*
*  Tarsos is developed by Joren Six at 
*  The Royal Academy of Fine Arts & Royal Conservatory,
*  University College Ghent,
*  Hoogpoort 64, 9000 Ghent - Belgium
*
**/
package be.hogent.tarsos.ui.pitch.ph;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.RenderingHints;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.swing.JPanel;

import be.hogent.tarsos.Tarsos;
import be.hogent.tarsos.sampled.pitch.Annotation;
import be.hogent.tarsos.sampled.pitch.AnnotationListener;
import be.hogent.tarsos.sampled.pitch.PitchDetectionMode;
import be.hogent.tarsos.sampled.pitch.PitchUnit;
import be.hogent.tarsos.ui.pitch.AudioFileChangedListener;
import be.hogent.tarsos.ui.pitch.Layer;
import be.hogent.tarsos.ui.pitch.ScaleChangedListener;
import be.hogent.tarsos.util.AudioFile;
import be.hogent.tarsos.util.ConfKey;
import be.hogent.tarsos.util.Configuration;
import be.hogent.tarsos.util.ScalaFile;
import be.hogent.tarsos.util.histogram.Histogram;
import be.hogent.tarsos.util.histogram.PitchClassHistogram;

public final class PitchClassHistogramPanel extends JPanel implements ScaleChangedListener, AudioFileChangedListener,
		AnnotationListener {

	public static final int X_BORDER = 5; // pixels
	public static final int Y_BORDER = 5; // pixels

	/**
     */
	private static final long serialVersionUID = 5493280409705136547L;
	
	private static final int AMBITUS_STOP = Configuration.getInt(ConfKey.pitch_histogram_stop);
	private static final int AMBITUS_START = Configuration.getInt(ConfKey.pitch_histogram_start);

	

	private final Set<PitchDetectionMode> drawnModes;
	private final ScalaLayer scalaLayer;
	private final ScaleChangedListener scaleChangedPublisher;
	private AudioFile audioFile;


	public PitchClassHistogramPanel(final ScaleChangedListener scaleChangedPublisher) {
		super(new BorderLayout());
		//Focus should be enabled for the key listener (Scala layer editor)...
		setFocusable(true);
		this.scaleChangedPublisher = scaleChangedPublisher;

		scalaLayer = new ScalaLayer(this, ScalaFile.westernTuning().getPitches(), scaleChangedPublisher);

		drawnModes = new HashSet<PitchDetectionMode>();
	}

	public void audioFileChanged(final AudioFile newAudioFile) {
		audioFile = newAudioFile;
		//reset the list of which histograms are drawn.
		drawnModes.clear();
	}

	
	public void paint(final Graphics g) {
		final Graphics2D graphics = (Graphics2D) g;
		graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		graphics.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS,
				RenderingHints.VALUE_FRACTIONALMETRICS_ON);
		graphics.setBackground(Color.WHITE);
		graphics.clearRect(0, 0, getWidth(), getHeight());
		
	}
	

	public List<Layer> getLayers() {
		return null;
	}

	public void scaleChanged(final double[] newScale, final boolean isChanging, boolean shiftHisto) {
		this.scalaLayer.scaleChanged(newScale, isChanging, shiftHisto);
		Histogram histo = null;
		
		
		//search best shift
		boolean setScalaXOffset = shiftHisto && histo instanceof PitchClassHistogram; 
		if( setScalaXOffset){
			Histogram oneHistogram = PitchClassHistogram.createToneScale(newScale.clone());
			int displacement = oneHistogram.normalize().displacementForOptimalCorrelation(histo.normalize());
			double offsetInCents = displacement * Configuration.getDouble(ConfKey.histogram_bin_width);
			double offsetInPositiveCents = (offsetInCents + 1200.0 ) % 1200.0;
			double offsetInPercent = offsetInPositiveCents/1200.0;
			this.scalaLayer.setXOffset(offsetInPercent);
		}
		
		/*
		for (Layer layer : layers) {
			if (layer instanceof HistogramLayer) {
				HistogramLayer histoLayer = (HistogramLayer) layer;
				histoLayer.scaleChanged(newScale, isChanging, shiftHisto);
				if(!setScalaXOffset){
					this.scalaLayer.setXOffset(histoLayer.getXOffset());
				}
			}
		}
		*/
	}


	public void addAnnotation(Annotation annotation) {
		double pitchInAbsCents = annotation.getPitch(PitchUnit.ABSOLUTE_CENTS);
		if (pitchInAbsCents > AMBITUS_START && pitchInAbsCents <= AMBITUS_STOP) {
			if (!drawnModes.contains(annotation.getSource())) {
				drawnModes.add(annotation.getSource());
				final Histogram histo;
				/*
				if (stop > 1200) {
					histo = KDEData.getPitchHistogramInstance().getHistogram(annotation.getSource());
				} else {
					histo = KDEData.getPitchClassHistogramInstance().getHistogram(annotation.getSource());;
				}
				Color color = Tarsos.COLORS[annotation.getSource().ordinal() % Tarsos.COLORS.length];
				HistogramLayer layer = new HistogramLayer(this, histo, scaleChangedPublisher, color);
				layer.audioFileChanged(audioFile);
				layers.add(layer);
				*/
			}
		}
	}

	public void clearAnnotations() {
		
	}

	public void annotationsAdded() {
		repaint();
	}

	
	public void extractionStarted() {
		// TODO Auto-generated method stub
	}

	
	public void extractionFinished() {
		// TODO Auto-generated method stub
		
	}
}
