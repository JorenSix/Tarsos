/*
*              _______                      
*             |__   __|                     
*                | | __ _ _ __ ___  ___  ___
*                | |/ _` | '__/ __|/ _ \/ __| 
*                | | (_| | |  \__ \ (_) \__ \    
*                |_|\__,_|_|  |___/\___/|___/    
*                                                         
* -----------------------------------------------------------
*
*  Tarsos is developed by Joren Six at 
*  The School of Arts,
*  University College Ghent,
*  Hoogpoort 64, 9000 Ghent - Belgium
*  
* -----------------------------------------------------------
*
*  Info: http://tarsos.0110.be
*  Github: https://github.com/JorenSix/Tarsos
*  Releases: http://tarsos.0110.be/releases/Tarsos/
*  
*  Tarsos includes some source code by various authors,
*  for credits and info, see README.
* 
*/

package be.tarsos.ui.pitch;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import javax.swing.JButton;
import javax.swing.SwingUtilities;

import ptolemy.plot.Plot;
import be.tarsos.Tarsos;
import be.tarsos.sampled.pitch.Annotation;
import be.tarsos.sampled.pitch.AnnotationListener;
import be.tarsos.sampled.pitch.AnnotationPublisher;
import be.tarsos.sampled.pitch.AnnotationSelection;
import be.tarsos.sampled.pitch.Pitch;
import be.tarsos.sampled.pitch.PitchDetectionMode;
import be.tarsos.sampled.pitch.PitchUnit;
import be.tarsos.util.AudioFile;
import be.tarsos.util.ConfKey;
import be.tarsos.util.Configuration;

public class PitchContour extends Plot implements AudioFileChangedListener, ScaleChangedListener,
		AnnotationListener {

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
	private final WaveForm waveForm;
	
	public PitchContour(WaveForm waveForm) {
		this.waveForm = waveForm;
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
					final double hertzValue = PitchUnit.absoluteCentToHertz(tickValue);
					final double axisValue = Math.round(pitchUnit.convert(hertzValue,PitchUnit.HERTZ));
					final double tickPosition;
					// if the y axis is logarithmic then
					// set the marker at the correct position
					if (getYLog()) {
						tickPosition = convertToLog(axisValue);
					} else {
						tickPosition = axisValue;
					}
					LOG.finer(String.format("Added %s cents marker at position %s %s", tickValue,
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

		final String title;
		final String shortTitle;
		
		// not in live mode
		setXRange(0, newAudioFile.getLengthInMilliSeconds() / 1000.0);
		shortTitle = newAudioFile.originalBasename();
		
		title = String.format("%s - pitch in %s", shortTitle, pitchUnit.getHumanName());

		setButtons(true);

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

	boolean scaleIsChanging = false;

	public void scaleChanged(double[] newScale, boolean isChanging, boolean shiftHisto) {
		scaleIsChanging = isChanging;
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

	public void addAnnotation(Annotation annotation) {
		final double convertedPitch = annotation.getPitch(pitchUnit);
		final int dataset = annotation.getSource().ordinal();
		// LOG.finest(String.format("Added %.2f %s to pitch contour",
		// convertedPitch, pitchUnit.getHumanName()));
		addPoint(dataset, annotation.getStart(), convertedPitch, false);
		if (getLegend(dataset) == null) {
			addLegend(dataset, annotation.getSource().name());
			SwingUtilities.invokeLater(new Runnable() {
				public void run() {
					repaint();
				}
			});
		}
	}

	public void clearAnnotations() {
		// clear all data sets
		for (int i = 0; i < PitchDetectionMode.values().length; i++) {
			clear(i);
		}
	}

	public void extractionStarted() {
		// NO OP
	}

	public void extractionFinished() {
		// NO OP
	}

	@Override
	public void setYRange(double min, double max) {
		super.setYRange(min, max);

		if (!scaleIsChanging) {
			// This is dangerous: it expects that setXRange is called first,
			// i.e.
			// before setYRange()
			double minPitch = min;
			double maxPitch = max;
			double minTime = getXRange()[0];
			double maxTime = getXRange()[1];

			if (minTime < 0) {
				minTime = 0;
			}
			if (minPitch < 0) {
				minPitch = 0;
			}
			
			waveForm.setMarker(minTime, true);
			waveForm.setMarker(maxTime, false);
			
			AnnotationPublisher.getInstance().clear();
			AnnotationPublisher.getInstance().alterSelection(minTime, maxTime, minPitch, maxPitch);
			AnnotationPublisher.getInstance().delegateAddAnnotations(minTime, maxTime, minPitch, maxPitch);
			
		}
	}

	private static final int AMBITUS_STOP = Configuration.getInt(ConfKey.pitch_histogram_stop);

	public void annotationsAdded() {
		AnnotationSelection selection = AnnotationPublisher.getInstance().getCurrentSelection();
		if (selection.getStopTime() - selection.getStartTime() > 1) {

			super.setXRange(selection.getStartTime(), selection.getStopTime());

			final double stopPitch;
			// TODO make unit independent:
			if (selection.getStopPitch() > AMBITUS_STOP) {
				stopPitch = AMBITUS_STOP;
			} else {
				stopPitch = selection.getStopPitch();
			}

			super.setYRange(selection.getStartPitch(), stopPitch);
		}
	}
}
