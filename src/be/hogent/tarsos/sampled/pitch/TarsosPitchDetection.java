package be.hogent.tarsos.sampled.pitch;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.UnsupportedAudioFileException;

import be.hogent.tarsos.Tarsos;
import be.hogent.tarsos.sampled.AudioDispatcher;
import be.hogent.tarsos.sampled.AudioProcessor;
import be.hogent.tarsos.util.AudioFile;
import be.hogent.tarsos.util.FileUtils;

/**
 * @author Joren Six
 */
public final class TarsosPitchDetection implements PitchDetector {

	/**
	 * Logs exceptions.
	 */
	private static final Logger LOG = Logger.getLogger(TarsosPitchDetection.class.getName());

	/**
	 * Ticks per second.
	 */
	private static final double TICKS_PER_SEC = 1000.0;

	/**
	 * The file to process.
	 */
	private final AudioFile file;
	/**
	 * A list of samples.
	 */
	private final List<Sample> samples;

	/**
	 * Which pitch detector to use.
	 */
	private final PitchDetectionMode detectionMode;

	public TarsosPitchDetection(final AudioFile audioFile, final PitchDetectionMode pitchDetectionMode) {
		this.file = audioFile;
		this.samples = new ArrayList<Sample>();
		this.detectionMode = pitchDetectionMode;
	}

	public void executePitchDetection() {
		try {
			String directory = file.transcodedDirectory();
			String annotationsFileName = detectionMode.getParametername() + "_" + file.basename() + ".txt";
			annotationsFileName = FileUtils.combine(directory, annotationsFileName);
			if (FileUtils.exists(annotationsFileName)) {
				samples.addAll(FileUtils.readPitchAnnotations(annotationsFileName));
			} else {
				processFile(file.transcodedPath(), detectionMode, new DetectedPitchHandler() {

					public void handleDetectedPitch(final float time, final float pitch) {
						final long start = (long) (time * TICKS_PER_SEC);
						final Sample s;
						if (pitch == -1) {
							s = new Sample(start);
						} else {
							s = new Sample(start, pitch);
						}
						samples.add(s);
					}
				});
				FileUtils.writePitchAnnotations(annotationsFileName, samples);
				LOG.fine(String.format("Cached pitch detection results to %s", annotationsFileName));
			}
		} catch (final UnsupportedAudioFileException e) {
			LOG.log(Level.SEVERE, "Unsupported audio file: " + file.basename() + " " + e.getMessage(), e);
		} catch (final IOException e) {
			LOG.log(Level.SEVERE,
					"Exception while reading audio file: " + file.basename() + " " + e.getMessage(), e);
		}
	}

	public String getName() {
		final String name;
		if (PitchDetectionMode.TARSOS_MPM == detectionMode) {
			name = "tarsos_mpm";
		} else if (PitchDetectionMode.TARSOS_YIN == detectionMode) {
			name = "tarsos_yin";
		} else {
			name = "tarsos_meta";
		}
		return name;
	}

	public List<Sample> getSamples() {
		return samples;
	}

	/**
	 * Annotate a file with pitch information.
	 * 
	 * @param fileName
	 *            the file to annotate.
	 * @param detectedPitchHandler
	 *            handles the pitch information.
	 * @throws UnsupportedAudioFileException
	 *             Currently only WAVE files with one channel (MONO) are
	 *             supported.
	 * @throws IOException
	 *             If there is an error reading the file.
	 */
	public static void processFile(final String fileName, final PitchDetectionMode detectionMode,
			final DetectedPitchHandler detectedPitchHandler) throws UnsupportedAudioFileException,
			IOException {
		final AudioInputStream ais = AudioSystem.getAudioInputStream(new File(fileName));
		processStream(ais, detectedPitchHandler, detectionMode);
	}

	/**
	 * Annotate an audio stream: useful for real-time pitch tracking.
	 * 
	 * @param afis
	 *            The audio stream.
	 * @param detectedPitchHandler
	 *            Handles the pitch information.
	 * @throws UnsupportedAudioFileException
	 *             Currently only WAVE streams with one channel (MONO) are
	 *             supported.
	 * @throws IOException
	 *             If there is an error reading the stream.
	 */
	public static void processStream(final AudioInputStream ais,
			final DetectedPitchHandler detectedPitchHandler, final PitchDetectionMode detectionMode)
			throws UnsupportedAudioFileException, IOException {
		final float sampleRate = ais.getFormat().getSampleRate();
		final PurePitchDetector pureDetector;
		final int bufferSize;
		final int overlapSize;

		if (PitchDetectionMode.TARSOS_MPM == detectionMode) {
			pureDetector = new McLeodPitchMethod(sampleRate);
			bufferSize = McLeodPitchMethod.DEFAULT_BUFFER_SIZE;
			overlapSize = McLeodPitchMethod.DEFAULT_OVERLAP;
		} else if (PitchDetectionMode.TARSOS_YIN == detectionMode) {
			pureDetector = new Yin(sampleRate, Yin.DEFAULT_BUFFER_SIZE);
			bufferSize = Yin.DEFAULT_BUFFER_SIZE;
			overlapSize = Yin.DEFAULT_OVERLAP;
		} else {
			bufferSize = Yin.DEFAULT_BUFFER_SIZE;
			overlapSize = Yin.DEFAULT_OVERLAP;
			pureDetector = new MetaPitchDetector(sampleRate);
		}

		final int bufferStepSize = bufferSize - overlapSize;

		final AudioDispatcher dispatcher = new AudioDispatcher(ais, bufferSize, overlapSize);
		dispatcher.addAudioProcessor(new AudioProcessor() {
			private long samplesProcessed = 0;
			private float time = 0;

			public void processFull(final float[] audioFloatBuffer, final byte[] audioByteBuffer) {
				samplesProcessed += audioFloatBuffer.length;
				processBuffer(audioFloatBuffer);
			}

			public void processOverlapping(final float[] audioFloatBuffer, final byte[] audioByteBuffer) {
				samplesProcessed += bufferStepSize;
				processBuffer(audioFloatBuffer);
			}

			private void processBuffer(final float[] audioFloatBuffer) {
				final float pitch = pureDetector.getPitch(audioFloatBuffer);
				time = samplesProcessed / sampleRate;
				detectedPitchHandler.handleDetectedPitch(time, pitch);
			}

			public void processingFinished() {
			}
		});

		new Thread(dispatcher).run();

	}

	/**
	 * Prints the detected pitch to STD OUT.
	 */
	public static final DetectedPitchHandler PRINT_DETECTED_PITCH_HANDLER = new DetectedPitchHandler() {
		public void handleDetectedPitch(final float time, final float pitch) {
			Tarsos.println(time + "\t" + pitch);
		}
	};
}
