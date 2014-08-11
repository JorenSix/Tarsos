package be.tarsos.cli;

import java.util.Arrays;
import java.util.List;

import joptsimple.OptionParser;
import joptsimple.OptionSet;
import be.tarsos.util.FileUtils;
import be.tarsos.util.KernelDensityEstimate;
import be.tarsos.util.ScalaFile;
import be.tarsos.util.histogram.HistogramFactory;
import be.tarsos.util.histogram.PitchClassHistogram;
import be.tarsos.util.histogram.peaks.Peak;
import be.tarsos.util.histogram.peaks.PeakDetector;

public class HistogramToScala extends AbstractTarsosApp {

	@Override
	public void run(String... args) {
		final OptionParser parser = new OptionParser();		 
		final OptionSet options = parse(args, parser, this);
		
		if (isHelpOptionSet(options)) {
			printHelp(parser);
		} else if (options.nonOptionArguments().size() == 1) {
			String fileName = options.nonOptionArguments().get(0);
			if(fileName.endsWith(".txt")){
				String[] data = FileUtils.readFile(fileName).split("\n");
				for(String csvFile:data){
					try{
						handleCSVFile(csvFile, parser);
					}catch(Exception e){
						e.printStackTrace();
					}
				}
			}else{
				handleCSVFile(fileName, parser);
			}
		} else {
			for(String fileName:options.nonOptionArguments()){
				handleCSVFile(fileName, parser);
			}
		}
	}
	
	private void handleCSVFile(String fileName,OptionParser parser){
		if (!FileUtils.exists(fileName)) {
			printError(parser, fileName + " does not exist, it should be a CSV file");
		}else{
			List<String[]> data = FileUtils.readCSVFile(fileName, "(,|;)", -1);
			if(data.get(0).length < 2){
				printError(parser,"Expects comma or semi-colon separated data of at least two nummeric fields.");
			} else {
				try{
					actuallyDoSomething(data,fileName);
				}catch(NumberFormatException e){
					printError(parser,"Expects comma or semi-colon separated data of at least two nummeric fields." + e + " is not a number.");	
				}catch(ArrayIndexOutOfBoundsException e){
					printError(parser,"Array index out of bounds, expects comma or semi-colon separated data of at least two nummeric fields.");
				}
			}
		}
	}

	private void actuallyDoSomething(List<String[]> data,String csvFileName) {
		double[] accumulator = new double[data.size()];
		for(int i = 0 ; i < data.size(); i++){
			accumulator[i] = Double.valueOf(data.get(i)[1]);
		}
		KernelDensityEstimate kde = new KernelDensityEstimate(new KernelDensityEstimate.GaussianKernel(5),accumulator);
		PitchClassHistogram pch = HistogramFactory.createPitchClassHistogram(kde);
		List<Peak> peaks = PeakDetector.detect(pch);
		double[] peakArray = new double[peaks.size()];
		for(int i=0;i<peaks.size();i++){
			peakArray[i] = refinePeakLocation(kde,peaks.get(i).getPosition());
		}
		Arrays.sort(peakArray);
		ScalaFile scala = new ScalaFile("Detected",peakArray);
		
		FileUtils.writeFile(scala.toString(),csvFileName.replace("csv", "scl") );
		System.out.println("Created " + csvFileName.replace("csv", "scl"));
	}
	
	private double refinePeakLocation(KernelDensityEstimate kde, double currentPeak){
		double maxValue = -1000;
		int refinedPeakLocation = 0;
		for(int peak = (int) Math.max(currentPeak - 5,0) ; peak < Math.min(kde.size(), currentPeak+5) ; peak++){
			if(kde.getValue(peak)>maxValue){
				maxValue = kde.getValue(peak);
				refinedPeakLocation = peak;
			}
		}
	   return refinedPeakLocation;
	}

	@Override
	public String description() {
		return "Creates a scala file for an input pitch (class) histogram";
	}
}
