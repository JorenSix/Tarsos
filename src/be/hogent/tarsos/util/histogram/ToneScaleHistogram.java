package be.hogent.tarsos.util.histogram;

import java.util.List;
import java.util.logging.Logger;

import be.hogent.tarsos.peak.Peak;
import be.hogent.tarsos.peak.PeakDetector;
import be.hogent.tarsos.util.Configuration;
import be.hogent.tarsos.util.FileUtils;
import be.hogent.tarsos.util.Configuration.Config;

/**
 * @author Joren Six
 * Create a new tone scale. A tone scale is defined as a wrapping histogram with
 * values from 0 to 1200 cents. <br>
 * Warning: some exotic 'tone scales' span more than one octave these will be
 * folded to one octave. E.g.
 * <br>
 * <pre>
 * ! mambuti.scl
 * !
 * African Mambuti Flutes (aerophone; vertical wooden; one note each)
 * 8
 * !
 *  204.000 cents
 *  411.000 cents
 *  710.000 cents
 * 1000.000 cents
 * 1206.000 cents
 * 1409.000 cents
 * 1918.000 cents
 * 2321.001 cents
 * </pre>
 * <br>
 * Would be (mistakenly?) represented as:
 * <br>
 * <pre>
 *    6.000 cents (1206 - 1200)
 *  204.000 cents                |
 *  209.000 cents (1409 - 1200)  |
 *  411.000 cents
 *  710.000 cents                |
 *  718.000 cents (1918 - 1200)  |
 * 1000.000 cents
 * 1121.001 cents (2321 - 1200)
 * </pre>
 * The <a href="http://www.huygens-fokker.org/scala/downloads.html#scales">
 * scala scale archive</a> is a collection of 3772 scales; 424 of which do not
 * end on an octave (mostly ambitus descriptions).
 *
 */
public class ToneScaleHistogram extends Histogram {
	private static final Logger log = Logger.getLogger(ToneScaleHistogram.class.getName());


	/**
	 * Create a new tone scale using the configured bin width. A tone scale is a
	 * wrapping histogram with values from 0 to 1200 cents.
	 */
	public ToneScaleHistogram() {
		super(0, 1200, 1200 / Configuration.getInt(Config.histogram_bin_width),true);
	}

	/**
	 * Executes peak detection on this histogram and saves
	 * the scale in the
	 * <a href="http://www.huygens-fokker.org/scala/scl_format.html">
	 * Scala scale file format</a>: <i>This file format for musical tunings
	 * is becoming a standard for exchange of scales, owing to the size of
	 * the scale archive of over 3700+ scales and the popularity
	 * of the Scala program.</i>
	 */
	public void exportToScalaScaleFileFormat(String fileName, String toneScaleName){
		List<Peak> peaks = PeakDetector.detect(this, 15, 0.5);
		exportPeaksToScalaFileFormat(fileName,toneScaleName,peaks);
	}

	/**
	 * Saves
	 * the scale in the
	 * <a href="http://www.huygens-fokker.org/scala/scl_format.html">
	 * Scala scale file format</a>: <i>This file format for musical tunings
	 * is becoming a standard for exchange of scales, owing to the size of
	 * the scale archive of over 3700+ scales and the popularity
	 * of the Scala program.</i>
	 * @param fileName
	 * @param toneScaleName
	 * @param peaks
	 */
	public static void exportPeaksToScalaFileFormat(String fileName,String toneScaleName,List<Peak> peaks){
		if(peaks.size() > 0){
			StringBuilder sb = new StringBuilder();
			sb.append("! ")
			  .append(FileUtils.basename(fileName))
			  .append(".scl \n")
			  .append("!\n")
			  .append(toneScaleName)
			  .append("\n")
			  .append(peaks.size())
			  .append("\n!\n");
			double firstPeakPosition = peaks.get(0).getPosition();
			for(int i = 1;i < peaks.size();i++){
				double peakPosition = peaks.get(i).getPosition() - firstPeakPosition;
				sb.append(peakPosition).append("\n");
			}
			sb.append("2/1");
			FileUtils.writeFile(sb.toString(), fileName);
		} else {
			log.warning("No peaks detected: file: " + fileName + " not created");
		}
	}

	/**
	 * Creates a theoretical tone scale using a mixture of gaussian functions
	 * See <a href="http://www.informaworld.com/smpp/content~content=a901755973~db=all~jumptype=rss">
	 * an automatic pitch analysis method for turkish maqam music</a>. The theoretic scale
	 * can be used to search for similar tone scales using a histogram correlation.
	 * <br>
	 * @param peaks the position of the peaks. The position is defined in cents.
	 * @param heights the heights of the peaks. If null the heights for all peaks is 200.
	 * @param widths the widths of the peaks in cents. If null the width is 25 cents for all peaks. The width is measured at a certain height. The mean?
	 * @param standardDeviations defines the shape of the peak. If null the standard deviation is 1. Bigger values give a wider peak.
	 * @return a histogram with values from 0 to 1200 and the requested peaks.
	 */
	public static ToneScaleHistogram createToneScale(double[] peaks,double[] heights, double[] widths,double[] standardDeviations){
		ToneScaleHistogram toneScaleHistogram = new ToneScaleHistogram();
		//unwrapped histogram of 3 x 1200 cents wide. Used to correctly calculate wrapping peaks
		//The middle octave is the 'real' octave, the other two are used to 'fold' onto the middle one
		Histogram unWrappedHistogram = new Histogram(0,3*1200, 3600 / Configuration.getInt(Config.histogram_bin_width));

		//shift the peaks to the middle octave + sanity checks
		for(int i =0;i<peaks.length;i++){
			assert peaks[i] >= 0 && peaks[i] <= 1200;
			peaks[i] += 1200.0;
		}

		if(heights == null){
			heights = new double[peaks.length];
			for(int i =0;i<peaks.length;i++)
				heights[i] = 200.0;
		}

		if(standardDeviations == null){
			standardDeviations = new double[peaks.length];
			for(int i =0;i<peaks.length;i++)
				standardDeviations[i] = 1;
		}

		if(widths == null){
			widths = new double[peaks.length];
			for(int i =0;i<peaks.length;i++)
				widths[i] = 25;
		}

		for(Double key : unWrappedHistogram.keySet()){
			double currentValue = 0.0;
			for(int i=0;i<peaks.length;i++){
				double difference = key - peaks[i];
				//do not calculate 0 values
				if(Math.abs(difference) > 10 * widths[i] )
					continue;
				double power = Math.pow(difference/(widths[i]/2 * standardDeviations[i]),2.0);
				currentValue += heights[i] * Math.pow(Math.E,-0.5 * power );
			}
			//add to the current value (for correct folding)
			long currentCount = toneScaleHistogram.getCount(key);
			long newCount =  currentCount + Math.round(currentValue);
			toneScaleHistogram.setCount(key, newCount);

		}
		return toneScaleHistogram;
	}

}
