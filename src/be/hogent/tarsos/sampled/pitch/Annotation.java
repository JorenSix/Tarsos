package be.hogent.tarsos.sampled.pitch;

import java.util.List;
import java.util.Locale;

import be.hogent.tarsos.util.histogram.AmbitusHistogram;

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
	 * The pitch: an object defining a value in Hz.
	 */
	private final Pitch pitch;

	/**
	 * The probability or salience. A value between zero and one (inclusive).
	 */
	private final double probability; // values from 0 to 1 (inclusive).
	// One probability per pitch.

	/**
	 * The source of the sample.
	 */
	private final PitchDetectionMode source;

	/**
	 * Create a new annotation.
	 * 
	 * @param timeStamp
	 * 
	 * @param pitchList
	 *            the pitches: one or more, in Hz).
	 * @param probabilityList
	 *            The probabilities corresponding with the pitches [0,1].
	 */
	/**
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
		this.start = timeStamp;
		this.pitch = Pitch.getInstance(PitchUnit.HERTZ, pitchInHz);
		this.probability = salience;
		this.source = annotationSource;
		if (salience > 1.0 || 0.0 > salience) {
			throw new IllegalArgumentException(
					"The salience should be a value between zero and one (inclusive).");
		}
	}

	public Annotation(final double timeStamp, final double pitchInHz,
			final PitchDetectionMode annotationSource) {
		this(timeStamp, pitchInHz, annotationSource, 1.0);
	}

	/**
	 * @return the starting time of the sample in ms
	 */
	public double getStart() {
		return this.start;
	}

	public PitchDetectionMode getSource() {
		return source;
	}

	public double getProbability() {
		return probability;
	}

	public Pitch getPitch() {
		return pitch;
	}

	public double getPitch(final PitchUnit unit) {
		return pitch.getPitch(unit);
	}

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

	@Override
	public boolean equals(final Object o) {
		boolean isEqual = false;
		if (o != null && o instanceof Annotation) {
			final Annotation sample = (Annotation) o;
			isEqual = start == sample.start && getSource().equals(sample.getSource());
		}
		return isEqual;
	}

	@Override
	public int hashCode() {
		return Double.valueOf(start).hashCode() + getSource().hashCode();
	}

	@Override
	public String toString() {
		return String.format(Locale.US, "%.5f,%.5f,%.5f,%s", start, getPitch(PitchUnit.HERTZ), probability,
				source);
	}

	public static Annotation parse(final String line) {
		final String[] data = line.split(",");
		final double timeStamp = Double.parseDouble(data[0]);
		final double pitch = Double.parseDouble(data[1]);
		final double probability = Double.parseDouble(data[2]);
		final PitchDetectionMode source = PitchDetectionMode.valueOf(data[3]);
		return new Annotation(timeStamp, pitch, source, probability);

	}

	public static AmbitusHistogram ambitusHistogram(final List<Annotation> annotations) {
		final AmbitusHistogram ambitusHistogram = new AmbitusHistogram();
		for (final Annotation annotation : annotations) {
			ambitusHistogram.add(annotation.getPitch(PitchUnit.ABSOLUTE_CENTS));
		}
		return ambitusHistogram;
	}
}
