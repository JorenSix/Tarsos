package be.hogent.tarsos.util.histogram.peaks;

import java.util.List;

import be.hogent.tarsos.util.histogram.Histogram;

public interface DetectionStrategy {
	List<Peak> detect(final Histogram histogram, final int windowSize);
}
