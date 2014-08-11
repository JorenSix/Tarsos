package be.tarsos.cli;

import java.io.File;
import java.util.List;

import joptsimple.OptionParser;
import joptsimple.OptionSet;
import joptsimple.OptionSpec;
import be.tarsos.Tarsos;
import be.tarsos.sampled.pitch.PitchUnit;
import be.tarsos.util.FileUtils;
import be.tarsos.util.KernelDensityEstimate;
import be.tarsos.util.KernelDensityEstimate.GaussianKernel;

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
		final OptionSpec<Boolean> listModeSpec =  parser.accepts("list", descr).withOptionalArg().ofType(Boolean.class).defaultsTo(false);
		final OptionSet options = parse(args, parser, this);
		
		if (isHelpOptionSet(options)) {
			printHelp(parser);
		} else if (options.nonOptionArguments().size() == 1 && listModeSpec.value(options)) {
			//process list.
			String fileName = options.nonOptionArguments().get(0);	
			processList(fileName, parser);
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
						Tarsos.println(actuallyDoSomething(data,type));
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
	
	private void processList(String fileName,OptionParser parser){
		if (!FileUtils.exists(fileName)) {
			printError(parser, fileName + " does not exist, it should be a CSV file");
		}else{
			List<String[]> fileData = FileUtils.readCSVFile(fileName, ";", 3);
			int counter = 0;
			for(String[] row : fileData){
				String pitchFile = row[0];
				String phFile = row[1];
				String pchFile = row[2];
				Tarsos.println((++counter) + " " + pitchFile);
				
				List<String[]> data = FileUtils.readCSVFile(pitchFile, "(,|;)", -1);
				if(!data.isEmpty()){
					if(data.get(0).length < 2){
						printError(parser,"Expects comma or semi-colon separated data of at least two nummeric fields.");
					} else {
						try{
							if(!new File(pchFile).exists()){
								String pchData = actuallyDoSomething(data,HistogramType.PITCH_CLASS_HISTOGRAM);
								FileUtils.writeFile(pchData, pchFile);
							}
							
							if(!new File(phFile).exists()){
								String phData = actuallyDoSomething(data,HistogramType.PITCH_HISTOGRAM);
								FileUtils.writeFile(phData, phFile);
							}
							
							String pcshFile = pchFile.replace("pch.", "pcsh.");
							if(!new File(pcshFile).exists()){
								String smallPchData = smallPCH(data);
								FileUtils.writeFile(smallPchData,pcshFile);
							} else {
								System.out.println("Already exists: " + pcshFile);
							}
						}catch(NumberFormatException e){
							printError(parser,"Expects comma or semi-colon separated data of at least two nummeric fields." + e + " is not a number.");	
						}catch(ArrayIndexOutOfBoundsException e){
							printError(parser,"Array index out of bounds, expects comma or semi-colon separated data of at least two nummeric fields.");
						}
					}
				}
				
			}
		}
	}
	
	private String actuallyDoSomething(List<String[]> data, HistogramType histogramType) {
		StringBuilder sb = new StringBuilder();
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
			sb.append(i).append(",").append(estimate[i]).append("\n");
		}
		return sb.toString();
	}
	
	private String smallPCH(List<String[]> data) {
		StringBuilder sb = new StringBuilder();
		int start = 0;
		try{
			Double.valueOf(data.get(0)[0]);
			start = 0;
		}catch(NumberFormatException e){
			start = 1;	
		}
		int size = 256;  
		KernelDensityEstimate kde = new KernelDensityEstimate(new GaussianKernel(5),size);
		
		double scaleFactor = 256.0/1200.0;
		for(int i = start ; i < data.size();i++){
			double pitch = 0;
			pitch = PitchUnit.hertzToRelativeCent(Double.valueOf(data.get(i)[1]));
			kde.add(pitch * scaleFactor);
		}
		
		kde.normalize(256);
		double[] estimate = kde.getEstimate();
		for(int i = 0; i< estimate.length ;i++){
			sb.append(i).append(",").append(Math.round(estimate[i])).append("\n");
		}
		return sb.toString();
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
