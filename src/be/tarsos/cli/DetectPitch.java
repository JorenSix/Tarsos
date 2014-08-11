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

import java.io.File;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import joptsimple.OptionParser;
import joptsimple.OptionSet;
import joptsimple.OptionSpec;
import be.tarsos.Tarsos;
import be.tarsos.sampled.pitch.Annotation;
import be.tarsos.sampled.pitch.PitchDetectionMode;
import be.tarsos.sampled.pitch.PitchDetector;
import be.tarsos.transcoder.ffmpeg.EncoderException;
import be.tarsos.util.AudioFile;
import be.tarsos.util.ConfKey;
import be.tarsos.util.Configuration;
import be.tarsos.util.FileUtils;

/**
 * Detects pitch for an input file using a pitch detector. Outputs two columns,
 * one in ms and the oters in Hz.
 * 
 * @author Joren Six
 */
public final class DetectPitch extends AbstractTarsosApp {

	@Override
	public String description() {
		return "Detects pitch for one or more input audio files using a pitch detector. If a directory is given it traverses the directory _recursively_. "
				+ "Writes csv data to standard out with five columns. The first is the start of the analyzed window (seconds), the second the estimated pitch, the third the saillence of the pitch. "
				+ "The name of the algorithm follows and the last column shows the original filename.";
	}
	
	@Override
	public String synopsis(){
		return "[option] input_file..."; 
	}

	@Override
	public void run(final String... args) {
		final OptionParser parser = new OptionParser();
		final OptionSpec<PitchDetectionMode> detectionModeSpec = createDetectionModeSpec(parser);
		final OptionSet options = parse(args, parser, this);
		List<String> arguments = options.nonOptionArguments();
		
		
		//check
		String errorMessage = "";
		for(String audioFile : arguments){
			if(! FileUtils.exists(audioFile) ){
				errorMessage = audioFile + " does not exist.\n";
			}else if(! (FileUtils.isAudioFile(new File(audioFile)) || FileUtils.isDirectory(audioFile) ) ){
				errorMessage = audioFile + " is not a directory or a recognized audio file (according to " + Configuration.get(ConfKey.audio_file_name_pattern) + ").\n";
			}
		}
		
		if (isHelpOptionSet(options) || arguments.size() == 0) {
			printHelp(parser);
		} else if(errorMessage !="") {
			printError(parser, errorMessage);
		} else {
			final PitchDetectionMode detectionMode = options.valueOf(detectionModeSpec);
			executeApplication(arguments,detectionMode);
		}
	}
	
	public void executeApplication(List<String> arguments,final PitchDetectionMode detectionMode){
		Set<File> files = getAudioFileListFromArguments(arguments);
		Tarsos.println("Start(s),Frequency(Hz),Probability,Source,file");
		for(File inputFile : files){
			AudioFile audioFile;
			try {
				audioFile = new AudioFile(inputFile.getAbsolutePath());
				final PitchDetector detector = detectionMode.getPitchDetector(audioFile);
				detector.executePitchDetection();
				for (final Annotation sample : detector.getAnnotations()) {
					Tarsos.println(sample.toString() + "," + inputFile.getAbsolutePath());
				}
			} catch (EncoderException e) {
				//log message
			}
		}
		
	}
	
	private Set<File> getAudioFileListFromArguments(List<String> arguments){
		Set<File> files = new HashSet<File>();
		for(int i = 0 ; i < arguments.size() ; i++){
			File file = new File(arguments.get(i));
			//Recursively traverse directory
			if(file.isDirectory()){
				for(String fileInDir : FileUtils.glob(file.getAbsolutePath(), Configuration.get(ConfKey.audio_file_name_pattern), true)){
					files.add(new File(fileInDir));
				}
			// or else add the file	
			} else if(FileUtils.isAudioFile(file)){
				files.add(file);
			}
		}
		return files;
	}
}
