package be.hogent.tarsos.util.histogram;

/**
 * @author Joren Six
 * City Block (L1 distance) for modulo type histograms.
 * 
 * Works good on pitch frequency histograms according to Bozkurt
 *
 */
public class CityBlockDistance implements HistogramCorrelation {

	@Override
	public double correlation(Histogram thisHistogam, int displacement,
			Histogram otherHistogram) {
		//number of bins (classes)
		int numberOfClasses = thisHistogam.getNumberOfClasses();
		//start value
		double start = thisHistogam.getStart();
		//stop value
		double stop = thisHistogam.getStop();
		//classWidth
		double classWidth = thisHistogam.getClassWidth();
		
		//make displacement positive
		if(displacement < 0)
			displacement = ((displacement % numberOfClasses) + numberOfClasses) % numberOfClasses;
		
		double distance = 0.0;

		for(double current  = start + classWidth/2;current <= stop;current += classWidth){
			double displacedValue = (current + displacement * classWidth) % (numberOfClasses * classWidth);
			distance += Math.abs(thisHistogam.getCount(current) - otherHistogram.getCount(displacedValue));
		}
		
		return -1*(distance/thisHistogam.getSumFreq())+1;
	}

	public void plotCorrelation(Histogram thisHistogram, int displacement, Histogram otherHistogram) {
		new Intersection().plotCorrelation(thisHistogram, displacement, otherHistogram);
	}
	

}
