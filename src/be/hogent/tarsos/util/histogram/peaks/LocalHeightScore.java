package be.hogent.tarsos.util.histogram.peaks;

import org.apache.commons.math.stat.StatUtils;

import be.hogent.tarsos.util.histogram.Histogram;

/**
 * The local height score defines a measure for the height of a peak within a
 * window. The score is the height minus the mean (height in the window) divided
 * by the standard deviation (of the height in the window) with a lower bound of
 * zero. The window size is defined in number of classes in the given histogram.
 * 
 * @author Joren Six
 */
public final class LocalHeightScore implements PeakScore {

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * be.hogent.tarsos.util.histogram.peaks.PeakScore#score(be.hogent.tarsos
	 * .util.histogram.Histogram, int, int)
	 */
	public double score(final Histogram originalHistogram, final int index, final int windowSize) {
		int before = 0;
		int after = 0;
		final double[] heightRange = new double[windowSize * 2 + 1];
		int heightRangeIndex = 0;
		for (int j = 0; j < windowSize; j++) {
			before--;
			after++;
			heightRange[heightRangeIndex] = originalHistogram.getCountForClass(index + before);
			heightRangeIndex++;
			heightRange[heightRangeIndex] = originalHistogram.getCountForClass(index + after);
			heightRangeIndex++;
		}
		heightRange[heightRangeIndex] = originalHistogram.getCountForClass(index);

		final double mean = StatUtils.mean(heightRange);
		final double standardDeviation = Math.pow(StatUtils.variance(heightRange, mean), 0.5);
		final double heigthScore = (originalHistogram.getCountForClass(index) - mean) / standardDeviation;

		final double actualScore;
		if (heigthScore > 0) {
			actualScore = heigthScore;
		} else {
			actualScore = 0.0;
		}
		return actualScore;
	}
}
