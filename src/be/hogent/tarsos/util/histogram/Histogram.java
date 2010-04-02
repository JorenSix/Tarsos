package be.hogent.tarsos.util.histogram;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.TreeMap;
import java.util.logging.Logger;

import org.apache.commons.math.stat.StatUtils;

import be.hogent.tarsos.util.FileUtils;
import be.hogent.tarsos.util.SimplePlot;

/**
 *
 * A histogram is defined by a start value, a stop value and a number of classes.
 * The 'key' of a class is the middle of the class. E.g. the keys of a histogram that starts
 * at 0, stops at 5 and has 5 classes are {0.5,1.5,2.5,3.5,4.5}. The intervals for each key
 * are {[0,1[;[1,2[;[2,3[;[3,4];[4,5[} with [0,1[ meaning the interval between 0 inclusive and 1 exclusive.
 *
 * <p>
 * The histogram uses a red and black tree as underlying structure:
 * Search, insert and delete are O(log n).
 * The tree keeps the keys in order and makes iteration (in order) easy.
 * Optimization is possible by replacing the tree with arrays.
 * </p>
 *
 * <p>
 * The histogram uses doubles as key values. Java doubles are prone to rounding errors.
 * To prevent rounding errors the keys are rounded to a predefined number of decimals.
 * The number can be found in {@link #PRECISION_FACTOR}. E.g. if  {@link #PRECISION_FACTOR}
 * is 10000 then the number of significant decimals is 4; the minimum classWidth is 0.0001.
 * </p>
 * @author Joren Six
 */
public class Histogram {
	private static final Logger log = Logger.getLogger(Histogram.class.getName());

	/**
	 * The width of each class (or bin) is equal to stop - start / number of classes.
	 */
	private final double classWidth;
	/**
	 * The number of classes (or bins) in the histogram
	 */
	private final int numberOfClasses;
	/**
	 * A red black tree backing the frequency table, easy to iterate (in order)
	 * TODO Optimization: serious optimization possible by using a plain array (or two)
	 */
	private final TreeMap<Double, Long> freqTable;
	/**
	 * The starting value != the first class middle
	 * start == the first class middle - classWidth / 2
	 */
	private final double start;//the starting value
	/**
	 * The last value != the last class middle
	 * stop == the last class middle + classWidth / 2
	 */
	private final double stop;//the stopping value
	/**
	 * If the histogram wraps values outside the range <code>]start - classWidht / 2, stop + classWidth / 2 [</code> are
	 * mapped to values inside the range using a modulo calculation. If wraps
	 */
	private final boolean wraps;


	/**
	 * if <code>true</code> values outside the valid range are ignored. Otherwise if a value
	 * outside the valid range is added an IllegalArgumentException is thrown.
	 */
	private final boolean ignoreValuesOutsideRange;


	/**
	 * <p>
	 * Create a Histogram with a certain number of classes with values in the range
	 * <code>]start - classWidht / 2, stop + classWidth / 2 [</code> if the histogram wraps otherwise values
	 * outside the range are mapped to values inside using a modulo calculation
	 * </p>
	 *
	 * @param start the starting value of the histogram. The starting value is not the same as
	 * the first class middle. The starting value is equal to <code>the first class middle - classWidth / 2</code>
	 * @param stop the stopping value of the histogram. The stopping value is not the same as the last
	 * class middle. The stopping value is equal to <code>the last class middle + classWidth / 2</code>
	 * @param numberOfClasses the number of classes between  the starting and stopping values. Also defines the classWidth.
	 * @param wraps indicates if the histogram wraps around the edges. More formal: If the histogram wraps
	 * values outside the range <code>]start - classWidht / 2, stop + classWidth / 2 [</code> are mapped to values
	 * inside the range using a modulo calculation.
	 * @ignoreValuesOutsideRange if <code>true</code> values outside the valid range are ignored. Otherwise if a value
	 * outside the valid range is added an IllegalArgumentException is thrown.
	 */
	public Histogram(double start, double stop,int numberOfClasses,boolean wraps,boolean ignoreValuesOutsideRange){
		if( stop <= start)
			throw new IllegalArgumentException("The stopping value (" + stop + ") should be bigger than the starting value (" + start + ") .");

		this.classWidth = preventRoundingErrors((stop - start) / numberOfClasses);
		this.start = start;
		this.stop = stop;
		this.freqTable = new TreeMap<Double, Long>();
		this.wraps = wraps;
		this.ignoreValuesOutsideRange = ignoreValuesOutsideRange;

		double lastKey = stop - getClassWidth() / 2;

		for(double current = start + getClassWidth()/2;
			wraps ? current < lastKey :current <= lastKey;
			current =  preventRoundingErrors(current  + getClassWidth()))
			freqTable.put(current,0l);

		this.numberOfClasses = freqTable.keySet().size();
	}

	/**
	 * Creates a new, empty histogram using the same parameters of
	 * the original histogram. The parameter being start, wraps and stop and number of classes.
	 * @param original the original histogram
	 */
	public Histogram(Histogram original) {
		this(original.getStart(),original.getStop(),original.numberOfClasses,original.wraps,original.ignoreValuesOutsideRange);
	}

	/**
	 * <p>
	 * Create a Histogram with a certain number of classes with values in the range
	 * <code>]start - classWidht / 2, stop + classWidth / 2 [</code> if the histogram wraps otherwise values
	 * outside the range are mapped to values inside using a modulo calculation
	 * </p>
	 *
	 * @param start the starting value of the histogram. The starting value is not the same as
	 * the first class middle. The starting value is equal to <code>the first class middle - classWidth / 2</code>
	 * @param stop the stopping value of the histogram. The stopping value is not the same as the last
	 * class middle. The stopping value is equal to <code>the last class middle + classWidth / 2</code>
	 * @param numberOfClasses the number of classes between  the starting and stopping values. Also defines the classWidth.
	 */
	public Histogram(double start, double stop,int numberOfClasses){
		this(start,stop,numberOfClasses,false,false);
	}

	/**
	 * <p>
	 * Create a Histogram with a certain number of classes with values in the range
	 * <code>]start - classWidht / 2, stop + classWidth / 2 [</code> if the histogram wraps otherwise values
	 * outside the range are mapped to values inside using a modulo calculation
	 * </p>
	 *
	 * @param start the starting value of the histogram. The starting value is not the same as
	 * the first class middle. The starting value is equal to <code>the first class middle - classWidth / 2</code>
	 * @param stop the stopping value of the histogram. The stopping value is not the same as the last
	 * class middle. The stopping value is equal to <code>the last class middle + classWidth / 2</code>
	 * @param numberOfClasses the number of classes between  the starting and stopping values. Also defines the classWidth.
	 * @param wraps indicates if the histogram wraps around the edges. More formal: If the histogram wraps
	 * values outside the range <code>]start - classWidht / 2, stop + classWidth / 2 [</code> are mapped to values
	 * inside the range using a modulo calculation.
	 */
	public Histogram(double start, double stop,int numberOfClasses,boolean wraps) {
		this(start,stop,numberOfClasses,wraps,true);
	}

	/**
	 * Returns the key for class with index i
	 * @param i a class index. If i lays outside the interval <code>[0,getNumberOfClasses()[</code>
	 * it is mapped to a value inside the interval using a modulo calculation.
	 * @return the key for class with index i
	 */
	public double getKeyForClass(int i){
		while(i < 0){//make sure i is positive
			i +=  getNumberOfClasses();
		}
		//make sure i is within range
		i = i % getNumberOfClasses();
		double key = getStart() + i * getClassWidth() + getClassWidth() / 2.0;
		return preventRoundingErrors(key);
	}

	/**
	 * Returns the number of items in class with index i
	 * @param i a class index. If i lays outside the interval <code>[0,getNumberOfClasses()[</code>
	 * it is mapped to a value inside the interval using a modulo calculation.
	 * @return the number of items in bin with index i
	 */
	public long getCountForClass(int i){
		return getCount(getKeyForClass(i));
	}

	/**
	 * @return the set with histogram keys;
	 * Do not add keys to the set directly.
	 * Use histogram methods instead.
	 * For performance reasons it is not wrapped
	 * in an immutable set so handle with care.
	 */
	public Set<Double> keySet(){
		return freqTable.keySet();
	}

	/**
	 * Adds a value to the Histogram. Assigns the value to the right bin automatically.
	 * @param value the value to add.
	 * @exception throws a null pointer exception when the value is not in the range of the histogram
	 */
	public Histogram add(double value){

		if(!wraps && !ignoreValuesOutsideRange && !validValue(value) ) {
			throw new IllegalArgumentException("Value not in the correct interval: "
					 + value +" not between " +
					"["+ this.firstValidValue() + "," + this.lastValidValue() + "].");
		} else if (!wraps && ignoreValuesOutsideRange && !validValue(value)) {
			log.info("Ignored value " + value +" (not between " +
					"["+ this.firstValidValue() + "," + this.lastValidValue() + "]).");
		}

		if(value > 0){
			double key = valueToKey(value);
			Long count = freqTable.get(key);
			assert count != null: "All key values should be initialized, " + key + " is not.";
			if(count != null)
				freqTable.put(key, Long.valueOf(count.longValue() + 1));
		}
		return this;
	}


	private static final double PRECISION_FACTOR = 10000.0;
	/**
	 * Prevents rounding errors by multiplying and dividing by
	 * {@link Histogram#PRECISION_FACTOR}
	 * Limits the use of the histogram class for values (class widths) smaller
	 * than 1 / {@link Histogram#PRECISION_FACTOR}
	 * XXX This is dangerous. Alternatives to using doubles: casting to BigDecimal internally?
	 * @param value to prevent errors for.
	 * @return a rounded value to
	 */
	private double preventRoundingErrors(double value){
		return Math.floor(value * PRECISION_FACTOR) / PRECISION_FACTOR;
	}
	/**
	 * returns the key for a value.
	 * E.g. if the bin width is 1 then valueToKey(3.2) returns 3.5
	 * @param value the value to get the key to
	 * @return the key closest to the value
	 */
	private double valueToKey(double value){
		//TODO remove the value below zero limitation
		//by changing the wraps modulo calculation and test
		if(value < 0)
			throw new IllegalArgumentException("Currently no values below zero are accepted");

		if (wraps){
			double interval = stop-start;
			while(value < freqTable.firstKey() )
				value = preventRoundingErrors(value + interval);
			value = preventRoundingErrors(start + ((value - start) %  interval));
		}

		assert validValue(value);

		double numberOfClasses = Math.floor((value + start)/classWidth);
		double offset = classWidth/2 - start;
		double key =  preventRoundingErrors(numberOfClasses * classWidth + offset);
		assert key >= freqTable.firstKey();
		assert key <= freqTable.lastKey();
		return key;
	}

	/**
	 * Returns the number of values = v.
	 *
	 * @param value the value to lookup.
	 * @return the frequency of v.
	 */
	public long getCount(double value){
		value = valueToKey(value);
		long result = 0;
		Long count =  freqTable.get(value);
		if(count != null){
			result = count.longValue();
		}
		return result;
	}

	/**
	 * Sets the number of values for a key (bin)
	 * The value is automatically mapped to a key.
	 * @param value the value mapped to a key of the class to set the count for.
	 * @param count the number of items in the bin
	 */
	public void setCount(double value,long count){
		value = valueToKey(value);
		freqTable.put(value,count);
	}

	/**
	 * @return the width of a class (bin)
	 */
	public double getClassWidth(){
		return classWidth;
	}

	/**
	 * @return the number of classes
	 */
	public int getNumberOfClasses(){
		return numberOfClasses;
	}


	/**
	 * The starting value is not the same as the first key. It is equal
	 * to <code>firstKey - classWidth / 2.0</code>
	 * @return the starting value
	 */
	public double getStart(){
		assert start == (freqTable.firstKey() - classWidth / 2.0);
		return start;
	}

	/**
	 * The stopping value is not the same as the last key. It is equal
	 * to <code>lastKey + classWidth / 2.0</code>
	 * @return the stop value
	 */
	public double getStop(){
		assert stop == (freqTable.lastKey() + classWidth / 2.0);
		//stop is cached for performance reasons
		return stop;
	}

	/**
	 * @return <code>true</code> if values outside the interval are wrapped,
	 * <code>false</code> otherwise.
	 */
	public boolean isWrapped(){
		return this.wraps;
	}

	/**
	 * @return the first value that correctly maps to a key. A valid value lays in the interval
	 * [{@link Histogram#firstValidValue()},{@link  Histogram#lastValidValue()}]
	 *
	 */
	private double firstValidValue(){
		return this.freqTable.firstKey() - classWidth / 2.0;
	}

	/**
	 * @return the last value that correctly maps to a key. A valid value lays in the interval
	 * [{@link Histogram#firstValidValue()},{@link  Histogram#lastValidValue()}]
	 */
	private double lastValidValue(){
		return this.freqTable.lastKey() + classWidth / 2.0;
	}

	/**
	 * c
	 * A valid value lays in the interval
	 * [{@link Histogram#firstValidValue()},{@link  Histogram#lastValidValue()}]
	 * @param value the value to check
	 * @return
	 */
	private boolean validValue(double value){
		return value >= firstValidValue() && value <= lastValidValue();
	}

	/**
	 * Returns the cumulative frequency of values less than or equal to v.
	 * <p>
	 * Returns 0 if v is not comparable to the values set.
	 * </p>
	 * <p>
	 * Uses code from <a href="http://commons.apache.org/math">Apache Commons Math"</a>
	 * licensed to the Apache Software Foundation (ASF) under one or more contributor license agreements.
	 * </p>
	 * @param v the value to lookup.
	 * @return the proportion of values equal to v
	 */
	public long getCumFreq(Double v) {
		if (getSumFreq() == 0) {
			return 0;
		}

		//v is less than first value
		if (v.compareTo(freqTable.firstKey()) < 0)
			return 0;

		//v is greater than the last key
		if (v.compareTo(freqTable.lastKey()) >= 0)
			return getSumFreq();

		//the frequency of this key
		long result = 0;
		Long value = freqTable.get(v);
		if (value != null) {
			result = value.longValue();
		}

		//add the frequencies of values smaller than this key
		Iterator<Double> values = freqTable.keySet().iterator();
		while (values.hasNext()) {
			Double nextValue = values.next();
			if(v.compareTo(nextValue) > 0) {
				result += getCount(nextValue);
			}else{
				return result;
			}
		}
		throw new AssertionError("The key is greather than te last key but this is impossible." +
				" It should have been returned already.");
	}

	/**
	 * Returns the cumulative percentage of values less than or equal to v
	 * (as a proportion between 0 and 1).
	 * <p>
	 * Returns <code>Double.NaN</code> if no values have been added.
	 * </p>
	 *
	 * @param v the value to lookup
	 * @return the proportion of values less than or equal to v
	 */
	public double getCumPct(Double v) {
		final long sumFreq = getSumFreq();
		if (sumFreq == 0) {
			return Double.NaN;
		}
		return (double) getCumFreq(v) / (double) sumFreq;
	}

	/**
	 * Returns the sum of all frequencies (bin counts).
	 *
	 * @return the total frequency count.
	 */
	public long getSumFreq() {
		long result = 0;
		Iterator<Long> iterator = freqTable.values().iterator();
		while (iterator.hasNext())  {
			result += iterator.next().longValue();
		}
		return result;
	}

	/**
	 * Returns the percentage of values that are equal to v
	 * (as a proportion between 0 and 1).
	 * <p>
	 * Returns <code>Double.NaN</code> if no values have been added.</p>
	 *
	 * @param v the value to lookup
	 * @return the proportion of values equal to v
	 */
	public double getPct(Double v) {
		final long sumFreq = getSumFreq();
		if (sumFreq == 0) {
			return Double.NaN;
		}
		return (double) getCount(v) / (double) sumFreq;
	}

	/**
	 * Returns the entropy of the histogram.
	 *
	 * <p>
	 * The histogram entropy is defined to be the negation of the sum
	 * of the products of the probability associated with each bin with
	 * the base-2 log of the probability.
	 * </p>
	 *
	 * <p>
	 * Uses code from https://jai-core.dev.java.net/
	 * The source code for the core Java Advanced Imaging API reference implementation is licensed under the
	 * Java Research License (JRL) for non-commercial use. The JRL allows users to download, build, and
	 * modify the source code in the jai-core project for research use, subject to the terms of the license.
	 * </p>
	 *
	 * @return The entropy of the histogram.
	 *
	 */
	public double getEntropy() {
		double log2 = Math.log(2.0);
		double entropy = 0.0;
		double total = getSumFreq();
		for(int b = 0; b < numberOfClasses; b++) {
			double p = getCountForClass(b)/total;
			if(p != 0.0) {
				entropy -= p*(Math.log(p)/log2);
			}
		}
		return entropy;
	}

	/**
	 * Return a string representation of this histogram
	 *
	 * @return a string representation.
	 */
	@Override
	public String toString() {
		return toString(false);
	}

	/**
	 * Returns a string representation of the histogram
	 *
	 * <p>
	 * Uses code from <a href="http://commons.apache.org/math">Apache Commons Math"</a>
	 * licensed to the Apache Software Foundation (ASF) under one or more contributor license agreements.
	 * </p>
	 *
	 * @param asciiArt If true it generates an ascii representation of a histogram, otherwise it generates a frequency table
	 * @return a string representation.
	 */
	public String toString(boolean asciiArt) {
		if(asciiArt){
			StringBuffer outBuffer = new StringBuffer();
			outBuffer.append('\n');
			Iterator<Double> iter = freqTable.keySet().iterator();
			while (iter.hasNext()) {
				Double value = iter.next();
				outBuffer.append(value).append("\t").append("\t|");
				for(int i = 0 ; i<getPct(value) * 100 ; i++)
					outBuffer.append('x');
				outBuffer.append('\n');
			}
			outBuffer.append('\n');
			return outBuffer.toString();
		}else{
			NumberFormat nf = NumberFormat.getPercentInstance();
			StringBuffer outBuffer = new StringBuffer();
			outBuffer.append("\nValue \t Freq. \t Pct. \t Cum Pct. \n");
			Iterator<Double> iter = freqTable.keySet().iterator();
			while (iter.hasNext()) {
				Double value = iter.next();
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

	//-----------------------------------------------------------
	//--                                                       --
	//--                Modifications & Math                   --
	//--                                                       --
	//-----------------------------------------------------------

	/**
	 * Normalizes the peaks in a histogram.
	 * Every peak is reduced to it's relative weight (percent).
	 * <p>
	 * Changes the current histogram and returns it so it is possible
	 * to chain modifications. E.g. <code>histo.normalize().addToEachBin(10)</code>
	 * </p>
	 * @return a Histogram with normalized peak.
	 */
	public Histogram normalize(){
		List<Long> normalizedCounts = new ArrayList<Long>();
		for(double key : freqTable.keySet()){
			normalizedCounts.add((long)(getPct(key) * 1000));
		}
		int index = 0;
		for(double key : freqTable.keySet()){
			this.setCount(key,normalizedCounts.get(index));
			index++;
		}
		return this;
	}

	/**
	 * Adds a number of items to each bin.
	 * Use a negative number to subtract a value from each bin.
	 * <p>
	 * Changes the current histogram and returns it so it is possible
	 * to chain modifications. E.g. <code>histo.normalize().addToEachBin(10)</code>
	 * </p>
	 * @param value the number of items to add.
	 * @return returns the current histogram so it is possible to chain modifications.
	 */
	public Histogram addToEachBin(long value){
		//do nothing if value == 0
		if(value != 0){
			for(double key: freqTable.keySet()){
				this.setCount(key,getCount(key) + value);
			}
		}
		return this;
	}

	/**
	 * Searches the minimum number of items in a bin and
	 * subtracts all bins with this value.
	 * <pre>
	 * *
	 * * *              *
	 * * *   *          * *
	 * * * * *    =>    * *   *
	 * -------          -------
	 * </pre>
	 * <p>
	 * Changes the current histogram and returns it so it is possible
	 * to chain modifications. E.g. <code>histo.normalize().addToEachBin(10)</code>
	 * </p>
	 * @return a baselined histogram
	 */
	public Histogram baselineHistogram(){
		long smallestValue = Long.MAX_VALUE;
		for(double key: freqTable.keySet())
			smallestValue = Math.min(getCount(key),smallestValue);
		long valueToAdd = (long) -1.0 * smallestValue;
		return addToEachBin(valueToAdd);
	}

	/**
	 * Calculates the sum of two histograms. The value for each bin of other is added
	 * to the corresponding bin of this histogram.
	 * The other histogram must have the same start, stop
	 * and binWidth otherwise adding histograms makes no sense!
	 * <p>
	 * Changes the current histogram and returns it so it is possible
	 * to chain modifications. E.g.<code>histo.normalize().addToEachBin(10)</code>
	 * </p>
	 * @param other The other histogram
	 * @return the changed histogram with more (or the same) number of items in the bins.
	 */
	public Histogram add(Histogram other) {
		assert freqTable.keySet().size() == other.keySet().size();
		assert start == other.start;
		assert stop == other.stop;
		for(double key : freqTable.keySet())
			this.setCount(key, this.getCount(key) + other.getCount(key));
		return this;
	}

	/**
	 * Multiplies each class (bin) count with a factor.
	 * <p>
	 * Changes the current histogram and returns it so it is possible
	 * to chain modifications. E.g. <code>histo.normalize().addToEachBin(10)</code>
	 * </p>
	 * @param factor the factor to multiply each bin value with.
	 * @return histogram with each bin value multiplied by the factor.
	 */
	public Histogram multiply(double factor) {
		for(double key : this.freqTable.keySet())
			this.setCount(key, Math.round(this.getCount(key) * factor));
		return this;
	}

	/**
	 * Raises each class count to the power of exponent.
	 * <p>
	 * Changes the current histogram and returns it so it is possible
	 * to chain modifications. E.g. <code>histo.normalize().addToEachBin(10)</code>
	 * </p>
	 * @param exponent The exponent to raise each bincount with.
	 * @return Histogram with each bin count raised with exponent.
	 */
	public Histogram raise(double exponent) {
		for(double key : this.freqTable.keySet())
			this.setCount(key, Math.round(Math.pow(this.getCount(key), exponent)));
		return this;
	}


	/* (non-Javadoc)
	 * @see java.lang.Object#clone()
	 */
	@Override
	public Histogram clone(){
		Histogram clone = new Histogram(this);
		for(double key : this.freqTable.keySet())
			clone.setCount(key, this.getCount(key));
		return clone;
	}

	/**
	 * Calculates a histogram mean of a list of histograms. All histograms must have
	 * the same start, stop and binWidth otherwise the mean histogram makes no sense!
	 * @param histograms a list of histograms
	 * @return a histogram with the mean values. If the list is empty it returns null.
	 */
	public static Histogram mean(List<Histogram> histograms){
		Histogram mean = null;
		if(histograms.size()!=0){
			Histogram first = histograms.get(0);
			mean = new Histogram(first);
			for(double key : first.freqTable.keySet()){
				double[] values = new double[histograms.size()];
				int countIndex = 0;
				for(Histogram h : histograms){
					assert h.keySet().size() == first.keySet().size();
					assert first.classWidth == h.classWidth;
					assert first.start == h.start;
					assert first.stop == h.stop;
					values[countIndex] = h.getCount(key);
					countIndex++;
				}
				long currentMean = Math.round(StatUtils.mean(values));
				mean.setCount(key, currentMean);
			}
		}
		return mean;
	}

	//-----------------------------------------------------------
	//--                                                       --
	//--                   smoothing methods                   --
	//--                                                       --
	//-----------------------------------------------------------

	/**
	 * Computes a smoothed version of the histogram.
	 *
	 *
	 * <p> The histogram is smoothed by averaging over a
	 * moving window of a size specified by the method parameter: if
	 * the value of the parameter is <i>k</i> then the width of the window
	 * is <i>2*k + 1</i>.  If the window runs off the end of the histogram
	 * only those values which intersect the histogram are taken into
	 * consideration.  The smoothing may optionally be weighted to favor
	 * the central value using a "triangular" weighting.  For example,
	 * for a value of <i>k</i> equal to 2 the central bin would have weight
	 * 1/3, the adjacent bins 2/9, and the next adjacent bins 1/9.
	 *
	 * <p>
	 * Changes the current histogram and returns it so it is possible
	 * to chain modification e.g. <code>histo.normalize().addToEachBin(10)</code>
	 * </p>
	 *
	 * <p>
	 * Uses code from https://jai-core.dev.java.net/
	 * The source code for the core Java Advanced Imaging API reference
	 * implementation is licensed under the Java Research License (JRL)
	 * for non-commercial use. The JRL allows users to download, build, and
	 * modify the source code in the jai-core project for research use,
	 * subject to the terms of the license.
	 * </p>
	 *
	 * @param isWeighted Whether bins will be weighted using a triangular
	 * weighting scheme favoring bins near the central bin.
	 * @param k The smoothing parameter which must be non-negative or an
	 * <code>IllegalArgumentException</code> will be thrown.  If zero, the
	 * histogram object will be returned with no smoothing applied.
	 * @return A smoothed version of the histogram.
	 *
	 */
	public Histogram smooth(boolean isWeighted, int k) {
		if(k < 0) {
			throw new IllegalArgumentException("k");
		} else if(k == 0) {
			return this;
		}

		// Initialize the smoothing weights if needed.
		double[] weights = null;
		if(isWeighted) {
			int numWeights = 2*k + 1;
			double denom = numWeights*numWeights;
			weights = new double[numWeights];
			for(int i = 0; i <= k; i++) {
				weights[i] = (i + 1)/denom;
			}
			for(int i = k + 1; i < numWeights; i++) {
				weights[i] = weights[numWeights - 1 - i];
			}
		}


		int[] smoothedCounts = new int[numberOfClasses];

		// Clear the band total count for the smoothed histogram.
		int sum = 0;

		if(isWeighted) {
			for(int b = 0; b < numberOfClasses; b++) {
				// Determine clipped range.
				int min = Math.max(b - k, 0);
				int max = Math.min(b + k, numberOfClasses);

				// Calculate the offset into the weight array.
				int offset = k > b ? k - b : 0;

				// Accumulate the total for the range.
				double acc = 0;
				double weightTotal = 0;
				for(int i = min; i < max; i++) {
					double w = weights[offset++];
					acc += getCountForClass(i)*w;
					weightTotal += w;
				}

				// Round the accumulated value.
				smoothedCounts[b] = (int)(acc/weightTotal + 0.5);

				// Accumulate total for band.
				sum += smoothedCounts[b];
			}
		} else {
			for(int b = 0; b < numberOfClasses; b++) {
				// Determine clipped range.
				int min = Math.max(b - k, 0);
				int max = Math.min(b + k, numberOfClasses);

				// Accumulate the total for the range.
				int acc = 0;
				for(int i = min; i < max; i++) {
					acc += getCountForClass(i);
				}

				// Calculate the average for the range.
				smoothedCounts[b] = (int)(acc / (double)(max - min + 1) + 0.5);

				// Accumulate total for band.
				sum += smoothedCounts[b];
			}
		}

		// Rescale the counts such that the band total is approximately
		// the same as for the same band of the original histogram.
		double factor = getSumFreq()/(double)sum;
		for(int b = 0; b < numberOfClasses; b++) {
			int smoothedCount = (int)(smoothedCounts[b]*factor + 0.5);
			double key = getKeyForClass(b);
			this.setCount(key, smoothedCount);
		}

		return this;
	}

	/**
	 * Smooth the histogram using Gaussians.
	 *
	 * <p> Each band of the histogram is smoothed by discrete convolution
	 * with a kernel approximating a Gaussian impulse response with the
	 * specified standard deviation.
	 * <p>
	 * <em>Changes the current histogram</em> and returns it so it is possible
	 * to chain modification e.g. <code>histo.normalize().addToEachBin(10)</code>
	 * </p>
	 *
	 * <p>
	 * Uses code from https://jai-core.dev.java.net/
	 * The source code for the core Java Advanced Imaging API reference implementation is licensed under the
	 * Java Research License (JRL) for non-commercial use. The JRL allows users to download, build, and
	 * modify the source code in the JAI-core project for research use, subject to the terms of the license.
	 * </p>
	 *
	 * @param standardDeviation The standard deviation of the Gaussian
	 * smoothing kernel which must be non-negative or an
	 * <code>IllegalArgumentException</code> will be thrown.  If zero, the
	 * histogram object will be returned with no smoothing applied.
	 * @return A Gaussian smoothed version of the histogram.
	 *
	 */
	public Histogram gaussianSmooth(double standardDeviation){
		if(standardDeviation < 0.0) {
			throw new IllegalArgumentException("standardDeviation invalid");
		} else if(standardDeviation == 0.0) {
			return this;
		}

		// Create a new, identical but empty Histogram.
		// Histogram smoothedHistogram = new Histogram(this);

		// Determine the number of weights (must be odd).
		int numWeights = (int)(2*2.58*standardDeviation + 0.5);
		if(numWeights % 2 == 0) {
			numWeights++;
		}

		// Initialize the smoothing weights.
		double[] weights = new double[numWeights];
		int m = numWeights/2;
		double var = standardDeviation*standardDeviation;
		double gain = 1.0/Math.sqrt(2.0*Math.PI*var);
		double exp = -1.0/(2.0*var);
		for(int i = m; i < numWeights; i++) {
			double del = i - m;
			weights[i] = weights[numWeights-1-i] = gain*Math.exp(exp*del*del);
		}


		// Clear the band total count for the smoothed histogram.
		int sum = 0;

		int[] smoothedCounts = new int[numberOfClasses];

		for(int b = 0; b < numberOfClasses; b++) {
			// Determine clipped range.
			int min = Math.max(b - m, 0);
			int max = Math.min(b + m, numberOfClasses);

			// Calculate the offset into the weight array.
			int offset = m > b ? m - b : 0;

			// Accumulate the total for the range.
			double acc = 0;
			double weightTotal = 0;
			for(int i = min; i < max; i++) {
				double w = weights[offset++];
				acc += getCountForClass(i)*w;
				weightTotal += w;
			}

			// Round the accumulated value.
			smoothedCounts[b]= (int)(acc/weightTotal + 0.5);

			// Accumulate total for band.
			sum += smoothedCounts[b];
		}

		// Rescale the counts such that the band total is approximately
		// the same as for the same band of the original histogram.
		double factor = getSumFreq()/(double)sum;
		for(int b = 0; b < numberOfClasses; b++) {
			int smoothedCount = (int)(smoothedCounts[b]*factor + 0.5);
			double key = getKeyForClass(b);
			this.setCount(key, smoothedCount);
		}
		return this;
	}

	//-----------------------------------------------------------
	//--                                                       --
	//--                 correlation methods                   --
	//--                                                       --
	//-----------------------------------------------------------
	//TODO Document correlation methods

	public int displacementForOptimalCorrelation(Histogram otherHistogram){
		return displacementForOptimalCorrelation(otherHistogram,CorrelationMeasure.INTERSECTION);
	}

	public double correlationWithDisplacement(int displacement,Histogram otherHistogram,CorrelationMeasure correlationMeasure){
		return correlationMeasure.getHistogramCorrelation().correlation(this, displacement, otherHistogram);
	}

	public double correlationWithDisplacement(int displacement,Histogram otherHistogram){
		return correlationWithDisplacement(displacement, otherHistogram,CorrelationMeasure.INTERSECTION);
	}

	/**
	 * Return the correlation of this histogram with another one.
	 *
	 * @param otherHistogram
	 * @param correlationMeasure
	 * @return the correlation between this histogram with another histogram.
	 */
	public double correlation(Histogram otherHistogram,CorrelationMeasure correlationMeasure) {
		if(otherHistogram.classWidth!=classWidth)
			throw new IllegalArgumentException("Computation of correlation only correct when the classwidth of both histograms are the same");
		return correlationWithDisplacement(0, otherHistogram,correlationMeasure);

	}

	/**
	 * Return the correlation of this histogram with another one.
	 * By default it uses the {@link CorrelationMeasure#INTERSECTION INTERSECTION} correlation measure.
	 * @param otherHistogram the other histogram
	 * @return the correlation the computed correlation
	 */
	public double correlation(Histogram otherHistogram){
		return correlation(otherHistogram,CorrelationMeasure.INTERSECTION);
	}

	public int displacementForOptimalCorrelation(Histogram otherHistogram,CorrelationMeasure correlationMeasure){
		int optimalDisplacement=0;//displacement with best correlation
		double maximumCorrelation = -1;//best found correlation
		int numberOfClasses = getNumberOfClasses();

		//current displacement, incremented with class width
		for(int currentDisplacement=0;currentDisplacement < numberOfClasses;currentDisplacement++){
			double currentCorrelation = correlationWithDisplacement(currentDisplacement,otherHistogram,correlationMeasure);
			if(maximumCorrelation < currentCorrelation){
				maximumCorrelation=currentCorrelation;
				optimalDisplacement = currentDisplacement;
			}
		}
		if(optimalDisplacement > getNumberOfClasses()/2.0)
			optimalDisplacement = optimalDisplacement -  getNumberOfClasses();

		return optimalDisplacement;
	}

	public void plotCorrelation(Histogram otherHistogram,CorrelationMeasure correlationMeasure){
		int displacement = displacementForOptimalCorrelation(otherHistogram);
		correlationMeasure.getHistogramCorrelation().plotCorrelation(this, displacement, otherHistogram);
	}

	//-----------------------------------------------------------
	//--                                                       --
	//--                 Plot method                           --
	//--                                                       --
	//-----------------------------------------------------------

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
	public void plot(String fileName, String title) {
		title = title == null ? "" : title;
		SimplePlot plot = new SimplePlot(title);
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
	public void export(String fileName){
		StringBuilder sb = new StringBuilder();
		sb.append("key;value\n");
		for(double key : keySet())
			sb.append(key)
			  .append(";")
			  .append(getCount(key))
			  .append("\n");
		FileUtils.writeFile(sb.toString(), fileName);
	}
}
