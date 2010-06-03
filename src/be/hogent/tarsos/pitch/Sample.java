package be.hogent.tarsos.pitch;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.ListIterator;

import be.hogent.tarsos.util.histogram.AmbitusHistogram;

/**
 * A sample is a collection of pitches with corresponding probabilities that
 * starts at a certain time.
 * 
 * @author Joren Six
 */
public final class Sample implements Comparable<Sample> {

    private long start;
    private final List<Double> pitches; // in Hz
    private final List<Double> probabilities; // values from 0 to 1 (inclusive).
    // One probability per pitch.

    /**
     * The source of the sample.
     */
    public PitchDetectionMode source;


    /**
     * Create a new sample.
     * 
     * @param start
     *            the starting time
     * @param pitches
     *            the pitches (one or more)
     * @param probabilities
     *            the probabilities corresponding with the pitches
     */
    public Sample(final long start, final List<Double> pitches, final List<Double> probabilities) {
        this(start, pitches, probabilities, 0);
    }

    /**
     * Create a new sample.
     * 
     * @param start
     *            the starting time
     * @param pitches
     *            the pitches (one or more)
     * @param probabilities
     *            the probabilities corresponding with the pitches
     * @param minimumAcceptableProbability
     *            the minimum accepted probability for each pitch, values with a
     *            lower probability are discarded.
     */
    public Sample(final long start, final List<Double> pitches, final List<Double> probabilities,
            final double minimumAcceptableProbability) {
        this(start);
        for (int i = 0; i < probabilities.size(); i++) {
            final Double probability = probabilities.get(i);
            if (probability >= minimumAcceptableProbability) {
                this.pitches.add(pitches.get(i));
                this.probabilities.add(probability);
            }
        }
        this.start = start;
    }

    /**
     * Create a simple sample somple sumple semple. The only pitch has a
     * probability of 1.
     * 
     * @param start
     *            the starting time
     * @param pitch
     *            the pitch
     */
    public Sample(final long start, final double pitch) {
        this(start);
        this.pitches.add(pitch);
        this.probabilities.add(1.0);
    }

    /**
     * Create an empty sample: no pitch detected at this time.
     * 
     * @param start
     *            the starting time
     */
    public Sample(final long start) {
        this.start = start;
        this.pitches = new ArrayList<Double>();
        this.probabilities = new ArrayList<Double>();
    }

    /**
     * Returns a list of non harmonic frequencies. This function only makes
     * sense on polyphonic pitch samples.
     * 
     * @param unit
     * @param errorPercentage
     *            defines the limits of what is seen as a harmonic. The list
     *            [100Hz,202Hz,230Hz] is reduced to [100Hz,230Hz] if the error
     *            percentage is greater than or equal to 2% (0.02)
     * @return a reduced list without harmonics
     */
    public List<Double> getPitchesWithoutHarmonicsIn(final PitchUnit unit, final double errorPercentage) {

        if (pitches.size() == 1) {
            return pitches;
        }

        final int numberOfHarmonicsToRemove = 5;
        final List<Double> pitches = new ArrayList<Double>(this.pitches);
        final List<Double> pitchesWithoutHarmonics = new ArrayList<Double>();
        Collections.sort(pitches);
        for (final Double pitch : pitches) {
            boolean pitchIsHarmonic = false;
            for (int i = 2; i <= numberOfHarmonicsToRemove; i++) {
                final Double pitchToCheck = pitch / i;
                final Double deviation = pitchToCheck * errorPercentage;
                final Double maxPitchLimit = pitchToCheck + deviation;
                final Double minPitchLimit = pitchToCheck - deviation;
                for (final Double pitchToCheckWith : pitches) {
                    if (maxPitchLimit >= pitchToCheckWith && pitchToCheckWith >= minPitchLimit) {
                        // System.out.println(pitch + " is harmonic of " +
                        // pitchToCheckWith + ": " + maxPitchLimit + " >= " +
                        // pitchToCheckWith + " >= " + minPitchLimit);
                        pitchIsHarmonic = true;
                    }
                }
            }
            if (!pitchIsHarmonic) {
                pitchesWithoutHarmonics.add(pitch);
            }
        }
        return PitchFunctions.convertHertzTo(unit, pitchesWithoutHarmonics);
    }

    /**
     * Convert from the base unit (Hz) to another unit.
     * 
     * @param unit
     *            the other unit
     * @return each pitch converted to the requested unit
     */
    public List<Double> getPitchesIn(final PitchUnit unit) {
        return PitchFunctions.convertHertzTo(unit, pitches);
    }

    /**
     * @return the starting time of the sample
     */
    public long getStart() {
        return this.start;
    }

    /**
     * Removes pitches that are not present in both samples. The idea here is to
     * compare pitch annotations of the same musical piece at the same time by
     * different algorithms and only keep the annotations both agree on.
     * @param other
     *            the other sample
     * @param errorPercentage
     *            a percentage that defines when two pitches are the same if the
     *            <code>errorPercentage</code> is 0.05 than 95, 100 , 105 are
     *            perceived as being the same.
     */
    public final void removeUniquePitches(final Sample other, final double errorPercentage) {
        // TODO use another scale instead of simple percentages.
        final ListIterator<Double> thisPitchIterator = pitches.listIterator();
        while (thisPitchIterator.hasNext()) {
            final Double thisPitch = thisPitchIterator.next();
            final Double deviation = thisPitch * errorPercentage;
            final Double maxPitchLimit = thisPitch + deviation;
            final Double minPitchLimit = thisPitch - deviation;
            boolean removeThisPitch = !other.pitches.isEmpty();
            for (final Double otherPitch : other.pitches) {
                if (maxPitchLimit >= otherPitch && otherPitch >= minPitchLimit) {
                    removeThisPitch = false;
                }
            }
            if (removeThisPitch && pitches.size() > 1) {
                thisPitchIterator.remove();
            }
        }
    }

    public final double returnMatchingPitch(final Sample other, final double errorPercentage) {
        double matchingPitch = Double.NEGATIVE_INFINITY;
        if (!pitches.isEmpty()) {
            final Double thisPitch = pitches.get(0);
            final Double deviation = thisPitch * errorPercentage;
            final Double maxPitchLimit = thisPitch + deviation;
            final Double minPitchLimit = thisPitch - deviation;

            for (final Double otherPitch : other.pitches) {
                if (maxPitchLimit >= otherPitch && otherPitch >= minPitchLimit) {
                    matchingPitch = (otherPitch + thisPitch) / 2;
                    break;
                }
            }
        }

        return matchingPitch;
    }

    public static AmbitusHistogram ambitusHistogram(final List<Sample> samples) {
        final AmbitusHistogram ambitusHistogram = new AmbitusHistogram();
        for (final Sample sample : samples) {
            for (final Double pitch : sample.getPitchesIn(PitchUnit.ABSOLUTE_CENTS)) {
                ambitusHistogram.add(pitch);
            }
        }
        return ambitusHistogram;
    }

    @Override
    public int compareTo(final Sample o) {
        // starttime first
        final int startCompare = Long.valueOf(start).compareTo(Long.valueOf(o.start));
        // then order by source name
        return startCompare == 0 ? source.toString().compareTo(o.source.toString()) : startCompare;
    }

    @Override
    public final boolean equals(final Object o) {
        boolean isEqual = false;

        if (o != null && o instanceof Sample) {
            final Sample sample = (Sample) o;
            isEqual = start == sample.start && source.equals(sample.source);
        }
        return isEqual;
    }

    @Override
    public final String toString() {
        final String separator = "\t";
        final StringBuilder sb = new StringBuilder();
        sb.append(getStart() / 1000.0);
        sb.append(separator);
        final List<Double> hertzValues = this.getPitchesIn(PitchUnit.HERTZ);
        for (final Double hertz : hertzValues) {
            sb.append(hertz);
            sb.append(separator);
        }
        return sb.toString();
    }

    @Override
    public final int hashCode() {
        return Long.valueOf(start).hashCode() + source.hashCode();
    }

}
