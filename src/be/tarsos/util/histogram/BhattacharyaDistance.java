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
 * Bhattacharya histogram distance measure.
 * 
 * @author Joren Six
 */
public final class BhattacharyaDistance implements HistogramCorrelation {

    public double correlation(final Histogram first, final int displacement, final Histogram second) {
        // number of bins (classes)
        final int numberOfClasses = first.getNumberOfClasses();
        // start value
        final double start = first.getStart();
        // stop value
        final double stop = first.getStop();
        // classWidth
        final double classWidth = first.getClassWidth();

        int actualDisplacement = displacement;
        // make displacement positive
        if (actualDisplacement < 0) {
            actualDisplacement = ((actualDisplacement % numberOfClasses) + numberOfClasses) % numberOfClasses;
        }

        double distance = 0.0;

        for (double current = start + classWidth / 2; current <= stop; current += classWidth) {
            final double displacedValue = (current + actualDisplacement * classWidth)
            % (numberOfClasses * classWidth);
            distance += Math.pow(first.getCount(current) * second.getCount(displacedValue),
                    0.5);
        }

        return Math.log(distance);
    }

    public void plotCorrelation(final Histogram thisHistogram, final int displacement,
            final Histogram otherHistogram, final String fileName, final String title) {
        new Intersection().plotCorrelation(thisHistogram, displacement, otherHistogram, fileName, title);
    }
}
