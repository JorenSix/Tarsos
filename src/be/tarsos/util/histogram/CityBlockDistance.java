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

package be.tarsos.util.histogram;

/**
 * City Block (L1 distance) for modulo type histograms. Works good on pitch
 * frequency histograms according to Bozkurt
 * 
 * @author Joren Six
 */
public final class CityBlockDistance implements HistogramCorrelation {

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
			final double displacedValue = (current + actualDisplacement * classWidth)
					% (numberOfClasses * classWidth);
			distance += Math.abs(thisHistogam.getCount(current) - otherHistogram.getCount(displacedValue));
		}

		return -1 * distance / thisHistogam.getSumFreq() + 1;
	}

	public void plotCorrelation(final Histogram thisHistogram, final int displacement,
			final Histogram otherHistogram, final String fileName, final String title) {
		new Intersection().plotCorrelation(thisHistogram, displacement, otherHistogram, fileName, title);
	}

}
