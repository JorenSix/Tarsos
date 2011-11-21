package be.hogent.tarsos.exp.ui.fingerprinter;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;

import be.hogent.tarsos.sampled.pitch.PitchDetectionMode;
import be.hogent.tarsos.sampled.pitch.PitchDetector;
import be.hogent.tarsos.transcoder.ffmpeg.EncoderException;
import be.hogent.tarsos.util.AudioFile;
import be.hogent.tarsos.util.KernelDensityEstimate;
import be.hogent.tarsos.util.histogram.HistogramFactory;
import be.hogent.tarsos.util.histogram.PitchClassHistogram;


public class AudioFingerprinter {
	
	private static final Logger LOG = Logger.getLogger(AudioFingerprinter.class.getName());
	
	final Set<File> hayStack;
	final File needle;
	
	public AudioFingerprinter(Set<File> theHayStack, File theNeedle){
		needle = theNeedle;
		hayStack = theHayStack;
	}
	
	public AudioFingerprintMatch match(){
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
		return list.get(list.size()-1);
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
				PitchDetector detector = PitchDetectionMode.TARSOS_FAST_YIN.getPitchDetector(audioFile);	
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

		@Override
		public int compareTo(AudioFingerprintMatch o) {
			return Double.valueOf(value).compareTo(Double.valueOf(o.value));
		}
	}
}
