package be.hogent.tarsos.util.histogram.peaks;

import org.apache.commons.math.stat.StatUtils;

import be.hogent.tarsos.util.histogram.Histogram;

/**
 * 
 * 
 * @author Joren Six
 * 
 */
public class DifferenceScore implements PeakScore {

    private final int windowSize;
    private final double[] scores;
    private final Histogram histogram;

    public DifferenceScore(Histogram histogram, int windowSize) {
        this.histogram = histogram;
        this.windowSize = windowSize;
        scores = new double[histogram.getNumberOfClasses()];
        // calculate scores
        for (int i = 0; i < histogram.getNumberOfClasses(); i++) {
            calculateScore(i);
        }
        // remove smallest score in window
        for (int i = 0; i < histogram.getNumberOfClasses(); i++) {
            double currentScore = scores[i];
            if (currentScore != 0.0) {
                int before = i;
                int after = i;
                for (int j = 0; j < windowSize; j++) {
                    before--;
                    after++;
                    double scoreBefore = scores[(before + histogram
                            .getNumberOfClasses())
                            % histogram.getNumberOfClasses()];
                    double scoreAfter = scores[after
                            % histogram.getNumberOfClasses()];

                    // if there is a bigger score in this window
                    // set the current score to 0.0
                    if (scoreBefore >= currentScore
                            || scoreAfter >= currentScore) {
                        scores[i] = 0.0;
                        break;
                    }
                }
            }
        }
    }

    private void calculateScore(int index) {
        int before = 0;
        int after = 0;
        double[] beforeRange = new double[windowSize];
        double[] afterRange = new double[windowSize];
        for (int j = 0; j < windowSize; j++) {
            before--;
            after++;
            beforeRange[j] += histogram.getCountForClass(index + before);
            afterRange[j] += histogram.getCountForClass(index + after);
        }
        long current = histogram.getCountForClass(index);
        boolean isPeak = StatUtils.mean(beforeRange) < current
                && current > StatUtils.mean(afterRange);
        scores[index] = (isPeak ? 1.0 : 0.0)
                * histogram.getCountForClass(index);
    }

    @Override
    public double score(Histogram originalHistogram, int index, int windowSize) {
        return scores[index];
    }

}
