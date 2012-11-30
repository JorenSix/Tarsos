package be.hogent.tarsos.cli;

import java.util.Arrays;
import java.util.List;

import joptsimple.OptionParser;
import joptsimple.OptionSet;
import be.hogent.tarsos.util.FileUtils;
import be.hogent.tarsos.util.KernelDensityEstimate;
import be.hogent.tarsos.util.ScalaFile;
import be.hogent.tarsos.util.histogram.HistogramFactory;
import be.hogent.tarsos.util.histogram.PitchClassHistogram;
import be.hogent.tarsos.util.histogram.peaks.Peak;
import be.hogent.tarsos.util.histogram.peaks.PeakDetector;

public class HistogramToScala extends AbstractTarsosApp {

	@Override
	public void run(String... args) {
		final OptionParser parser = new OptionParser();		 
		final OptionSet options = parse(args, parser, this);
		
		if (isHelpOptionSet(options)) {
			printHelp(parser);
		} else if (options.nonOptionArguments().size() == 1) {
			String fileName = options.nonOptionArguments().get(0);
			if (!FileUtils.exists(fileName)) {
				printError(parser, fileName + " does not exist, it should be a CSV file");
			}else{
				List<String[]> data = FileUtils.readCSVFile(fileName, "(,|;)", -1);
				if(data.get(0).length < 2){
					printError(parser,"Expects comma or semi-colon separated data of at least two nummeric fields.");
				} else {
					try{
						actuallyDoSomething(data);
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

	private void actuallyDoSomething(List<String[]> data) {
		double[] accumulator = new double[data.size()];
		for(int i = 0 ; i < data.size(); i++){
			accumulator[i] = Double.valueOf(data.get(i)[1]);
		}
		KernelDensityEstimate kde = new KernelDensityEstimate(new KernelDensityEstimate.GaussianKernel(5),accumulator);
		PitchClassHistogram pch = HistogramFactory.createPitchClassHistogram(kde);
		List<Peak> peaks = PeakDetector.detect(pch);
		double[] peakArray = new double[peaks.size()];
		for(int i=0;i<peaks.size();i++){
			peakArray[i] = peaks.get(i).getPosition();
		}
		Arrays.sort(peakArray);
		ScalaFile scala = new ScalaFile("Detected",peakArray);
		System.out.println(scala.toString());
	}

	@Override
	public String description() {
		return "Creates a scala file for an input pitch (class) histogram";
	}
}
