package be.hogent.tarsos.ui.pitch;

import java.util.List;
import java.util.logging.Logger;

import ptolemy.plot.Plot;
import be.hogent.tarsos.sampled.pitch.PitchConverter;
import be.hogent.tarsos.sampled.pitch.PitchUnit;
import be.hogent.tarsos.sampled.pitch.Sample;
import be.hogent.tarsos.ui.pitch.ControlPanel.SampleHandler;
import be.hogent.tarsos.util.AudioFile;

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

	private final PitchUnit pitchUnit;
	private double[] scale;

	public PitchContour() {
		pitchUnit = PitchUnit.ABSOLUTE_CENTS;
	}

	private void setYTicks() {
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
					LOG.fine(String.format("Added %s cents marker at position %s %s", tickValue,
							tickPosition, pitchUnit.getHumanName()));
					addYTick(String.format("%.0f (%.0f)", axisValue, pitchClass), tickPosition);
				}
			}
			final double min;
			final double max;
			if (pitchUnit == PitchUnit.RELATIVE_CENTS) {
				min = 0.0;
				max = 1200.0;
			} else {
				max = (Double) getYTicks()[0].lastElement();
				min = (Double) getYTicks()[0].firstElement();
			}

			setYRange(min, max);
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

	@Override
	public void audioFileChanged(AudioFile newAudioFile) {
		clear(0);

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
		setTitle(title);
		setXLabel("Time (seconds)");

		setMarksStyle("points");
		setReuseDatasets(true);

		setYTicks();

		setConnected(false);
		repaint();
	}

	@Override
	public void scaleChanged(double[] newScale, boolean isChanging) {
		scale = newScale;
		clearYTicks();
		setYTicks();
		repaint();
	}

	@Override
	public void addSample(Sample sample) {
		List<Double> pitches = sample.getPitchesIn(pitchUnit);
		double time = sample.getStart() / 1000.0;
		for (Double pitch : pitches) {
			if (pitch != -1) {
				double convertedPitch = pitchUnit.convertFromHertz(pitch);
				LOG.finest(String.format("Added %.2f %s to pitch contour", convertedPitch,
						pitchUnit.getHumanName()));
				addPoint(0, time, convertedPitch, false);
			}
		}

	}

	@Override
	public void removeSample(Sample sample) {
		// TODO Auto-generated method stub

	}
}
