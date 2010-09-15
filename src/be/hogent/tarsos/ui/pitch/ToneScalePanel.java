/**
 */
package be.hogent.tarsos.ui.pitch;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JPanel;

import be.hogent.tarsos.pitch.PitchConverter;
import be.hogent.tarsos.pitch.PitchDetectionMode;
import be.hogent.tarsos.pitch.pure.DetectedPitchHandler;
import be.hogent.tarsos.util.AudioFile;
import be.hogent.tarsos.util.ConfKey;
import be.hogent.tarsos.util.Configuration;
import be.hogent.tarsos.util.FileDrop;
import be.hogent.tarsos.util.FileUtils;
import be.hogent.tarsos.util.ScalaFile;
import be.hogent.tarsos.util.histogram.Histogram;

/**
 * @author Joren Six
 */
public final class ToneScalePanel extends JPanel {

	/**
     */
	private static final long serialVersionUID = 5493280409705136547L;

	private BufferedImage image;
	private Graphics2D graphics;
	private final Histogram histo;
	private final List<Layer> layers;

	private final HistogramLayer histoLayer;
	private final ScalaLayer scalaLayer;

	public ToneScalePanel(final Histogram histogram) {
		this.setSize(640, 480);
		initializeGraphics();
		this.histo = histogram;
		histoLayer = new HistogramLayer(this, histogram);
		final double[] referenceScale = { 0, 100, 200, 300, 400, 500, 600, 700, 800, 900, 1000, 1100 };
		scalaLayer = new ScalaLayer(this, referenceScale, 1200);
		layers = new ArrayList<Layer>();
		layers.add(histoLayer);
		layers.add(scalaLayer);

		addComponentListener(new ComponentAdapter() {
			@Override
			public void componentResized(final ComponentEvent e) {
				initializeGraphics();
			}
		});

		new FileDrop(this, new FileDrop.Listener() {
			@Override
			public void filesDropped(final java.io.File[] files) {
				for (final File droppedFile : files) {
					if (droppedFile.getName().endsWith(".scl")) {
						double[] scale = new ScalaFile(droppedFile.getAbsolutePath()).getPitches();
						scalaLayer.setScale(scale);
					} else if (FileUtils.isAudioFile(droppedFile)) {
						final AudioFile audioFile = new AudioFile(droppedFile.getAbsolutePath());
						histoLayer.setAudioFile(audioFile);
						histogram.clear();
						final DetectedPitchHandler detectedPitchHandler = new DetectedPitchHandler() {
							private final List<Double> markers = new ArrayList<Double>();

							@Override
							public void handleDetectedPitch(final float time, final float pitch) {
								if (pitch != -1) {
									histogram.add(PitchConverter.hertzToRelativeCent(pitch));
									if (getShouldPlay()) {
										markers.add(PitchConverter.hertzToRelativeCent(pitch));
									}
								}
								if ((int) (time * 100) % 5 == 0) {
									histoLayer.setMarkers(markers);
									repaint();
								}
							}
						};
						Runnable r = new Runnable() {
							@Override
							public void run() {
								PitchDetectionMode mode = Configuration
										.getPitchDetectionMode(ConfKey.pitch_tracker_current);
								audioFile.detectPitch(mode, detectedPitchHandler, 10);
							}
						};
						new Thread(r, "Pitch Contour Data Collection Thread").start();
					}

				}

			} // end filesDropped
		}); // end FileDrop.Listener
	}

	private void initializeGraphics() {
		image = new BufferedImage(getWidth(), getHeight(), BufferedImage.TYPE_INT_BGR);
		graphics = image.createGraphics();
		graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		graphics.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS,
				RenderingHints.VALUE_FRACTIONALMETRICS_ON);
	}

	@Override
	public void paint(final Graphics g) {
		graphics.setBackground(Color.WHITE);
		graphics.clearRect(0, 0, getWidth(), getHeight());
		for (final Layer layer : layers) {
			layer.draw(graphics);
		}
		g.drawImage(image, 0, 0, null);
	}

	public Histogram getHistogram() {
		return this.histo;
	}

	public void setReferenceScale(double[] peaksInCents) {
		this.scalaLayer.setScale(peaksInCents);
		this.scalaLayer.setXOffset(histoLayer.getXOffset());
	}

	private boolean play = false;

	void setShouldPlay(boolean shouldPlay) {
		play = shouldPlay;
	}

	boolean getShouldPlay() {
		return play;
	}

	public List<Layer> getLayers() {
		return layers;
	}

}
