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
	/**
	 * A boolean to start and stop the algorithm.
	 * Practical for real time processing of data.
	 */
	private volatile boolean running;

	/**
	 * The buffer that stores the calculated values.
	 * It is exactly half the size of the input buffer.
	 */
	private final float[] yinBuffer;
	/**
	 * The original input buffer
	 */
	private final float[] inputBuffer;

	private Yin(float sampleRate){
		this.sampleRate = sampleRate;
		bufferSize = 1024;
		overlapSize = bufferSize/2;//half of the buffer overlaps
		running = true;
		inputBuffer = new float[bufferSize];
		yinBuffer = new float[bufferSize/2];
	}

	/**
	 * Implements the difference function as described
	 * in step 2 of the YIN paper
	 */
	private void difference(){
		int j,tau;
		float delta;
		for(tau=0;tau < yinBuffer.length;tau++){
			yinBuffer[tau] = 0;
		}
		for(tau = 1 ; tau < yinBuffer.length ; tau++){
			for(j = 0 ; j < yinBuffer.length ; j++){
				delta = inputBuffer[j] - inputBuffer[j+tau];
				yinBuffer[tau] += delta * delta;
			}
		}
	}

	/**
	 * The cumulative mean normalized difference function
	 * as described in step 3 of the YIN paper
	 *
	 * yinBuffer[0] == yinBuffer[1] = 1
	 *
	 */
	private void cumulativeMeanNormalizedDifference(){
		int tau;
		yinBuffer[0] = 1;
		//Very small optimization in comparison with AUBIO
		//start the running sum with the correct value:
		//the first value of the yinBuffer
		float runningSum = yinBuffer[1];
		//yinBuffer[1] is always 1
		yinBuffer[1] = 1;
		//now start at tau = 2
		for(tau = 2 ; tau < yinBuffer.length ; tau++){
			runningSum += yinBuffer[tau];
			yinBuffer[tau] *= tau / runningSum;
		}
	}

	/**
	 * Implements step 4 of the YIN paper
	 */
	private int absoluteThreshold(){
		//other loop construction
		//compared with AUBIO
		for(int tau = 1;tau<yinBuffer.length;tau++){
			if(yinBuffer[tau] < threshold){
				while(tau+1 < yinBuffer.length && yinBuffer[tau+1] < yinBuffer[tau])
					tau++;
				return tau;
			}
		}
		//no pitch found
		return -1;
	}

	private float getPitch(){
		//step 2
		difference();

		//step 3
		cumulativeMeanNormalizedDifference();

		//step 4
		int tau =  absoluteThreshold();

		//step 5:
		//skip interpolation: has
		//little effect on error rates,
		//using the data of the YIN paper.
		//parabolicInterpolation();

		//step 6
		//TODO: implement this optimization
		//0.77% => 0.5% error rate,
		//using the data of the YIN paper
		//bestLocalEstimate()

		//convert to Hz
		return tau==-1 ? -1 : sampleRate/tau;
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
			for(int i = 0 ; i < bufferStepSize ; i++)
				yin.inputBuffer[i]=yin.inputBuffer[i+yin.overlapSize];
			hasMoreBytes = afis.read(yin.inputBuffer,yin.overlapSize,bufferStepSize) != -1;
			floatsProcessed += bufferStepSize;
		}
	}

	/**
	 * Stop the
	 */
	public void stop(){
		running=false;
	}

	public static void main(String... args) throws UnsupportedAudioFileException, IOException{
		final SimplePlot p = new SimplePlot("Pitch tracking");
		Yin.processFile("../Tarsos/data/transcoded_audio/de_Machault_-_Loyaut__que_point_ne_delay.wav", new DetectedPitchHandler() {
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
