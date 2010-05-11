package be.hogent.tarsos.util.histogram;

/**
 * 
 * Crosscorrelation as used in Baris Bozkurt's 'An automatic pitch analysis
 * method for Turkish maquam music'. The measure is defined for modulo type
 * histograms!
 * 
 * @author Joren Six
 */
public class CrossCorrelation implements HistogramCorrelation {
    public double correlation(Histogram thisHistogam, int displacement,
            Histogram otherHistogram) {
        // number of bins (classes)
        int numberOfClasses = thisHistogam.getNumberOfClasses();
        // start value
        double start = thisHistogam.getStart();
        // stop value
        double stop = thisHistogam.getStop();
        // classWidth
        double classWidth = thisHistogam.getClassWidth();

        // make displacement positive
        if (displacement < 0) {
            displacement = ((displacement % numberOfClasses) + numberOfClasses)
                    % numberOfClasses;
        }

        double distance = 0.0;

        for (double current = start + classWidth / 2; current <= stop; current += classWidth) {
            double displacedValue = (current + displacement * classWidth)
                    % (numberOfClasses * classWidth);
            distance += thisHistogam.getCount(current)
                    * otherHistogram.getCount(displacedValue);
        }

        return distance / numberOfClasses;
    }

    public void plotCorrelation(Histogram thisHistogram, int displacement,
            Histogram otherHistogram) {
        new Intersection().plotCorrelation(thisHistogram, displacement,
                otherHistogram);
    }
}
