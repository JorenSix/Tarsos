package be.hogent.tarsos.sampled.pitch;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.sound.sampled.UnsupportedAudioFileException;

import be.hogent.tarsos.dsp.AudioDispatcher;
import be.hogent.tarsos.dsp.AudioEvent;
import be.hogent.tarsos.dsp.AudioProcessor;
import be.hogent.tarsos.dsp.pitch.PitchProcessor;
import be.hogent.tarsos.dsp.pitch.PitchProcessor.DetectedPitchHandler;
import be.hogent.tarsos.dsp.pitch.PitchProcessor.PitchEstimationAlgorithm;
import be.hogent.tarsos.util.AudioFile;

public class TarsosPitchDetection implements PitchDetector {
	
	private final AudioFile audioFile;
	private final PitchEstimationAlgorithm algorithm;
	private final List<Annotation> annotations;
	private final PitchDetectionMode annotationSource;
	
	private double progress;
	
	private DetectedPitchHandler handler = new DetectedPitchHandler() {
		public void handlePitch(float pitch, float probability, float timeStamp,
				float progress) {
			if(pitch != -1){
				Annotation annotation = new Annotation(timeStamp, pitch, annotationSource);
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
		} else {
			throw new IllegalArgumentException("Algorithm not recognized, should be MPM, YIN or Dynamic Wavelet, is " + pitchDetectionMode.name());
		}
	}

	public List<Annotation> executePitchDetection() {
		try {
			float sampleRate = audioFile.fileFormat().getFormat().getSampleRate();
			int bufferSize = 2048;
			int overlap = 1024;			
			AudioDispatcher dispatcher = AudioDispatcher.fromFile(new File(audioFile.transcodedPath()), bufferSize, overlap);
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
