package be.hogent.tarsos.pitch;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.ListIterator;

import javax.imageio.ImageIO;

import be.hogent.tarsos.util.FileUtils;
import be.hogent.tarsos.util.histogram.Histogram;

import ptolemy.plot.Plot;

/**
 * @author Joren Six
 * A sample is a collection of pitches with corresponding probabilities.  
 */
public class Sample implements Comparable<Sample> {
	
	private long start;	
	private final List<Double> pitches;//in hertz
	private final List<Double> probabilities;//values from 0 to 1 (inclusive). One probability per pitch.
	
	public enum PitchUnit{
		HERTZ,
		ABSOLUTE_CENTS,
		RELATIVE_CENTS,
		MIDI_KEY;
	}
	
	public SampleSource source;
	
	public enum SampleSource{
		IPEM,
		AUBIO_YIN,
		AUBIO_YINFFT,
		AUBIO_SCHMITT,
		AUBIO_FCOMB,
		AUBIO_MCOMB
	}
	
	
	public Sample(long start,List<Double> pitches,List<Double> probabilities){
		this(start,pitches,probabilities,0);
	}
	
	public Sample(long start,List<Double> pitches,List<Double> probabilities,double minimumAcceptableProbability){
		this(start);
		for(int i=0;i<probabilities.size();i++){
			Double probability = probabilities.get(i);
			if(probability >= minimumAcceptableProbability){
				this.pitches.add(pitches.get(i));
				this.probabilities.add(probability);
			}
		}
		this.start=start;
	}
	
	public Sample(long start,double pitch){
		this(start);
		this.pitches.add(pitch);
		this.probabilities.add(1.0);
	}

	public Sample(long start) {
		this.start=start;
		this.pitches = new ArrayList<Double>();
		this.probabilities = new ArrayList<Double>();
	}

	/**
	 * Returns a list of non harmonic frequencies. This function only makes sense on polyphonic
	 * pitch samples. 
	 * @param unit 
	 * @param errorPercentage defines the limits of what is seen as a harmonic. 
	 * The list [100Hz,202Hz,230Hz] is reduced to [100Hz,230Hz] 
	 * if the error percentage is greater than or equal to 2% (0.02)
	 *  
	 * @return a reduced list without harmonics
	 */
	public List<Double> getPitchesWithoutHarmonicsIn(PitchUnit unit,double errorPercentage){
		
		if(pitches.size()==1)
			return pitches; 
		
		int numberOfHarmonicsToRemove = 5;
		List<Double> pitches = new ArrayList<Double>(this.pitches);
		List<Double> pitchesWithoutHarmonics = new ArrayList<Double>();
		Collections.sort(pitches);
		for(Double pitch : pitches){
			boolean pitchIsHarmonic = false;
			for(int i=2; i <= numberOfHarmonicsToRemove;i++){
				Double pitchToCheck = pitch/i;
				Double deviation = pitchToCheck * errorPercentage;
				Double maxPitchLimit = pitchToCheck + deviation;
				Double minPitchLimit = pitchToCheck - deviation;
				for(Double pitchToCheckWith : pitches){
					if(maxPitchLimit  >= pitchToCheckWith  && pitchToCheckWith >= minPitchLimit){
						//System.out.println(pitch  + " is harmonic of " + pitchToCheckWith + ": " + maxPitchLimit + " >= " + pitchToCheckWith + " >= " + minPitchLimit);
						pitchIsHarmonic = true;						
					}					
				}
			}
			if(!pitchIsHarmonic)
				pitchesWithoutHarmonics.add(pitch);
		}
		return PitchFunctions.convertHertzTo(unit, pitchesWithoutHarmonics);		
	}
	
	public List<Double> getPitchesIn(PitchUnit unit){
		return PitchFunctions.convertHertzTo(unit, pitches);
	}

	public long getStart() {
		return this.start;
	}
	
	public static Histogram printOctaveInformation(String fileName, List<Sample> samples) {
		List<Double> pitchValuesInAbsoluteCent = new ArrayList<Double>();
		double maxPitchInAbsoluteCents= Double.NEGATIVE_INFINITY;
		double minPitchInAbsoluteCents = Double.POSITIVE_INFINITY;
		for(Sample sample:samples){
			for(Double pitch: sample.getPitchesIn(PitchUnit.ABSOLUTE_CENTS)){
				//only values greater than 32.7Hz (the reference cent value )
				if(pitch > 0){
					pitchValuesInAbsoluteCent.add(pitch);
					maxPitchInAbsoluteCents = Math.max(maxPitchInAbsoluteCents, pitch);
					minPitchInAbsoluteCents = Math.min(minPitchInAbsoluteCents, pitch);
				}				
			}
		}
		
		//List<Double> newPitchValuesInCent  = PitchFunctions.medianFilter(pitchValuesInCent,9);			
		//assert pitchValuesInCent.size() == newPitchValuesInCent.size();
		//Histogram histogram = PitchFunctions.createFrequencyTable(pitchValuesInRelativeCent,6.0,0,1200);
		//PitchFunctions.exportFrequencyTable(histogram, "data/octave/"+ fileName);
		
		double stop = Math.ceil(maxPitchInAbsoluteCents / 1200) * 1200;
		double start = minPitchInAbsoluteCents > 0 ? Math.floor(minPitchInAbsoluteCents / 1200) * 1200 : 0;		
		stop = stop == Double.NEGATIVE_INFINITY ? 12000 : stop ;
		start = start == Double.POSITIVE_INFINITY ? 0 : start;		
		Histogram histogram = PitchFunctions.createFrequencyTable(pitchValuesInAbsoluteCent,6.0,start,stop);
		
		Plot h = new Plot();
		h.setXRange(0,1200);
		
		/*int numberOfColors = 7;
		Color [] spectrum = new Color[ numberOfColors ];

	      // Generate the colors and store them in the array.
	      for ( int i = 0; i < numberOfColors; ++i ) {
	         // Here we specify colors by Hue, Saturation, and Brightness,
	         // each of which is a number in the range [0,1], and use
	         // a utility routine to convert it to an RGB value before
	         // passing it to the Color() constructor.
	         spectrum[i] = new Color(Color.HSBtoRGB((float) ((i/(float)(numberOfColors*2))+ 0.5),(float)1,(float)1));
	      }

		
		h.setColors(spectrum);
		*/
		
		Histogram octaveHistogram = new Histogram(0,1200,200);
		
		for(double octaveKey : octaveHistogram.keySet()){
			long sum = 0;
			int dataset=0;
			for(double rangeKey = octaveKey ; rangeKey < histogram.getStop() + histogram.getClassWidth()/2; rangeKey += 1200 ){
				sum = sum + histogram.getCount(rangeKey);
				dataset = (int)(rangeKey / 1200);
				h.addPoint(dataset,octaveKey - histogram.getStart(),sum,true);			
			}
			octaveHistogram.setCount(octaveKey,sum);
		}
		
		for(double temp = histogram.getStart() + histogram.getClassWidth()/2 ; temp < histogram.getStop() + histogram.getClassWidth()/2; temp += 1200 ){
			int dataset = (int)(temp / 1200);
			h.addLegend(dataset,"C" + dataset + "-B" + dataset + " [" + dataset * 1200  + "-"+ ((dataset * 1200) + 1199) +"]" );			
		}
		
		h.setSize(1680,1050);
		h.setTitle(FileUtils.basename(fileName));
	
		try {	
			Thread.sleep(60);
			BufferedImage image = h.exportImage();
			ImageIO.write(image, "png", new File("data/range/" + fileName.substring(0,fileName.length()-4) + "color.octave.png"));
		}catch (IOException e){
			e.printStackTrace();
		}catch (InterruptedException e1){
			e1.printStackTrace();
		}	
		return octaveHistogram;
	}
	
	public void removeUniquePitches(Sample other,double errorPercentage){		
		ListIterator<Double> thisPitchIterator = pitches.listIterator();
		while(thisPitchIterator.hasNext()){
			Double thisPitch = thisPitchIterator.next();
			Double deviation = thisPitch * errorPercentage;
			Double maxPitchLimit = thisPitch + deviation;
			Double minPitchLimit = thisPitch - deviation;
			boolean removeThisPitch = other.pitches.size() != 0;
			for(Double otherPitch:other.pitches){
				if(maxPitchLimit  >= otherPitch && otherPitch >= minPitchLimit){
					removeThisPitch = false;
				}
			}
			if(removeThisPitch && pitches.size() > 1){
				thisPitchIterator.remove();	
			}
		}
	}
	
	public double returnMatchingPitch(Sample other,double errorPercentage){		
		if(pitches.size()==0)
			return Double.NEGATIVE_INFINITY;
		
		Double thisPitch = pitches.get(0);
		Double deviation = thisPitch * errorPercentage;
		Double maxPitchLimit = thisPitch + deviation;
		Double minPitchLimit = thisPitch - deviation;
		
		for(Double otherPitch:other.pitches){
			if(maxPitchLimit  >= otherPitch && otherPitch >= minPitchLimit){
				return (otherPitch + thisPitch)/2;
			}
		}
		
		return Double.NEGATIVE_INFINITY;
	}
		
	public static void printRangeInformation(String fileName, List<Sample> samples) {		
		List<Double> pitchValuesInCent = new ArrayList<Double>();
		double maxPitchInAbsoluteCents= Double.NEGATIVE_INFINITY;
		double minPitchInAbsoluteCents = Double.POSITIVE_INFINITY;
		for(Sample sample:samples){
			for(Double pitch : sample.getPitchesIn(PitchUnit.ABSOLUTE_CENTS)){
				pitchValuesInCent.add(pitch);
				maxPitchInAbsoluteCents = Math.max(maxPitchInAbsoluteCents, pitch);
				minPitchInAbsoluteCents = Math.min(minPitchInAbsoluteCents, pitch);				
			}
		}		
		double stop = Math.ceil(maxPitchInAbsoluteCents / 1200) * 1200;
		double start = minPitchInAbsoluteCents > 0 ? Math.floor(minPitchInAbsoluteCents / 1200) * 1200 : 0;
		
		stop = stop == Double.NEGATIVE_INFINITY ? 12000 : stop ;
		start = start == Double.POSITIVE_INFINITY ? 0 : start;
		
		Histogram histogram = PitchFunctions.createFrequencyTable(pitchValuesInCent,6.0,start,stop);
		
		Plot h = new Plot();
		h.setXRange(histogram.getStart(),histogram.getStop());

		for(double current  : histogram.keySet()){
			h.addPoint((int)(current / 1200),current,histogram.getCount(current),true);	
		}
		
		h.setSize(1024,786);

		h.setTitle(FileUtils.basename(fileName));
	
		try {			
			Thread.sleep(60);
			BufferedImage image = h.exportImage();
			ImageIO.write(image, "png", new File("data/range/" + fileName.substring(0,fileName.length()-4) + "color.png"));
		}catch (IOException e){
			e.printStackTrace();
		}catch (InterruptedException e1){
			e1.printStackTrace();
		}
		//PitchFunctions.exportFrequencyTable(histogram, "data/range/"+ fileName);
	}
	
	public static void findPeaks(List<Sample> samples) {
		List<Double> pitchValuesInCent = new ArrayList<Double>();
		for(Sample sample:samples){
			for(Double pitch : sample.getPitchesIn(PitchUnit.RELATIVE_CENTS)){
				pitchValuesInCent.add(pitch);
			}
		}
		//Histogram frequencyTable = PitchFunctions.createFrequencyTable(pitchValuesInCent,6.0);
		//PitchFunctions.peakDetection(frequencyTable,0.0,1200.0);
	}

	@Override
	public int compareTo(Sample o) {
		//starttime first
		int startCompare = new Long(start).compareTo(new Long(o.start));
		//then order by source name
		return startCompare == 0 ? source.toString().compareTo(o.source.toString()) : startCompare;
	}
	
	
}
