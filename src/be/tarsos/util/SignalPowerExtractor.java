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

package be.tarsos.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.UnsupportedAudioFileException;

import com.sun.media.sound.AudioFloatInputStream;

/**
 * An utility class to calculate and access the power of an audio file at any
 * given time.
 * 
 * @author Joren Six
 */
public final class SignalPowerExtractor {

	/**
	 * The sample rate for the power calculations. A sample is a point where the
	 * waveform or power is calculated. E.g. a song of 300sec long at 10 Hz =>
	 * 3000 measurements takes place.
	 */
	private static final int POWER_SAMPLE_RATE = 50;

	/**
	 * Log messages.
	 */
	private static final Logger LOG = Logger.getLogger(SignalPowerExtractor.class.getName());

	private final AudioFile audioFile;

	private double[] linearPowerArray;
	private double maxLinearPower = -1;
	private double minLinearPower = Double.MAX_VALUE;
	private final double readWindow; // seconds

	private final double sampleRate;
	private final double frameSize;
	private final double frameRate;
	private final double audioLengtInSecs;

	/**
	 * Create a new power extractor.
	 * 
	 * @param file
	 *            The audio file to extract power from.
	 */
	public SignalPowerExtractor(final AudioFile file) {
		this.audioFile = file;
		AudioInputStream ais = null;
		final File inputFile = new File(audioFile.transcodedPath());
		AudioFormat format = null;
		try {
			ais = AudioSystem.getAudioInputStream(inputFile);
			format = ais.getFormat();
		} catch (final UnsupportedAudioFileException e) {
			LOG.log(Level.SEVERE, e.getLocalizedMessage(), e);
		} catch (final IOException e) {
			LOG.log(Level.SEVERE, e.getLocalizedMessage(), e);
		} finally {
			try {
				ais.close();
			} catch (final IOException e) {
				LOG.log(Level.SEVERE, "Failed to close audio input stream.", e);
			} catch (final NullPointerException e) {
				LOG.log(Level.SEVERE, "Failed to initialize audio input stream.", e);
			}
		}
		sampleRate = format.getSampleRate();
		frameSize = format.getFrameSize();
		frameRate = format.getFrameRate();
		audioLengtInSecs = inputFile.length() / (frameSize * frameRate);
		readWindow = 1.0 / POWER_SAMPLE_RATE;
	}

	/**
	 * Returns the relative power [0.0;1.0] at the given time.
	 * 
	 * @param seconds
	 *            The time to get the relative power for.
	 * @param relative 
	 * @return A number between 0 and 1 inclusive that shows the relative power
	 *         at the given time
	 * @exception IndexOutOfBoundsException
	 *                when the number of seconds is not between the start and
	 *                end of the song.
	 */
	public double powerAt(final double seconds, final boolean relative) {
		if (linearPowerArray == null) {
			extractPower();
		}
		double power = linearPowerArray[secondsToIndex(seconds)];
		if (relative) {
			final double powerDifference = maxLinearPower - minLinearPower;
			power = (power - minLinearPower) / powerDifference;
		} else {
			power = linearToDecibel(power);
		}
		return power;
	}

	/**
	 * Calculates an index value for the power array from a number of seconds.
	 */
	private int secondsToIndex(final double seconds) {
		return (int) Math.ceil(seconds / readWindow);
	}

	/**
	 * Fills the power array with linear power values. Also stores the min and
	 * max linear power.
	 */
	private void extractPower() {
		final File inputFile = new File(audioFile.transcodedPath());
		AudioInputStream ais = null;
		try {
			ais = AudioSystem.getAudioInputStream(inputFile);
			final AudioFloatInputStream afis = AudioFloatInputStream.getInputStream(ais);

			linearPowerArray = new double[secondsToIndex(audioLengtInSecs) + 1];
			final int readAmount = (int) (readWindow * sampleRate);
			final float[] buffer = new float[readAmount];

			int index = 0;
			while (afis.read(buffer, 0, readAmount) != -1) {
				final double power = SignalPowerExtractor.localEnergy(buffer);
				minLinearPower = Math.min(power, minLinearPower);
				maxLinearPower = Math.max(power, maxLinearPower);
				linearPowerArray[index] = power;
				index++;
			}
		} catch (final UnsupportedAudioFileException e) {
			LOG.log(Level.SEVERE, e.getLocalizedMessage(), e);
		} catch (final IOException e) {
			LOG.log(Level.SEVERE, e.getLocalizedMessage(), e);
		} finally {
			try {
				ais.close();
			} catch (final IOException e) {
				LOG.log(Level.SEVERE, "Failed to close audio input stream.", e);
			} catch (final NullPointerException e) {
				LOG.log(Level.SEVERE, "Failed to initialize audio input stream.", e);
			}
		}
	}

	/**
	 * Creates a wave from plot.
	 * 
	 * 
	 * @param aggregator
	 *            The aggregator to save to.
	 */
	public void waveFormPlot(WaveFormDataAggregator aggregator) {
		FileInputStream file = null;
		try {
			final File inputFile = new File(audioFile.transcodedPath());
			file = new FileInputStream(inputFile);
			final double timeFactor = 1.0 / (frameSize * frameRate);

			// TODO This method expects the file to be a WAV
			// Skip WAV header (44 bytes, fixed length).
			for (int i = 0; i < 44; i++) {
				file.read();
			}

			int intByte;
			int index = 0;
			// read a window at a time (in bytes)
			int bufferSize = (int) (readWindow / timeFactor);
			// make buffer size even so byteBuffer[0] and 1 are correctly
			// aligned.
			if (bufferSize % 2 != 0) {
				bufferSize++;
			}
			final byte[] byteBuffer = new byte[bufferSize];
			intByte = file.read(byteBuffer);
			index = bufferSize;

			// TODO This method expects the file to be a WAV with 16 as bit
			// depth
			while (intByte != -1) {
				final double seconds = index * timeFactor;
				final double power = (byteBuffer[0] << 8 | byteBuffer[1] & 0xFF) / 32767.0;
				aggregator.addDataPoint(seconds, power);
				index += bufferSize;
				intByte = file.read(byteBuffer);
			}
		} catch (final IOException e) {
			LOG.log(Level.SEVERE, "Failed to write audio file.", e);
		} finally {
			try {
				file.close();
			} catch (final IOException e) {
				LOG.log(Level.SEVERE, "Failed to close audio file.", e);
			} catch (final NullPointerException e) {
				LOG.log(Level.SEVERE, "Failed to initialize audio file stream.", e);
			}
		}
	}

	public interface WaveFormDataAggregator {
		void addDataPoint(double seconds, double power);
	}

	/**
	 * Creates a text file with relative power values for each sample.
	 * 
	 * @param textFileName
	 *            Where to save the text file?
	 * @param relative
	 * 			Compare the current power with the max extracted power, or not? 
	 */
	public void saveTextFile(final String textFileName, final boolean relative) {
		if (linearPowerArray == null) {
			extractPower();
		}
		final StringBuilder stringBuilder = new StringBuilder("Time (in seconds);Power\n");
		for (int index = 0; index < linearPowerArray.length; index++) {
			final double seconds = index * readWindow;
			stringBuilder.append(seconds).append(";").append(powerAt(seconds, relative)).append("\n");
		}
		FileUtils.writeFile(stringBuilder.toString(), textFileName);
	}

	/**
	 * Creates a 'power plot' of the signal.
	 * 
	 * @param powerPlotFileName
	 *            Where to save the plot.
	 * @param silenceThreshold
	 *            Draw a line at this threshold. Can be used to show where the
	 *            signal is 'silent'.
	 */
	public void savePowerPlot(final String powerPlotFileName, final double silenceThreshold) {
		if (linearPowerArray == null) {
			extractPower();
		}

		final SimplePlot plot = new SimplePlot("Powerplot for " + audioFile.originalBasename());
		for (int index = 0; index < linearPowerArray.length; index++) {
			// prevents negative infinity
			final double power = linearToDecibel(linearPowerArray[index] == 0.0 ? 0.00000000000001
					: linearPowerArray[index]);
			final double timeInSeconds = index * readWindow;
			plot.addData(0, timeInSeconds, power);
			plot.addData(1, timeInSeconds, silenceThreshold);
		}
		plot.save(powerPlotFileName);
	}

	/**
	 * Calculates the local (linear) energy of an audio buffer.
	 * 
	 * @param buffer
	 *            The audio buffer.
	 * @return The local (linear) energy of an audio buffer.
	 */
	public static double localEnergy(final float[] buffer) {
		double power = 0.0D;
		for (float element : buffer) {
			power += element * element;
		}
		return power;
	}

	/**
	 * Returns the dBSPL for a buffer.
	 * 
	 * @param buffer
	 *            The buffer with audio information.
	 * @return The dBSPL level for the buffer.
	 */
	public static double soundPressureLevel(final float[] buffer) {
		double value = Math.pow(localEnergy(buffer), 0.5);
		value = value / buffer.length;
		return linearToDecibel(value);
	}

	/**
	 * Converts a linear to a dB value.
	 * 
	 * @param value
	 *            The value to convert.
	 * @return The converted value.
	 */
	private static double linearToDecibel(final double value) {
		return 20.0 * Math.log10(value);
	}

	/**
	 * Checks if the dBSPL level in the buffer falls below a certain threshold.
	 * 
	 * @param buffer
	 *            The buffer with audio information.
	 * @param silenceThreshold
	 *            The threshold in dBSPL
	 * @return True if the audio information in buffer corresponds with silence,
	 *         false otherwise.
	 */
	public static boolean isSilence(final float[] buffer, final double silenceThreshold) {
		return soundPressureLevel(buffer) < silenceThreshold;
	}

	public static boolean isSilence(final float[] buffer) {
		return SignalPowerExtractor.isSilence(buffer, Configuration.getDouble(ConfKey.silence_threshold));
	}
}
