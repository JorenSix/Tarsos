/**
*
*  Tarsos is developed by Joren Six at 
*  The Royal Academy of Fine Arts & Royal Conservatory,
*  University College Ghent,
*  Hoogpoort 64, 9000 Ghent - Belgium
*
**/
package be.hogent.tarsos.util.histogram;

/**
 * Crosscorrelation as used in Baris Bozkurt's 'An automatic pitch analysis
 * method for Turkish maquam music'. The measure is defined for modulo type
 * histograms!
 * 
 * @author Joren Six
 */
public final class CrossCorrelation implements HistogramCorrelation {
	public double correlation(final Histogram thisHistogam, final int displacement,
			final Histogram otherHistogram) {
		// number of bins (classes)
		final int numberOfClasses = thisHistogam.getNumberOfClasses();
		// start value
		final double start = thisHistogam.getStart();
		// stop value
		final double stop = thisHistogam.getStop();
		// classWidth
		final double classWidth = thisHistogam.getClassWidth();

		int actualDisplacement = displacement;
		// make displacement positive
		if (actualDisplacement < 0) {
			actualDisplacement = (actualDisplacement % numberOfClasses + numberOfClasses) % numberOfClasses;
		}

		double distance = 0.0;

		for (double current = start + classWidth / 2; current <= stop; current += classWidth) {
			final double displacedValue;
			displacedValue = (current + actualDisplacement * classWidth) % (numberOfClasses * classWidth);
			distance += thisHistogam.getCount(current) * otherHistogram.getCount(displacedValue);
		}

		return distance / numberOfClasses;
	}

	public void plotCorrelation(final Histogram thisHistogram, final int displacement,
			final Histogram otherHistogram, final String fileName, final String title) {
		new Intersection().plotCorrelation(thisHistogram, displacement, otherHistogram, fileName, title);
	}
}
