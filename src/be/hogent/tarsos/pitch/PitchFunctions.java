package be.hogent.tarsos.pitch;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import javax.imageio.ImageIO;

import org.apache.commons.math.stat.StatUtils;
import org.apache.commons.math.stat.descriptive.DescriptiveStatistics;

import be.hogent.tarsos.pitch.Sample.PitchUnit;
import be.hogent.tarsos.util.FileUtils;
import be.hogent.tarsos.util.histogram.Histogram;

import ptolemy.plot.Plot;

/**
 * @author Joren Six
 *
 */
public class PitchFunctions {
	private PitchFunctions(){}

	/**
	 * Converts the Hertz values to cent. The reference frequency is 32.7032 Hz. 
	 * This is C1 on a piano keyboard with A4 tuned to 440 Hz.
	 * This means that 0 cents is C1; 1200 is C2; 2400 is C3; ... also -1200 cents is C0 
	 * @param pitchValuesInHertz the pitch values in Hertz
	 * @return the values converted to the requested unit. The original list remains unchanged.
	 */
	public static List<Double> convertHertzTo(PitchUnit unit,List<Double> pitchValuesInHertz){
		List<Double> convertedValues = new ArrayList<Double>(pitchValuesInHertz);
		switch(unit){		
		case ABSOLUTE_CENTS:
			convertHertzToAbsoluteCent(convertedValues);
			break;
		case RELATIVE_CENTS:
			convertHertzToRelativeCent(convertedValues);
			break;
		case MIDI_KEY:
			throw new Error("Currently not Implemented");
		case HERTZ:
			break;
		default:
			throw new Error("Unsupported unit: " + unit.name());
		}		
		return convertedValues;
	}

	private static void convertHertzToAbsoluteCent(List<Double> convertedValues){
		//reference frequency of 32.7032... Hz
		//27.5 Hz is A0 (440, 220, 110, 55, 27.5) 
		double reference_frequency = 27.5 * Math.pow(2.0,0.25);//32.7 Hz
		double log_two = Math.log(2.0);
		for(int i=0;i<convertedValues.size();i++){
			Double valueInHertz = convertedValues.get(i);
			double pitchValueInAbsoluteCent = 0.0;
			if(valueInHertz != 0)
				pitchValueInAbsoluteCent = 1200 * Math.log(valueInHertz/reference_frequency) / log_two;
			//else
			//	pitchValueInAbsoluteCent = 0.0 and not log(0) => Infinity
			convertedValues.set(i, pitchValueInAbsoluteCent);
		}			
	}

	/**
	 * Folds the pitch values to one octave. 
	 * E.g. 1203 becomes 3 and 956 remains 956 
	 * @param pitchValuesInCent a list of double values in cent
	 */
	private static void convertHertzToRelativeCent(List<Double> convertedValues){
		convertHertzToAbsoluteCent(convertedValues);
		for(int i=0;i<convertedValues.size();i++){
			Double value = convertedValues.get(i) >= 0 ? convertedValues.get(i) : Math.abs(1200  + convertedValues.get(i)); 
			Double pitchValueInCentFoldedToOneOctave =  value % 1200.0;
			convertedValues.set(i, pitchValueInCentFoldedToOneOctave);
		}		
	}


	/**
	 * Removes all frequencies that are not in the specified band. 
	 * The remaining band consists only of frequencies between minValueInHertz and maxValueInHertz (inclusive).
	 * @param pitchValuesInHertz the values to filter.
	 * @param minValueInHertz the minimum frequency in Hertz.
	 * @param maxValueInHertz the maximum frequency in Hertz.
	 */
	public static void bandwithFilter(List<Double> pitchValuesInHertz,double minValueInHertz,double maxValueInHertz){
		Iterator<Double> it = pitchValuesInHertz.iterator();
		while(it.hasNext()){
			double value = it.next();
			if(value < minValueInHertz || maxValueInHertz < value){
				it.remove();
			}
		}
	}	


	/**
	 * Calculates the median for a list of doubles.
	 * The list is sorted in-place. 
	 * @param The list.
	 * @return The median.
	 * @exception If the list is null or empty the median is undefined and an error is thrown.
	 */
	public static Double median(List<Double> list){
		int size = list.size();
		if(list == null || size==0)
			throw new Error("Mean of an empty list is undefined");

		Collections.sort(list);
		double median=0.0;
		if(size % 2 == 0){
			median = (list.get(size/2) + list.get((size/2)-1))/2.0; 
		}else{
			median = list.get(size/2);
		}
		return median;
	}

	/**
	 * <p>
	 * Applies an order n one-dimensional median filter to the list to filter. The function
	 * considers the signal to be 0 beyond the end points.
	 * The output has the same length as the list to filter.
	 * </p>
	 * <p>
	 * Returns a new list.
	 * </p>
	 * <p>
	 * n must be odd! The function is defined as:<br>
	 * f(i) = median( list(i-n/2) .. list(i+n/2) )<br>
	 * </p>
	 * <pre>
	 *  So for n = 3 and list to filter: 
	 *    [3 7 4]
	 *  0 [3 7 4] 0
	 *    [3 4 7]
	 * </pre>
	 * 
	 * @param listToFilter the list to filter
	 * @param n an odd number. The order n of the one-dimensional median filter
	 * @return An order n one-dimensional median filtered list. The original list
	 * remains untouched. A new list is created
	 */
	public static List<Double> medianFilter(List<Double> listToFilter,int n){

		if(n%2==0)
			throw new Error("Medianfilter not implemented for even n values");

		List<Double> filteredList = new ArrayList<Double>();

		int numberOfZeroesToAddBefore= (n-1)/2;
		int numberOfZeroesToAddAfter = (n-1)/2;

		for(int i = 0 ; i < numberOfZeroesToAddBefore;i++){
			listToFilter.add(0,0.0);
		}
		for(int i = 0 ; i < numberOfZeroesToAddAfter;i++){
			listToFilter.add(0.0);
		}

		for(int i = numberOfZeroesToAddBefore ; i < listToFilter.size() - numberOfZeroesToAddAfter; i++){
			double median = median(new ArrayList<Double>(listToFilter.subList(i-(n/2), i+(n/2)+1)));
			filteredList.add(median);
		}

		for(int i = 0 ; i < numberOfZeroesToAddBefore;i++){
			listToFilter.remove(0);
		}
		for(int i = 0 ; i < numberOfZeroesToAddAfter;i++){
			listToFilter.remove(listToFilter.size()-1);
		}
		return filteredList;
	}
	
	public static  List<Double> getGaussianSmoothed(List<Double> listToSmooth , double standardDeviation){
        if(standardDeviation < 0.0) {
            throw new IllegalArgumentException("standardDeviation invalid");
        } else if(standardDeviation == 0.0) {
            return listToSmooth;
        }

        // Create a new, identical but empty Histogram.
        List<Double> smoothedList  = new ArrayList<Double>();


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
        double sum = 0;
        double originalSum = 0;
        
        double[] smoothedValues = new double[listToSmooth.size()];

        for(int b = 0; b < listToSmooth.size(); b++) {
            // Determine clipped range.
            int min = Math.max(b - m, 0);
            int max = Math.min(b + m, listToSmooth.size());

            // Calculate the offset into the weight array.
            int offset = m > b ? m - b : 0;

            // Accumulate the total for the range.
            double acc = 0;
            double weightTotal = 0;
            for(int i = min; i < max; i++) {
                double w = weights[offset++];
                acc += listToSmooth.get(i)*w;
                weightTotal += w;
            }

            // Round the accumulated value.
            smoothedValues[b]=  acc/weightTotal;

            // Accumulate total for band.
            sum += smoothedValues[b];
            originalSum +=  listToSmooth.get(b);
        }
        
       
        
        // Rescale the counts such that the band total is approximately
        // the same as for the same band of the original histogram.
        double factor = originalSum/sum;
        for(int b = 0; b < listToSmooth.size(); b++) {
            double smoothedValue = smoothedValues[b]*factor;
            smoothedList.add(smoothedValue);
        }
        
        assert smoothedList.size() == listToSmooth.size();
        
        return smoothedList;
	}

	/**
	 * Applies a Gaussian filter to the list to filter. The parameter is
	 * arbitrary and can be 1/(2*standard deviation^2). 
	 * 
	 * @param listToFilter the list to filter
	 * @param parameter the parameter defining the impulse response of the filter. 
	 * @return a Gaussian filtered list
	 */
	public static List<Double> gaussianFilter(List<Double> listToFilter,double parameter){
		int windowSize = 7;
		List<Double> filteredList = new ArrayList<Double>();
		
		//double firstFactor = Math.pow(parameter/Math.PI, 0.5) * Math.pow(Math.E, -1 * parameter);
		
		for(int j=0;j<windowSize/2;j++)
			filteredList.add(0.0);
		
		
		for(int i=windowSize/2;i<listToFilter.size() - windowSize/2;i++){	
			double sumValues =  0;
			double sumWeight = 0;
			for(int j=0;j<windowSize;j++){
				double weight = Math.pow(Math.E, -1 * (j - windowSize/2) * (j - windowSize/2) / 2 * parameter);
				
				sumWeight += weight;
				sumValues += weight * listToFilter.get(i);
			}
			double newValue = 1.0/sumWeight  * sumValues / sumWeight;
			
			filteredList.add(newValue);			
		}
	
		for(int j=0;j<windowSize/2;j++)
			filteredList.add(0.0);	
		
		/*
		//scale
		double maxOriginalValue = Double.NEGATIVE_INFINITY;
		double maxNewValue = Double.NEGATIVE_INFINITY;
		double scale = maxOriginalValue / maxNewValue;
		for(int i=0;i<filteredList.size();i++){
			maxOriginalValue = Math.max(maxOriginalValue,originalValue);	
			maxNewValue = Math.max(maxNewValue,newValue);
			filteredList.set(i, filteredList.get(i) * scale);
		}
		*/
		return filteredList;
	}

	/**
	 * Applies a Gaussian filter to the list to filter. The parameter is
	 * 1/(2*standard deviation^2). 
	 * 
	 * @param listToFilter the list to filter
	 * @return An order n one-dimensional median filtered list.
	 */
	public static List<Double> gaussianFilter(List<Double> listToFilter){
		DescriptiveStatistics stats = new DescriptiveStatistics();
		// Add the data from the array
		for(Double value:listToFilter) {
			stats.addValue(value);
		}
		double std = stats.getStandardDeviation();
		double parameter = 1.0/(std * std * 2);
		return gaussianFilter(listToFilter,parameter);			
	}


	/**
	 * Creates a frequency table. The number of items in each class is returned.
	 * Classes are defined by the limit and resolution. E.g. for a limit of 1200
	 * with a resolution of 400 there are 3 classes: 
	 * [0-400[, [400-800[ and [800-1200[. 
	 *
	 * @param values the data to distribute over the bins/classes.
	 * @param resolution the with of the classes
	 * @return The number of items in each class
	 */
	public static Histogram createFrequencyTable(List<Double> values,double classWidth,double start,double stop){
		Histogram histogram = new Histogram(start,stop,(int) ((stop-start)/classWidth));
		for(Double value: values){ 
			histogram.add(value);			
		}
		assert histogram.getSumFreq() == values.size():"Number of items in bins does not correspond with total number of items";

		return histogram;
	}
	
	public enum EportType{
		HISTOGRAM_PNG,
		HISTOGRAM_CSV,
		TONE_SCALE_MIDI,
		PEAKS_PNG,
		PEAKS_CSV
	}

	public static void exportFrequencyTable(Histogram histogram,String fileName,double start,double stop){
		StringBuilder sb = new StringBuilder();
		for(double current  = start + histogram.getClassWidth()/2;current <= stop;current += histogram.getClassWidth()){
			double count = histogram.getCount(current);
			long cumFreq = histogram.getCumFreq(current);
			double derivative = (current + histogram.getClassWidth() > stop)? 0 : (histogram.getCount(current) - histogram.getCount(current + histogram.getClassWidth())) /histogram.getClassWidth();

			double psd = 0.0;
			if (current + 2 * histogram.getClassWidth() <= stop) {
				psd = (histogram.getCount(current + 2 * histogram.getClassWidth()) - histogram.getCount(current) ) / (2 * histogram.getClassWidth());
			}
			//double derivative = (current + frequencyTable.getClassWidth() <= stop)? 0 : (frequencyTable.getCount(current + frequencyTable.getClassWidth()) - frequencyTable.getCount(current)) /frequencyTable.getClassWidth(); 
			sb
			.append(current)
			.append(";")
			.append(count)
			.append(";")
			.append(cumFreq)
			.append(";")
			.append(derivative)
			.append(";")
			.append(psd)
			.append("\n");
		}
		FileUtils.writeFile(sb.toString(), fileName);
	
		
		Plot h = new Plot();
		h.setXRange(start,stop);

		boolean first=true;

		double highWaterMark = 0;
		double[] values = new double[histogram.getNumberOfClasses()];
		int i = 0;
		for(double current  = start + histogram.getClassWidth()/2;current <= stop;current += histogram.getClassWidth()){
			
			h.addPoint(0,current,histogram.getCount(current),!first);
			values[i] = histogram.getCount(current);
			if(histogram.getCount(current) > highWaterMark){
				highWaterMark = histogram.getCount(current);
			}
			i++;
			first=false;
		}
		
		/*	
		List<Peak> peaks = PeakDetector.peakDetection(histogram,fileName + ".midi", start, stop);
		int peakIndex = 1;
		for(Peak peak:peaks){
			int position = peak.getPosition();
			h.addPoint(1,peak.getLift(),peak.getLTop(),false);
			h.addPoint(1,position,peak.getTop(),true);
			h.addPoint(1,peak.getRight(),peak.getRTop(),true);
			peakIndex++;
		}*/
		
		if(stop == 1200.0){
			
			h.setXLabel("n (cents)");
			h.setYLabel("frequency of ocurrence");
			
			//h.addLegend(0,"Pitch histogram");
			
			for(int j = 0 ; j<= 1200 ; j+=100){
				h.addXTick(j + "", j);			
			}
			/*
			h.addXTick("Fifth", reference - 700);
			h.addXTick("Fifth", reference + 700);
			
			h.addXTick("Tritonus", reference - 600);
			h.addXTick("Tritonus", reference + 600);
			
			h.addXTick("Kleine terts",reference + 300);
			h.addXTick("Kleine terts",reference  - 300);
			
			h.addXTick("Grote terts",reference + 400);
			h.addXTick("Grote terts",reference  - 400);
			
			h.setWrap(true);
			*/
			
			h.addYTick("Gem", histogram.getSumFreq() / histogram.getNumberOfClasses() );
			h.addYTick("Med", StatUtils.percentile(values, 0.5));
			h.setXRange(43,1147);
		}
		
		h.setSize(1024,786);

		h.setTitle(FileUtils.basename(fileName));
	
		try {			
			Thread.sleep(60);
			BufferedImage image = h.exportImage();
			ImageIO.write(image, "png", new File(fileName.substring(0,fileName.length()-4) + ".png"));
		}catch (IOException e){
			e.printStackTrace();
		}catch (InterruptedException e1){
			e1.printStackTrace();
		}
	}	

	public static void exportFrequencyTable(Histogram histogram,String fileName){
		exportFrequencyTable(histogram,fileName,histogram.getStart(),histogram.getStop());
	}
	
	
	/**
	 * Reads a frequency table (histogram) from disk. 
	 * The source file is expected to be a CSV-file in the format:
	 * 
	 * <code>value;frequency[;other data; is discarded;...]</code>
	 * 
	 * The lowest value is on the first row, the highest on the last!
	 *  
	 * @param fileName
	 * @return a frequencytable
	 */
	public static Histogram readFrequencyTable(String fileName){
		List<String[]> data = FileUtils.readCSVFile(fileName, ";",-1);
		
		double classWidth = Double.parseDouble(data.get(1)[0]) - Double.parseDouble(data.get(0)[0]);
		double start = Double.parseDouble(data.get(0)[0]) - classWidth / 2.0;
		double stop = Double.parseDouble(data.get(data.size()-1)[0]) + classWidth /2.0;
		
		Histogram table = new Histogram(start,stop,(int) ((stop-start)/classWidth));
		for(String[] row:data){
			int frequency = (int) Double.parseDouble(row[1]);
			double value = Double.parseDouble(row[0]);			
			for(int i = 0;i<frequency;i++){
				table.add(value);
			}
		}
		return table;
	}

}
