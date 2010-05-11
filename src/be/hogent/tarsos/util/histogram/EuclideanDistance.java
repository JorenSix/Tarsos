package be.hogent.tarsos.util.histogram;

/**
 * Euclidean Distance (L2 norm) for modulo type histograms.
 * 
 * @author Joren Six
 */
public class EuclideanDistance implements HistogramCorrelation {

    public double correlation(Histogram thisHistogam, int displacement, Histogram otherHistogram) {
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
            displacement = ((displacement % numberOfClasses) + numberOfClasses) % numberOfClasses;
        }

        double distance = 0.0;

        for (double current = start + classWidth / 2; current <= stop; current += classWidth) {
            double displacedValue = (current + displacement * classWidth) % (numberOfClasses * classWidth);
            distance += Math.pow(thisHistogam.getCount(current) - otherHistogram.getCount(displacedValue), 2);
        }

        return -1 * Math.pow(distance, 0.5);
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * tarsos.util.histogram.HistogramCorrelation#plotCorrelation(tarsos.util
     * .histogram.Histogram, int, tarsos.util.histogram.Histogram)
     */
    public void plotCorrelation(Histogram thisHistogram, int displacement, Histogram otherHistogram) {
        // for the moment this plots the intersection (not the euclidean
        // distance)
        new Intersection().plotCorrelation(thisHistogram, displacement, otherHistogram);
    }

}
