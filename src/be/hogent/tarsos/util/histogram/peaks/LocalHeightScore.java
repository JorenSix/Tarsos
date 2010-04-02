package be.hogent.tarsos.util.histogram.peaks;

import org.apache.commons.math.stat.StatUtils;

import be.hogent.tarsos.util.histogram.Histogram;


public class LocalHeightScore implements PeakScore {
	
	
	@Override
	public double score(Histogram originalHistogram, int index, int windowSize) {
		int before = 0;
		int after = 0;
		double[] heightRange = new double[windowSize * 2 + 1];
		int heightRangeIndex = 0;
		for(int j = 0; j < windowSize; j++ ){
			before--;			
			after++;
			heightRange[heightRangeIndex] = originalHistogram.getCountForClass(index + before);
			heightRangeIndex++;
			heightRange[heightRangeIndex] = originalHistogram.getCountForClass(index + after);
			heightRangeIndex++;
		}
		heightRange[heightRangeIndex] = originalHistogram.getCountForClass(index);
		
		double mean = StatUtils.mean(heightRange);
		double standardDeviation = Math.pow(StatUtils.variance(heightRange,mean),0.5);
		double heigthScore = (originalHistogram.getCountForClass(index) - mean )/standardDeviation;
		
		return heigthScore > 0 ? heigthScore : 0.0;
	}
}
