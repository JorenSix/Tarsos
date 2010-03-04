package be.hogent.tarsos.util.histogram;

import be.hogent.tarsos.util.Configuration;
import be.hogent.tarsos.util.Configuration.Config;

public class ToneScaleHistogram extends Histogram {
	public ToneScaleHistogram() {
		super(0, 1200, 1200 / Configuration.getInt(Config.histogram_bin_width));
	}
}
