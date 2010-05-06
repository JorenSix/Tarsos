package be.hogent.tarsos.pitch;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.ListIterator;

import be.hogent.tarsos.util.histogram.AmbitusHistogram;

/**
 * 
 * A sample is a collection of pitches with corresponding probabilities that
 * starts at a certain time.
 * 
 * @author Joren Six
 */
public class Sample implements Comparable<Sample> {

    private long start;
    private final List<Double> pitches;// in Hz
    private final List<Double> probabilities;// values from 0 to 1 (inclusive).
    // One probability per pitch.

    /**
     * The source of the sample
     */
    public SampleSource source;

    /**
     * The source of the sample: the originating software and/or algorithm.
     * 
     */
    public enum SampleSource {
        IPEM, AUBIO_YIN, AUBIO_YINFFT, AUBIO_SCHMITT, AUBIO_FCOMB, AUBIO_MCOMB
    }

    /**
     * Create a new sample
     * 
     * @param start
     *            the starting time
     * @param pitches
     *            the pitches (one or more)
     * @param probabilities
     *            the probabilities corresponding with the pitches
     */
    public Sample(long start, List<Double> pitches, List<Double> probabilities) {
        this(start, pitches, probabilities, 0);
    }

    /**
     * Create a new sample
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
    public Sample(long start, List<Double> pitches, List<Double> probabilities,
            double minimumAcceptableProbability) {
        this(start);
        for (int i = 0; i < probabilities.size(); i++) {
            Double probability = probabilities.get(i);
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
    public Sample(long start, double pitch) {
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
    public Sample(long start) {
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
     * 
     * @return a reduced list without harmonics
     */
    public List<Double> getPitchesWithoutHarmonicsIn(PitchUnit unit, double errorPercentage) {

        if (pitches.size() == 1) {
            return pitches;
        }

        int numberOfHarmonicsToRemove = 5;
        List<Double> pitches = new ArrayList<Double>(this.pitches);
        List<Double> pitchesWithoutHarmonics = new ArrayList<Double>();
        Collections.sort(pitches);
        for (Double pitch : pitches) {
            boolean pitchIsHarmonic = false;
            for (int i = 2; i <= numberOfHarmonicsToRemove; i++) {
                Double pitchToCheck = pitch / i;
                Double deviation = pitchToCheck * errorPercentage;
                Double maxPitchLimit = pitchToCheck + deviation;
                Double minPitchLimit = pitchToCheck - deviation;
                for (Double pitchToCheckWith : pitches) {
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
     * Convert from the base unit (Hz) to another unit
     * 
     * @param unit
     *            the other unit
     * @return each pitch converted to the requested unit
     */
    public List<Double> getPitchesIn(PitchUnit unit) {
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
     * 
     * @param other
     *            the other sample
     * @param errorPercentage
     *            a percentage that defines when two pitches are the same if the
     *            <code>errorPercentage</code> is 0.05 than 95, 100 , 105 are
     *            perceived as being the same.
     */
    public void removeUniquePitches(Sample other, double errorPercentage) {
        // TODO use another scale instead of simple percentages.
        ListIterator<Double> thisPitchIterator = pitches.listIterator();
        while (thisPitchIterator.hasNext()) {
            Double thisPitch = thisPitchIterator.next();
            Double deviation = thisPitch * errorPercentage;
            Double maxPitchLimit = thisPitch + deviation;
            Double minPitchLimit = thisPitch - deviation;
            boolean removeThisPitch = other.pitches.size() != 0;
            for (Double otherPitch : other.pitches) {
                if (maxPitchLimit >= otherPitch && otherPitch >= minPitchLimit) {
                    removeThisPitch = false;
                }
            }
            if (removeThisPitch && pitches.size() > 1) {
                thisPitchIterator.remove();
            }
        }
    }

    public double returnMatchingPitch(Sample other, double errorPercentage) {
        if (pitches.size() == 0) {
            return Double.NEGATIVE_INFINITY;
        }

        Double thisPitch = pitches.get(0);
        Double deviation = thisPitch * errorPercentage;
        Double maxPitchLimit = thisPitch + deviation;
        Double minPitchLimit = thisPitch - deviation;

        for (Double otherPitch : other.pitches) {
            if (maxPitchLimit >= otherPitch && otherPitch >= minPitchLimit) {
                return (otherPitch + thisPitch) / 2;
            }
        }

        return Double.NEGATIVE_INFINITY;
    }

    public static AmbitusHistogram ambitusHistogram(List<Sample> samples) {
        AmbitusHistogram ambitusHistogram = new AmbitusHistogram();
        for (Sample sample : samples) {
            for (Double pitch : sample.getPitchesIn(PitchUnit.ABSOLUTE_CENTS)) {
                ambitusHistogram.add(pitch);
            }
        }
        return ambitusHistogram;
    }

    @Override
    public int compareTo(Sample o) {
        // starttime first
        int startCompare = Long.valueOf(start).compareTo(Long.valueOf(o.start));
        // then order by source name
        return startCompare == 0 ? source.toString().compareTo(o.source.toString()) : startCompare;
    }

    @Override
    public boolean equals(Object o) {
        boolean isEqual = false;
        if (o != null && o instanceof Sample) {
            Sample sample = (Sample) o;
            isEqual = start == sample.start && source.equals(sample.source);
        }
        return isEqual;
    }

}
