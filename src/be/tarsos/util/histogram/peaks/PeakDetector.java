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

package be.tarsos.util.histogram.peaks;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Vector;

import org.apache.commons.math.stat.StatUtils;

import be.tarsos.util.histogram.Histogram;
import be.tarsos.util.histogram.PitchClassHistogram;

/**
 * @author Joren Six
 */
public final class PeakDetector {

	private PeakDetector() {
	}
	
	public interface PeakDetectionStrategy{
		 /**
		 * @param histogram The histogram to detect peaks for
		 * @param windowSize The windowSize in number of bins.
		 * @param thresholdFactor A number between 0 and 100 that determines a threshold.
		 * @return A list of peaks in the histogram
		 */
		List<Peak> detect(final Histogram histogram, final int windowSize, final int thresholdFactor);
	}
	
	public static final PeakDetectionStrategy LOCALHEIGHTSCORE = new PeakDetectionStrategy() {
		
		
		public List<Peak> detect(Histogram histogram, int windowSize,
				int thresholdFactor) {
			// 1. CALCULATE SCORES
			final double[] localHeightScores = new double[histogram.getNumberOfClasses()];
			for (int i = 0; i < histogram.getNumberOfClasses(); i++) {
				final double currentValue = histogram.getCountForClass(i);
				final double previousValue = histogram.getCountForClass(i - 1);
				final double nextValue = histogram.getCountForClass(i + 1);

				final boolean isPeak = currentValue >= previousValue && currentValue >= nextValue;

				// Don't bother calculating the local height score if it is not a
				// peak. This is not strictly needed, only for performance.
				if (isPeak) {
					localHeightScores[i] = calculateLocalHeightScore(histogram, i, windowSize);
				}
			}

			// 2. TRESHOLD
			// Add the peaks to a list if the value is bigger than a threshold
			// value. This step is not strictly needed but it improves the
			// performance of the next steps.
			final double localHeightScoreTreshold = thresholdFactor/10.0;
			final List<Peak> peaks = new ArrayList<Peak>();
			for (int i = 0; i < histogram.getNumberOfClasses(); i++) {
				if (localHeightScores[i] > localHeightScoreTreshold) {
					peaks.add(new Peak(i, localHeightScores[i]));
				}
			}

			// 3. FILTER
			// Sort the peaks on local height score, descending.
			Collections.sort(peaks, new Comparator<Peak>() {
				public int compare(final Peak first, final Peak second) {
					final Double firstHeight = Double.valueOf(first.getHeight());
					final Double secondHeight = Double.valueOf(second.getHeight());
					return secondHeight.compareTo(firstHeight);
				}
			});
			// Remove peaks that are too close to each other.
			// If peaks are closer than the window size they are too close.
			// We are starting from the one with the best local height score and
			// remove the ones with lower scores (within the window).
			for (int i = 0; i < peaks.size(); i++) {
				final int firstPeakIndex = (int) peaks.get(i).getPosition();
				for (int j = i + 1; j < peaks.size(); j++) {
					final int secondPeakIndex = (int) peaks.get(j).getPosition();
					if (Math.abs(firstPeakIndex - secondPeakIndex) < windowSize) {
						peaks.remove(j);
						// Removed a peak, so shift index j;
						j--;
					}
				}
			}

			// 4. SANITIZE RESULTS
			// Correct the peak units. The caller expects position in <em>cents</em>
			// and height in <em>number of annotations</em> and not position as a
			// bin index and height as a local height score.
			for (int i = 0; i < peaks.size(); i++) {
				final Peak peak = peaks.get(i);
				final int peakIndex = (int) peak.getPosition();
				final double position = histogram.getKeyForClass(peakIndex);
				final double height = histogram.getCountForClass(peakIndex);
				peak.setPosition(position);
				peak.setHeight(height);
			}

			return peaks;
		}
	};
	
	public static final PeakDetectionStrategy ABSOLUTEHEIGHT = new PeakDetectionStrategy() {
		
		
		public List<Peak> detect(Histogram histogram, int windowSize,
				int thresholdFactor) {
			final double threshold = histogram.getMedian() * thresholdFactor / 10;
			
			final List<Peak> peaks = new ArrayList<Peak>();
			
			for (int i = 0; i < histogram.getNumberOfClasses(); i++) {
				final double currentValue = histogram.getCountForClass(i);
				final double previousValue = histogram.getCountForClass(i - 1);
				final double nextValue = histogram.getCountForClass(i + 1);

				final boolean isPeak = currentValue >= previousValue && currentValue >= nextValue && currentValue > threshold;
				

				// Don't bother calculating the local height score if it is not a
				// peak. This is not strictly needed, only for performance.
				if (isPeak) {
					peaks.add(new Peak(i, currentValue));
				}
			}
			
			Collections.sort(peaks);
			Collections.reverse(peaks);
			
			for (int i = 0; i < peaks.size(); i++) {
				final int firstPeakIndex = (int) peaks.get(i).getPosition();
				for (int j = i + 1; j < peaks.size(); j++) {
					final int secondPeakIndex = (int) peaks.get(j).getPosition();
					int diff = Math.abs(firstPeakIndex - secondPeakIndex);
					//wrapping code:
					int halfSize = histogram.getNumberOfClasses() / 2;
					if (diff > halfSize )
						diff = halfSize - (diff % halfSize);
					
					if (diff < windowSize) {
						peaks.remove(j);
						// Removed a peak, so shift index j;
						j--;
					}
				}
			}

			// 4. SANITIZE RESULTS
			// Correct the peak units. The caller expects position in <em>cents</em>
			// and height in <em>number of annotations</em> and not position as a
			// bin index and height as a local height score.
			for (int i = 0; i < peaks.size(); i++) {
				final Peak peak = peaks.get(i);
				final int peakIndex = (int) peak.getPosition();
				final double position = histogram.getKeyForClass(peakIndex);
				peak.setPosition(position);
			}
			
			return peaks;
		}
	};
	
	

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
		// final double[] peakHeights = new double[peaks.size()];
		for (int i = 0; i < peaks.size(); i++) {
			peakPositionsDouble[i] = peaks.get(i).getPosition();
			// peakHeights[i] = peaks.get(i).getHeight();
		}
		return PitchClassHistogram.createToneScale(peakPositionsDouble);
	}
	
	public static List<Peak> detect(final Histogram histogram, final int windowSize, final int thresholdFactor){
		return ABSOLUTEHEIGHT.detect(histogram, windowSize, thresholdFactor);
	}

	
	/**
	 * The local height score defines a measure for the height of a peak within
	 * a window. The score is the height minus the mean (height in the window)
	 * divided by the standard deviation (of the height in the window). The
	 * window size is defined in number of classes in the given histogram.
	 * 
	 * @param histogram
	 *            The histogram to calculate the local height score for.
	 * @param index
	 *            The index of the bin to calculate the score for.
	 * @param windowSize
	 *            The window size used to calculate the mean, standard deviation
	 *            and score.
	 * @return The local height score.
	 */
	public static double calculateLocalHeightScore(final Histogram histogram, final int index,
			final int windowSize) {
		assert windowSize % 2 != 0 : "Window size should be odd";
		assert windowSize >= 3 : "Window size should be minimum 3: a center value and one before and after.";

		int before = 0;
		int after = 0;
		// The range is defined by an element in the middle and a number of
		// elements before and after equal to floor(window size / 2)
		final double[] heightRange = new double[windowSize];
		int heightRangeIndex = 0;
		for (int j = 0; j < (windowSize - 1) / 2; j++) {
			before--;
			after++;
			heightRange[heightRangeIndex] = histogram.getCountForClass(index + before);
			heightRangeIndex++;
			heightRange[heightRangeIndex] = histogram.getCountForClass(index + after);
			heightRangeIndex++;
		}

		// Middle element.
		heightRange[heightRangeIndex] = histogram.getCountForClass(index);

		// Calculate mean and STD. Some useful trivia: Calculation is the worlds
		// most boring way of getting an STD. It is also almost the only way
		// computer scientists can get an STD.
		final double mean = StatUtils.mean(heightRange);
		final double standardDeviation = Math.pow(StatUtils.variance(heightRange, mean), 0.5);

		final double localHeightScore;
		if (standardDeviation == 0.0) {
			// If all values are equal the standard deviation is zero,
			// set the local height score as low as possible.
			localHeightScore = Double.NEGATIVE_INFINITY;
		} else {
			// Otherwise calculate the height score.
			localHeightScore = (histogram.getCountForClass(index) - mean) / standardDeviation;
		}
		return localHeightScore;
	}

	/**
	 * Finds the requested number of (most salient) peaks in the histogram.
	 * 
	 * @param histogram
	 *            The histogram.
	 * @param numberOfPeaks
	 *            The number of peaks.
	 * @return A list of peaks equal in size as the requested number of peaks.
	 */
	public static List<Peak> detectNumberOfPeaks(final Histogram histogram, final int numberOfPeaks) {
		final List<Peak> peaks = new ArrayList<Peak>();

		// 1. Calculate a list of peaks for each window size.
		HashMap<Integer, List<Peak>> peaksPerWindowSize = new HashMap<Integer, List<Peak>>();
		for (int i = 3; i < histogram.getNumberOfClasses() / 2; i += 2) {
			peaksPerWindowSize.put(i, detect(histogram, i,15));
		}

		// 2. Count the number of times each peak occurs (for each window size).
		HashMap<Double, Integer> peakPositionCount = new HashMap<Double, Integer>();
		for (int i = 5; i < histogram.getNumberOfClasses() / 2; i += 2) {
			for (Peak p : peaksPerWindowSize.get(i)) {
				Double key = p.getPosition();
				if (peakPositionCount.containsKey(key)) {
					int current = peakPositionCount.get(key);
					peakPositionCount.put(key, current + 1);
				} else {
					peakPositionCount.put(key, 1);
				}
			}
		}

		// 3. Order a list by the number of times a peak occurs (descending).
		List<Map.Entry<Double, Integer>> entryList = new Vector<Map.Entry<Double, Integer>>(
				peakPositionCount.entrySet());
		Collections.sort(entryList, new Comparator<Map.Entry<Double, Integer>>() {
			public int compare(final Entry<Double, Integer> firstEntry,
					final Entry<Double, Integer> secondEntry) {
				final Integer firstEntryCount = firstEntry.getValue();
				final Integer secondEntryCount = secondEntry.getValue();
				return secondEntryCount.compareTo(firstEntryCount);
			}
		});

		// 4. Adds the requested number of peaks to the list.
		for (int i = 0; i < numberOfPeaks; i++) {
			double positionOfPeak = entryList.get(i).getKey();
			double heightOfPeak = histogram.getCount(positionOfPeak);
			peaks.add(new Peak(positionOfPeak, heightOfPeak));
		}

		return peaks;
	}

	public static List<Peak> detect(final Histogram histogram) {
		List<Peak> peaks = new ArrayList<Peak>();

		// 1. Calculate a list of peaks for each window size.
		HashMap<Integer, List<Peak>> peaksPerWindowSize = new HashMap<Integer, List<Peak>>();
		for (int i = 3; i < histogram.getNumberOfClasses() / 5; i += 2) {
			peaksPerWindowSize.put(i, detect(histogram, i,15));
		}

		// 2. Count the number of times each number of detected peaks occurs
		// (for each window size).
		HashMap<Integer, Integer> peakPositionCount = new HashMap<Integer, Integer>();
		for (int i = 5; i < histogram.getNumberOfClasses() / 5; i += 2) {
			Integer key = peaksPerWindowSize.get(i).size();
			if (peakPositionCount.containsKey(key)) {
				int current = peakPositionCount.get(key);
				peakPositionCount.put(key, current + 1);
			} else {
				peakPositionCount.put(key, 1);
			}
		}

		// 3. Order the entries descending
		List<Map.Entry<Integer, Integer>> entryList = new Vector<Map.Entry<Integer, Integer>>(
				peakPositionCount.entrySet());
		Collections.sort(entryList, new Comparator<Map.Entry<Integer, Integer>>() {
			public int compare(final Entry<Integer, Integer> firstEntry,
					final Entry<Integer, Integer> secondEntry) {
				final Integer firstEntryCount = firstEntry.getValue();
				final Integer secondEntryCount = secondEntry.getValue();
				return secondEntryCount.compareTo(firstEntryCount);
			}
		});

		// 4. find the first peak list with the most likely number of peaks
		Integer mostLikelyNumberOfPeaks = entryList.get(0).getKey();
		for (int i = 3; i < histogram.getNumberOfClasses() / 5; i += 2) {
			if (peaksPerWindowSize.get(i).size() == mostLikelyNumberOfPeaks) {
				peaks = peaksPerWindowSize.get(i);
				break;
			}
		}

		return peaks;
	}

}
