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
package be.tarsos.sampled.pitch;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.sound.sampled.UnsupportedAudioFileException;

import be.tarsos.dsp.AudioDispatcher;
import be.tarsos.dsp.AudioEvent;
import be.tarsos.dsp.AudioProcessor;
import be.tarsos.dsp.io.jvm.AudioDispatcherFactory;
import be.tarsos.dsp.pitch.PitchDetectionHandler;
import be.tarsos.dsp.pitch.PitchDetectionResult;
import be.tarsos.dsp.pitch.PitchProcessor;
import be.tarsos.dsp.pitch.PitchProcessor.PitchEstimationAlgorithm;
import be.tarsos.util.AudioFile;

public class TarsosPitchDetection implements PitchDetector {
	
	private final AudioFile audioFile;
	private final PitchEstimationAlgorithm algorithm;
	private final List<Annotation> annotations;
	private final PitchDetectionMode annotationSource;
	
	private double progress;
	
	private PitchDetectionHandler handler = new PitchDetectionHandler() {


		public void handlePitch(PitchDetectionResult pitchDetectionResult,
				AudioEvent audioEvent) {
			if(pitchDetectionResult.isPitched()){
				Annotation annotation = new Annotation(audioEvent.getTimeStamp(), pitchDetectionResult.getPitch(), annotationSource,pitchDetectionResult.getProbability());
				annotations.add(annotation);
			}
		}
	};
	
	private AudioProcessor progressProcessor = new AudioProcessor() {
		public void processingFinished() {
		}		
		public boolean process(AudioEvent audioEvent) {
			progress = audioEvent.getProgress();
			return true;
		}
	};
	
	public TarsosPitchDetection(AudioFile audioFile, PitchDetectionMode pitchDetectionMode) {
		this.audioFile = audioFile;
		annotationSource = pitchDetectionMode;
		annotations = new ArrayList<Annotation>();
		if(pitchDetectionMode == PitchDetectionMode.TARSOS_MPM){
			algorithm = PitchEstimationAlgorithm.MPM;
		} else if (pitchDetectionMode == PitchDetectionMode.TARSOS_YIN){
			algorithm = PitchEstimationAlgorithm.YIN;
		}else if (pitchDetectionMode == PitchDetectionMode.TARSOS_DYNAMIC_WAVELET){
			algorithm = PitchEstimationAlgorithm.DYNAMIC_WAVELET;
		} else if (pitchDetectionMode == PitchDetectionMode.TARSOS_FFT_YIN){
			algorithm = PitchEstimationAlgorithm.FFT_YIN;
		} else {
			throw new IllegalArgumentException("Algorithm not recognized, should be MPM, YIN or Dynamic Wavelet, is " + pitchDetectionMode.name());
		}
	}

	public List<Annotation> executePitchDetection() {
		try {
			float sampleRate = audioFile.fileFormat().getFormat().getSampleRate();
			int bufferSize = 2048;
			int overlap = 1024;			
			AudioDispatcher dispatcher = AudioDispatcherFactory.fromFile(new File(audioFile.transcodedPath()), bufferSize, overlap);
			dispatcher.addAudioProcessor(new PitchProcessor(algorithm, sampleRate, bufferSize, handler ));
			dispatcher.addAudioProcessor(progressProcessor );
			dispatcher.run();
		} catch (UnsupportedAudioFileException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return annotations;
	}
	
	

	public double progress() {
		return progress;
	}

	public List<Annotation> getAnnotations() {
		return annotations;
	}

	public String getName() {
		return annotationSource.getParametername();
	}

}
