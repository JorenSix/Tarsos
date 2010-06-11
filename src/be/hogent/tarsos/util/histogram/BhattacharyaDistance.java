package be.hogent.tarsos.util.histogram;

/**
 * Bhattacharya histogram distance measure.
 * 
 * @author Joren Six
 */
public final class BhattacharyaDistance implements HistogramCorrelation {

    public double correlation(final Histogram thisHistogam, final int displacement, final Histogram otherHistogram) {
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
            actualDisplacement = ((actualDisplacement % numberOfClasses) + numberOfClasses) % numberOfClasses;
        }

        double distance = 0.0;

        for (double current = start + classWidth / 2; current <= stop; current += classWidth) {
            final double displacedValue = (current + actualDisplacement * classWidth)
            % (numberOfClasses * classWidth);
            distance += Math.pow(thisHistogam.getCount(current) * otherHistogram.getCount(displacedValue),
                    0.5);
        }

        return Math.log(distance);
    }

    public void plotCorrelation(final Histogram thisHistogram, final int displacement,
            final Histogram otherHistogram, final String fileName, final String title) {
        new Intersection().plotCorrelation(thisHistogram, displacement, otherHistogram, fileName, title);
    }
}
