package be.tarsos.cli;

import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import joptsimple.OptionParser;
import joptsimple.OptionSet;
import joptsimple.OptionSpec;
import be.tarsos.Tarsos;
import be.tarsos.util.FileUtils;
import be.tarsos.util.KernelDensityEstimate;
import be.tarsos.util.KernelDensityEstimate.GaussianKernel;
import be.tarsos.util.histogram.Histogram;
import be.tarsos.util.histogram.HistogramFactory;
import be.tarsos.util.histogram.PitchClassHistogram;
import be.tarsos.util.histogram.peaks.Peak;
import be.tarsos.util.histogram.peaks.PeakDetector;

/**
 * Creates a pitch (class) histogram from an input file containing pitch data.
 * It uses a kernel density method.
 * @author Joren Six
 */
public class PitchHistogramRating extends AbstractTarsosApp {
	
	public static enum HistogramType{PITCH_CLASS_HISTOGRAM,PITCH_HISTOGRAM}

	@Override
	public void run(String... args) {
		final OptionParser parser = new OptionParser();		 
		
		
		final StringBuilder names = new StringBuilder();
		for (final HistogramType modes : HistogramType.values()) {
			names.append(modes.name()).append(" | ");
		}
		final String descr = "The histogram type to construct [" + names.toString() + "]";
		final OptionSpec<HistogramType> detectionModeSpec =  parser.accepts("histogram-type", descr).withRequiredArg().ofType(HistogramType.class).defaultsTo(HistogramType.PITCH_CLASS_HISTOGRAM);
		
		final OptionSpec<Boolean> listModeSpec =  parser.accepts("list", "Argument is a list of histograms").withOptionalArg().ofType(Boolean.class).defaultsTo(false);
		final OptionSet options = parse(args, parser, this);
		
		if (isHelpOptionSet(options)) {
			printHelp(parser);
		} else if (options.nonOptionArguments().size() == 1 && listModeSpec.value(options)) {
			//process list.
			String fileName = options.nonOptionArguments().get(0);	
			HistogramType type = detectionModeSpec.value(options);
			processList(fileName, parser,type);
		} else if (options.nonOptionArguments().size() == 1 ) {
			String fileName = options.nonOptionArguments().get(0);
			if (!FileUtils.exists(fileName)) {
				printError(parser, fileName + " does not exist, it should be a CSV file");
			}else{
				HistogramType type = detectionModeSpec.value(options);
				List<String[]> data = FileUtils.readCSVFile(fileName, "(,|;)", -1);
				
				if(data.get(0).length < 2){
					printError(parser,"Expects comma or semi-colon separated data of at least two nummeric fields.");
				} else {
					try{
						Tarsos.println(fileName + " ; " + actuallyDoSomething(data,type));
					}catch(NumberFormatException e){
						printError(parser,"Expects comma or semi-colon separated data of at least two nummeric fields." + e + " is not a number.");	
					}catch(ArrayIndexOutOfBoundsException e){
						printError(parser,"Array index out of bounds, expects comma or semi-colon separated data of at least two nummeric fields.");
					}
				}
			}
		} else {
			printError(parser, "Accepts exactly one CSV file, no more, no less.");
		}
	}
	
	private void processList(String fileName,OptionParser parser,HistogramType type){
		if (!FileUtils.exists(fileName)) {
			printError(parser, fileName + " does not exist, it should be a CSV file");
		}else{
			List<String[]> fileData = FileUtils.readCSVFile(fileName, ";", 1);
			int counter = 0;
			for(String[] row : fileData){
				String pitchFile = row[0];
				List<String[]> data = FileUtils.readCSVFile(pitchFile, "(,|;)", -1);
				if(!data.isEmpty()){
					if(data.get(0).length < 2){
						printError(parser,"Expects comma or semi-colon separated data of at least two nummeric fields.");
					} else {
						Tarsos.println((++counter) + ";" + pitchFile + ";" + actuallyDoSomething(data, type)) ;							
					}
				}
			}
		}
	}
	
	private String actuallyDoSomething(List<String[]> data, HistogramType histogramType) {
		int start = 0;
		int size = histogramType == HistogramType.PITCH_CLASS_HISTOGRAM ? 1200 : 9600;  
		
		double[] accumulator = new double[size]; 
		for(int i = start ; i < data.size();i++){
			accumulator[i] = Double.valueOf(data.get(i)[1]) * 1000;
		}
		KernelDensityEstimate kde = new KernelDensityEstimate(new GaussianKernel(5),accumulator);
		Histogram h = HistogramFactory.createPitchClassHistogram(kde);
		h = h.gaussianSmooth(1.0);
		
		PitchClassHistogram pch = HistogramFactory.createPitchClassHistogram(kde);
		
		
		List<Peak> peaks = PeakDetector.detect(pch);
		List<Peak> otherPeaks = PeakDetector.detect(pch,13,24);
		List<Peak> stillOtherPeaks = PeakDetector.detect(pch,13,30);
		
		int maxPeakSize = Math.max(peaks.size(), otherPeaks.size());
		maxPeakSize = Math.max(maxPeakSize, stillOtherPeaks.size());
		
		int minPeakSize = Math.min(peaks.size(), otherPeaks.size());
		minPeakSize = Math.min(minPeakSize, stillOtherPeaks.size());
		
		double peakStabilityScore = minPeakSize / (double) maxPeakSize;
		
		
		
		double[] peakArray = new double[peaks.size()];
		double[] heightArray = new double[peaks.size()];
		double[] widthArray = new double[peaks.size()];
		Collections.sort(peaks,new Comparator<Peak>() {
			@Override
			public int compare(Peak o1, Peak o2) {
				return new Double(o1.getPosition()).compareTo(o2.getPosition());
			}
		});
		for(int i=0;i<peaks.size();i++){
			peakArray[i] = peaks.get(i).getPosition();
			heightArray[i] = peaks.get(i).getHeight();
			double halfHeight = heightArray[i]/2.0;
			double width = 50;
			for(double j= peakArray[i];j<peakArray[i]  + 50;j++){
				if( pch.getCountForClass(i)   <  halfHeight ){
					width = j - peakArray[i];
				}
			}
			widthArray[i] = width;
			 
		}
		Arrays.sort(peakArray);
		
		double score = 0;
		if(peaks.size() > 3){
			Histogram other = PitchClassHistogram.createToneScale(peakArray, heightArray);
			score = (2 * other.correlation(h) + peakStabilityScore) / 3.0;
		}
		return "" + score;
	}

	@Override
	public String synopsis(){
		return "input_file.csv"; 
	}

	@Override
	public String description() {
		return "Creates a pitch class histogram for a given list of pitches." +
				" It expects data to come in as 'timestamp(s);pitch(Hz)' pairs. ";
	}

}
