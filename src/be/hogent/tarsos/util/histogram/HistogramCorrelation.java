package be.hogent.tarsos.util.histogram;

/**
 * @author Joren Six
 * Defines an implementation of a histogram correlation or distance measure 
 */
public interface HistogramCorrelation {	
	/**
	 * The implementation of a histogram correlation or distance measure
	 * @param thisHistogram the first histogram
	 * @param displacement the value to displace the otherHistogram (e.g. for optimal correlation, minimum distance between the two)
	 * @param otherHistogram the second (not displaced) histogram
	 * @return the correlation between a histogram and a displaced histogram
	 */
	double correlation(Histogram thisHistogram,int displacement,Histogram otherHistogram);

	/**
	 * Plots two histograms and the intersection between the two. 
	 * @param thisHistogram the first histogram
	 * @param displacement the value to displace the otherHistogram (e.g. for optimal correlation between the two)
	 * @param otherHistogram the second (not displaced) histogram
	 */
	void plotCorrelation(Histogram thisHistogram, int displacement, Histogram otherHistogram);
}
