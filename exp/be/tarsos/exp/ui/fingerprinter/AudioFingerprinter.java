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

package be.tarsos.exp.ui.fingerprinter;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.logging.LogManager;
import java.util.logging.Logger;

import javax.swing.UIManager;

import be.tarsos.Tarsos;
import be.tarsos.sampled.pitch.PitchDetectionMode;
import be.tarsos.sampled.pitch.PitchDetector;
import be.tarsos.transcoder.ffmpeg.EncoderException;
import be.tarsos.util.AudioFile;
import be.tarsos.util.ConfKey;
import be.tarsos.util.Configuration;
import be.tarsos.util.FileUtils;
import be.tarsos.util.KernelDensityEstimate;
import be.tarsos.util.histogram.HistogramFactory;
import be.tarsos.util.histogram.PitchClassHistogram;


public class AudioFingerprinter {
	
	private static final Logger LOG = Logger.getLogger(AudioFingerprinter.class.getName());
	
	final Set<File> hayStack;
	final File needle;
	
	public AudioFingerprinter(Set<File> theHayStack, File theNeedle){
		needle = theNeedle;
		hayStack = theHayStack;
	}
	
	/**
	 * @return An ordered list of AudioFingerprintMatches. The first match is the best.
	 */
	public List<AudioFingerprintMatch> match(){
		Iterator<File> iterator = hayStack.iterator();
		List<AudioFingerprintMatch> list = new ArrayList<AudioFingerprintMatch>();
		while(iterator.hasNext()){
			File grass = iterator.next();
			LOG.info("Matching " + needle.getName() + " with " + grass.getName());
			AudioFingerprintMatch match = new AudioFingerprintMatch(grass,needle);
			match.calculateMatch();
			LOG.info("Matched " + needle.getName() + " with " + grass.getName() + " result is " + Math.round(match.getValue() * 100) + "%.");
			list.add(match);
		}
		Collections.sort(list);
		return list;
	}
	
	public static class AudioFingerprintMatch implements Comparable<AudioFingerprintMatch>{
		
		/**
		 * The cache stores a PCH for a file. It makes the matching much quicker
		 * (the pitch detection and PCH construction step is skipped). With a
		 * large hay stack the cache can become quite large, watch out for heap
		 * size issues.
		 */
		private static HashMap<File,PitchClassHistogram> cache = new HashMap<File, PitchClassHistogram>();
		
		private double value;
		private final File original;
		private final File match;
		
		public AudioFingerprintMatch(File original,File match){
			this.value = -1;
			this.original = original;
			this.match = match;
		}
		
		public double getValue(){
			return value;
		}
		
		public File getOriginal(){
			return original;
		}
		
		public File getMatch(){
			return match;
		}
		
		public void calculateMatch(){
			try {
				PitchClassHistogram originalPCH = createHistogram(original);
				PitchClassHistogram matchPCH = createHistogram(match);
				//allows for pitch shift
				int displacement = matchPCH.displacementForOptimalCorrelation(originalPCH);
				value = matchPCH.correlationWithDisplacement(displacement, originalPCH);
			} catch (EncoderException e) {
				value = -1;
				e.printStackTrace();
				LOG.warning("Could not calculate a pitch class histogram, encoding failed: " + e.getMessage());
			}
		}
		
		private PitchClassHistogram createHistogram(File file) throws EncoderException{
			//If the histogram is not in the cache, create the histogram and put it there
			//The cache can become quite large: watch out for heap overflow issues..
			if(!cache.containsKey(file)){
				AudioFile audioFile;
				audioFile = new AudioFile(file.getAbsolutePath());
				PitchDetector detector = PitchDetectionMode.TARSOS_MPM.getPitchDetector(audioFile);	
				detector.executePitchDetection();
				KernelDensityEstimate kde = HistogramFactory.createPichClassKDE(detector.getAnnotations(), 7);
				cache.put(file, HistogramFactory.createPitchClassHistogram(kde));
			}
			//return the cached histogram
			return cache.get(file);
		}
		
		public boolean isMatch(){
			return value > 0.98;
		}

		public int compareTo(AudioFingerprintMatch o) {
			return Double.valueOf(o.value).compareTo(Double.valueOf(value));
		}
	}
	

	public static void main(final String[] args) {
		Configuration.checkForConfigurationAndWriteDefaults();
		Tarsos.configureDirectories(Logger.getLogger(AudioFingerprinter.class.getName()));
		if(args.length == 0){
			startUI();
		}else if (args.length==1){
			printHelp("");
		}else{
			startCLI(args);
		}
	}

	/**
	 * Properties file that defines the logging behavior.
	 */
	private static final String LOG_PROPS = "/be/hogent/tarsos/util/logging.properties";
	private static void startUI(){
		Logger.getLogger(AudioFingerprinter.class.getName());
		try {
			final InputStream stream = Tarsos.class.getResourceAsStream(LOG_PROPS);
			LogManager.getLogManager().readConfiguration(stream);
			// a configured logger
			Logger.getLogger(Tarsos.class.getName());
		} catch (final SecurityException e) {
			//ignore
		} catch (final IOException e) {
			//ignore
		}
		
		javax.swing.SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				try {					
					UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
				} catch (Exception e) {
					//ignore
				}
				new AudioFingerPrinterFrame();
			}
		});	
	}
	
	private static void printHelp(String prefix) {
		String message = "USAGE-----------------------------------------------------\n\n" +
				"java -jar AudioFingerPrinter.jar needle haystack [haystack] [haystack] ... \n\n " +
				"To use the audio fingerprinter provide one audio file (the needle)\n" +
				"and one or more audiofiles or directories (with audio files in them) as haystack.\n\n" +
				"Beware the haystack directories are traversed recursively.";
	    if(prefix != null && !prefix.isEmpty()){
	    	message = prefix + "\n\n" + message;
	    }
		System.err.println(message);
		System.exit(-1);
	}
	
	private static void startCLI(String... args) {
	
		if(!FileUtils.exists(args[0]) || (!FileUtils.isDirectory(args[0]) && !FileUtils.isAudioFile(new File(args[0])))){
			printHelp("The needle (" + args[0] + ") was not found or is not recognized audio file. Please provide an existing audio file or directory.");
			System.exit(-1);
		}
		
		for(int i = 1 ; i < args.length ; i++){
			if(!FileUtils.exists(args[i]) || (!FileUtils.isDirectory(args[i]) && !FileUtils.isAudioFile(new File(args[i])))){
				printHelp("Each haystack should be a valid directory or audio file. "+ args[i] + " is not. Please provide a valid directory or audio file.");
				System.exit(-1);
			}
		}
		
		Set<File> needles = new HashSet<File>();
		File file = new File(args[0]);
		//Recursively traverse directory
		if(file.isDirectory()){
			for(String fileInDir : FileUtils.glob(file.getAbsolutePath(), Configuration.get(ConfKey.audio_file_name_pattern), true)){
				needles.add(new File(fileInDir));
			}
			// or else add the file	
		} else if(FileUtils.isAudioFile(file)){
			needles.add(file);			
		}
		
		Set<File> haystack = new HashSet<File>();
		for(int i = 1 ; i < args.length ; i++){
			file = new File(args[i]);
			//Recursively traverse directory
			if(file.isDirectory()){
				for(String fileInDir : FileUtils.glob(file.getAbsolutePath(), Configuration.get(ConfKey.audio_file_name_pattern), true)){
					haystack.add(new File(fileInDir));
				}
			// or else add the file	
			} else if(FileUtils.isAudioFile(file)){
				haystack.add(file);
			}
		}
		System.out.println("Will inspect a haystack of " + haystack.size() + " files for " + needles.size() + " needles.");
		
		Iterator<File> it  = needles.iterator();
		while(it.hasNext()){
			doMatch(haystack,it.next());
		}
	}
	
	private static void doMatch( final Set<File> haystack,  final File needle){
		AudioFingerprinter afp = new AudioFingerprinter(haystack, needle);
		List<AudioFingerprintMatch> matches = afp.match();
		System.out.println("Best "+ Math.min(15, matches.size()) + " matches for " + matches.get(0).getMatch().getName() + ":");
		for(int i = 0; i < Math.min(15, matches.size()) ; i++){
			AudioFingerprintMatch afpm = matches.get(i);
			System.out.print("\t" + (i+1)+ " ");
			if (afpm.isMatch()) {
				System.out.println("Match       (" + Math.round(afpm.getValue() * 100) + "% with:\t" + afpm.getOriginal().getName());
			} else {
				System.out.println("Close match (" + Math.round(afpm.getValue() * 100) + "%) with:\t" + afpm.getOriginal().getName());
			}
		}
	}
}
