package be.hogent.tarsos.util.histogram.peaks;

import org.apache.commons.math.stat.StatUtils;

import be.hogent.tarsos.util.histogram.Histogram;

/**
 * @author Joren Six
 */
public class LocalVolumeScore implements PeakScore {

    private final long[] volumes;
    private final int windowSize;

    public LocalVolumeScore(Histogram originalHistogram, int windowSize) {
        volumes = new long[originalHistogram.getNumberOfClasses()];
        this.windowSize = windowSize;
        // initialize first volume
        for (int j = 0; j < windowSize; j++) {
            int before = -j - 1;
            volumes[0] = volumes[0] + originalHistogram.getCountForClass(before);
            int after = j + 1;
            volumes[0] = volumes[0] + originalHistogram.getCountForClass(after);
        }
        volumes[0] = volumes[0] + originalHistogram.getCountForClass(0);

        // from now iterate histogram and use the first volume to calculate
        // the other volumes
        for (int i = 1; i < originalHistogram.getNumberOfClasses(); i++) {
            int after = i + windowSize;
            int before = i - windowSize - 1;
            volumes[i] = volumes[i - 1] + originalHistogram.getCountForClass(after)
            - originalHistogram.getCountForClass(before);
        }
    }

    public double getVolumeAt(int index) {
        return volumes[index];
    }

    @Override
    public double score(Histogram originalHistogram, int index, int windowSize) {
        assert this.windowSize == windowSize;
        int before = 0;
        int after = 0;
        double[] volumeRange = new double[windowSize * 2 + 1];
        int volumeRangeIndex = 0;
        for (int j = 0; j < windowSize; j++) {
            before--;
            after++;
            volumeRange[volumeRangeIndex] = volumes[(index + before + originalHistogram.getNumberOfClasses()) % originalHistogram
                                                    .getNumberOfClasses()];
            volumeRangeIndex++;
            volumeRange[volumeRangeIndex] = volumes[(index + after) % originalHistogram.getNumberOfClasses()];
            volumeRangeIndex++;
        }
        volumeRange[volumeRangeIndex] = volumes[index];

        double mean = StatUtils.mean(volumeRange);
        double standardDeviation = Math.pow(StatUtils.variance(volumeRange, mean), 0.5);
        return standardDeviation == 0.0 ? 0.0 : (volumes[index] - mean) / standardDeviation;
    }
}
