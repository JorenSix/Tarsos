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

package be.tarsos.sampled.pitch;

import java.util.List;
import java.util.Locale;


/**
 * An annotation has one time stamp, one pitch and an optional probability. Also
 * the source of the annotation needs to be defined.
 * 
 * @author Joren Six
 */
public final class Annotation implements Comparable<Annotation> {

	/**
	 * The time of the annotation in seconds.
	 */
	private final double start;

	/**
	 * A lookup table with pitch values in different units. The calculation is
	 * cached to prevent a lot of unnecessary unit conversion calculations.
	 */
	private final double[] pitchValues;

	/**
	 * The probability or salience. A value between zero and one (inclusive).
	 */
	private final double probability;

	/**
	 * The source of the sample.
	 */
	private final PitchDetectionMode source;

	/**
	 * Create a new annotation with the given data.
	 * @param timeStamp
	 *            The starting time (in seconds).
	 * @param pitchInHz
	 *            The pitch in Hz.
	 * @param annotationSource
	 *            The source of the annotation.
	 * @param salience
	 *            A probability between zero and one (inclusive) defining the
	 *            saliance of the pitch.
	 */
	public Annotation(final double timeStamp, final double pitchInHz,
			final PitchDetectionMode annotationSource, final double salience) {
		// sanity check
		if (salience > 1.0 || 0.0 > salience) {
			throw new IllegalArgumentException(
					"The salience should be a value between zero and one (inclusive): " + salience);
		}
		if (pitchInHz <= 0) {
			throw new IllegalArgumentException("The pitch in Hz should be a value above zero, it is: "
					+ pitchInHz);
		}
		if (timeStamp < 0) {
			throw new IllegalArgumentException(
					"The timestamp in seconds should be equal or above zero, it is: " + timeStamp);
		}

		// Initialize members.
		this.start = timeStamp;
		this.probability = salience;
		this.source = annotationSource;

		// Pitch object to calculate unit conversions.
		final Pitch pitch = Pitch.getInstance(PitchUnit.HERTZ, pitchInHz);

		// Cache pitch in different units.
		this.pitchValues = new double[PitchUnit.values().length];
		for (int i = 0; i < pitchValues.length; i++) {
			pitchValues[i] = pitch.getPitch(PitchUnit.values()[i]);
		}

	}

	/**
	 * Create a new annotation with the given data. The default salience of 1.0 is used.
	 * @param timeStamp
	 *            The starting time (in seconds).
	 * @param pitchInHz
	 *            The pitch in Hz.
	 * @param annotationSource
	 *            The source of the annotation.
	 */
	public Annotation(final double timeStamp, final double pitchInHz,
			final PitchDetectionMode annotationSource) {
		this(timeStamp, pitchInHz, annotationSource, 1.0);
	}

	/**
	 * Returns the starting time in seconds.
	 * 
	 * @return The starting time of the sample (in seconds).
	 */
	public double getStart() {
		return this.start;
	}

	public PitchDetectionMode getSource() {
		return source;
	}

	/**
	 * The probability for the annotation. If the algorithm does not define a
	 * probability the default is 1.0.
	 * 
	 * @return The probability or salience for the annotation (a value between 0
	 *         and 1).
	 */
	public double getProbability() {
		return probability;
	}

	/**
	 * Return the pitch in the requested unit. The conversion is cached
	 * automatically.
	 * 
	 * @param unit
	 *            The unit requested.
	 * @return The converted value.
	 */
	public double getPitch(final PitchUnit unit) {
		return pitchValues[unit.ordinal()];
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Comparable#compareTo(java.lang.Object)
	 */
	public int compareTo(final Annotation o) {
		// start time first
		final int startCompare = Double.valueOf(start).compareTo(Double.valueOf(o.start));
		final int compareValue;
		// then order by source name
		if (startCompare == 0) {
			compareValue = getSource().toString().compareTo(o.getSource().toString());
		} else {
			compareValue = startCompare;
		}
		return compareValue;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	@Override
	public boolean equals(final Object o) {
		boolean isEqual = false;
		if (o != null && o instanceof Annotation) {
			final Annotation sample = (Annotation) o;
			isEqual = start == sample.start && getSource().equals(sample.getSource());
		}
		return isEqual;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode() {
		return Double.valueOf(start).hashCode() + getSource().hashCode();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return String.format(Locale.US, "%.5f,%.5f,%.5f,%s", start, getPitch(PitchUnit.HERTZ), probability,source);
	}

	/**
	 * Parses an annotation as written by the toString() method.
	 * 
	 * @param line
	 *            The line to parse.
	 * @return A new annotation.
	 */
	public static Annotation parse(final String line) {
		final String[] data = line.split(",");
		final double timeStamp = Double.parseDouble(data[0]);
		final double pitch = Double.parseDouble(data[1]);
		final double probability = Double.parseDouble(data[2]);
		final PitchDetectionMode source = PitchDetectionMode.valueOf(data[3]);
		return new Annotation(timeStamp, pitch, source, probability);

	}

	/**
	 * Calculates a maximum possible number of annotations and compares it with
	 * the actual number of annotations. The result is an indication how much of
	 * the audio is annotated and therefore possibly how 'pitched' the original
	 * audio is (or how easily it can be annotated using the chosen algorithm).
	 * E.g. ten actual annotations from YIN at a sample rate of 100HZ with an
	 * audio file of two seconds gives 10 /(2s x 100Hz) = 5%
	 * 
	 * @param annotations
	 *            A list of annotations for an audio file.
	 * @param audioLenght
	 *            The length of the annotated audio.
	 * @return A percentage indicating how much of the audio is annotated
	 */
	public static double percentAnnotated(final List<Annotation> annotations, final double audioLenght) {
		double percentage;
		double delta = Double.MAX_VALUE;
		double previousTimeStamp = 0;
		for (Annotation annotation : annotations) {
			double currentTimeStamp = annotation.getStart();
			delta = Math.min(delta, currentTimeStamp - previousTimeStamp);
			previousTimeStamp = currentTimeStamp;
		}
		int actualAnnotations = annotations.size();
		int maxAnnotations = (int) (audioLenght / delta);
		percentage = actualAnnotations / (double) maxAnnotations;
		return percentage;
	}
}
