package be.tarsos.cli;

import java.io.File;
import java.util.List;

import joptsimple.OptionParser;
import joptsimple.OptionSet;
import joptsimple.OptionSpec;
import be.tarsos.util.FileUtils;
import be.tarsos.util.ScalaFile;

/**
 * Creates a pitch (class) histogram from an input file containing pitch data.
 * It uses a kernel density method.
 * @author Joren Six
 */
public class PrintScalaIntervals extends AbstractTarsosApp {

	@Override
	public void run(String... args) {
		final OptionParser parser = new OptionParser();
	
		final OptionSpec<Boolean> listModeSpec =  parser.accepts("list", "Argument is a list of scala files").withOptionalArg().ofType(Boolean.class).defaultsTo(false);
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
				printError(parser, fileName + " does not exist, it should be a scala file");
			}else{
				processScalaFile(parser,new File(fileName));
			}
		} else {
			printError(parser, "Accepts exactly one file, no more, no less.");
		}
	}
	
	private void processList(String fileName,OptionParser parser){
		if (!FileUtils.exists(fileName)) {
			printError(parser, fileName + " does not exist, it should be a CSV file");
		}else{
			List<String[]> fileData = FileUtils.readCSVFile(fileName, ";", 1);
			for(String[] row : fileData){
				String scalaFile = row[0];
				if(FileUtils.exists(scalaFile)){
					processScalaFile(parser,new File(scalaFile));
				}
			}
		}
	}
	
	private String processScalaFile(OptionParser parser,File scalaFile) {
		
		ScalaFile scala = new ScalaFile(scalaFile.getAbsolutePath());
		List<Integer> intervals = scala.getIntervals(false);
		
		System.out.print(scalaFile.getAbsolutePath()  + ";");
		
		/*
		//interessante intervallen tellen
		int [] referenceIntervals ={
				100, //minor second (ji 112)
				171, //7 TET
				200, //major second (ji 204 or 182)
				240, //5 TET
				300, //minor third (ji 316 or 298)
				400, //major third (ji 386)
				500, //perfect fourth (ji 498)
		};
		int[] referenceIntervalCounts = new int[referenceIntervals.length];
		int maxDeviation = 10;//cents
		*/
		
		
		//aantal tonen
		System.out.print(scala.getPitches().length + ";");
		
		//aantal intervallen
		System.out.print(intervals.size() + ";");
		
		//intervallen zelf
		for(int i = 0 ; i < intervals.size() ; i++){
			int interval = intervals.get(i);
			int rounded = Math.round(interval);
			System.out.print(rounded + ";");
		}
		
		System.out.println();
		return  "";
	}

	@Override
	public String synopsis(){
		return "[scala_file.scl|scala_list.txt] [--list]"; 
	}

	@Override
	public String description() {
		return "Calculates a score for one or more scala files. The score determines how close it is to a western tone scale.";
	}

}
