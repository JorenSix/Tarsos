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

package be.tarsos.cli;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import joptsimple.OptionParser;
import joptsimple.OptionSet;
import joptsimple.OptionSpec;
import be.tarsos.midi.ToneSequenceBuilder;
import be.tarsos.midi.ToneSequenceBuilder.AnnotationCVSFileHandlers;
import be.tarsos.midi.ToneSequenceBuilder.CSVFileHandler;
import be.tarsos.util.FileUtils;

/**
 * Generates audio from a set of annotations. AnnotationSynth is used to
 * sonificate pitch annotation files. For the moment it understands the pitch
 * files used by BOZKURT, AUBIO and IPEM_SIX. It reads the data from a file or
 * from STDIN.
 * 
 * @author Joren Six
 */
public final class AnnotationSynth extends AbstractTarsosApp {
	/**
	 * Log messages.
	 */
	private static final Logger LOG = Logger.getLogger(AnnotationSynth.class.getName());

	@Override
	public String description() {
		return "Generates audio from a set of annotations. AnnotationSynth is used to "
				+ "sonificate pitch annotation files. For now it uderstands the pitch "
				+ "files used by BOZKURT, AUBIO and IPEM_SIX. It reads the data from a file or from STDIN.";
	}

	@Override
	public void run(final String... args) {
		final OptionParser parser = new OptionParser();

		final OptionSpec<File> outputSpec = parser.accepts("out", "Output WAV-file.").withRequiredArg()
				.ofType(File.class);

		final OptionSpec<AnnotationCVSFileHandlers> annoFormatSpec = parser
				.accepts("format", "Annotation format of the input file: AUBIO|IPEM_SIX|BOZKURT")
				.withRequiredArg().ofType(AnnotationCVSFileHandlers.class)
				.defaultsTo(AnnotationCVSFileHandlers.TARSOS);

		final OptionSpec<Integer> filterSpec = parser
				.accepts(
						"filter",
						"Defines the number of samples are used in a median filter. "
								+ "With samples every 10ms and a median filter of 5 "
								+ "there can be a 50/2 ms delay").withRequiredArg().ofType(Integer.class)
				.defaultsTo(0);

		final OptionSet options = parse(args, parser, this);

		if (isHelpOptionSet(options)) {
			printHelp(parser);
		} else {
			
			final int filterSize = options.valueOf(filterSpec);
			//final boolean listen = options.has("listen");
			final ToneSequenceBuilder builder = new ToneSequenceBuilder();
				
			final CSVFileHandler handler = options.valueOf(annoFormatSpec).getCvsFileHandler();
			final File outputFile = options.valueOf(outputSpec);
			if(options.nonOptionArguments().isEmpty()){
				readFromStandardInput(handler,builder);
			} else {
				for(String inputFile : options.nonOptionArguments()){
					final List<String[]> rows = FileUtils.readCSVFile(inputFile, handler.getSeparator(), -1);
					for (final String[] row : rows) {
						try{
							handler.handleRow(builder, row);
						}catch(NumberFormatException e){
							
						}
					}
				}
			}

			if (options.valueOf(outputSpec) == null) {
				builder.playAnnotations(filterSize);
			} else {
				try {
					builder.writeFile(outputFile.getAbsolutePath(), filterSize,null);
				} catch (final Exception e) {
					LOG.log(Level.SEVERE, "Could not write: " + outputFile + "\n", e);
				}
			}
		}
		
		
	}
	
	private void readFromStandardInput(CSVFileHandler handler, ToneSequenceBuilder builder){
		final BufferedReader stdIn = new BufferedReader(new InputStreamReader(System.in));
		String line;
		try {
			line = stdIn.readLine();
			while (line != null && line.length() != 0) {
				final String[] row = line.split(handler.getSeparator());
				try{
					handler.handleRow(builder, row);
				}catch(NumberFormatException e){
					
				}		
				line = stdIn.readLine();
			}
		} catch (final IOException e1) {
			LOG.log(Level.SEVERE, "Could not read from standard input.", e1);
		}
	}
}
