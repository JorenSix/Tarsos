package be.hogent.tarsos.util.histogram;

import be.hogent.tarsos.util.SimplePlot;

/**
 * 
 * Implements a histogram intersection distance for modulo type histograms.
 * 
 * Works well on pitch frequency histograms according to Bozkurt
 * 
 * @author Joren Six
 */
public class Intersection implements HistogramCorrelation {

    @Override
    public double correlation(Histogram thisHistogram, int displacement, Histogram otherHistogram) {

        // number of bins (classes)
        int numberOfClasses = thisHistogram.getNumberOfClasses();
        // start value
        double start = thisHistogram.getStart();
        // stop value
        double stop = thisHistogram.getStop();
        // classWidth
        double classWidth = thisHistogram.getClassWidth();

        // make displacement positive
        if (displacement < 0)
            displacement = ((displacement % numberOfClasses) + numberOfClasses) % numberOfClasses;

        // matching area, displaced
        double matchingArea = 0.0;

        for (double current = start + classWidth / 2; current <= stop; current += classWidth) {
            double displacedValue = (current + displacement * classWidth) % (numberOfClasses * classWidth);
            matchingArea += Math
                    .min(thisHistogram.getCount(current), otherHistogram.getCount(displacedValue));
        }

        // the biggest area under the curve
        double biggestHistogramArea = Math.max(thisHistogram.getSumFreq(), otherHistogram.getSumFreq());

        double correlation = 0.0;

        // avoid the dreaded division by 0
        if (matchingArea != 0.0)
            correlation = matchingArea / biggestHistogramArea;
        return correlation;
    }

    public void plotCorrelation(Histogram thisHistogram, int displacement, Histogram otherHistogram) {
        // number of bins (classes)
        int numberOfClasses = thisHistogram.getNumberOfClasses();
        // start value
        double start = thisHistogram.getStart();
        // stop value
        double stop = thisHistogram.getStop();
        // classWidth
        double classWidth = thisHistogram.getClassWidth();

        // make displacement positive
        if (displacement < 0)
            displacement = ((displacement % numberOfClasses) + numberOfClasses) % numberOfClasses;

        // matching area, displaced
        double matchingArea = 0.0;

        SimplePlot correlationPlot = new SimplePlot();
        // plots the first histogram
        correlationPlot.addData(0, thisHistogram);
        // plots the other (displaced) histogram
        correlationPlot.addData(1, otherHistogram, displacement);

        // Visualize the intersection using impulses
        for (double current = start + classWidth / 2; current <= stop; current += classWidth) {
            double displacedValue = (current + displacement * classWidth) % (numberOfClasses * classWidth);
            double areaAdded = Math.min(thisHistogram.getCount(current), otherHistogram
                    .getCount(displacedValue));
            matchingArea += areaAdded;
            for (int i = 0; i < areaAdded; i++) {
                correlationPlot.addData(2, current, areaAdded, true);
            }
        }

        correlationPlot.save();

    }
}
