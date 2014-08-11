/*
*              _______                      
*             |__   __|                     
*                | | __ _ _ __ ___  ___  ___
*                | |/ _` | '__/ __|/ _ \/ __| 
*                | | (_| | |  \__ \ (_) \__ \    
*                |_|\__,_|_|  |___/\___/|___/    
*                                                         
* -----------------------------------------------------------
*
*  Tarsos is developed by Joren Six at 
*  The School of Arts,
*  University College Ghent,
*  Hoogpoort 64, 9000 Ghent - Belgium
*  
* -----------------------------------------------------------
*
*  Info: http://tarsos.0110.be
*  Github: https://github.com/JorenSix/Tarsos
*  Releases: http://tarsos.0110.be/releases/Tarsos/
*  
*  Tarsos includes some source code by various authors,
*  for credits and info, see README.
* 
*/

package be.tarsos.sampled.pitch;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import javax.imageio.ImageIO;

import org.apache.commons.math.stat.descriptive.DescriptiveStatistics;

import ptolemy.plot.Plot;
import be.tarsos.util.FileUtils;
import be.tarsos.util.histogram.Histogram;

/**
 * Utility class for pitch conversions See <a href=
 * "http://www.lenmus.org/sw/page.php?pid=docs&doc=hacking_guide&pag=pitch-representation&lang=en"
 * >pitch-representatio</a> for some background.
 * 
 * @author Joren Six
 */
public final class PitchFunctions {
	private PitchFunctions() {
	}

	/**
	 * Converts pitches in Hertz to the requested unit.
	 * 
	 * @param unit
	 *            The unit to convert from.
	 * @param pitchValuesInHertz 
	 * 		 The list of values to convert.
	 * @return The values converted to the requested unit. The original list
	 *         remains unchanged.
	 */
	public static List<Double> convertHertzTo(final PitchUnit unit, final List<Double> pitchValuesInHertz) {
		final List<Double> convertedValues = new ArrayList<Double>(pitchValuesInHertz);
		switch (unit) {
		case ABSOLUTE_CENTS:
			convertHertzToAbsoluteCent(convertedValues);
			break;
		case RELATIVE_CENTS:
			convertHertzToRelativeCent(convertedValues);
			break;
		case MIDI_KEY:
			convertHertzToMidiKey(convertedValues);
			break;
		case MIDI_CENT:
			convertHertzToMidiCent(convertedValues);
			break;
		case HERTZ:
			break;
		default:
			throw new AssertionError("Unsupported unit: " + unit.name());
		}
		return convertedValues;
	}

	/**
	 * Converts a list of pitches in Hertz to absolute cents.
	 * 
	 * @param convertedValues
	 */
	private static void convertHertzToAbsoluteCent(final List<Double> convertedValues) {
		for (int i = 0; i < convertedValues.size(); i++) {
			final Double valueInHertz = convertedValues.get(i);
			convertedValues.set(i, PitchUnit.hertzToAbsoluteCent(valueInHertz));
		}
	}

	private static void convertHertzToMidiCent(final List<Double> convertedValues) {
		for (int i = 0; i < convertedValues.size(); i++) {
			final Double valueInHertz = convertedValues.get(i);
			convertedValues.set(i, PitchUnit.hertzToMidiCent(valueInHertz));
		}
	}

	private static void convertHertzToMidiKey(final List<Double> convertedValues) {
		for (int i = 0; i < convertedValues.size(); i++) {
			final Double valueInHertz = convertedValues.get(i);
			convertedValues.set(i, (double) PitchUnit.hertzToMidiKey(valueInHertz));
		}
	}

	/**
	 * Folds the pitch values to one octave. E.g. 1203 becomes 3 and 956 remains
	 * 956
	 * 
	 * @param convertedValues
	 *            A list of double values in cent.
	 */
	private static void convertHertzToRelativeCent(final List<Double> convertedValues) {
		for (int i = 0; i < convertedValues.size(); i++) {
			final Double hertzValue = convertedValues.get(i);
			final Double pitchValueInCentFoldedToOneOctave = PitchUnit.hertzToRelativeCent(hertzValue);
			convertedValues.set(i, pitchValueInCentFoldedToOneOctave);
		}
	}

	/**
	 * Removes all frequencies that are not in the specified band. The remaining
	 * band consists only of frequencies between minValueInHertz and
	 * maxValueInHertz (inclusive).
	 * 
	 * @param pitchValuesInHertz
	 *            the values to filter.
	 * @param minValueInHertz
	 *            the minimum frequency in Hertz.
	 * @param maxValueInHertz
	 *            the maximum frequency in Hertz.
	 */
	public static void bandwithFilter(final List<Double> pitchValuesInHertz, final double minValueInHertz,
			final double maxValueInHertz) {
		final Iterator<Double> it = pitchValuesInHertz.iterator();
		while (it.hasNext()) {
			final double value = it.next();
			if (value < minValueInHertz || maxValueInHertz < value) {
				it.remove();
			}
		}
	}

	/**
	 * Calculates the median for a list of doubles. The list is sorted in-place.
	 * 
	 * @param list
	 *            The list.
	 * @return The median.
	 * @exception If
	 *                the list is null or empty the median is undefined and an
	 *                error is thrown.
	 */
	public static Double median(final List<Double> list) {
		if (list == null || list.size() == 0) {
			throw new AssertionError("Mean of an empty list is undefined");
		}
		final int size = list.size();
		Collections.sort(list);
		double median = 0.0;
		if (size % 2 == 0) {
			median = (list.get(size / 2) + list.get(size / 2 - 1)) / 2.0;
		} else {
			median = list.get(size / 2);
		}
		return median;
	}

	/**
	 * <p>
	 * Applies an order n one-dimensional median filter to the list to filter.
	 * The function considers the signal to be 0 beyond the end points. The
	 * output has the same length as the list to filter.
	 * </p>
	 * <p>
	 * Returns a new list.
	 * </p>
	 * <p>
	 * n must be odd! The function is defined as:<br>
	 * f(bufferCount) = median( list(bufferCount-n/2) .. list(bufferCount+n/2) )
	 * <br>
	 * </p>
	 * 
	 * <pre>
	 *  So for n = 3 and list to filter:
	 *    [3 7 4]
	 *  0 [3 7 4] 0
	 *    [3 4 7]
	 * </pre>
	 * 
	 * @param listToFilter
	 *            The list to filter.
	 * @param n
	 *            An odd number. The order n of the one-dimensional median
	 *            filter. An IllegalArgumentException is thrown when n is even.
	 * @return An order n one-dimensional median filtered list. The original
	 *         list remains untouched. A new list is created.
	 */
	public static List<Double> medianFilter(final List<Double> listToFilter, final int n) {

		if (n % 2 == 0) {
			throw new IllegalArgumentException("Medianfilter not implemented for even n values");
		}

		final List<Double> filteredList = new ArrayList<Double>();

		final int numberOfZeroesToAddBefore = (n - 1) / 2;
		final int numberOfZeroesToAddAfter = (n - 1) / 2;

		for (int i = 0; i < numberOfZeroesToAddBefore; i++) {
			listToFilter.add(0, 0.0);
		}
		for (int i = 0; i < numberOfZeroesToAddAfter; i++) {
			listToFilter.add(0.0);
		}

		for (int i = numberOfZeroesToAddBefore; i < listToFilter.size() - numberOfZeroesToAddAfter; i++) {
			List<Double> sublist = new ArrayList<Double>(listToFilter.subList(i - n / 2, i + n / 2 + 1));
			final double median = median(sublist);
			filteredList.add(median);
		}

		for (int i = 0; i < numberOfZeroesToAddBefore; i++) {
			listToFilter.remove(0);
		}
		for (int i = 0; i < numberOfZeroesToAddAfter; i++) {
			listToFilter.remove(listToFilter.size() - 1);
		}
		return filteredList;
	}

	/**
	 * Smooths a list of doubles using a gaussian.
	 * 
	 * @param listToSmooth
	 *            the list to smooth
	 * @param standardDeviation
	 *            the standard deviation, 0 means return the original list,
	 *            below zero is invalid.
	 * @return a list of Gaussian smoothed values.
	 */
	public static List<Double> getGaussianSmoothed(final List<Double> listToSmooth,
			final double standardDeviation) {
		if (standardDeviation < 0.0) {
			throw new IllegalArgumentException("standardDeviation invalid");
		} else if (standardDeviation == 0.0) {
			return listToSmooth;
		}

		// Create a new, identical but empty Histogram.
		final List<Double> smoothedList = new ArrayList<Double>();

		// Determine the number of weights (must be odd).
		int numWeights = (int) (2 * 2.58 * standardDeviation + 0.5);
		if (numWeights % 2 == 0) {
			numWeights++;
		}

		// Initialize the smoothing weights.
		final double[] weights = new double[numWeights];
		final int m = numWeights / 2;
		final double var = standardDeviation * standardDeviation;
		final double gain = 1.0 / Math.sqrt(2.0 * Math.PI * var);
		final double exp = -1.0 / (2.0 * var);
		for (int i = m; i < numWeights; i++) {
			final double del = i - m;
			weights[i] = gain * Math.exp(exp * del * del);
			weights[numWeights - 1 - i] = weights[i];
		}

		// Clear the band total count for the smoothed histogram.
		double sum = 0;
		double originalSum = 0;

		final double[] smoothedValues = new double[listToSmooth.size()];

		for (int b = 0; b < listToSmooth.size(); b++) {
			// Determine clipped range.
			final int min = Math.max(b - m, 0);
			final int max = Math.min(b + m, listToSmooth.size());

			// Calculate the offset into the weight array.
			int offset;
			if (m > b) {
				offset = m - b;
			} else {
				offset = 0;
			}

			// Accumulate the total for the range.
			double acc = 0;
			double weightTotal = 0;
			for (int i = min; i < max; i++) {
				final double w = weights[offset++];
				acc += listToSmooth.get(i) * w;
				weightTotal += w;
			}

			// Round the accumulated value.
			smoothedValues[b] = acc / weightTotal;

			// Accumulate total for band.
			sum += smoothedValues[b];
			originalSum += listToSmooth.get(b);
		}

		// Rescale the counts such that the band total is approximately
		// the same as for the same band of the original histogram.
		final double factor = originalSum / sum;
		for (int b = 0; b < listToSmooth.size(); b++) {
			final double smoothedValue = smoothedValues[b] * factor;
			smoothedList.add(smoothedValue);
		}

		assert smoothedList.size() == listToSmooth.size();

		return smoothedList;
	}

	/**
	 * Applies a Gaussian filter to the list to filter. The parameter is
	 * arbitrary and can be 1/(2*standard deviation^2).
	 * 
	 * @param listToFilter
	 *            the list to filter
	 * @param parameter
	 *            the parameter defining the impulse response of the filter.
	 * @return a Gaussian filtered list
	 */
	public static List<Double> gaussianFilter(final List<Double> listToFilter, final double parameter) {
		final int windowSize = 7;
		final List<Double> filteredList = new ArrayList<Double>();

		// double firstFactor = Math.pow(parameter/Math.PI, 0.5)
		// Math.pow(Math.E, -1 * parameter);

		for (int j = 0; j < windowSize / 2; j++) {
			filteredList.add(0.0);
		}

		for (int i = windowSize / 2; i < listToFilter.size() - windowSize / 2; i++) {
			double sumValues = 0;
			double sumWeight = 0;
			for (int j = 0; j < windowSize; j++) {
				final double weight = Math.pow(Math.E, -1 * (j - windowSize / 2.0) * (j - windowSize / 2.0)
						/ 2.0 * parameter);

				sumWeight += weight;
				sumValues += weight * listToFilter.get(i);
			}
			final double newValue = 1.0 / sumWeight * sumValues / sumWeight;

			filteredList.add(newValue);
		}

		for (int j = 0; j < windowSize / 2; j++) {
			filteredList.add(0.0);
		}

		return filteredList;
	}

	/**
	 * Applies a Gaussian filter to the list to filter. The parameter is
	 * 1/(2*standard deviation^2).
	 * 
	 * @param listToFilter
	 *            the list to filter
	 * @return An order n one-dimensional median filtered list.
	 */
	public static List<Double> gaussianFilter(final List<Double> listToFilter) {
		final DescriptiveStatistics stats = new DescriptiveStatistics();
		// Add the data from the array
		for (final Double value : listToFilter) {
			stats.addValue(value);
		}
		final double std = stats.getStandardDeviation();
		final double parameter = 1.0 / (std * std * 2);
		return gaussianFilter(listToFilter, parameter);
	}

	/**
	 * Creates a frequency table. The number of items in each class is returned.
	 * Classes are defined by the limit and resolution. E.g. for a limit of 1200
	 * with a resolution of 400 there are 3 classes: [0-400[, [400-800[ and
	 * [800-1200[.
	 * 
	 * @param values
	 *            the data to distribute over the bins/classes.
	 * @param classWidth
	 *            the resolution or the with of the classes
	 * @param start
	 *            the starting value
	 * @param stop
	 *            the stopping value
	 * @return The number of items in each class
	 */
	public static Histogram createFrequencyTable(final List<Double> values, final double classWidth,
			final double start, final double stop) {
		final Histogram histogram = new Histogram(start, stop, (int) ((stop - start) / classWidth));
		for (final Double value : values) {
			histogram.add(value);
		}
		assert histogram.getSumFreq() == values.size() : "Number of items in bins does no"
				+ "t correspond with total number of items";

		return histogram;
	}

	public enum EportType {
		HISTOGRAM_PNG, HISTOGRAM_CSV, TONE_SCALE_MIDI, PEAKS_PNG, PEAKS_CSV
	}

	public static void exportFrequencyTable(final Histogram histogram, final String fileName,
			final double start, final double stop) {
		final StringBuilder sb = new StringBuilder();
		for (double current = start + histogram.getClassWidth() / 2; current <= stop; current += histogram
				.getClassWidth()) {
			final double count = histogram.getCount(current);
			final long cumFreq = histogram.getCumFreq(current);
			final double derivative = current + histogram.getClassWidth() > stop ? 0 : (histogram
					.getCount(current) - histogram.getCount(current + histogram.getClassWidth()))
					/ histogram.getClassWidth();

			double psd = 0.0;
			if (current + 2 * histogram.getClassWidth() <= stop) {
				psd = (histogram.getCount(current + 2 * histogram.getClassWidth()) - histogram
						.getCount(current)) / (2 * histogram.getClassWidth());
			}
			// double derivative = (current + frequencyTable.getClassWidth() <=
			// stop)? 0 : (frequencyTable.getCount(current +
			// frequencyTable.getClassWidth()) -
			// frequencyTable.getCount(current))
			// /frequencyTable.getClassWidth();
			sb.append(current).append(";").append(count).append(";").append(cumFreq).append(";")
					.append(derivative).append(";").append(psd).append("\n");
		}
		FileUtils.writeFile(sb.toString(), fileName);

		final Plot h = new Plot();
		h.setXRange(start, stop);

		boolean first = true;

		double highWaterMark = 0;
		final double[] values = new double[histogram.getNumberOfClasses()];
		int i = 0;
		for (double current = start + histogram.getClassWidth() / 2; current <= stop; current += histogram
				.getClassWidth()) {

			h.addPoint(0, current, histogram.getCount(current), !first);
			values[i] = histogram.getCount(current);
			if (histogram.getCount(current) > highWaterMark) {
				highWaterMark = histogram.getCount(current);
			}
			i++;
			first = false;
		}

		/*
		 * List<Peak> peaks = PeakDetector.peakDetection(histogram,fileName +
		 * ".midi", start, stop); int peakIndex = 1; for(Peak peak:peaks){ int
		 * position = peak.getPosition();
		 * h.addPoint(1,peak.getLift(),peak.getLTop(),false);
		 * h.addPoint(1,position,peak.getTop(),true);
		 * h.addPoint(1,peak.getRight(),peak.getRTop(),true); peakIndex++; }
		 */

		if (stop == 1200.0) {

			h.setXLabel("n (cents)");
			h.setYLabel("frequency of ocurrence");

			// h.addLegend(0,"Pitch histogram");

			for (int j = 0; j <= 1200; j += 100) {
				h.addXTick(j + "", j);
			}
		}

		h.setSize(1024, 786);

		h.setTitle(FileUtils.basename(fileName));

		try {
			Thread.sleep(60);
			final BufferedImage image = h.exportImage();
			ImageIO.write(image, "png", new File(fileName.substring(0, fileName.length() - 4) + ".png"));
		} catch (final IOException e) {
			e.printStackTrace();
		} catch (final InterruptedException e1) {
			e1.printStackTrace();
		}
	}

	public static void exportFrequencyTable(final Histogram histogram, final String fileName) {
		exportFrequencyTable(histogram, fileName, histogram.getStart(), histogram.getStop());
	}

	/**
	 * Reads a frequency table (histogram) from disk. The source file is
	 * expected to be a CSV-file in the format:
	 * <code>value;frequency[;other data; is discarded;...]</code> The lowest
	 * value is on the first row, the highest on the last!
	 * 
	 * @param fileName
	 * @return a histogram.
	 */
	public static Histogram readFrequencyTable(final String fileName) {
		final List<String[]> data = FileUtils.readCSVFile(fileName, ";", -1);

		final double classWidth = Double.parseDouble(data.get(1)[0]) - Double.parseDouble(data.get(0)[0]);
		final double start = Double.parseDouble(data.get(0)[0]) - classWidth / 2.0;
		final double stop = Double.parseDouble(data.get(data.size() - 1)[0]) + classWidth / 2.0;

		final Histogram table = new Histogram(start, stop, (int) ((stop - start) / classWidth));
		for (final String[] row : data) {
			final int frequency = (int) Double.parseDouble(row[1]);
			final double value = Double.parseDouble(row[0]);
			for (int i = 0; i < frequency; i++) {
				table.add(value);
			}
		}
		return table;
	}

}
