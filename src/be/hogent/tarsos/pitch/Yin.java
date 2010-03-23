package be.hogent.tarsos.pitch;

import java.io.File;
import java.io.IOException;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.UnsupportedAudioFileException;

import be.hogent.tarsos.util.SimplePlot;

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
	private final float sampleRate;
	private volatile boolean running;

	private final float[] yinBuffer;
	private final float[] inputBuffer;

	private Yin(float sampleRate){
		this.sampleRate = sampleRate;
		bufferSize = 1024;
		overlapSize = bufferSize/2;
		running = true;
		inputBuffer = new float[bufferSize];
		yinBuffer = new float[bufferSize/2];
	}

	private void difference(){
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

	private void cumulativeMeanNormalizedDifference(){
		int tau;
		float tmp = 0;
		yinBuffer[0] = 1;
		for(tau = 1 ; tau < yinBuffer.length ; tau++){
			tmp += yinBuffer[tau];
			yinBuffer[tau] *= (tau) /tmp;
		}
	}

	private float getPitch(){
		difference();
		cumulativeMeanNormalizedDifference();
		int tau = 1;
		do{
			if(yinBuffer[tau] < threshold){
				//change to aubio implementation: extra array bounds check
				while(tau + 1 <yinBuffer.length && yinBuffer[tau+1] < yinBuffer[tau])
					tau++;
				return sampleRate/tau;
			}
			tau++;
		}while(tau<yinBuffer.length);
		return -1;
	}

	public interface DetectedPitchHandler{
		/**
		 * @param time in seconds
		 * @param pitch in Hz
		 */
		void handleDetectedPitch(float time,float pitch);
	}

	public static void processFile(String fileName,DetectedPitchHandler detectedPitchHandler)  throws UnsupportedAudioFileException, IOException{
		AudioInputStream ais = AudioSystem.getAudioInputStream(new File(fileName));
		AudioFloatInputStream afis = AudioFloatInputStream.getInputStream(ais);
		Yin.processStream(afis,detectedPitchHandler);
	}

	public static void processStream(AudioFloatInputStream afis,DetectedPitchHandler detectedPitchHandler) throws UnsupportedAudioFileException, IOException{
		AudioFormat format = afis.getFormat();
		float sampleRate = format.getSampleRate();
		double frameSize = format.getFrameSize();
		double frameRate = format.getFrameRate();
		float time = 0;
		//number of bytes / frameSize * frameRate gives the number of seconds
		//because we use float buffers there is a factor 2: 2 bytes per float?
		//Seems to be correct but a float uses 4 bytes: confused programmer is confused.
		float timeCalculationDivider = (float) (frameSize * frameRate / 2);
		long floatsProcessed = 0;
		Yin yin = new Yin(sampleRate);
		int bufferStepSize = yin.bufferSize - yin.overlapSize;

		//read full buffer
		boolean hasMoreBytes = afis.read(yin.inputBuffer,0, yin.bufferSize) != -1;
		floatsProcessed += yin.inputBuffer.length;
		while(hasMoreBytes && yin.running) {
			float pitch = yin.getPitch();
			time = floatsProcessed / timeCalculationDivider;
			if(detectedPitchHandler!=null)
				detectedPitchHandler.handleDetectedPitch(time,pitch);
			//slide buffer with predefined overlap
			for(int i = 0 ; i < yin.overlapSize ; i++)
				yin.inputBuffer[i]=yin.inputBuffer[i+yin.overlapSize];
			hasMoreBytes = afis.read(yin.inputBuffer,yin.overlapSize,bufferStepSize) != -1;
			floatsProcessed += bufferStepSize;
		}
	}

	public void stop(){
		running=false;
	}

	public static void main(String... args) throws UnsupportedAudioFileException, IOException{
		final SimplePlot p = new SimplePlot("Pitch tracking");
		Yin.processFile("../Tarsos/audio/pitch_check/flute.novib.mf.C5B5.wav", new DetectedPitchHandler() {
			@Override
			public void handleDetectedPitch(float time,float pitch) {
				System.out.println(time + "\t\t\t" + pitch);
				if(pitch == -1)
					pitch = 0;
				p.addData(time,pitch);
			}
		});
		//p.setYRange(0.0,200.0);
		p.save();
	}
}
