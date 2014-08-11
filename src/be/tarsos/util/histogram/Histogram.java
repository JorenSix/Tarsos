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

package be.tarsos.util.histogram;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.TreeMap;
import java.util.logging.Logger;

import org.apache.commons.math.stat.StatUtils;

import be.tarsos.util.FileUtils;
import be.tarsos.util.SimplePlot;

/**
 * A histogram is defined by a start value, a stop value and a number of
 * classes. The 'key' of a class is the middle of the class. E.g. the keys of a
 * histogram that starts at 0, stops at 5 and has 5 classes are
 * {0.5,1.5,2.5,3.5,4.5}. The intervals for each key are
 * {[0,1[;[1,2[;[2,3[;[3,4];[4,5[} with [0,1[ meaning the interval between 0
 * inclusive and 1 exclusive.
 * <p>
 * The histogram uses a red and black tree as underlying structure: Search,
 * insert and delete are O(LOG n). The tree keeps the keys in order and makes
 * iteration (in order) easy. Optimization is possible by replacing the tree
 * with arrays.
 * </p>
 * <p>
 * The histogram uses doubles as key values. Java doubles are prone to rounding
 * errors. To prevent rounding errors the keys are rounded to a predefined
 * number of decimals. The number can be found in {@link #PRECISION_FACTOR}.
 * E.g. if {@link #PRECISION_FACTOR} is 10000 then the number of significant
 * decimals is 4; the minimum classWidth is 0.0001.
 * </p>
 * 
 * @author Joren Six
 */
public class Histogram implements Cloneable {
	private static final Logger LOG = Logger.getLogger(Histogram.class.getName());

	/**
	 * The width of each class (or bin) is equal to stop - start / number of
	 * classes.
	 */
	private final double classWidth;
	/**
	 * The number of classes (or bins) in the histogram.
	 */
	private final int numberOfClasses;
	/**
	 * A red black tree backing the frequency table, easy to iterate (in order)
	 * TODO Optimization: serious optimization possible by using a plain array
	 * (or two).
	 */
	private final TreeMap<Double, Long> freqTable;
	/**
	 * The starting value != the first class middle start == the first class
	 * middle - classWidth / 2.
	 */
	private final double start; // the starting value
	/**
	 * The last value != the last class middle stop == the last class middle +
	 * classWidth / 2.
	 */
	private final double stop; // the stopping value
	/**
	 * If the histogram wraps values outside the range
	 * <code>]start - classWidht / 2, stop + classWidth / 2 [</code> are mapped
	 * to values inside the range using a modulo calculation.
	 */
	private final boolean wraps;

	/**
	 * if <code>true</code> values outside the valid range are ignored.
	 * Otherwise if a value outside the valid range is added an
	 * IllegalArgumentException is thrown.
	 */
	private final boolean ignoreValuesOutsideRange;

	/**
	 * <p>
	 * Create a Histogram with a certain number of classes with values in the
	 * range <code>]start - classWidht / 2, stop + classWidth / 2 [</code> if
	 * the histogram wraps otherwise values outside the range are mapped to
	 * values inside using a modulo calculation.
	 * </p>
	 * 
	 * @param startVal
	 *            the starting value of the histogram. The starting value is not
	 *            the same as the first class middle. The starting value is
	 *            equal to <code>the first class middle - classWidth / 2</code>
	 * @param stopVal
	 *            the stopping value of the histogram. The stopping value is not
	 *            the same as the last class middle. The stopping value is equal
	 *            to <code>the last class middle + classWidth / 2</code>
	 * @param totalClasses
	 *            the number of classes between the starting and stopping
	 *            values. Also defines the classWidth.
	 * @param wrapping
	 *            indicates if the histogram wraps around the edges. More
	 *            formal: If the histogram wraps values outside the range
	 *            <code>]start - classWidht / 2, stop + classWidth / 2 [</code>
	 *            are mapped to values inside the range using a modulo
	 *            calculation.
	 * @param ignoreOutsideRange
	 *            if <code>true</code> values outside the valid range are
	 *            ignored. Otherwise if a value outside the valid range is added
	 *            an IllegalArgumentException is thrown.
	 */
	public Histogram(final double startVal, final double stopVal, final int totalClasses,
			final boolean wrapping, final boolean ignoreOutsideRange) {
		if (stopVal <= startVal) {
			throw new IllegalArgumentException("The stopping value (" + stopVal
					+ ") should be bigger than the starting value (" + startVal + ") .");
		}

		this.classWidth = preventRoundingErrors((stopVal - startVal) / totalClasses);
		this.start = startVal;
		this.stop = stopVal;
		this.freqTable = new TreeMap<Double, Long>();
		this.wraps = wrapping;
		this.ignoreValuesOutsideRange = ignoreOutsideRange;

		final double lastKey = stopVal - getClassWidth() / 2;

		final double stopValue;
		if (wrapping) {
			stopValue = lastKey;
		} else {
			stopValue = lastKey + getClassWidth() / 2;
		}
		freqTable.put(preventRoundingErrors(startVal + getClassWidth() / 2), 0L);
		for (double current = startVal + getClassWidth() / 2; current <= stopValue;) {
			freqTable.put(valueToKey(current), 0L);
			current = current + getClassWidth();
		}

		this.numberOfClasses = freqTable.keySet().size();
	}

	/**
	 * Creates a new, empty histogram using the same parameters of the original
	 * histogram. The parameter being start, wraps and stop and number of
	 * classes.
	 * 
	 * @param original
	 *            the original histogram
	 */
	public Histogram(final Histogram original) {
		this(original.getStart(), original.getStop(), original.numberOfClasses, original.wraps,
				original.ignoreValuesOutsideRange);
	}

	/**
	 * <p>
	 * Create a Histogram with a certain number of classes with values in the
	 * range <code>]start - classWidht / 2, stop + classWidth / 2 [</code> if
	 * the histogram wraps otherwise values outside the range are mapped to
	 * values inside using a modulo calculation.
	 * </p>
	 * 
	 * @param startVal
	 *            the starting value of the histogram. The starting value is not
	 *            the same as the first class middle. The starting value is
	 *            equal to <code>the first class middle - classWidth / 2</code>
	 * @param stopVal
	 *            the stopping value of the histogram. The stopping value is not
	 *            the same as the last class middle. The stopping value is equal
	 *            to <code>the last class middle + classWidth / 2</code>
	 * @param totalClasses
	 *            the number of classes between the starting and stopping
	 *            values. Also defines the classWidth.
	 */
	public Histogram(final double startVal, final double stopVal, final int totalClasses) {
		this(startVal, stopVal, totalClasses, false, false);
	}

	/**
	 * <p>
	 * Create a Histogram with a certain number of classes with values in the
	 * range <code>]start - classWidht / 2, stop + classWidth / 2 [</code> if
	 * the histogram wraps otherwise values outside the range are mapped to
	 * values inside using a modulo calculation.
	 * </p>
	 * 
	 * @param startVal
	 *            the starting value of the histogram. The starting value is not
	 *            the same as the first class middle. The starting value is
	 *            equal to <code>the first class middle - classWidth / 2</code>
	 * @param stopVal
	 *            the stopping value of the histogram. The stopping value is not
	 *            the same as the last class middle. The stopping value is equal
	 *            to <code>the last class middle + classWidth / 2</code>
	 * @param totalClasses
	 *            the number of classes between the starting and stopping
	 *            values. Also defines the classWidth.
	 * @param wrapping
	 *            indicates if the histogram wraps around the edges. More
	 *            formal: If the histogram wraps values outside the range
	 *            <code>]start - classWidht / 2, stop + classWidth / 2 [</code>
	 *            are mapped to values inside the range using a modulo
	 *            calculation.
	 */
	public Histogram(final double startVal, final double stopVal, final int totalClasses,
			final boolean wrapping) {
		this(startVal, stopVal, totalClasses, wrapping, true);
	}

	/**
	 * Returns the key for class with index bufferCount.
	 * 
	 * @param i
	 *            a class index. If bufferCount lays outside the interval
	 *            <code>[0,getNumberOfClasses()[</code> it is mapped to a value
	 *            inside the interval using a modulo calculation.
	 * @return the key for class with index bufferCount
	 */
	public final double getKeyForClass(final int i) {
		int classIndex = i;
		// make sure classIndex is positive
		while (classIndex < 0) {
			classIndex += getNumberOfClasses();
		}
		// make sure bufferCount is within range
		classIndex = classIndex % getNumberOfClasses();
		final double key = getStart() + classIndex * getClassWidth() + getClassWidth() / 2.0;
		return preventRoundingErrors(key);
	}

	/**
	 * Returns the number of items in class with index bufferCount.
	 * 
	 * @param i
	 *            A class index. If bufferCount lays outside the interval
	 *            <code>[0,getNumberOfClasses()[</code> it is mapped to a value
	 *            inside the interval using a modulo calculation.
	 * @return the number of items in bin with index bufferCount
	 */
	public final long getCountForClass(final int i) {
		return getCount(getKeyForClass(i));
	}

	/**
	 * @return the set with histogram keys; Do not add keys to the set directly.
	 *         Use histogram methods instead. For performance reasons it is not
	 *         wrapped in an immutable set so handle with care.
	 */
	public final Set<Double> keySet() {
		return freqTable.keySet();
	}

	/**
	 * Adds a value to the Histogram. Assigns the value to the right bin
	 * automatically.
	 * 
	 * @param value
	 *            The value to add.
	 * @return This histogram with the added value.
	 * @throws IllegalArgumentException
	 *             when the value is not in the range of the histogram.
	 */
	public final Histogram add(final double value) {

		if (!wraps && !ignoreValuesOutsideRange && !validValue(value)) {
			throw new IllegalArgumentException("Value not in the correct interval: " + value
					+ " not between " + "[" + this.firstValidValue() + "," + this.lastValidValue() + "].");
		} else if (!wraps && ignoreValuesOutsideRange && !validValue(value)) {
			LOG.info("Ignored value " + value + " (not between " + "[" + this.firstValidValue() + ","
					+ this.lastValidValue() + "]).");
		}

		if (value > 0) {
			final double key = valueToKey(value);
			final Long count = freqTable.get(key);
			assert count != null : "All key values should be initialized, " + key + " is not.";
			if (count != null) {
				freqTable.put(key, Long.valueOf(count.longValue() + 1));
			}
		} else {
			LOG.warning("Using values below zero in is not tested, "
					+ "it can yield unexpected results. Values below zero are ignored!");
		}
		valueAddedHook(value);
		return this;
	}

	/**
	 * A hook to intercept added values.
	 * 
	 * @param value
	 *            The value added
	 */
	protected void valueAddedHook(final double value) {
	}

	private static final double PRECISION_FACTOR = 10000.0;

	/**
	 * Prevents rounding errors by multiplying and dividing by
	 * {@link Histogram#PRECISION_FACTOR} Limits the use of the histogram class
	 * for values (class widths) smaller than 1 /
	 * {@link Histogram#PRECISION_FACTOR} XXX This is dangerous. Alternatives to
	 * using doubles: casting to BigDecimal internally? BigDecimal has ordering
	 * problems in a map. See
	 * http://java.sun.com/j2se/1.5.0/docs/api/java/math/BigDecimal.html.
	 * 
	 * @param value
	 *            to prevent errors for.
	 * @return a rounded value to
	 */
	private double preventRoundingErrors(final double value) {
		return Math.floor(value * PRECISION_FACTOR) / PRECISION_FACTOR;
	}

	/**
	 * returns the key for a value. E.g. if the bin width is 1 then
	 * valueToKey(3.2) returns 3.5
	 * 
	 * @param value
	 *            the value to get the key to
	 * @return the key closest to the value
	 */
	private double valueToKey(final double value) {
		// TODO remove the value below zero limitation
		// by changing the wraps modulo calculation and test
		if (value < 0) {
			throw new IllegalArgumentException("Currently no values below zero are accepted");
		}

		double roundedValue = value;
		if (wraps) {
			final double interval = stop - start;
			while (roundedValue < freqTable.firstKey()) {
				roundedValue = preventRoundingErrors(roundedValue + interval);
			}
			roundedValue = preventRoundingErrors(start + (roundedValue - start) % interval);
		}

		// assert validValue(roundedValue);

		final double classes = Math.floor((roundedValue + start) / classWidth);
		final double offset = classWidth / 2 - start;
		final double key = preventRoundingErrors(classes * classWidth + offset);
		// assert key >= freqTable.firstKey();
		// assert key <= freqTable.lastKey();
		return key;
	}

	/**
	 * Returns the number of values = v.
	 * 
	 * @param value
	 *            the value to lookup.
	 * @return the frequency of v.
	 */
	public final long getCount(final double value) {
		final double key = valueToKey(value);
		long result = 0;
		final Long count = freqTable.get(key);
		if (count != null) {
			result = count.longValue();
		}
		return result;
	}

	/**
	 * Sets the number of values for a key (bin) The value is automatically
	 * mapped to a key.
	 * 
	 * @param value
	 *            the value mapped to a key of the class to set the count for.
	 * @param count
	 *            the number of items in the bin
	 */
	public final void setCount(final double value, final long count) {
		final double key = valueToKey(value);
		freqTable.put(key, count);
	}

	/**
	 * @return the width of a class (bin)
	 */
	public final double getClassWidth() {
		return classWidth;
	}

	/**
	 * @return the number of classes
	 */
	public final int getNumberOfClasses() {
		return numberOfClasses;
	}

	/**
	 * The starting value is not the same as the first key. It is equal to
	 * <code>firstKey - classWidth / 2.0</code>
	 * 
	 * @return the starting value
	 */
	public final double getStart() {
		// assert Math.abs(start - freqTable.firstKey() - classWidth / 2.0) <
		// 0.0001;
		return start;
	}

	/**
	 * The stopping value is not the same as the last key. It is equal to
	 * <code>lastKey + classWidth / 2.0</code>
	 * 
	 * @return the stop value
	 */
	public double getStop() {
		// assert Math.abs(stop - freqTable.lastKey() + classWidth / 2.0) <
		// 0.001;
		// stop is cached for performance reasons
		return stop;
	}

	/**
	 * @return <code>true</code> if values outside the interval are wrapped,
	 *         <code>false</code> otherwise.
	 */
	public boolean isWrapped() {
		return this.wraps;
	}

	/**
	 * @return the first value that correctly maps to a key. A valid value lays
	 *         in the interval [{@link Histogram#firstValidValue()},
	 *         {@link Histogram#lastValidValue()}]
	 */
	private double firstValidValue() {
		return this.freqTable.firstKey() - classWidth / 2.0;
	}

	/**
	 * @return the last value that correctly maps to a key. A valid value lays
	 *         in the interval [{@link Histogram#firstValidValue()},
	 *         {@link Histogram#lastValidValue()}]
	 */
	private double lastValidValue() {
		return this.freqTable.lastKey() + classWidth / 2.0;
	}

	/**
	 * A valid value lays in the interval [{@link Histogram#firstValidValue()} ,
	 * {@link Histogram#lastValidValue()}].
	 * 
	 * @param value
	 *            the value to check
	 */
	private boolean validValue(final double value) {
		return value >= firstValidValue() && value <= lastValidValue();
	}

	/**
	 * Returns the cumulative frequency of values less than or equal to v.
	 * <p>
	 * Returns 0 if v is not comparable to the values set.
	 * </p>
	 * <p>
	 * Uses code from <a href="http://commons.apache.org/math">Apache Commons
	 * Math"</a> licensed to the Apache Software Foundation (ASF) under one or
	 * more contributor license agreements.
	 * </p>
	 * 
	 * @param v
	 *            the value to lookup.
	 * @return the proportion of values equal to v
	 */
	public long getCumFreq(final Double v) {
		long cumulativeFreq = -1;
		if (getSumFreq() == 0) {
			cumulativeFreq = 0;
		} else if (v.compareTo(freqTable.firstKey()) < 0) {
			cumulativeFreq = 0;
		} else if (v.compareTo(freqTable.lastKey()) >= 0) {
			cumulativeFreq = getSumFreq();
		} else {

			// the frequency of this key
			long result = 0;
			final Long value = freqTable.get(v);
			if (value != null) {
				result = value.longValue();
			}

			// add the frequencies of values smaller than this key
			final Iterator<Double> values = freqTable.keySet().iterator();
			while (values.hasNext()) {
				final Double nextValue = values.next();
				if (v.compareTo(nextValue) > 0) {
					result += getCount(nextValue);
				} else {
					cumulativeFreq = result;
					break;
				}
			}
		}
		if (cumulativeFreq == -1) {
			throw new AssertionError("The key is greather than te last key but this is impossible."
					+ " It should have been returned already.");
		}
		return cumulativeFreq;
	}

	/**
	 * Returns the cumulative percentage of values less than or equal to v (as a
	 * proportion between 0 and 1).
	 * <p>
	 * Returns <code>Double.NaN</code> if no values have been added.
	 * </p>
	 * 
	 * @param v
	 *            the value to lookup
	 * @return the proportion of values less than or equal to v
	 */
	public double getCumPct(final Double v) {
		final long sumFreq = getSumFreq();
		final double cumPercentage;
		if (sumFreq == 0) {
			cumPercentage = Double.NaN;
		} else {
			cumPercentage = (double) getCumFreq(v) / (double) sumFreq;
		}
		return cumPercentage;
	}

	/**
	 * Returns the sum of all frequencies (bin counts). If there are negative
	 * bin counts the sum is smaller than <code>getAbsoluteSumFreq()</code>
	 * 
	 * @return the total frequency count.
	 */
	public long getSumFreq() {
		long result = 0;
		final Iterator<Long> iterator = freqTable.values().iterator();
		while (iterator.hasNext()) {
			result += iterator.next().longValue();
		}
		return result;
	}

	/**
	 * Returns the sum of all frequencies. The absolute values of the bin counts
	 * are used.
	 * 
	 * @return the total frequency count.
	 */
	public long getAbsoluteSumFreq() {
		long result = 0;
		final Iterator<Long> iterator = freqTable.values().iterator();
		while (iterator.hasNext()) {
			result += Math.abs(iterator.next().longValue());
		}
		return result;
	}

	/**
	 * Returns the percentage of values that are equal to v (as a proportion
	 * between 0 and 1).
	 * <p>
	 * Returns <code>Double.NaN</code> if no values have been added.
	 * </p>
	 * 
	 * @param v
	 *            the value to lookup
	 * @return the proportion of values equal to v
	 */
	public double getPct(final Double v) {
		final long sumFreq = getSumFreq();
		double percentage;
		if (sumFreq == 0) {
			percentage = Double.NaN;
		} else {
			percentage = (double) getCount(v) / (double) sumFreq;
		}
		return percentage;
	}

	/**
	 * Returns the entropy of the histogram.
	 * <p>
	 * The histogram entropy is defined to be the negation of the sum of the
	 * products of the probability associated with each bin with the base-2 LOG
	 * of the probability.
	 * </p>
	 * <p>
	 * Uses code from https://jai-core.dev.java.net/ The source code for the
	 * core Java Advanced Imaging API reference implementation is licensed under
	 * the Java Research License (JRL) for non-commercial use. The JRL allows
	 * users to download, build, and modify the source code in the jai-core
	 * project for research use, subject to the terms of the license.
	 * </p>
	 * 
	 * @return The entropy of the histogram.
	 */
	public double getEntropy() {
		final double log2 = Math.log(2.0);
		double entropy = 0.0;
		final double total = getSumFreq();
		for (int b = 0; b < numberOfClasses; b++) {
			final double p = getCountForClass(b) / total;
			if (p != 0.0) {
				entropy -= p * Math.log(p) / log2;
			}
		}
		return entropy;
	}

	/**
	 * Calculates the mean count of each bin. It iterates over each bin, stores
	 * the bin count temporarily and returns the mean bin count. It does not
	 * cache the result.
	 * 
	 * @return the mean bin count.
	 */
	public double getMean() {
		final double[] binCounts = new double[this.getNumberOfClasses() + 1];
		int i = 0;
		for (final double key : this.keySet()) {
			binCounts[i] = this.getCount(key);
			i++;
		}
		return StatUtils.mean(binCounts);
	}
	
	/**
	 * Calculates the mean count of each bin. It iterates over each bin, stores
	 * the bin count temporarily and returns the mean bin count. It does not
	 * cache the result.
	 * 
	 * @return the mean bin count.
	 */
	public double getMedian() {
		final double[] binCounts = new double[this.getNumberOfClasses() + 1];
		int i = 0;
		for (final double key : this.keySet()) {
			binCounts[i] = this.getCount(key);
			i++;
		}
		return StatUtils.percentile(binCounts, 50);
	}

	/**
	 * Return a string representation of this histogram.
	 * 
	 * @return a string representation.
	 */

	@Override
	public String toString() {
		return toString(false);
	}

	/**
	 * Returns a string representation of the histogram.
	 * <p>
	 * Uses code from <a href="http://commons.apache.org/math">Apache Commons
	 * Math"</a> licensed to the Apache Software Foundation (ASF) under one or
	 * more contributor license agreements.
	 * </p>
	 * 
	 * @param asciiArt
	 *            If true it generates an ascii representation of a histogram,
	 *            otherwise it generates a frequency table
	 * @return a string representation.
	 */
	public String toString(final boolean asciiArt) {
		if (asciiArt) {
			final StringBuffer outBuffer = new StringBuffer();
			outBuffer.append('\n');
			final Iterator<Double> iter = freqTable.keySet().iterator();
			while (iter.hasNext()) {
				final Double value = iter.next();
				outBuffer.append(value).append("\t\t|");
				for (int i = 0; i < getPct(value) * 100; i++) {
					outBuffer.append('x');
				}
				outBuffer.append('\n');
			}
			outBuffer.append('\n');
			return outBuffer.toString();
		} else {
			final NumberFormat nf = NumberFormat.getPercentInstance();
			final StringBuffer outBuffer = new StringBuffer();
			outBuffer.append("\nValue \t Freq. \t Pct. \t Cum Pct. \n");
			final Iterator<Double> iter = freqTable.keySet().iterator();
			while (iter.hasNext()) {
				final Double value = iter.next();
				outBuffer.append(value);
				outBuffer.append('\t');
				outBuffer.append(getCount(value));
				outBuffer.append('\t');
				outBuffer.append(nf.format(getPct(value)));
				outBuffer.append('\t');
				outBuffer.append(nf.format(getCumPct(value)));
				outBuffer.append('\n');
			}
			return outBuffer.toString();
		}
	}

	// -----------------------------------------------------------
	// -- --
	// -- Modifications & Math --
	// -- --
	// -----------------------------------------------------------

	/**
	 * Normalizes the peaks in a histogram. Every peak is reduced to it's
	 * relative weight (percent).
	 * <p>
	 * Changes the current histogram and returns it so it is possible to chain
	 * modifications. E.g. <code>histo.normalize().addToEachBin(10)</code>
	 * </p>
	 * 
	 * @return a Histogram with normalized peak.
	 */
	public Histogram normalize() {
		final List<Long> normalizedCounts = new ArrayList<Long>();
		for (final double key : freqTable.keySet()) {
			normalizedCounts.add((long) (getPct(key) * 10000));
		}
		int index = 0;
		for (final double key : freqTable.keySet()) {
			this.setCount(key, normalizedCounts.get(index));
			index++;
		}
		return this;
	}

	/**
	 * Adds a number of items to each bin. Use a negative number to subtract a
	 * value from each bin.
	 * <p>
	 * Changes the current histogram and returns it so it is possible to chain
	 * modifications. E.g. <code>histo.normalize().addToEachBin(10)</code>
	 * </p>
	 * 
	 * @param value
	 *            the number of items to add.
	 * @return returns the current histogram so it is possible to chain
	 *         modifications.
	 */
	public Histogram addToEachBin(final long value) {
		// do nothing if value == 0
		if (value != 0) {
			for (final double key : freqTable.keySet()) {
				this.setCount(key, getCount(key) + value);
			}
		}
		return this;
	}

	/**
	 * Searches the minimum number of items in a bin and subtracts all bins with
	 * this value.
	 * 
	 * <pre>
	 * *
	 * * *              *
	 * * *   *          * *
	 * * * * *    =>    * *   *
	 * -------          -------
	 * </pre>
	 * <p>
	 * Changes the current histogram and returns it so it is possible to chain
	 * modifications. E.g. <code>histo.normalize().addToEachBin(10)</code>
	 * </p>
	 * 
	 * @return a baselined histogram
	 */
	public Histogram baselineHistogram() {
		long smallestValue = Long.MAX_VALUE;
		for (final double key : freqTable.keySet()) {
			smallestValue = Math.min(getCount(key), smallestValue);
		}
		final long valueToAdd = (long) -1.0 * smallestValue;
		return addToEachBin(valueToAdd);
	}

	/**
	 * Calculates the sum of two histograms. The value for each bin of other is
	 * added to the corresponding bin of this histogram. The other histogram
	 * must have the same start, stop and binWidth otherwise adding histograms
	 * makes no sense!
	 * <p>
	 * Changes the current histogram and returns it so it is possible to chain
	 * modifications. E.g.<code>histo.normalize().addToEachBin(10)</code>
	 * </p>
	 * 
	 * @param other
	 *            The other histogram
	 * @return the changed histogram with more (or the same) number of items in
	 *         the bins.
	 */
	public Histogram add(final Histogram other) {
		assert freqTable.keySet().size() == other.keySet().size();
		assert start == other.start;
		assert stop == other.stop;
		for (final double key : freqTable.keySet()) {
			this.setCount(key, this.getCount(key) + other.getCount(key));
		}
		return this;
	}
	
	/**
	 * Calculates the sum of two histograms. The value for each bin of other is
	 * added to the corresponding bin of this histogram. The other histogram
	 * must have the same start, stop and binWidth otherwise adding histograms
	 * makes no sense!
	 * <p>
	 * Changes the current histogram and returns it so it is possible to chain
	 * modifications. E.g.<code>histo.normalize().addToEachBin(10)</code>
	 * </p>
	 * 
	 * @param other
	 *            The other histogram
	 * @param offset 
	 * 				The offset.
	 * @return the changed histogram with more (or the same) number of items in
	 *         the bins.
	 */
	public Histogram add(final Histogram other,final int offset) {
		assert freqTable.keySet().size() == other.keySet().size();
		assert start == other.start;
		assert stop == other.stop;
		final int size = keySet().size();
		Double[] keys = this.keySet().toArray(new Double[size]);
		for(int i = 0 ; i < other.keySet().size() ; i++){			
			this.setCount(keys[i], this.getCount(keys[i]) + other.getCount(keys[(size+i+offset)%size]));
		}
		return this;
	}
	
	
	/**
	 * Takes the maximum of the bin value in each histogram and changes the
	 * current histogram to this maximum value.
	 * 
	 * @param other
	 *            Another histogram with the same number of bins.
	 * @return A histogram with the maximum values. It is the changed current
	 *         histogram, not a new one.
	 */
	public Histogram max(final Histogram other){
		assert freqTable.keySet().size() == other.keySet().size();
		assert start == other.start;
		assert stop == other.stop;
		for (final double key : freqTable.keySet()) {
			this.setCount(key, Math.max(this.getCount(key),other.getCount(key)));
		}
		return this;
	}

	/**
	 * Subtracts two histograms. The value for each bin of other is removed to
	 * the corresponding bin of this histogram. The other histogram must have
	 * the same start, stop and binWidth otherwise subtracting histograms makes
	 * no sense!
	 * <p>
	 * Changes the current histogram and returns it so it is possible to chain
	 * modifications. E.g.<code>histo.normalize().addToEachBin(10)</code>
	 * </p>
	 * 
	 * @param other
	 *            The other histogram
	 * @return The changed histogram with less (or the same) number of items in
	 *         the bins.
	 */
	public Histogram subtract(final Histogram other) {
		this.invert();
		this.add(other);
		return this;
	}

	/**
	 * Inverts this histograms. The value for each bin is multiplied with -1.
	 * <p>
	 * Changes the current histogram and returns it so it is possible to chain
	 * modifications. E.g.<code>histo.normalize().addToEachBin(10)</code>
	 * </p>
	 * 
	 * @return The changed histogram with each bin multiplied with -1.
	 */
	public Histogram invert() {
		this.multiply(-1.0);
		return this;
	}

	/**
	 * Multiplies each class (bin) count with a factor.
	 * <p>
	 * Changes the current histogram and returns it so it is possible to chain
	 * modifications. E.g. <code>histo.normalize().addToEachBin(10)</code>
	 * </p>
	 * 
	 * @param factor
	 *            the factor to multiply each bin value with.
	 * @return histogram with each bin value multiplied by the factor.
	 */
	public Histogram multiply(final double factor) {
		for (final double key : this.freqTable.keySet()) {
			this.setCount(key, Math.round(this.getCount(key) * factor));
		}
		return this;
	}
	


	/**
	 * Raises each class count to the power of exponent.
	 * <p>
	 * Changes the current histogram and returns it so it is possible to chain
	 * modifications. E.g. <code>histo.normalize().addToEachBin(10)</code>
	 * </p>
	 * 
	 * @param exponent
	 *            The exponent to raise each bincount with.
	 * @return Histogram with each bin count raised with exponent.
	 */
	public Histogram raise(final double exponent) {
		for (final double key : this.freqTable.keySet()) {
			this.setCount(key, Math.round(Math.pow(this.getCount(key), exponent)));
		}
		return this;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Object#clone()
	 */

	@Override
	public Histogram clone() throws CloneNotSupportedException {
		final Histogram clone = (Histogram) super.clone();
		for (final double key : this.freqTable.keySet()) {
			clone.setCount(key, this.getCount(key));
		}
		return clone;
	}

	/**
	 * Calculates a histogram mean of a list of histograms. All histograms must
	 * have the same start, stop and binWidth otherwise the mean histogram makes
	 * no sense!
	 * 
	 * @param histograms
	 *            a list of histograms
	 * @return a histogram with the mean values. If the list is empty it returns
	 *         null.
	 */
	public static Histogram mean(final List<Histogram> histograms) {
		Histogram mean = null;
		if (!histograms.isEmpty()) {
			final Histogram first = histograms.get(0);
			mean = new Histogram(first);
			for (final double key : first.freqTable.keySet()) {
				final double[] values = new double[histograms.size()];
				int countIndex = 0;
				for (final Histogram h : histograms) {
					assert h.keySet().size() == first.keySet().size();
					assert first.classWidth == h.classWidth;
					assert first.start == h.start;
					assert first.stop == h.stop;
					values[countIndex] = h.getCount(key);
					countIndex++;
				}
				final long currentMean = Math.round(StatUtils.mean(values));
				mean.setCount(key, currentMean);
			}
		}
		return mean;
	}

	// -----------------------------------------------------------
	// -- --
	// -- smoothing methods --
	// -- --
	// -----------------------------------------------------------

	/**
	 * Computes a smoothed version of the histogram.
	 * <p>
	 * The histogram is smoothed by averaging over a moving window of a size
	 * specified by the method parameter: if the value of the parameter is
	 * <bufferCount>k</bufferCount> then the width of the window is
	 * <bufferCount>2*k + 1</bufferCount>. If the window runs off the end of the
	 * histogram only those values which intersect the histogram are taken into
	 * consideration. The smoothing may optionally be weighted to favor the
	 * central value using a "triangular" weighting. For example, for a value of
	 * <bufferCount>k</bufferCount> equal to 2 the central bin would have weight
	 * 1/3, the adjacent bins 2/9, and the next adjacent bins 1/9.
	 * <p>
	 * Changes the current histogram and returns it so it is possible to chain
	 * modification e.g. <code>histo.normalize().addToEachBin(10)</code>
	 * </p>
	 * <p>
	 * Uses code from https://jai-core.dev.java.net/ The source code for the
	 * core Java Advanced Imaging API reference implementation is licensed under
	 * the Java Research License (JRL) for non-commercial use. The JRL allows
	 * users to download, build, and modify the source code in the jai-core
	 * project for research use, subject to the terms of the license.
	 * </p>
	 * 
	 * @param isWeighted
	 *            Whether bins will be weighted using a triangular weighting
	 *            scheme favoring bins near the central bin.
	 * @param k
	 *            The smoothing parameter which must be non-negative or an
	 *            <code>IllegalArgumentException</code> will be thrown. If zero,
	 *            the histogram object will be returned with no smoothing
	 *            applied.
	 * @return A smoothed version of the histogram.
	 */
	public Histogram smooth(final boolean isWeighted, final int k) {
		if (k < 0) {
			throw new IllegalArgumentException("k");
		} else if (k == 0) {
			return this;
		}

		// Initialize the smoothing weights if needed.
		double[] weights = null;
		if (isWeighted) {
			final int numWeights = 2 * k + 1;
			final double denom = numWeights * numWeights;
			weights = new double[numWeights];
			for (int i = 0; i <= k; i++) {
				weights[i] = (i + 1) / denom;
			}
			for (int i = k + 1; i < numWeights; i++) {
				weights[i] = weights[numWeights - 1 - i];
			}
		}

		final int[] smoothedCounts = new int[numberOfClasses];

		// Clear the band total count for the smoothed histogram.
		int sum = 0;

		if (isWeighted) {
			for (int b = 0; b < numberOfClasses; b++) {
				// Determine clipped range.
				final int min = Math.max(b - k, 0);
				final int max = Math.min(b + k, numberOfClasses);

				// Calculate the offset into the weight array.
				int offset = k > b ? k - b : 0;

				// Accumulate the total for the range.
				double acc = 0;
				double weightTotal = 0;
				for (int i = min; i < max; i++) {
					final double w = weights[offset++];
					acc += getCountForClass(i) * w;
					weightTotal += w;
				}

				// Round the accumulated value.
				smoothedCounts[b] = (int) (acc / weightTotal + 0.5);

				// Accumulate total for band.
				sum += smoothedCounts[b];
			}
		} else {
			for (int b = 0; b < numberOfClasses; b++) {
				// Determine clipped range.
				final int min = Math.max(b - k, 0);
				final int max = Math.min(b + k, numberOfClasses);

				// Accumulate the total for the range.
				int acc = 0;
				for (int i = min; i < max; i++) {
					acc += getCountForClass(i);
				}

				// Calculate the average for the range.
				smoothedCounts[b] = (int) (acc / (double) (max - min + 1) + 0.5);

				// Accumulate total for band.
				sum += smoothedCounts[b];
			}
		}

		// Rescale the counts such that the band total is approximately
		// the same as for the same band of the original histogram.
		final double factor = getSumFreq() / (double) sum;
		for (int b = 0; b < numberOfClasses; b++) {
			final int smoothedCount = (int) (smoothedCounts[b] * factor + 0.5);
			final double key = getKeyForClass(b);
			this.setCount(key, smoothedCount);
		}

		return this;
	}

	/**
	 * Smooth the histogram using Gaussians.
	 * <p>
	 * Each band of the histogram is smoothed by discrete convolution with a
	 * kernel approximating a Gaussian impulse response with the specified
	 * standard deviation.
	 * <p>
	 * <em>Changes the current histogram</em> and returns it so it is possible
	 * to chain modification e.g.
	 * <code>histo.normalize().addToEachBin(10)</code>
	 * </p>
	 * <p>
	 * Uses code from https://jai-core.dev.java.net/ The source code for the
	 * core Java Advanced Imaging API reference implementation is licensed under
	 * the Java Research License (JRL) for non-commercial use. The JRL allows
	 * users to download, build, and modify the source code in the JAI-core
	 * project for research use, subject to the terms of the license.
	 * </p>
	 * 
	 * @param standardDeviation
	 *            The standard deviation of the Gaussian smoothing kernel which
	 *            must be non-negative or an
	 *            <code>IllegalArgumentException</code> will be thrown. If zero,
	 *            the histogram object will be returned with no smoothing
	 *            applied.
	 * @return A Gaussian smoothed version of the histogram.
	 */
	public Histogram gaussianSmooth(final double standardDeviation) {
		if (standardDeviation < 0.0) {
			throw new IllegalArgumentException("standardDeviation invalid");
		} else if (standardDeviation == 0.0) {
			return this;
		}

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
		int sum = 0;

		final int[] smoothedCounts = new int[numberOfClasses];

		for (int b = 0; b < numberOfClasses; b++) {
			// Determine clipped range.
			final int min = Math.max(b - m, 0);
			final int max = Math.min(b + m, numberOfClasses);

			// Calculate the offset into the weight array.
			int offset = m > b ? m - b : 0;

			// Accumulate the total for the range.
			double acc = 0;
			double weightTotal = 0;
			for (int i = min; i < max; i++) {
				final double w = weights[offset++];
				acc += getCountForClass(i) * w;
				weightTotal += w;
			}

			// Round the accumulated value.
			smoothedCounts[b] = (int) (acc / weightTotal + 0.5);

			// Accumulate total for band.
			sum += smoothedCounts[b];
		}

		// Rescale the counts such that the band total is approximately
		// the same as for the same band of the original histogram.
		final double factor = getSumFreq() / (double) sum;
		for (int b = 0; b < numberOfClasses; b++) {
			final int smoothedCount = (int) (smoothedCounts[b] * factor + 0.5);
			final double key = getKeyForClass(b);
			this.setCount(key, smoothedCount);
		}
		return this;
	}

	// -----------------------------------------------------------
	// -- --
	// -- correlation methods --
	// -- --
	// -----------------------------------------------------------
	// TODO Document correlation methods

	public int displacementForOptimalCorrelation(final Histogram otherHistogram) {
		return displacementForOptimalCorrelation(otherHistogram, CorrelationMeasure.INTERSECTION);
	}

	public void displace(final int displacement) {
		try {
			Histogram original = clone();
			// Makes sure the displacement is positive.
			final int actualDisplacement = (displacement + numberOfClasses) % numberOfClasses;

			for (final double key : freqTable.keySet()) {
				final double displacedValue = (key + actualDisplacement * classWidth)
						% (numberOfClasses * classWidth);
				this.setCount(key, original.getCount(displacedValue));
			}
		} catch (CloneNotSupportedException e) {
			throw new AssertionError("Cloning a histogram is supported!");
		}
	}

	public double correlationWithDisplacement(final int displacement, final Histogram otherHistogram,
			final CorrelationMeasure correlationMeasure) {
		return correlationMeasure.getHistogramCorrelation().correlation(this, displacement, otherHistogram);
	}

	public double correlationWithDisplacement(final int displacement, final Histogram otherHistogram) {
		return correlationWithDisplacement(displacement, otherHistogram, CorrelationMeasure.INTERSECTION);
	}

	/**
	 * Return the correlation of this histogram with another one.
	 * 
	 * @param otherHistogram
	 * @param correlationMeasure
	 * @return the correlation between this histogram with another histogram.
	 */
	public double correlation(final Histogram otherHistogram, final CorrelationMeasure correlationMeasure) {
		if (otherHistogram.classWidth != classWidth) {
			throw new IllegalArgumentException(
					"Computation of correlation only correct when the classwidth of both histograms are the same");
		}
		return correlationWithDisplacement(0, otherHistogram, correlationMeasure);

	}

	/**
	 * Return the correlation of this histogram with another one. By default it
	 * uses the {@link CorrelationMeasure#INTERSECTION INTERSECTION} correlation
	 * measure.
	 * 
	 * @param otherHistogram
	 *            the other histogram
	 * @return the correlation the computed correlation
	 */
	public double correlation(final Histogram otherHistogram) {
		return correlation(otherHistogram, CorrelationMeasure.INTERSECTION);
	}

	/**
	 * Returns the number of classes the other histogram needs to be displaced
	 * to get optimal correlation with this histogram. The correlation is
	 * defined by the chosen correlation measure.
	 * 
	 * @param otherHistogram
	 *            The other histogram.
	 * @param correlationMeasure
	 *            The correlation strategy.
	 * @return Returns the number of classes the other histogram needs to be
	 *         displaced to get optimal correlation with this histogram.
	 */
	public int displacementForOptimalCorrelation(final Histogram otherHistogram,
			final CorrelationMeasure correlationMeasure) {
		int optimalDisplacement = 0; // displacement with best correlation
		double maximumCorrelation = -1; // best found correlation
		final int numberOfClasses = getNumberOfClasses();

		// current displacement, incremented with class width
		for (int currentDisplacement = 0; currentDisplacement < numberOfClasses; currentDisplacement++) {
			final double currentCorrelation = correlationWithDisplacement(currentDisplacement,
					otherHistogram, correlationMeasure);
			if (maximumCorrelation < currentCorrelation) {
				maximumCorrelation = currentCorrelation;
				optimalDisplacement = currentDisplacement;
			}
		}
		if (optimalDisplacement > getNumberOfClasses() / 2.0) {
			optimalDisplacement = optimalDisplacement - getNumberOfClasses();
		}

		return optimalDisplacement;
	}

	public void plotCorrelation(final Histogram otherHistogram, final CorrelationMeasure correlationMeasure,
			final String fileName, final String title) {
		final int displacement = displacementForOptimalCorrelation(otherHistogram);
		correlationMeasure.getHistogramCorrelation().plotCorrelation(this, displacement, otherHistogram,
				fileName, title);
	}

	// -----------------------------------------------------------
	// -- --
	// -- Plot method --
	// -- --
	// -----------------------------------------------------------

	/**
	 * Plots the histogram to a x y plot. The file is saved in PNG file format
	 * so the fileName should end on PNG.
	 * 
	 * @param fileName
	 *            The file is saved in PNG file format so the fileName should
	 *            end on PNG.
	 * @param title
	 *            The title of the histogram. Use an empty string or null for an
	 *            empty title.
	 */
	public void plot(final String fileName, final String title) {
		final String actualTitle = title == null ? "" : title;
		final SimplePlot plot = new SimplePlot(actualTitle);
		plot.addData(0, this);
		plot.save(fileName);
	}

	/**
	 * Export the histogram data as a plain text file. The format uses ; to
	 * separate keys from values.
	 * 
	 * @param fileName
	 *            Where to save the text file.
	 */
	public final void export(final String fileName) {
		final StringBuilder sb = new StringBuilder();
		sb.append("Bin (cents); Number of Annotations (#)\n");
		for (final double key : keySet()) {
			sb.append(key).append(";").append(getCount(key)).append("\n");
		}
		FileUtils.writeFile(sb.toString(), fileName);
	}

	/**
	 * Export the histogram data as a matlab text file. The format can be read
	 * using octave or MatLab.
	 * 
	 * @param fileName
	 *            Where to save the matlab (.m) file.
	 */
	public final void exportMatLab(final String fileName) {
		final StringBuilder sb = new StringBuilder();
		sb.append("histogram_values = [");
		for (final double key : keySet()) {
			for (int i = 0; i < getCount(key); i++) {
				sb.append(key).append(",");
			}
		}
		sb.append("]\n");
		sb.append("hist(histogram_values,1200)");
		FileUtils.writeFile(sb.toString(), fileName);
	}

	/**
	 * @return The maximum bin count.
	 */
	public final long getMaxBinCount() {
		long maxValue = -1;
		if (!freqTable.values().isEmpty()) {
			for (final long value : freqTable.values()) {
				maxValue = Math.max(maxValue, value);
			}
		}
		return maxValue;
	}

	/**
	 * Sets each bin to 0.
	 */
	public void clear() {
		for (final double key : keySet()) {
			setCount(key, 0);
		}
	}
}
