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

package be.tarsos.midi;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import javax.sound.sampled.AudioFileFormat;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.UnsupportedAudioFileException;

import be.tarsos.dsp.AudioDispatcher;
import be.tarsos.dsp.io.jvm.AudioPlayer;
import be.tarsos.dsp.io.jvm.JVMAudioInputStream;
import be.tarsos.sampled.pitch.PitchFunctions;
import be.tarsos.util.FileUtils;
import be.tarsos.util.SignalPowerExtractor;

/**
 * Create a sequence of tones. Tones are in this case a sine wave of a certain
 * frequency (in Hertz) starting at a certain time (in seconds) the current tone
 * stops when another tone starts: this class generates only one tone at the
 * time (monophonic).
 * 
 * @author Joren Six
 */
public final class ToneSequenceBuilder {
	/**
	 * A list of frequencies.
	 */
	private List<Double> frequencies;
	/**
	 * A list of starting times, frequencies.size() == realTimes.size().
	 */
	private final List<Double> realTimes;

	/**
	 * Values between 0 and 1 that signify the strength of the signal
	 * <code>frequencies.size() == realTimes.size() == powers.size();</code>.
	 */
	private List<Double> powers;

	/**
	 * Initializes the lists of frequencies and times.
	 */
	public ToneSequenceBuilder() {
		frequencies = new ArrayList<Double>();
		realTimes = new ArrayList<Double>();
		powers = new ArrayList<Double>();
	}

	/**
	 * Add a tone with a certain frequency (in Hertz) starting at a certain time
	 * (seconds).
	 * <p>
	 * The tone stops when the next tone starts. The last tone has a duration of
	 * 0 seconds. The entries should be added <b>chronologically</b>! Strange
	 * things will happen if you ignore this rule.
	 * </p>
	 * 
	 * @param frequency
	 *            the frequency in Hertz of the tone to add
	 * @param realTime
	 *            the starttime in seconds of the tone. The tone stops when the
	 *            next one starts. The last tone is never played.
	 */
	public void addTone(final double frequency, final double realTime) {
		frequencies.add(frequency);
		realTimes.add(realTime);
		powers.add(0.75);
	}

	public void addTone(final double frequency, final double realTime, final double power) {
		frequencies.add(frequency);
		realTimes.add(realTime);
		powers.add(power);
	}

	/**
	 * Clears the frequencies and times. When this method finishes the object is
	 * in the same state as a new instance of {@link ToneSequenceBuilder}.
	 */
	public void clear() {
		frequencies.clear();
		realTimes.clear();
	}

	public void playAnnotations(final int smootFilterWindowSize) {
		try {
			writeFile(null, smootFilterWindowSize,null);
		} catch (final IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (final UnsupportedAudioFileException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (final LineUnavailableException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	/**
	 * Write a stereo WAV-file (sample rate 44.1 kHz) with frequencies and their
	 * respective durations (start times). If the fileName is null the file played.
	 * If sourceFile is given the source file is written on the left ch
	 * 
	 * @param fileName
	 *            The name of the file to render. e.g. "out.wav".
	 * @param smootFilterWindowSize
	 *            To prevent (very) sudden changes in the frequency of tones a
	 *            smoothing function can be applied. The window size of the
	 *            smoothing function defines what an unexpected value is and if
	 *            it is smoothed. When no smoothing is required <strong>set it
	 *            to zero</strong>. Otherwise a value between 5 and 50 is
	 *            normal. ( 50 x 10 ms = 500 ms = 0.5 seconds). A
	 *            <em>median filter</em> is used.
	 * @param sourceFile 
	 *            The source file used 
	 * @throws IOException
	 *             When something goes awry.
	 * @throws UnsupportedAudioFileException
	 * @throws LineUnavailableException
	 * @throws SinkIsFullException
	 * @throws InterruptedException
	 * @throws BufferNotAvailableException
	 */
	public void writeFile(final String fileName, final int smootFilterWindowSize,String sourceFile) throws IOException,
			UnsupportedAudioFileException, LineUnavailableException {
		// invariant: at any time the lists are equal in length
		assert frequencies.size() == realTimes.size();

		if (smootFilterWindowSize > 0) {
			frequencies = PitchFunctions.medianFilter(frequencies, smootFilterWindowSize);
			powers = PitchFunctions.medianFilter(powers, smootFilterWindowSize);
		}
		
		//PitchFunctions.gaussianFilter(frequencies);

		final double sampleRate = 44100.0;
		final double lenghtInSeconds = realTimes.get(realTimes.size() - 1);

		final int numberOfSamples = (int) (lenghtInSeconds * sampleRate);
		//2 bytes per sample, stereo (2 channels)
		final byte[] byteBuffer = new byte[numberOfSamples * 2 * 2]; 
		final float[] floatBuffer = new float[numberOfSamples];

		double previousTime = 0;
		double phase = 0;
		double phaseFirst = 0;
		double phaseSecond = 0;
		
		for (int i = 0; i < frequencies.size(); i++) {
			final double frequency = frequencies.get(i);
			final double currentTime = realTimes.get(i);
			final double twoPiF = 2 * Math.PI * frequency;
			final int startSample = (int) (previousTime * sampleRate);
			final int stopSample = (int) (currentTime * sampleRate);		

			for (int sample = startSample; sample < stopSample; sample++) {
				final double time = (sample - startSample) / sampleRate;
				final double fundamental = 0.65 * Math.sin(twoPiF * time + phase);
				final double firstHarmonic = 0.08 * Math.sin(twoPiF * 4 * time + phaseFirst);
				final double secondHarmonic = 0.03 * Math.sin(twoPiF * 6 * time + phaseSecond);
				floatBuffer[sample] += (float) fundamental + firstHarmonic + secondHarmonic;
			}
			
			previousTime = currentTime;
			phase = 2 * Math.PI * frequency * (stopSample - startSample) / sampleRate + phase;
			phaseFirst = 4 * 2 * Math.PI * frequency * (stopSample - startSample) / sampleRate + phaseFirst;
			phaseSecond = 6 * 2 * Math.PI * frequency * (stopSample - startSample) / sampleRate + phaseSecond;
		}
		

		/*
		 * Convert manually to PCM 16bits Little Endian, (still 44.1kHz) => 2
		 * bytes per sample, 2 channels.
		 */
		for (int sample = 0; sample < numberOfSamples; sample++) {
			final int quantizedValue = (int) (floatBuffer[sample] * 32767);
			byteBuffer[sample * 4 + 0] = (byte) quantizedValue;
			byteBuffer[sample * 4 + 1] = (byte) (quantizedValue >>> 8);
			byteBuffer[sample * 4 + 2] = byteBuffer[sample * 4 + 0];
			byteBuffer[sample * 4 + 3] = byteBuffer[sample * 4 + 1];
		}
		
		/*
		 * Read the source file data in the right channel
		 */
		if(sourceFile != null){
			AudioInputStream stream = AudioSystem.getAudioInputStream(new File(sourceFile));
			
			byte[] sampleAsByteArray = new byte[2]; 
			for (int sample = 0; sample < numberOfSamples; sample++) {
				stream.read(sampleAsByteArray);
				byteBuffer[sample * 4 + 2] = sampleAsByteArray[0];
				byteBuffer[sample * 4 + 3] = sampleAsByteArray[1];
			}
		}

		/*
		 * Write the data to a file.
		 */
		final AudioFormat audioFormat = new AudioFormat((float) sampleRate, 16, 2, true, false);
		final ByteArrayInputStream bais = new ByteArrayInputStream(byteBuffer);
		final AudioInputStream audioInputStream = new AudioInputStream(bais, audioFormat, numberOfSamples);
		JVMAudioInputStream stream = new JVMAudioInputStream(audioInputStream);
		if (fileName == null) {
			final AudioDispatcher dispatcher = new AudioDispatcher(stream, 1024, 0);
			dispatcher.addAudioProcessor(new AudioPlayer(audioFormat));
			dispatcher.run();
		} else {
			final File out = new File(fileName);
			AudioSystem.write(audioInputStream, AudioFileFormat.Type.WAVE, out);
			audioInputStream.close();
		}
	}

	/**
	 * Read data from a CSV-File, handle it with the handler, smooth it and save
	 * it to the generated audio folder.
	 * 
	 * @param csvFileName
	 *            the CSV-file to process
	 * @param handler
	 *            the handler
	 * @param smootFilterWindowSize
	 *            the window size for the smoothing function (Median filter).
	 */
	public static void saveAsWav(final String csvFileName, final CSVFileHandler handler,
			final int smootFilterWindowSize) {
		final String correctedFileName = new File(csvFileName).getAbsolutePath();
		try {
			final ToneSequenceBuilder builder = new ToneSequenceBuilder();
			final List<String[]> rows = FileUtils.readCSVFile(correctedFileName, handler.getSeparator(),
					handler.getNumberOfExpectedColumn());
			for (final String[] row : rows) {
				handler.handleRow(builder, row);
			}
			builder.writeFile("data/generated_audio/" + FileUtils.basename(correctedFileName) + ".wav",
					smootFilterWindowSize,null);
		} catch (final Exception e) {
			e.printStackTrace();
		}
	}

	/**
     *
     *
     */
	public interface CSVFileHandler {
		void handleRow(ToneSequenceBuilder builder, String[] row);

		String getSeparator();

		int getNumberOfExpectedColumn();

		void setExtractor(SignalPowerExtractor extractor);
	}

	/**
	 * @author Joren Six
	 */
	public enum AnnotationCVSFileHandlers {
		/**
		 * Handles files generated by BOZKURT.
		 */
		BOZKURT(new BozkurtCSVFileHandler()),
		/**
		 * Handles files generated by IPEM_SIX.
		 */
		IPEM(new IpemCSVFileHandler()),
		/**
		 * Handles files generated by AUBIO.
		 */
		AUBIO(new AubioCSVHandler()), TARSOS(new TarsosCSVHandler());

		/**
		 * The underlying handler.
		 */
		private final CSVFileHandler cvsFileHandler;

		/**
		 * Create a new annotation Handler.
		 * 
		 * @param handler
		 *            The underlying handler.
		 */
		private AnnotationCVSFileHandlers(final CSVFileHandler handler) {
			cvsFileHandler = handler;
		}

		/**
		 * @return the cvsFileHandler
		 */
		public CSVFileHandler getCvsFileHandler() {
			return cvsFileHandler;
		}

	}
	
	private static class TarsosCSVHandler implements CSVFileHandler{

		public void handleRow(ToneSequenceBuilder builder, String[] row) {
			final double realTime = Double.parseDouble(row[0]);
			final double frequency = Double.parseDouble(row[1]);
			builder.addTone(frequency, realTime);			
		}

		public String getSeparator() {
			return ",";
		}

		public int getNumberOfExpectedColumn() {
		
			return 0;
		}

		public void setExtractor(SignalPowerExtractor extractor) {
			// TODO Auto-generated method stub
		}
		
	}

	private static class BozkurtCSVFileHandler implements CSVFileHandler {
		private static final double REFFREQUENCY = 8.17579891564371; // Hz
		private static final int SAMPLERATE = 100; // Hz
		private static final int CENTSINOCTAVE = 1200;
		private SignalPowerExtractor extr;

		
		public void handleRow(final ToneSequenceBuilder builder, final String[] row) {
			final double realTime = Double.parseDouble(row[0]) / SAMPLERATE;
			final double frequency = REFFREQUENCY * Math.pow(2.0, Double.parseDouble(row[1]) / CENTSINOCTAVE);
			if (extr == null) {
				builder.addTone(frequency, realTime);
			} else {
				builder.addTone(frequency, realTime, extr.powerAt(realTime, true));
			}
		}

		
		public int getNumberOfExpectedColumn() {
			return 2;
		}

		
		public String getSeparator() {
			return "[\\s]+";
		}

		
		public void setExtractor(final SignalPowerExtractor extractor) {
			extr = extractor;
		}
	}

	private static class AubioCSVHandler implements CSVFileHandler {
		private SignalPowerExtractor extr;

		
		public void handleRow(final ToneSequenceBuilder builder, final String[] row) {
			final double realTime = Double.parseDouble(row[0]);
			final double frequency = Double.parseDouble(row[1]);
			if (extr == null) {
				builder.addTone(frequency, realTime);
			} else {
				builder.addTone(frequency, realTime, extr.powerAt(realTime, true));
			}
		}

		
		public int getNumberOfExpectedColumn() {
			return 2;
		}

		
		public String getSeparator() {
			return "\t";
		}

		
		public void setExtractor(final SignalPowerExtractor extractor) {
			extr = extractor;
		}
	};

	private static class IpemCSVFileHandler implements CSVFileHandler {
		/**
		 * Log messages.
		 */
		private static final Logger LOG = Logger.getLogger(IpemCSVFileHandler.class.getName());
		private int sampleNumber = 0;
		private SignalPowerExtractor extr;

		
		public void handleRow(final ToneSequenceBuilder builder, final String[] row) {
			sampleNumber++;
			final double realTime = sampleNumber / 100.0; // 100 Hz sample
															// frequency
			// (every 10 ms)
			double frequency = 0.0;
			try {
				frequency = Double.parseDouble(row[0]);
				if (extr == null) {
					builder.addTone(frequency, realTime);
				} else {
					builder.addTone(frequency, realTime, extr.powerAt(realTime, true));
				}
			} catch (final NumberFormatException e) {
				LOG.warning("Ignored invalid formatted number: " + row[0]);
			}
		}

		
		public int getNumberOfExpectedColumn() {
			return 0;
		}

		
		public String getSeparator() {
			return " ";
		}

		
		public void setExtractor(final SignalPowerExtractor extractor) {
			extr = extractor;
		}
	}

	public void handleRow(ToneSequenceBuilder builder, String[] row) {
		// TODO Auto-generated method stub
		
	}

	public String getSeparator() {
		// TODO Auto-generated method stub
		return null;
	}

	public int getNumberOfExpectedColumn() {
		// TODO Auto-generated method stub
		return 0;
	}

	public void setExtractor(SignalPowerExtractor extractor) {
		// TODO Auto-generated method stub
		
	}
}
