package be.hogent.tarsos.pitch;

import java.io.File;
import java.io.IOException;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.UnsupportedAudioFileException;

import com.sun.media.sound.AudioFloatInputStream;

/**
 * @author Joren Six
 *An implementation of the YIN pitch tracking algorithm.
 *See <a href="http://recherche.ircam.fr/equipes/pcm/cheveign/ps/2002_JASA_YIN_proof.pdf">the YIN paper.</a>
 *
 *Implementation based on <a href="http://aubio.org">aubio</a>
 *
 *
 */
public class Yin {

	private final double threshold = 0.10;
	private final int bufferSize;
	private final int overlapSize;
	private float sampleRate;
	private volatile boolean running;

	public Yin(){
		bufferSize = 1024;
		overlapSize = bufferSize/2;
		running = true;
	}

	/**
	 * @param inputBuffer input signal
	 * @param yinBuffer output buffer to store difference function (half length of input)
	 */
	private void difference(float[] inputBuffer, float[] yinBuffer){
		int j,tau;
		float tmp;
		for(tau=0;tau < yinBuffer.length;tau++){
			yinBuffer[tau] = 0;
		}
		for(tau = 1 ; tau < yinBuffer.length ; tau++){
			for(j = 0 ; j < yinBuffer.length ; j++){
				tmp = inputBuffer[j] - inputBuffer[j+tau];
				yinBuffer[tau]+=  tmp * tmp;
			}
		}
	}

	private void cumulativeMeanNormalizedDifference(float[] yinBuffer){
		int tau;
		float tmp = 0;
		yinBuffer[0] = 1;
		for(tau = 1 ; tau < yinBuffer.length ; tau++){
			tmp += yinBuffer[tau];
			yinBuffer[tau] *= (tau) /tmp;
		}
	}

	private int getPitch(float[] yinBuffer){
		int tau = 1;
		do{
			if(yinBuffer[tau] < threshold){
				//change to aubio implementation:
				while(tau + 1 <yinBuffer.length && yinBuffer[tau+1] < yinBuffer[tau])
					tau++;
				return tau;
			}
			tau++;
		}while(tau<yinBuffer.length);
		return 0;
	}

	public interface DetectedPitchHandler{
		void handleDetectedPitch(float pitch);
	}

	public void processFile(String fileName,DetectedPitchHandler detectedPitchHandler)  throws UnsupportedAudioFileException, IOException{
		AudioInputStream ais = AudioSystem.getAudioInputStream(new File(fileName));
		AudioFloatInputStream afis = AudioFloatInputStream.getInputStream(ais);
		AudioFormat format = afis.getFormat();
		sampleRate = format.getSampleRate();
		processStream(afis,detectedPitchHandler);
	}

	public void processStream(AudioFloatInputStream afis,DetectedPitchHandler detectedPitchHandler) throws UnsupportedAudioFileException, IOException{
		float inputBuffer[] = new float[bufferSize];
		float yinBuffer[] = new float[bufferSize/2];
		AudioFormat format = afis.getFormat();
		sampleRate = format.getSampleRate();
		//read full buffer
		boolean hasMoreBytes = afis.read(inputBuffer,0, bufferSize) != -1;
		while(hasMoreBytes && running) {
			difference(inputBuffer, yinBuffer);
			cumulativeMeanNormalizedDifference(yinBuffer);
			float pitch = getPitch(yinBuffer);
			if(pitch > 0){
				pitch = sampleRate/pitch;//Hz
			}else{
				pitch = -1;
			}
			if(detectedPitchHandler!=null)
				detectedPitchHandler.handleDetectedPitch(pitch);
			//slide buffer with defined overlap
			for(int i = 0 ; i < overlapSize ; i++)
				inputBuffer[i]=inputBuffer[i+overlapSize];
			hasMoreBytes = afis.read(inputBuffer,overlapSize, bufferSize - overlapSize) != -1;
		}
	}

	public void stop(){
		running=false;
	}

	public static void main(String... args) throws UnsupportedAudioFileException, IOException{
		Yin yin = new Yin();
		yin.processFile("../Tarsos/audio/pitch_check/95-100Hz.wav", new DetectedPitchHandler() {
			@Override
			public void handleDetectedPitch(float pitch) {
				System.out.println(pitch);
			}
		});
	}
}
