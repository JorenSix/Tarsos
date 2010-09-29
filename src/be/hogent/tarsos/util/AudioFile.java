package be.hogent.tarsos.util;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.sound.sampled.AudioFileFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.UnsupportedAudioFileException;

import be.hogent.tarsos.sampled.BlockingAudioPlayer;
import be.hogent.tarsos.sampled.pitch.PitchDetectionMode;
import be.hogent.tarsos.sampled.pitch.PitchDetector;
import be.hogent.tarsos.sampled.pitch.PitchUnit;
import be.hogent.tarsos.sampled.pitch.Sample;

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
	 * Returns a list of AudioFiles included in one or more folders. Searches
	 * recursively.
	 * 
	 * @param folders
	 *            The fo
	 * 
	 * @return a list of AudioFiles
	 */
	public static List<AudioFile> audioFiles(final String... folders) {
		final List<AudioFile> files = new ArrayList<AudioFile>();
		String pattern = Configuration.get(ConfKey.audio_file_name_pattern);
		for (final String folder : folders) {
			List<String> audioFiles = FileUtils.glob(folder, pattern, true);
			for (final String originalFile : audioFiles) {
				files.add(new AudioFile(originalFile));
			}
		}
		return files;
	}

	/**
	 * Determines the length of the transcoded file (only audio data) in
	 * microseconds.
	 * 
	 * @return The length of the file in microseconds. If the length could not
	 *         be determined -1 is returned.
	 */
	public long getLengthInMilliSeconds() {
		long lengtInMicroSeconds = -1;
		try {
			AudioFileFormat fileFormat;
			fileFormat = AudioSystem.getAudioFileFormat(new File(transcodedPath()));
			int frames = fileFormat.getFrameLength();
			float frameRate = fileFormat.getFormat().getFrameRate();
			lengtInMicroSeconds = (long) (frames / frameRate * 1000);
			LOG.finest(String.format("Determined the lenght of %s: %s µs", basename(), lengtInMicroSeconds));
		} catch (UnsupportedAudioFileException e) {
			LOG.log(Level.WARNING, "Could not determine audio file length.", e);
		} catch (IOException e) {
			LOG.log(Level.SEVERE, "Could not determine audio file length.", e);
		}
		return lengtInMicroSeconds;
	}

	public double getMicrosecondsPositionOfFrame(final long frame) {
		long lengtInMicroSeconds = -1;
		try {
			AudioFileFormat fileFormat;
			fileFormat = AudioSystem.getAudioFileFormat(new File(transcodedPath()));
			float frameRate = fileFormat.getFormat().getFrameRate();
			lengtInMicroSeconds = (long) (frame / frameRate * 1000);
			LOG.finest(String.format("Determined the lenght of %s: %s µs", basename(), lengtInMicroSeconds));
		} catch (UnsupportedAudioFileException e) {
			LOG.log(Level.WARNING, "Could not determine audio file length.", e);
		} catch (IOException e) {
			LOG.log(Level.SEVERE, "Could not determine audio file length.", e);
		}
		return lengtInMicroSeconds;
	}

	/**
	 * Determines the size of the transcoded audio data in bytes.
	 * 
	 * @return The size of the audio data in bytes. If the length could not be
	 *         determined -1 is returned.
	 */
	public long getSizeInBytes() {
		long size = -1;
		try {
			AudioFileFormat fileFormat;
			fileFormat = AudioSystem.getAudioFileFormat(new File(transcodedPath()));
			int frames = fileFormat.getFrameLength();
			int frameSize = fileFormat.getFormat().getFrameSize();
			size = frames * frameSize;
			LOG.finest(String.format("Determined size of audio data for %s: %s bytes.", basename(), size));
		} catch (UnsupportedAudioFileException e) {
			LOG.log(Level.WARNING, "Could not determine audio file length.", e);
		} catch (IOException e) {
			LOG.log(Level.SEVERE, "Could not determine audio file length.", e);
		}
		return size;
	}

	public void playSelection(final double from, final double to) {
		final double actualFrom;
		if (from < 0) {
			actualFrom = 0;
		} else {
			actualFrom = from;
		}
		double[] selections = { actualFrom, to };
		playSelections(selections);
	}

	/**
	 * Play selections of the audio file.
	 * 
	 * @param selections
	 *            An interleaved array with in the form
	 *            [from,to,from,to,from,to...]. The from and to values are given
	 *            in seconds.
	 */
	public void playSelections(final double[] selections) {

		try {
			final AudioInputStream stream = AudioSystem.getAudioInputStream(new File(transcodedPath()));
			int frameSize = stream.getFormat().getFrameSize();
			int bytesPerSecond = (int) (frameSize * stream.getFormat().getFrameRate());
			BlockingAudioPlayer player = new BlockingAudioPlayer(stream.getFormat(), 512, 0);

			int previousStop = 0;
			for (int i = 0; i < selections.length; i += 2) {
				double from = selections[i];
				double to = selections[i + 1];
				int start = (int) (bytesPerSecond * from);
				int stop = (int) (bytesPerSecond * to);
				stream.skip(start - previousStop);
				previousStop = stop;
				int numberOfBytes = stop - start;
				// only read complete frames
				while (numberOfBytes % frameSize != 0) {
					numberOfBytes++;
				}
				byte[] audioInfo = new byte[numberOfBytes];
				stream.read(audioInfo, 0, numberOfBytes);
				player.processFull(null, audioInfo);
			}

		} catch (UnsupportedAudioFileException e) {
			LOG.log(Level.SEVERE, "Unsupported audio for a transcoded file. Check your configuration.", e);
		} catch (IOException e) {
			LOG.log(Level.SEVERE, "Could not read a transcoded audio file. Check your configuration.", e);
		} catch (LineUnavailableException e) {
			LOG.log(Level.WARNING, "Could not play an audio segment. Audio line not available.", e);
		}
	}

	/**
	 * Detects pitch and plays plays the samples that are in the interval from,
	 * to in the given pitch unit. E.g. when you want to hear all the b's in a
	 * western piece you could select TARSOS_YIN as detection mode, form and to
	 * would be [190-210] and the unit would be relative cents.
	 * 
	 * @param detectionMode
	 *            The detection mode.
	 * @param from
	 *            Interval start.
	 * @param to
	 *            Interval stop.
	 * @param unit
	 *            The pitch unit.
	 */
	public void playCents(final PitchDetectionMode detectionMode, final double from, final double to,
			final PitchUnit unit) {
		final PitchDetector pitchDetector = detectionMode.getPitchDetector(this);
		pitchDetector.executePitchDetection();
		final List<Sample> samples = pitchDetector.getSamples();
		List<Long> startPositions = new ArrayList<Long>();
		for (Sample sample : samples) {
			List<Double> pitchList = sample.getPitchesIn(unit);
			if (pitchList.size() > 0 && pitchList.get(0) > from && pitchList.get(0) < to) {
				startPositions.add(sample.getStart());
			}
		}

		HashMap<Long, Long> selections = new HashMap<Long, Long>();
		if (startPositions.size() > 1) {
			int last = startPositions.size() - 1;
			long startSegment = startPositions.get(0);
			selections.put(startSegment, startPositions.get(last));
			for (int i = 0; i < startPositions.size() - 1; i++) {
				long start = startPositions.get(i);
				long nextStart = startPositions.get(i + 1);
				if (nextStart - start > 100) {
					selections.put(startSegment, start);
					startSegment = nextStart;
					selections.put(startSegment, startPositions.get(last));
				}
			}
		}

		double[] selectionsDouble = new double[selections.size() * 2];
		int index = 0;
		for (Map.Entry<Long, Long> entry : selections.entrySet()) {
			selectionsDouble[index++] = entry.getKey() / 1000.0;
			selectionsDouble[index++] = entry.getValue() / 1000.0;
		}
		Arrays.sort(selectionsDouble);
		playSelections(selectionsDouble);
	}
}
