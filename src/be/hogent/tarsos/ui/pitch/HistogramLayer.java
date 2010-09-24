/**
 */
package be.hogent.tarsos.ui.pitch;

import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JSlider;
import javax.swing.JSpinner;
import javax.swing.SpinnerModel;
import javax.swing.SpinnerNumberModel;
import javax.swing.border.TitledBorder;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import be.hogent.tarsos.sampled.pitch.PitchDetectionMode;
import be.hogent.tarsos.sampled.pitch.PitchUnit;
import be.hogent.tarsos.util.AudioFile;
import be.hogent.tarsos.util.histogram.Histogram;
import be.hogent.tarsos.util.histogram.peaks.Peak;
import be.hogent.tarsos.util.histogram.peaks.PeakDetector;

import com.jgoodies.forms.builder.DefaultFormBuilder;
import com.jgoodies.forms.layout.FormLayout;

/**
 * @author Joren Six
 */
public final class HistogramLayer implements Layer {

	private final JComponent parent;
	private final MouseDragListener mouseDrag;
	private final Histogram histo;
	private final int maxMarkers = 50;
	private final List<Double> markerPositions;
	private AudioFile file;
	private final ScaleChangedListener scaleChangedPublisher;

	/**
	 * Log messages.
	 */
	private static final Logger LOG = Logger.getLogger(HistogramLayer.class.getName());

	public HistogramLayer(final JComponent component, final Histogram histogram,
			final ScaleChangedListener scalePublisher) {
		parent = component;
		mouseDrag = new MouseDragListener(component, MouseEvent.BUTTON1);
		histo = histogram;
		component.addMouseListener(mouseDrag);
		component.addMouseMotionListener(mouseDrag);
		markerPositions = new ArrayList<Double>();
		scaleChangedPublisher = scalePublisher;
	}

	public void setMarkers(final List<Double> newMarkers) {
		// add new markers with ttl 0
		for (Double newMarker : newMarkers) {
			markerPositions.add(newMarker);
		}

		// remove old markers
		while (markerPositions.size() > maxMarkers) {
			markerPositions.remove(0);
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see be.hogent.tarsos.ui.Layer#draw()
	 */
	@Override
	public void draw(final Graphics2D graphics) {

		double xOffset = mouseDrag.calculateXOffset();
		int yOffset = 20;
		final double delta = histo.getStop() - histo.getStart();
		final long maxCount = histo.getMaxBinCount();

		final int width = parent.getWidth();
		final int height = parent.getHeight();

		final int xOffsetPixels = (int) Math.round(xOffset * width);
		int x = xOffsetPixels;

		int y = height - yOffset - (int) (histo.getCount(histo.getStop()) / (double) maxCount * height * 0.9);
		Point previousPoint = new Point(x, y);

		graphics.setColor(Color.GRAY);
		graphics.drawLine(0, height - yOffset, width, height - yOffset);

		graphics.setColor(Color.RED);

		int prevOctave = 0;
		for (final double key : histo.keySet()) {

			x = (int) (key / delta * width + xOffsetPixels) % width;
			y = height - yOffset - (int) (histo.getCount(key) / (double) maxCount * height * 0.9);
			if (x > previousPoint.x) {
				graphics.drawLine(previousPoint.x, previousPoint.y, x, y);
			}
			previousPoint = new Point(x, y);

			int octave = (int) key / 1200;
			if (prevOctave != octave) {
				graphics.setColor(Color.GRAY);
				x = (int) (octave * 1200 / delta * width + xOffsetPixels) % width;
				graphics.drawLine(x, 0, x, height - yOffset);
				graphics.drawString("" + octave * 1200, x, height - 5);
				graphics.setColor(Color.RED);
				prevOctave = octave;
			}
		}

		// draw markers

		for (int i = markerPositions.size() / 2; i < markerPositions.size(); i++) {
			double position = markerPositions.get(i);
			x = (int) (position / delta * width + xOffsetPixels) % width;
			y = height - ToneScalePanel.Y_BORDER
					- (int) (histo.getCount(position) / (double) maxCount * height * 0.9);
			graphics.setColor(Color.BLUE);
			graphics.drawOval(x, y, 2, 2);
		}

		LOG.fine("Histogram layer redrawn.");

	}

	public double getXOffset() {
		return mouseDrag.calculateXOffset();
	}

	JComponent ui;

	@Override
	public Component ui() {
		if (ui == null) {

			JSlider speedSlider = new JSlider(0, 20);
			speedSlider.setValue(10);
			// speedSlider.setPaintTicks(true);
			// speedSlider.setMajorTickSpacing(5);
			// speedSlider.setPaintLabels(true);
			speedSlider.addChangeListener(new ChangeListener() {
				@Override
				public void stateChanged(final ChangeEvent e) {
					final JSlider source = (JSlider) e.getSource();
					final double value = source.getValue();
					if (file != null) {
						file.setSpeedFactor(value);
					}
					source.setToolTipText(String.valueOf(value));
				}
			});

			JSlider peakSlider = new JSlider(5, 105);
			peakSlider.setValue(5);
			peakSlider.setMajorTickSpacing(20);
			peakSlider.addChangeListener(new ChangeListener() {
				@Override
				public void stateChanged(final ChangeEvent e) {
					final JSlider source = (JSlider) e.getSource();
					//
					final double value = source.getValue();

					if (histo.getMaxBinCount() != 0) {
						final List<Peak> peaks = PeakDetector.detect(histo, (int) value, 0.5);
						final double[] peaksInCents = new double[peaks.size()];
						int i = 0;
						for (final Peak peak : peaks) {
							peaksInCents[i++] = peak.getPosition();
						}
						scaleChangedPublisher.scaleChanged(peaksInCents, source.getValueIsAdjusting());
					}
				}
			});

			JButton smoothButton = new JButton("Gaussian");
			smoothButton.addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent e) {
					histo.gaussianSmooth(0.8);
					parent.repaint();
				}
			});

			JButton ambitusButton = new JButton("Ambitus");
			ambitusButton.addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent e) {
					histo.clear();
					parent.repaint();
				}
			});

			JButton resetButton = new JButton("Reset");
			resetButton.addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent e) {
					histo.clear();
					parent.repaint();
				}
			});

			final SpinnerModel minModel = new SpinnerNumberModel(0, // initial
																	// value
					0, // min
					1200, // max
					1); // step
			final SpinnerModel maxModel = new SpinnerNumberModel(1200, // initial
																		// value
					0, // min
					1200, // max
					1); // step

			final JSpinner minJSpinner = new JSpinner(minModel);
			final JSpinner maxJSpinner = new JSpinner(maxModel);
			JButton showRangeButton = new JButton("show");

			showRangeButton.addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent e) {
					Thread t = new Thread(new Runnable() {
						@Override
						public void run() {
							file.playCents(PitchDetectionMode.TARSOS_YIN, (Integer) minJSpinner.getValue(),
									(Integer) maxJSpinner.getValue(), PitchUnit.RELATIVE_CENTS);

						}
					}, "Segment playing thread");
					t.start();
				}
			});

			FormLayout subLayout = new FormLayout("right:min,2dlu,min,min:grow");
			DefaultFormBuilder subBuilder = new DefaultFormBuilder(subLayout);
			subBuilder.setRowGroupingEnabled(true);
			subBuilder.append(minJSpinner);
			subBuilder.append(maxJSpinner);
			subBuilder.append(showRangeButton);

			FormLayout layout = new FormLayout("right:min,2dlu,min:grow");
			DefaultFormBuilder builder = new DefaultFormBuilder(layout);
			builder.setDefaultDialogBorder();
			builder.setRowGroupingEnabled(true);
			builder.append("Peakpicking:", peakSlider, true);
			builder.append("Smooth:", smoothButton, true);
			builder.append("Reset:", resetButton, true);
			builder.append("Selection:", subBuilder.getPanel(), true);

			ui = builder.getPanel();
			ui.setBorder(new TitledBorder("Histogram commands"));
		}
		return ui;
	}

}
