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
* Tarsos is developed by Joren Six at IPEM, University Ghent
*  
* -----------------------------------------------------------
*
*  Info: http://tarsos.0110.be
*  Github: https://github.com/JorenSix/Tarsos
*  Releases: http://0110.be/releases/Tarsos/
*  
*  Tarsos includes some source code by various authors,
*  for credits, license and info: see README.
* 
*/



package be.tarsos.util.histogram;


/**
 * Implements a histogram intersection distance for modulo type histograms.
 * Works well on pitch frequency histograms according to Bozkurt
 * 
 * @author Joren Six
 */
public final class Intersection implements HistogramCorrelation {
	public double correlation(final Histogram thisHistogram, final int displacement,
			final Histogram otherHistogram) {

		// number of bins (classes)
		final int numberOfClasses = thisHistogram.getNumberOfClasses();
		// start value
		final double start = thisHistogram.getStart();
		// stop value
		final double stop = thisHistogram.getStop();
		// classWidth
		final double classWidth = thisHistogram.getClassWidth();

		int actualDisplacement = displacement;
		// make displacement positive
		if (actualDisplacement < 0) {
			actualDisplacement = (actualDisplacement % numberOfClasses + numberOfClasses) % numberOfClasses;
		}

		// matching area, displaced
		double matchingArea = 0.0;

		for (double current = start + classWidth / 2; current <= stop; current += classWidth) {
			final double displacedValue;
			displacedValue = (current + actualDisplacement * classWidth) % (numberOfClasses * classWidth);
			matchingArea += Math
					.min(thisHistogram.getCount(current), otherHistogram.getCount(displacedValue));
		}

		// the biggest area under the curve
		final double biggestHistogramArea = Math.max(thisHistogram.getSumFreq(), otherHistogram.getSumFreq());

		double correlation = 0.0;

		// avoid the dreaded division by 0
		if (matchingArea != 0.0) {
			correlation = matchingArea / biggestHistogramArea;
		}
		return correlation;
	}
}
