package be.hogent.tarsos.util.histogram;

import java.text.DecimalFormat;
import java.util.List;
import java.util.Locale;

import be.hogent.tarsos.peak.Peak;
import be.hogent.tarsos.peak.PeakDetector;
import be.hogent.tarsos.util.Configuration;
import be.hogent.tarsos.util.FileUtils;
import be.hogent.tarsos.util.Configuration.Config;

/**
 * @author Joren Six
 * A ToneScaleHistogram is a wrapping histogram with values from
 * 0 (cents) to 1200 cents. 
 */
public class ToneScaleHistogram extends Histogram {
	
	public ToneScaleHistogram() {
		super(0, 1200, 1200 / Configuration.getInt(Config.histogram_bin_width),true);
	}
	
	
	/**
	 * Executes peak detection on this histogram and saves 
	 * the scale in the 
	 * <a href="http://www.huygens-fokker.org/scala/scl_format.html">
	 * Scala scale file format</a>: <i>This file format for musical tunings
	 * is becoming a standard for exchange of scales, owing to the size of 
	 * the scale archive of over 3700 scales and the popularity
	 * of the Scala program.</i>
	 */
	public void exportToScalaScaleFileFormat(String fileName, String toneScaleName){
		//Switching the Locale makes sure 
		//numbers use . as decimal separator
		Locale defaultLocale = Locale.getDefault();
		Locale.setDefault(Locale.US);
		DecimalFormat nf = new DecimalFormat("####.00");
		List<Peak> peaks = PeakDetector.detect(this, 15, 0.5);
		StringBuilder sb = new StringBuilder();
		sb.append("! ")
		  .append(FileUtils.basename(fileName))
		  .append(".scl \n")
		  .append("!\n")
		  .append(toneScaleName)
		  .append("\n")
		  .append(peaks.size())
		  .append("\n");
		if(peaks.size() > 0){
			double firstPeakPosition = peaks.get(0).getPosition();
			for(int i = 1;i < peaks.size();i++){
				double peakPosition = peaks.get(i).getPosition() - firstPeakPosition;
				sb.append(nf.format(peakPosition));
			}
		}
		sb.append(1200.0);
		//reset locale
		Locale.setDefault(defaultLocale);
		FileUtils.writeFile(sb.toString(), fileName);
	}
	
}
