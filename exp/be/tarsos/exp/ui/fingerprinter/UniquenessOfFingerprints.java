/**
*
*  Tarsos is developed by Joren Six at 
*  The Royal Academy of Fine Arts & Royal Conservatory,
*  University College Ghent,
*  Hoogpoort 64, 9000 Ghent - Belgium
*
**/
package be.tarsos.exp.ui.fingerprinter;

import java.io.File;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.logging.Handler;
import java.util.logging.Logger;

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

public class UniquenessOfFingerprints {



	public static void main(String... strings) {
		
		String datasetPath = strings[0]; 
		
		int numberOfFiles = Integer.parseInt(strings[1]);

		Logger globalLogger = Logger.getLogger("");
		Handler[] handlers = globalLogger.getHandlers();
		for (Handler handler : handlers) {
			globalLogger.removeHandler(handler);
		}

		List<String> dataset = FileUtils.glob(datasetPath, Configuration.get(ConfKey.audio_file_name_pattern), true);
		Collections.shuffle(dataset);
		dataset = dataset.subList(0, Math.min(numberOfFiles, dataset.size()-1));
		
		System.out.print(";");
		for(int i = 0 ; i < dataset.size() ; i++){
			System.out.print(new File(dataset.get(i)).getName() + ";");
		}
		System.out.println("");
		
		for(int i = 0 ; i < dataset.size() ; i++){
			System.out.print(new File(dataset.get(i)).getName() + ";");
			for(int j = 0; j <= i ; j++){
				System.out.print(";");
			}
			for(int j = i+1 ; j < dataset.size();j++){
			
				String original = dataset.get(i);
				String match = dataset.get(j);
				double value;
				try {
					PitchClassHistogram matchPCH = createHisto(new File(match));
					PitchClassHistogram originalPCH = createHisto(new File(original));
					//allows for pitch shift
					int displacement = matchPCH.displacementForOptimalCorrelation(originalPCH);
					value = matchPCH.correlationWithDisplacement(displacement, originalPCH);
				} catch (EncoderException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
					value = -1;
				}
				System.out.print(value + ";");
			}
			System.out.println();
		}
	}
	
	private static HashMap<File,PitchClassHistogram> cache = new HashMap<File, PitchClassHistogram>();
	
	private static PitchClassHistogram createHisto(File file) throws EncoderException{
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
}
