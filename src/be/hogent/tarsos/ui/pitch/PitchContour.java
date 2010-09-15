package be.hogent.tarsos.ui.pitch;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.logging.Logger;

import javax.swing.JButton;
import javax.swing.JFrame;

import ptolemy.plot.Plot;
import be.hogent.tarsos.pitch.PitchConverter;
import be.hogent.tarsos.pitch.PitchDetectionMode;
import be.hogent.tarsos.pitch.PitchUnit;
import be.hogent.tarsos.pitch.pure.DetectedPitchHandler;
import be.hogent.tarsos.util.AudioFile;
import be.hogent.tarsos.util.ConfKey;
import be.hogent.tarsos.util.Configuration;

public class PitchContour extends Plot {

	/**
	 * Log messages.
	 */
	private static final Logger LOG = Logger.getLogger(PitchContour.class.getName());

	/**
	 * 
	 */
	private static final long serialVersionUID = 1744724271250082834L;

	private final AudioFile audioFile;
	private final PitchUnit pitchUnit;

	public PitchContour(final AudioFile file, final double[] scale, final PitchUnit unit) {
		setSize(640, 480);

		pitchUnit = unit;

		if (unit == PitchUnit.HERTZ) {
			setYLog(true);
			setYRange(convertToLog(20), convertToLog(2000));
		} else if (unit == PitchUnit.RELATIVE_CENTS) {
			setYRange(0, 1200);
		}
		setYLabel("Pitch (" + unit.getHumanName() + ")");

		setXRange(0, file.getLengthInMicroSeconds() / 1000.0);
		String title = String.format("%s - pitch in %s", file.basename(), pitchUnit.getHumanName());

		setButtons(true);
		setTitle(title);
		setXLabel("Time (seconds)");

		setMarksStyle("dots");
		setReuseDatasets(true);

		setYTicks(scale);

		setConnected(false);

		JFrame frame = new JFrame(title);
		frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
		frame.add(this, BorderLayout.CENTER);
		JButton button = new JButton("Play");
		button.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(final ActionEvent event) {
				double from = getXRange()[0];
				double to = getXRange()[1];
				file.playSelection(from, to);
			}
		});

		frame.add(button, BorderLayout.SOUTH);
		frame.pack();
		frame.setVisible(true);

		analyseFile();

		this.audioFile = file;
	}

	private void setYTicks(final double[] scale) {
		if (scale != null) {
			// 9 octaves
			int octaves = 9;
			for (int octave = 1; octave < octaves; octave++) {
				for (double pitchClass : scale) {
					final double tickValue = pitchClass + 1200 * octave;
					final double hertzValue = PitchConverter.absoluteCentToHertz(tickValue);
					final double axisValue = pitchUnit.convertFromHertz(hertzValue);
					final double tickPosition;
					// if the y axis is logarithmic then
					// set the marker at the correct position
					if (getYLog()) {
						tickPosition = convertToLog(axisValue);
					} else {
						tickPosition = axisValue;
					}
					LOG.finest(String.format("Added %s cents marker at position %s %s", tickValue,
							tickPosition, pitchUnit.getHumanName()));
					addYTick(String.format("%.0f (%s)", axisValue, pitchClass), tickPosition);
				}
			}
			final double min = (Double) getYTicks()[0].firstElement();
			final double max = (Double) getYTicks()[0].lastElement();
			setYRange(min, max);
		}
	}

	private double convertToLog(double axisValue) {
		return Math.log(axisValue) / Math.log(10);
	}

	private void analyseFile() {
		final DetectedPitchHandler detectedPitchHandler = new DetectedPitchHandler() {
			@Override
			public void handleDetectedPitch(final float time, final float pitch) {
				if (pitch != -1) {
					double convertedPitch = pitchUnit.convertFromHertz(pitch);
					LOG.finest(String.format("Added %.2f %s to pitch contour", convertedPitch,
							pitchUnit.getHumanName()));
					addPoint(0, time, convertedPitch, false);
				}
			}
		};
		Runnable r = new Runnable() {
			@Override
			public void run() {
				PitchDetectionMode mode = Configuration.getPitchDetectionMode(ConfKey.pitch_tracker_current);
				audioFile.detectPitch(mode, detectedPitchHandler, 2);
			}
		};
		new Thread(r, "Pitch Contour Data Collection Thread").start();
	}
}
