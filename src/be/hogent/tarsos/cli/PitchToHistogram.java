package be.hogent.tarsos.cli;

import java.util.List;

import joptsimple.OptionParser;
import joptsimple.OptionSet;
import joptsimple.OptionSpec;
import be.hogent.tarsos.Tarsos;
import be.hogent.tarsos.sampled.pitch.PitchUnit;
import be.hogent.tarsos.util.FileUtils;
import be.hogent.tarsos.util.KernelDensityEstimate;
import be.hogent.tarsos.util.KernelDensityEstimate.GaussianKernel;

/**
 * Creates a pitch (class) histogram from an input file containing pitch data.
 * It uses a kernel density method.
 * @author Joren Six
 */
public class PitchToHistogram extends AbstractTarsosApp {
	
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
						actuallyDoSomething(data,detectionModeSpec.value(options));
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
	
	private void actuallyDoSomething(List<String[]> data, HistogramType histogramType) {
		int start = 0;
		try{
			Double.valueOf(data.get(0)[0]);
			start = 0;
		}catch(NumberFormatException e){
			start = 1;	
		}
		int size = histogramType == HistogramType.PITCH_CLASS_HISTOGRAM ? 1200 : 9600;  
		KernelDensityEstimate kde = new KernelDensityEstimate(new GaussianKernel(5),size);
		
		for(int i = start ; i < data.size();i++){
			double pitch = 0;
			if(histogramType == HistogramType.PITCH_CLASS_HISTOGRAM){
				pitch = PitchUnit.hertzToRelativeCent(Double.valueOf(data.get(i)[1]));
			} else {
				pitch = PitchUnit.hertzToAbsoluteCent(Double.valueOf(data.get(i)[1]));
			}
			kde.add(pitch);
		}
		
		double[] estimate = kde.getEstimate();
		for(int i = 0; i< estimate.length ;i++){
			Tarsos.println(i + "," + estimate[i]);
		}
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
