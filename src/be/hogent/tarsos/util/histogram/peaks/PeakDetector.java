package be.hogent.tarsos.util.histogram.peaks;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import be.hogent.tarsos.util.histogram.Histogram;
import be.hogent.tarsos.util.histogram.ToneScaleHistogram;


/**
 * @author Joren Six
 *
 */
public class PeakDetector {

	private PeakDetector(){}

	/**
	 * Create a histogram with peak information. Instead of triangular peaks it creates a histogram (with the
	 * same resolution (bin widths) as the original) with peaks in the form of gaussian curves.
	 * @param histogram the histogram to detect peaks for
	 * @param windowSize the windows to detect peaks in.
	 * @param meanFactorThreshold the threshold when peaks are accepted.
	 * @return a histogram with peak information. Can be used to match with other histograms (files)
	 */
	public static Histogram newPeakDetection(List<Peak> peaks){
		double peakPositionsDouble[] = new double[peaks.size()];
		double peakWidths[] = null;
		double peakHeights[] = new double[peaks.size()];
		double peakStandardDeviations[] = null;
		for(int i=0 ; i < peaks.size();i++){
			peakPositionsDouble[i] = peaks.get(i).getPosition();
			peakHeights[i] = peaks.get(i).getHeight();
		}
		return ToneScaleHistogram.createToneScale(peakPositionsDouble, peakHeights, peakWidths, peakStandardDeviations);
	}

	/**
	 * Detects peaks in a histogram. The peaks are positioned at places where
	 * DifferenceScore != 0 and HeigthScore is bigger than a certain threshold
	 * value.
	 *
	 * @param histogram
	 * @param windowSize in number of bins
	 * @param meanFactorThreshold
	 * @return
	 */
	public static List<Peak> detect(Histogram histogram, int windowSize, double meanFactorThreshold){
		double peakFunctionValues[] = new double[histogram.getNumberOfClasses()];
		PeakScore differenceScore = new DifferenceScore(histogram,windowSize);
		PeakScore localHeightScore = new LocalHeightScore();
		for(int i = 0 ; i < histogram.getNumberOfClasses(); i++){
			double score = differenceScore.score(histogram,i, 1);
			//If the peak is a real peak according to the difference score,
			//then set the height score value.
			if(score != 0) {
				peakFunctionValues[i] = localHeightScore.score(histogram,i, windowSize);
			}
		}

		//add the peaks to a list if the value is bigger than a threshold value.
		List<Integer> peakPositions = new ArrayList<Integer>();
		for(int i = 0;i<histogram.getNumberOfClasses();i++){
			if(peakFunctionValues[i]>meanFactorThreshold){
				peakPositions.add(i);
			}
		}

		//Sort the peaks on position.
		Collections.sort(peakPositions);

		//Remove peaks that are to close to each other.
		//If peaks are closer than the window size they are too close.
		//The one with the smallest value is removed.
		List<Integer> elementsToRemove = new ArrayList<Integer>();
		for(int i=0 ; i < peakPositions.size();i++){
			int firstPeakIndex = peakPositions.get(i);
			int secndPeakIndex = peakPositions.get(((i+1) % peakPositions.size()));
			if(Math.abs(secndPeakIndex - firstPeakIndex) <= windowSize)
				elementsToRemove.add(histogram.getCount(firstPeakIndex) > histogram.getCount(secndPeakIndex) ?
						peakPositions.get((i+1) % peakPositions.size()):
						peakPositions.get(i));
		}
		peakPositions.removeAll(elementsToRemove);

		//wrap the peaks in objects.
		List<Peak> peaks = new ArrayList<Peak>();
		for(int i=0 ; i < peakPositions.size();i++){
			double position = histogram.getKeyForClass(peakPositions.get(i));
			double height = histogram.getCountForClass(peakPositions.get(i));
			peaks.add(new Peak(position, height));
		}
		return peaks;
	}
}
