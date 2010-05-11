package be.hogent.tarsos.util.histogram.peaks;

import be.hogent.tarsos.util.histogram.Histogram;

/**
 * @author Joren Six
 */
public interface PeakScore {
    double score(Histogram originalHistogram, int index, int windowSize);
}
