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
package be.tarsos.exp.cli;

import java.util.List;

import be.tarsos.sampled.pitch.Annotation;
import be.tarsos.sampled.pitch.PitchDetectionMode;
import be.tarsos.sampled.pitch.PitchDetector;
import be.tarsos.transcoder.ffmpeg.EncoderException;
import be.tarsos.util.AudioFile;
import be.tarsos.util.ConfKey;
import be.tarsos.util.Configuration;
import be.tarsos.util.FileUtils;
import be.tarsos.util.KernelDensityEstimate;
import be.tarsos.util.histogram.HistogramFactory;

public class PCHSimilarityMatrix {
	
	
	
	public static void main(String... args) throws EncoderException{
		String directory = "/Users/joren/Desktop/aawm/audio op titel/_Mpundu";
		List<String> files = FileUtils.glob(directory, Configuration.get(ConfKey.audio_file_name_pattern), true);
		
		StringBuilder csvData = new StringBuilder(";");
		for(String firstFile:files){
			for(String secondFile:files){
				csvData.append(FileUtils.basename(secondFile)).append(";");
			}
		}
		for(String firstFile:files){
			csvData.append("\n").append(FileUtils.basename(firstFile)).append(";");
			
			AudioFile audioFile = new AudioFile(firstFile);
			PitchDetector detector = PitchDetectionMode.TARSOS_YIN.getPitchDetector(audioFile);
			detector.executePitchDetection();
			List<Annotation> annotations = detector.getAnnotations();
			KernelDensityEstimate firstKDE = HistogramFactory.createPichClassKDE(annotations, 6);
			
			
			for(String secondFile:files){
				AudioFile secondAudioFile = new AudioFile(secondFile);
				detector = PitchDetectionMode.TARSOS_YIN.getPitchDetector(secondAudioFile);
				detector.executePitchDetection();
				annotations = detector.getAnnotations();
				KernelDensityEstimate secondKDE = HistogramFactory.createPichClassKDE(annotations, 6);
				//csvData.append(firstKDE.optimalCorrelation(secondKDE)).append(";");
			}
		}
		System.out.println(csvData.toString());
		
	}

}
