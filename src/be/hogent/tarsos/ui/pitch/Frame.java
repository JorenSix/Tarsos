/**
 */
package be.hogent.tarsos.ui.pitch;

import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.util.List;

import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import be.hogent.tarsos.util.histogram.Histogram;
import be.hogent.tarsos.util.histogram.ToneScaleHistogram;
import be.hogent.tarsos.util.histogram.peaks.Peak;
import be.hogent.tarsos.util.histogram.peaks.PeakDetector;

/**
 * @author Joren Six
 */
public class Frame extends JFrame {
	/**
	 * Default height.
	 */
	private static final int INITIAL_HEIGHT = 480;
	/**
	 * Default width.
	 */
	private static final int INITIAL_WIDTH = 640;
	/**
     */
	private static final long serialVersionUID = -8095965296377515567L;

	final ToneScaleFrame panel;

	public Frame() {
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		setLayout(new BorderLayout());
		setSize(INITIAL_WIDTH, INITIAL_HEIGHT);
		final Histogram histo = new ToneScaleHistogram();
		panel = new ToneScaleFrame(histo);

		final JSlider silder = new JSlider(0, 100);
		silder.setValue(0);
		silder.addChangeListener(new ChangeListener() {
			@Override
			public void stateChanged(final ChangeEvent e) {
				final JSlider source = (JSlider) e.getSource();
				// if (!source.getValueIsAdjusting()) {
				final double value = source.getValue();
				// panel.getHistogram().gaussianSmooth(value);
				// panel.draw(0, 0);
				final List<Peak> peaks = PeakDetector.detect(panel.getHistogram(), (int) value, 0.5);
				final double[] peaksInCents = new double[peaks.size()];
				int i = 0;
				for (final Peak peak : peaks) {
					peaksInCents[i++] = peak.getPosition();
				}
				panel.setReferenceScale(peaksInCents);
			}
		});

		add(panel, BorderLayout.CENTER);
		add(silder, BorderLayout.SOUTH);
		// add(new ConfigurationPanel(), BorderLayout.CENTER);
	}

	private void addLayers() {
		List<Layer> layers = panel.getLayers();
		JPanel layersJPanel = new JPanel(new GridLayout(layers.size(), 1));
		for (int i = 0; i < layers.size(); i++) {
			layersJPanel.add(layers.get(i).ui());
		}
		add(layersJPanel, BorderLayout.WEST);
	}

	public static void main(final String... strings) {
		final Frame frame = new Frame();
		frame.addLayers();
		frame.setVisible(true);

	}
}
