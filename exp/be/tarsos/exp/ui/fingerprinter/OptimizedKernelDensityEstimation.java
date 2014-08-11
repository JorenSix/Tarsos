/**
*
*  Tarsos is developed by Joren Six at 
*  The Royal Academy of Fine Arts & Royal Conservatory,
*  University College Ghent,
*  Hoogpoort 64, 9000 Ghent - Belgium
*
**/
package be.tarsos.exp.ui.fingerprinter;

import java.util.ArrayList;
import java.util.List;



import be.tarsos.util.KernelDensityEstimate;

public class OptimizedKernelDensityEstimation extends KernelDensityEstimate {
	
	private int[] fastAccumulator;
	private int[] peaks;
	
	
	public OptimizedKernelDensityEstimation(Kernel kernel, int size) {
		super(kernel, size);
		peaks = new int[0];
	}
	
	
	public void populateFastAccumulator(){
		fastAccumulator = new int[accumulator.length];
		for(int i = 0 ; i < accumulator.length ;i++){
			fastAccumulator[i] = (int) Math.round(accumulator[i]);
		}
	}
	
	public void populatePeaks(){
		List<Integer> thisPeaks = new ArrayList<Integer>();
		for(int i = 0 ; i < accumulator.length ; i++){
			double thisValue = accumulator[i];
			double nextValue = accumulator[(accumulator.length + i + 1) % accumulator.length];
			double previousValue = accumulator[(accumulator.length + i - 1) % accumulator.length];
			if(previousValue < thisValue && thisValue > nextValue){
				thisPeaks.add(i);
			}
		}
		
		peaks = new int[thisPeaks.size()];
		for(int i = 0 ; i <peaks.length ; i++){
			peaks[i] = thisPeaks.get(i);
		}
	}
	
	private double fastOptimalCorrelation(final OptimizedKernelDensityEstimation other) {
		double maximumCorrelation = -1; // best found correlation
		double biggestKDEArea = Math.max(getSumFreq(), other.getSumFreq());
		//slow
		for (int i = 0; i < size(); i++) {
			int shift = i;
			final double currentCorrelation = fastCorrelation(other, shift, biggestKDEArea);
			maximumCorrelation = Math.max(currentCorrelation,maximumCorrelation);
		}		
		return maximumCorrelation;
	}
	
	
	
	private double fastCorrelation(final OptimizedKernelDensityEstimation other,
			final int positionsToShiftOther,double biggestKDEArea) {
		double correlation;
		int matchingArea = 0;
		for (int i = 0; i < fastAccumulator.length; i++) {
			int otherIndex = (other.size() + i + positionsToShiftOther) % other.size();
			matchingArea += Math.min(fastAccumulator[i],other.fastAccumulator[otherIndex]);
		}
		correlation = matchingArea / biggestKDEArea;
		return correlation;
	}
	
	/**
	 * Calculates the optimal correlation between two Kernel Density Estimates
	 * by shifting and searching for optimal correlation.
	 * 
	 * @param other
	 *            The other KernelDensityEstimate.
	 * @return A value between 0 and 1 representing how similar both estimates
	 *         are. 1 means total correlation, 0 no correlation.
	 */
	public double optimalCorrelation(final KernelDensityEstimate other) {
		assert other.size() == size() : "The kde size should be the same!";
		OptimizedKernelDensityEstimation otherOptimized = (OptimizedKernelDensityEstimation) other;
		populateFastAccumulator();
		otherOptimized.populateFastAccumulator();
		populatePeaks();
		otherOptimized.populatePeaks();
		return fastOptimalCorrelation(otherOptimized);
	}
	
	public double getMean(){
		return getSumFreq()/(double)accumulator.length;
	}
	
	public double getStandardDeviation(){
		double mean = getMean();
		double standardDeviation = 0;
		for(int i = 0 ; i < accumulator.length ; i++){
			double difference = accumulator[i] - mean;
			standardDeviation += difference * difference;
		}
		standardDeviation = Math.pow(standardDeviation/(double)accumulator.length,0.5);
		return standardDeviation;
	}
	
	public List<Peak> detectPeaks(int windowSize, double thresholdFactor){
		List<Peak> peaks = new ArrayList<Peak>();
		double[] peakScore = new double[accumulator.length];
		for(int i = 0 ; i < accumulator.length ; i++){
			//only calculate scores for peaks...
			//if(accumulator[i] > accumulator[(accumulator.length + i + 1) % accumulator.length] && accumulator[i] > accumulator[(accumulator.length + i - 1) % accumulator.length]){
				double before = 0;
				double after = 0;
				for(int j = 1; j <= windowSize ; j++){
					after  +=  accumulator[i] - accumulator[(accumulator.length + i + j) % accumulator.length];
					before +=  accumulator[i] - accumulator[(accumulator.length + i - j) % accumulator.length];                                    
				}
				peakScore[i] = ((before/(double)windowSize)+(after/(double)windowSize))/2.0;
			//}else{
			//	peakScore[i] = -1;
			//}
			
		}
		
		double mean = getSumFreq()/accumulator.length;
		double standardDeviation = getStandardDeviation();
		for(int i = 0 ; i < accumulator.length ; i++){
			if(peakScore[i] > 0 && (peakScore[i] - mean) > (thresholdFactor * standardDeviation)){
				peaks.add(new Peak(i, accumulator[i]));
			}
		}
		
		for (int i = 0; i < peaks.size() + peaks.size() / 2 ; i++) {
			int firstPeakI = i % peaks.size();
			int secndPeakI = (i + 1) % peaks.size();
			if(firstPeakI != secndPeakI){
				final int firstPeakIndex = (int) peaks.get(firstPeakI).getIndex();
				final int secndPeakIndex = (int) peaks.get(secndPeakI).getIndex();
				int diff = Math.abs(firstPeakIndex - secndPeakIndex);
				//wrapping behaviour:
				int halfLength = accumulator.length / 2;
				if (diff > halfLength )
					diff = halfLength - (diff % halfLength);
				if(diff < windowSize){
					if(peaks.get(firstPeakI).getSize() > peaks.get(secndPeakI).getSize()){
						peaks.remove(secndPeakI);
					}else{
						peaks.remove(firstPeakI);
					}
					//evaluate next
					i--;
				}
			}
			
		}
		
		return peaks;
	}
	
	
	public static class Peak{
		final int index;
		final double size;
		Peak(int index,double size){
			this.index = index;
			this.size = size;
		}
		
		public int compareTo(Peak o) {
			return Double.valueOf(size).compareTo(o.size);
		}
		
		public int getIndex(){
			return index;
		}
		
		public double getSize(){
			return size;
		}
	}
}
