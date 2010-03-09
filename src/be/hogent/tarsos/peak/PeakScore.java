package be.hogent.tarsos.peak;

import be.hogent.tarsos.util.histogram.Histogram;

public interface PeakScore {
	double score(Histogram originalHistogram, int index,int windowSize);
}
