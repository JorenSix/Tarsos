/**
 */
package be.hogent.tarsos.ui.pitch;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import javax.swing.JPanel;
import javax.swing.JTabbedPane;

import be.hogent.tarsos.Tarsos;
import be.hogent.tarsos.sampled.pitch.Annotation;
import be.hogent.tarsos.sampled.pitch.PitchDetectionMode;
import be.hogent.tarsos.sampled.pitch.PitchUnit;
import be.hogent.tarsos.ui.pitch.ControlPanel.SampleHandler;
import be.hogent.tarsos.util.AudioFile;
import be.hogent.tarsos.util.ConfKey;
import be.hogent.tarsos.util.Configuration;
import be.hogent.tarsos.util.ScalaFile;
import be.hogent.tarsos.util.histogram.Histogram;
import be.hogent.tarsos.util.histogram.ToneScaleHistogram;

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

	private final HashMap<PitchDetectionMode, Histogram> histos;
	private final List<Layer> layers;
	private final ScalaLayer scalaLayer;
	private final ScaleChangedListener scaleChangedPublisher;
	/**
	 * Hehe, feces.
	 */
	private final JTabbedPane layerUserInterfeces;

	public ToneScalePanel(final Histogram histogram, final ScaleChangedListener scaleChangedPublisher) {
		super(new BorderLayout());
		setSize(640, 480);
		histos = new HashMap<PitchDetectionMode, Histogram>();
		this.scaleChangedPublisher = scaleChangedPublisher;
		layers = new ArrayList<Layer>();
		scalaLayer = new ScalaLayer(this, ScalaFile.westernTuning().getPitches(), histogram.getStop()
				- histogram.getStart(), scaleChangedPublisher);
		layers.add(scalaLayer);

		layerUserInterfeces = new JTabbedPane();
	}

	public void audioFileChanged(final AudioFile audioFile) {
		for (Layer layer : layers) {
			if (layer instanceof HistogramLayer) {
				((HistogramLayer) layer).audioFileChanged(audioFile);
			}
		}

		for (Histogram histogram : histos.values()) {
			histogram.clear();
		}

		/*
		 * List<Histogram> histograms = new
		 * ArrayList<Histogram>(histos.values()); for (Histogram histogram :
		 * histograms) { for (Histogram other : histograms) { int displacement =
		 * other.displacementForOptimalCorrelation(histogram);
		 * other.displace(displacement); } }
		 */

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

	public List<Layer> getLayers() {
		return layers;
	}

	public void scaleChanged(final double[] newScale, final boolean isChanging) {
		this.scalaLayer.scaleChanged(newScale, isChanging);
		for (Layer layer : layers) {
			if (layer instanceof HistogramLayer) {
				HistogramLayer histoLayer = (HistogramLayer) layer;
				histoLayer.scaleChanged(newScale, isChanging);
				this.scalaLayer.setXOffset(histoLayer.getXOffset());
			}
		}
	}

	public void addSample(Annotation sample) {
		double pitchInAbsCents = sample.getPitch(PitchUnit.ABSOLUTE_CENTS);
		if (pitchInAbsCents > 0 && pitchInAbsCents <= Configuration.getInt(ConfKey.ambitus_stop)) {

			final Histogram histo;
			if (!histos.containsKey(sample.getSource())) {
				histo = new ToneScaleHistogram();
				histos.put(sample.getSource(), histo);
				Color color = Tarsos.COLORS[sample.getSource().ordinal() % Tarsos.COLORS.length];
				HistogramLayer layer = new HistogramLayer(this, histo, scaleChangedPublisher, color);
				layers.add(layer);
				layerUserInterfeces.addTab(sample.getSource().name(), layer.ui());
			} else {
				histo = histos.get(sample.getSource());
			}

			histo.add(pitchInAbsCents);

			if ((int) (sample.getStart() * 1000) % 5 == 0) {
				repaint();
			}
		}
	}

	public void removeSample(Annotation sample) {
		// TODO Auto-generated method stub

	}

	public Component controls() {
		return layerUserInterfeces;
	}
}
