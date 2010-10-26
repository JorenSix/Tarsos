package be.hogent.tarsos.util.histogram.peaks;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import be.hogent.tarsos.util.histogram.Histogram;
import be.hogent.tarsos.util.histogram.ToneScaleHistogram;

/**
 * @author Joren Six
 */
public final class PeakDetector {

	private PeakDetector() {
	}

	/**
	 * Create a histogram with peak information. Instead of triangular peaks it
	 * creates a histogram (with the same resolution (bin widths) as the
	 * original) with peaks in the form of gaussian curves.
	 * 
	 * @param peaks
	 *            A list With Peaks
	 * @return a histogram with peak information. Can be used to match with
	 *         other histograms (files)
	 */
	public static Histogram newPeakDetection(final List<Peak> peaks) {
		final double[] peakPositionsDouble = new double[peaks.size()];
		final double[] peakHeights = new double[peaks.size()];
		for (int i = 0; i < peaks.size(); i++) {
			peakPositionsDouble[i] = peaks.get(i).getPosition();
			peakHeights[i] = peaks.get(i).getHeight();
		}
		return ToneScaleHistogram.createToneScale(peakPositionsDouble, peakHeights);
	}

	/**
	 * Detects peaks in a histogram. The peaks are positioned at places where
	 * DifferenceScore != 0 and HeigthScore is bigger than a certain threshold
	 * value.
	 * 
	 * @param histogram
	 * @param windowSize
	 *            Number of bins.
	 * @return
	 */
	public static List<Peak> detect(final Histogram histogram, final int windowSize) {
		final double[] peakFunctionValues = new double[histogram.getNumberOfClasses()];
		final PeakScore differenceScore = new DifferenceScore(histogram, windowSize);
		final PeakScore localHeightScore = new LocalHeightScore();
		for (int i = 0; i < histogram.getNumberOfClasses(); i++) {
			final double score = differenceScore.score(histogram, i, 1);
			// If the peak is a real peak according to the difference score,
			// then set the height score value.
			if (score != 0) {
				peakFunctionValues[i] = localHeightScore.score(histogram, i, windowSize);
			}
		}

		// add the peaks to a list if the value is bigger than a threshold
		// value.
		final List<Integer> peakPositions = new ArrayList<Integer>();
		for (int i = 0; i < histogram.getNumberOfClasses(); i++) {
			if (peakFunctionValues[i] > 0.5) {
				peakPositions.add(i);
			}
		}

		// Sort the peaks on position.
		Collections.sort(peakPositions);

		// Remove peaks that are to close to each other.
		// If peaks are closer than the window size they are too close.
		// The one with the smallest value is removed.
		final List<Integer> elementsToRemove = new ArrayList<Integer>();
		for (int i = 0; i < peakPositions.size(); i++) {
			final int firstPeakIndex = peakPositions.get(i);
			final int secndPeakIndex = peakPositions.get((i + 1) % peakPositions.size());
			if (Math.abs(secndPeakIndex - firstPeakIndex) <= windowSize) {
				final int position;
				if (firstPeakIndex > histogram.getCount(secndPeakIndex)) {
					position = peakPositions.get((i + 1) % peakPositions.size());
				} else {
					position = peakPositions.get(i);
				}
				elementsToRemove.add((int) histogram.getCount(position));
			}
		}
		peakPositions.removeAll(elementsToRemove);

		// wrap the peaks in objects.
		final List<Peak> peaks = new ArrayList<Peak>();
		for (int i = 0; i < peakPositions.size(); i++) {
			final double position = histogram.getKeyForClass(peakPositions.get(i));
			final double height = histogram.getCountForClass(peakPositions.get(i));
			peaks.add(new Peak(position, height));
		}
		return peaks;
	}
}
