package be.hogent.tarsos.util.histogram;

import be.hogent.tarsos.util.Configuration;
import be.hogent.tarsos.util.Configuration.Config;

public class HistogramFunction {
	
	private HistogramFunction(){
		
	}
	
	public static Histogram mostEnergyRichOctaves(Histogram range, int numberOfOctaves){
		
		Histogram h = new Histogram(Configuration.getDouble(Config.histogram_bin_width),0,1200*numberOfOctaves);
		double mostEnergyRichInterval = -1.0;
		double startPositionEnergyRichOctaves = -1.0;
		
		for(double current  = range.getStart() + range.getClassWidth()/2;current <= range.getStop() ;current += range.getClassWidth()){
			double smallValue = range.getCumPct(current);
			double largerValue = range.getCumPct(current + h.getStop());
			double interval = largerValue - smallValue;
			if(interval > mostEnergyRichInterval){
				mostEnergyRichInterval = interval;
				startPositionEnergyRichOctaves = current;
			}
		}
		
		double stopPositionEnergyRichOctaves = startPositionEnergyRichOctaves + 1200 * numberOfOctaves;
		for(double current  = startPositionEnergyRichOctaves;current <= stopPositionEnergyRichOctaves ;current += range.getClassWidth()){
			for(int i = 0;i<range.getCount(current);i++)
				h.add(current-startPositionEnergyRichOctaves);
		}
		
		return h;
	}
	
	
	/**
	 * Creates a theoretical tone scale using a mixture of gaussian functions
	 * See 'an automatic pitch analysis method for turkish maqam music'.
	 * WARNING: values are not wrapped: a peak on 0 with with 25 does not flow over in 1200!!!
	 * TODO Wrap values around
	 * Uses a histogram with a configured bin width (Config.histogram_bin_width).
	 * @param peaks the position of the peaks. The position is defined in cents. 
	 * @param heights the heights of the peaks. If null the heights for all peaks is 200.
	 * @param widths the widths of the peaks in cents. If null the width is 25 cents for all peaks. The width is measured at a certain height. The mean? 
	 * @param standardDeviations defines the shape of the peak. If null the standarddeviation is 1. Bigger values give a wider peak.
	 * @return a histogram with values from 0 to 1200 and the requested peaks.
	 */
	public static Histogram createToneScale(double[] peaks,double[] heights, double[] widths,double[] standardDeviations){
		Histogram h = new Histogram(0,1200,200);
		
		if(heights == null){
			heights = new double[peaks.length];
			for(int i =0;i<peaks.length;i++)
				heights[i] = 200.0;
		}
		
		if(standardDeviations == null){
			standardDeviations = new double[peaks.length];
			for(int i =0;i<peaks.length;i++)
				standardDeviations[i] = 1;
		}
		
		if(widths == null){
			widths = new double[peaks.length];
			for(int i =0;i<peaks.length;i++)
				widths[i] = 25;
		}
		
		for(Double key : h.keySet()){
			double currentValue = 0.0;
			for(int i=0;i<peaks.length;i++){
				double power = Math.pow((key - peaks[i])/(widths[i]/2 * standardDeviations[i]),2.0);
				currentValue += heights[i] * Math.pow(Math.E,-0.5 * power );	
			}
			h.setCount(key, Math.round(currentValue));
		}
		return h;
	}
	
	/**
	 * Folds a histogram to one octave. 
	 * Use this to go from a range of cents to an octave: [0-1200] cents.
	 * This method only makes sense on pitch range histograms containing cent values!
	 * @return a new folded histogram.
	 */
	public static Histogram fold(Histogram range){
		Histogram foldedHistogram = new Histogram(0,1200,200);
		for(double rangeKey : range.keySet()){
			double foldKey = rangeKey % 1200;
			long count =  foldedHistogram.getCount(foldKey) + range.getCount(rangeKey);
			foldedHistogram.setCount(foldKey, count);
		}
		return foldedHistogram;
	}
}
