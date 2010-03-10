package be.hogent.tarsos.util.histogram;

import be.hogent.tarsos.util.Configuration;
import be.hogent.tarsos.util.Configuration.Config;

/**
 * @author Joren Six
 * The AmbitusHistogram accepts values from 0 to 9600 cents 
 * or +- from 16 to 40000Hz: the human hearing range is completely covered.<br>
 * The start and stop values can be configured.<br>
 * Values outside the defined range are ignored!
 */
public class AmbitusHistogram extends Histogram{
	public AmbitusHistogram() {
		super(
			Configuration.getInt(Config.ambitus_start),
			Configuration.getInt(Config.ambitus_stop), 
			1200 / Configuration.getInt(Config.histogram_bin_width),
			false,
			true
		);
	}
}