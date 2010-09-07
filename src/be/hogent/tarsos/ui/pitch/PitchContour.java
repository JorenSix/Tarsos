package be.hogent.tarsos.ui.pitch;

import java.awt.BorderLayout;

import javax.swing.JFrame;

import ptolemy.plot.Plot;
import be.hogent.tarsos.pitch.PitchDetectionMode;
import be.hogent.tarsos.pitch.PitchUnit;
import be.hogent.tarsos.pitch.pure.DetectedPitchHandler;
import be.hogent.tarsos.util.AudioFile;

public class PitchContour extends Plot {

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
		} else if (unit == PitchUnit.RELATIVE_CENTS) {
			setYRange(0, 1200);
		}
		setYLabel("Pitch (" + unit.getHumanName() + ")");

		setButtons(true);
		setTitle(file.basename());
		setXLabel("Time (seconds)");

		setMarksStyle("dots");
		setReuseDatasets(true);

		setYTicks(scale);

		setConnected(false);

		JFrame frame = new JFrame(file.basename());
		frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
		frame.add(this, BorderLayout.CENTER);
		frame.pack();
		frame.setVisible(true);

		analyseFile();

		this.audioFile = file;
	}

	private void setYTicks(final double[] scale) {
		if (scale != null) {
			// 9 octaves
			for (int octave = 1; octave < 9; octave++) {
				for (double pitchClass : scale) {
					double tickValue = pitchClass + 1200 * octave;
					addYTick(String.valueOf(tickValue), tickValue);
				}
			}
		}
	}

	private void analyseFile() {
		final DetectedPitchHandler detectedPitchHandler = new DetectedPitchHandler() {
			@Override
			public void handleDetectedPitch(final float time, final float pitch) {
				// double pichInRelCent =
				// PitchConverter.hertzToRelativeCent(pitch);
				// && pichInRelCent > start && pichInRelCent < stop
				if (pitch != -1) {
					double convertedPitch = pitchUnit.convertFromHertz(pitch);
					addPoint(0, time, convertedPitch, false);
				}
			}
		};
		Runnable r = new Runnable() {
			@Override
			public void run() {
				audioFile.detectPitch(PitchDetectionMode.TARSOS_YIN, detectedPitchHandler, 2);
			}
		};
		new Thread(r, "Pitch Contour Data Collection Thread").start();
	}
}
