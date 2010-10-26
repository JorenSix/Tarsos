/**
 */
package be.hogent.tarsos.ui.pitch;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JPanel;

import be.hogent.tarsos.sampled.pitch.Annotation;
import be.hogent.tarsos.sampled.pitch.PitchUnit;
import be.hogent.tarsos.ui.pitch.ControlPanel.SampleHandler;
import be.hogent.tarsos.util.AudioFile;
import be.hogent.tarsos.util.ConfKey;
import be.hogent.tarsos.util.Configuration;
import be.hogent.tarsos.util.ScalaFile;
import be.hogent.tarsos.util.histogram.Histogram;

/**
 * @author Joren Six
 */
public final class ToneScalePanel extends JPanel implements AudioFileChangedListener, ScaleChangedListener,
		SampleHandler {

	public static final int X_BORDER = 5; // pixels
	public static final int Y_BORDER = 5; // pixels

	/**
     */
	private static final long serialVersionUID = 5493280409705136547L;

	private final Histogram histo;
	private final List<Layer> layers;

	private final HistogramLayer histoLayer;
	private final ScalaLayer scalaLayer;

	public ToneScalePanel(final Histogram histogram, final ScaleChangedListener scaleChangedPublisher) {
		setSize(640, 480);
		histo = histogram;
		histoLayer = new HistogramLayer(this, histogram, scaleChangedPublisher);
		scalaLayer = new ScalaLayer(this, ScalaFile.westernTuning().getPitches(), histogram.getStop()
				- histogram.getStart(), scaleChangedPublisher);

		layers = new ArrayList<Layer>();
		layers.add(histoLayer);
		layers.add(scalaLayer);
	}

	public void audioFileChanged(final AudioFile audioFile) {
		histo.clear();
		histoLayer.audioFileChanged(audioFile);
	}

	@Override
	public void paint(final Graphics g) {
		final Graphics2D graphics = (Graphics2D) g;
		graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		graphics.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS,
				RenderingHints.VALUE_FRACTIONALMETRICS_ON);
		graphics.setBackground(Color.WHITE);
		graphics.clearRect(0, 0, getWidth(), getHeight());
		for (final Layer layer : layers) {
			layer.draw(graphics);
		}
	}

	public Histogram getHistogram() {
		return this.histo;
	}

	public List<Layer> getLayers() {
		return layers;
	}

	public void scaleChanged(final double[] newScale, final boolean isChanging) {
		this.scalaLayer.scaleChanged(newScale, isChanging);
		this.scalaLayer.setXOffset(histoLayer.getXOffset());
		histoLayer.scaleChanged(newScale, isChanging);
	}

	public void addSample(Annotation sample) {
		double pitchInAbsCents = sample.getPitch(PitchUnit.ABSOLUTE_CENTS);
		if (pitchInAbsCents > 0 && pitchInAbsCents <= Configuration.getInt(ConfKey.ambitus_stop)) {
			histo.add(pitchInAbsCents);
			if ((int) (sample.getStart() * 1000) % 5 == 0) {
				// histoLayer.setMarkers(markers);
				repaint();
			}
		}

	}

	public void removeSample(Annotation sample) {
		// TODO Auto-generated method stub

	}
}
