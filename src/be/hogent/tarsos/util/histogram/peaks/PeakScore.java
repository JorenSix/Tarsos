package be.hogent.tarsos.util.histogram.peaks;

import be.hogent.tarsos.util.histogram.Histogram;

public interface PeakScore {
	double score(Histogram originalHistogram, int index,int windowSize);
}
