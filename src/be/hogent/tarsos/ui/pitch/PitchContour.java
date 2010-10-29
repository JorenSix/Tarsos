package be.hogent.tarsos.ui.pitch;

import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import javax.swing.JButton;
import javax.swing.SwingUtilities;

import ptolemy.plot.Plot;
import ptolemy.plot.PlotPoint;
import be.hogent.tarsos.Tarsos;
import be.hogent.tarsos.sampled.pitch.Annotation;
import be.hogent.tarsos.sampled.pitch.Pitch;
import be.hogent.tarsos.sampled.pitch.PitchConverter;
import be.hogent.tarsos.sampled.pitch.PitchDetectionMode;
import be.hogent.tarsos.sampled.pitch.PitchUnit;
import be.hogent.tarsos.ui.pitch.ControlPanel.SampleHandler;
import be.hogent.tarsos.util.AudioFile;
import be.hogent.tarsos.util.ConfKey;
import be.hogent.tarsos.util.Configuration;
import be.hogent.tarsos.util.FileUtils;

public class PitchContour extends Plot implements AudioFileChangedListener, ScaleChangedListener,
		SampleHandler {

	/**
	 * Log messages.
	 */
	private static final Logger LOG = Logger.getLogger(PitchContour.class.getName());

	/**
	 * 
	 */
	private static final long serialVersionUID = 1744724271250082834L;

	private PitchUnit pitchUnit;
	private double[] scale;

	public PitchContour() {
		pitchUnit = PitchUnit.valueOf(Configuration.get(ConfKey.pitch_contour_unit));
		setColors(Tarsos.COLORS);
	}

	private void setYTicks() {
		if (scale != null) {
			// 9 octaves
			final int octaves;
			if (pitchUnit == PitchUnit.RELATIVE_CENTS) {
				octaves = 1;
			} else {
				octaves = 9;
			}
			for (int octave = 0; octave < octaves; octave++) {
				for (double pitchClass : scale) {
					final double tickValue = Math.round(pitchClass + 1200 * octave);
					final double hertzValue = PitchConverter.absoluteCentToHertz(tickValue);
					final double axisValue = Math.round(pitchUnit.convertFromHertz(hertzValue));
					final double tickPosition;
					// if the y axis is logarithmic then
					// set the marker at the correct position
					if (getYLog()) {
						tickPosition = convertToLog(axisValue);
					} else {
						tickPosition = axisValue;
					}
					LOG.fine(String.format("Added %s cents marker at position %s %s", tickValue,
							tickPosition, pitchUnit.getHumanName()));
					if (pitchUnit == PitchUnit.RELATIVE_CENTS) {
						addYTick(String.format("%.0f", axisValue), tickPosition);
					} else {
						addYTick(String.format("%.0f (%.0f)", axisValue, pitchClass), tickPosition);
					}

				}
			}

			if (pitchUnit == PitchUnit.RELATIVE_CENTS) {
				final double min;
				final double max;
				min = 0.0;
				max = 1200.0;
				setYRange(min, max);
			}
		}
	}

	private void clearYTicks() {
		if (getYTicks() != null) {
			getYTicks()[0].clear();
			getYTicks()[1].clear();
		}
	}

	private double convertToLog(final double axisValue) {
		return Math.log(axisValue) / Math.log(10);
	}

	JButton button;

	public void audioFileChanged(final AudioFile newAudioFile) {

		pitchUnit = PitchUnit.valueOf(Configuration.get(ConfKey.pitch_contour_unit));
		setMarksStyle(Configuration.get(ConfKey.pitch_contour_marks));

		for (int i = 0; i < PitchDetectionMode.values().length; i++) {
			clear(i);
		}

		if (pitchUnit == PitchUnit.HERTZ) {
			setYLog(true);
			setYRange(convertToLog(20), convertToLog(2000));
		} else if (pitchUnit == PitchUnit.RELATIVE_CENTS) {
			setYRange(0, 1200);
		}
		setYLabel("Pitch (" + pitchUnit.getHumanName() + ")");

		setXRange(0, newAudioFile.getLengthInMilliSeconds() / 1000.0);
		String title = String.format("%s - pitch in %s", newAudioFile.basename(), pitchUnit.getHumanName());

		setButtons(true);
		if (button != null) {
			remove(button);
		}
		button = new JButton(Frame.createImageIcon("/ptolemy/plot/img/fill.gif"));
		button.setPreferredSize(new Dimension(20, 20));
		button.setToolTipText("Export CSV and EPS file");
		button.addActionListener(new ActionListener() {
			final StringBuffer stringBuffer = new StringBuffer();

			public void actionPerformed(ActionEvent arg0) {
				// EPS
				OutputStream out;
				try {
					out = new BufferedOutputStream(new FileOutputStream(new File(newAudioFile.basename()
							+ ".eps")));
					export(out);
					out.close();
				} catch (FileNotFoundException e) {
					e.printStackTrace();
				} catch (IOException e) {
					e.printStackTrace();
				}
				// CSV
				stringBuffer.append("Time(sec);Frequency(Hz)\n");
				for (PlotPoint p : _points.get(0)) {
					stringBuffer.append(String.format("%.5f;%.5f\n", p.x, p.y));
				}
				FileUtils.writeFile(stringBuffer.toString(), newAudioFile.basename() + ".csv");
				stringBuffer.delete(0, stringBuffer.length());
			}
		});
		button.setBorderPainted(false);

		add(button);
		setTitle(title);
		setXLabel("Time (seconds)");

		setYTicks();

		setConnected(false);

		// Invoke repaint on the swing thread (not on the AWT event dispatching
		// thread)
		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				repaint();
			}
		});

	}

	public void scaleChanged(double[] newScale, boolean isChanging) {
		scale = newScale;
		clearYTicks();
		setYTicks();
		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				repaint();
			}
		});
	}

	/**
	 * Returns all the annotations of the given list in the visible area.
	 * 
	 * @param annotationsToFilter
	 *            The list of all annotations.
	 * @return A filtered list containing only the annotations in the currently
	 *         visible area as defined by getXRange() and getYRange()
	 */
	public List<Annotation> filterAnnotations(List<Annotation> annotationsToFilter) {
		final List<Annotation> filteredList = new ArrayList<Annotation>();
		final double startTime = getXRange()[0];
		final double stopTime = getXRange()[1];
		final double startPitch = Pitch.getInstance(pitchUnit, getYRange()[0]).getPitch(PitchUnit.HERTZ);
		final double stopPitch = Pitch.getInstance(pitchUnit, getYRange()[1]).getPitch(PitchUnit.HERTZ);
		for (Annotation annotation : annotationsToFilter) {
			if (annotation.getStart() >= startTime && annotation.getStart() <= stopTime
					&& annotation.getPitch(PitchUnit.HERTZ) >= startPitch
					&& annotation.getPitch(PitchUnit.HERTZ) <= stopPitch) {
				filteredList.add(annotation);
			}
		}
		return filteredList;
	}

	public void addSample(Annotation sample) {
		final double convertedPitch = sample.getPitch(pitchUnit);
		final int dataset = sample.getSource().ordinal();
		LOG.finest(String.format("Added %.2f %s to pitch contour", convertedPitch, pitchUnit.getHumanName()));
		addPoint(dataset, sample.getStart(), convertedPitch, false);
		addPoint(dataset, sample.getStart(), sample.getProbability(), false);
		if (getLegend(dataset) == null) {
			addLegend(dataset, sample.getSource().name());
			SwingUtilities.invokeLater(new Runnable() {
				public void run() {
					repaint();
				}
			});

		}

	}

	public void removeSample(Annotation sample) {
		// TODO Auto-generated method stub
	}

}
