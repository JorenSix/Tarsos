package be.hogent.tarsos.util;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.sound.sampled.AudioFileFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.UnsupportedAudioFileException;

import be.hogent.tarsos.pitch.PitchDetectionMode;
import be.hogent.tarsos.pitch.PitchDetector;
import be.hogent.tarsos.pitch.PitchUnit;
import be.hogent.tarsos.pitch.Sample;
import be.hogent.tarsos.pitch.pure.DetectedPitchHandler;

/**
 * Represents an audio file. Facilitates transcoding, handling of path names and
 * data sets.
 * 
 * @author Joren Six
 */
public final class AudioFile {

	/**
	 * Log messages.
	 */
	private static final Logger LOG = Logger.getLogger(AudioFile.class.getName());

	/**
	 * Where to save the transcoded files.
	 */
	public static final String TRANSCODED_AUDIO_DIR = Configuration.get(ConfKey.transcoded_audio_directory);
	private static final String ORIGINAL_AUDIO_DIR = Configuration.get(ConfKey.audio_directory);

	private final String path;

	/**
	 * Create and transcode an audio file.
	 * 
	 * @param filePath
	 *            the path for the audio file
	 */
	public AudioFile(final String filePath) {
		this.path = filePath;
		if (AudioTranscoder.transcodingRequired(transcodedPath())) {
			AudioTranscoder.transcode(filePath, transcodedPath());
		}
	}

	/**
	 * @return the path of the transcoded audio file.
	 */
	public String transcodedPath() {
		final String baseName = FileUtils.basename(FileUtils.sanitizedFileName(path));
		final String fileName = baseName + "." + Configuration.get(ConfKey.transcoded_audio_format);
		return FileUtils.combine(TRANSCODED_AUDIO_DIR, fileName);
	}

	/**
	 * @return the path of the original file
	 */
	public String path() {
		return this.path;
	}

	@Override
	public String toString() {
		return FileUtils.basename(path);
	}

	/**
	 * @return the name of the file (without extension)
	 */
	public String basename() {
		return this.toString();
	}

	/**
	 * Returns a list of AudioFiles included in one or more datasets. Datasets
	 * are sub folders of the audio directory.
	 * 
	 * @param datasets
	 *            the datasets to find AudioFiles for
	 * @return a list of AudioFiles
	 */
	public static List<AudioFile> audioFiles(final String... datasets) {
		final List<AudioFile> files = new ArrayList<AudioFile>();
		for (final String dataset : datasets) {
			for (final String originalFile : FileUtils.glob(FileUtils.combine(ORIGINAL_AUDIO_DIR, dataset),
					".*\\..*")) {
				files.add(new AudioFile(originalFile));
			}
		}
		return files;
	}

	/**
	 * Determines the length or the file in microseconds.
	 * 
	 * @return The length of the file in microseconds. If the length could not
	 *         be determined -1 is returned.
	 */
	public long getLengthInMicroSeconds() {
		long lengtInMicroSeconds = -1;
		try {
			AudioFileFormat fileFormat;
			fileFormat = AudioSystem.getAudioFileFormat(new File(transcodedPath()));
			int frames = fileFormat.getFrameLength();
			float frameRate = fileFormat.getFormat().getFrameRate();
			lengtInMicroSeconds = (long) (frames / frameRate * 1000);
			LOG.finest(String.format("Determined the lenght of " + "%s: %s microseconds.", basename(),
					lengtInMicroSeconds));
		} catch (UnsupportedAudioFileException e) {
			LOG.log(Level.WARNING, "Could not determine audio file length.", e);
		} catch (IOException e) {
			LOG.log(Level.SEVERE, "Could not determine audio file length.", e);
		}
		return lengtInMicroSeconds;
	}

	public void detectPitch(final PitchDetectionMode detectionMode, final DetectedPitchHandler handler,
			final double speedFactor) {
		final PitchDetector pitchDetector = detectionMode.getPitchDetector(this);
		pitchDetector.executePitchDetection();
		final List<Sample> samples = pitchDetector.getSamples();
		long previousSample = 0L;
		for (Sample sample : samples) {
			List<Double> pitchList = sample.getPitchesIn(PitchUnit.HERTZ);
			if (pitchList.size() > 0) {
				double pitchInHertz = pitchList.get(0);
				handler.handleDetectedPitch(sample.getStart() / 1000.0f, (float) pitchInHertz);
				try {
					long sleepTime = (long) ((sample.getStart() - previousSample) / speedFactor);
					System.out.println(sleepTime);
					Thread.sleep(sleepTime);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
				previousSample = sample.getStart();
			}
		}
	}
}
