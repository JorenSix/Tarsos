package be.hogent.tarsos.util.histogram.peaks;

import org.apache.commons.math.stat.StatUtils;

import be.hogent.tarsos.util.histogram.Histogram;

/**
 * @author Joren Six
 */
public final class LocalHeightScore implements PeakScore {

	@Override
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
