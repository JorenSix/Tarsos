package be.hogent.tarsos.util;

import it.sauronsoftware.jave.AudioAttributes;
import it.sauronsoftware.jave.AudioInfo;
import it.sauronsoftware.jave.Encoder;
import it.sauronsoftware.jave.EncoderException;
import it.sauronsoftware.jave.EncodingAttributes;
import it.sauronsoftware.jave.InputFormatException;

import java.io.File;
import java.util.logging.Logger;

/**
 * Transcodes audio. Uses ffmpeg: a vast number of formats are supported. A
 * compiled ffmpeg binary is provided for Windows and Linux (32bit). If support
 * on another platform is required check the <a
 * href="http://www.sauronsoftware.it/projects/jave/manual.php">documentation of
 * JAVE</a> or disable transcoding (trough configuration) and perform it
 * manually .
 * 
 * @author Joren Six
 */
public final class AudioTranscoder {
	private static final Logger LOG = Logger.getLogger(AudioTranscoder.class.getName());

	// makes sure no instances of AudioTranscoder are created.
	private AudioTranscoder() {
	};

	/**
	 * Defines the target codec: signed 16 bit little endian pcm.
	 */
	private static final String TARGET_CODEC = Configuration.get(ConfKey.transcoded_audio_codec);
	/**
	 * Defines the target format: wav.
	 */
	private static final String TARGET_FORMAT = Configuration.get(ConfKey.transcoded_audio_format);
	/**
	 * The default sampling rate is used when no sampling rate is specified. The
	 * default is set to 44.1kHz
	 */
	private static final Integer DEFAULT_SAMPLING_RATE = Configuration
			.getInt(ConfKey.transcoded_audio_sampling_rate); // Hertz

	/**
	 * The number of channels used int he transcoded file.
	 */
	private static final Integer DEFAULT_NUMBER_OF_CHANNELS = Configuration
			.getInt(ConfKey.transcoded_audio_number_of_channels); // Mono

	/**
	 * Transcode the source file to target using the requested number of
	 * channels and sampling rate.
	 * 
	 * @param source
	 *            The path of the source file.
	 * @param target
	 *            The path of the target file. A stereo stream can be down mixed
	 *            to a mono stream. Converting a mono stream to a stereo stream
	 *            results in a file with two channels with the same data.
	 * @throws IllegalArgumentException
	 *             if the source file can not be read or the target file is not
	 *             writable.
	 */
	public static void transcode(final String source, final String target) {
		transcode(source, target, DEFAULT_NUMBER_OF_CHANNELS, DEFAULT_SAMPLING_RATE);
	}

	/**
	 * Transcode the source file to target using the requested number of
	 * channels and sampling rate.
	 * 
	 * @param source
	 *            The path of the source file.
	 * @param target
	 *            The path of the target file.
	 * @param channels
	 *            The number of channels target should have. A stereo stream can
	 *            be down mixed to a mono stream. Converting a mono stream to a
	 *            stereo stream results in a file with two channels with the
	 *            same data.
	 * @param samplingRate
	 *            The sampling rate the target file should have;
	 * @throws IllegalArgumentException
	 *             if the source file can not be read or the target file is not
	 *             writable.
	 */
	public static void transcode(final String source, final String target, final Integer channels,
			final Integer samplingRate) {
		final File sourceFile = new File(source);
		final File targetFile = new File(target);

		// sanity checks
		if (!sourceFile.exists()) {
			throw new IllegalArgumentException(source + " does not exist. It should"
					+ " be a readable audiofile.");
		}
		if (sourceFile.isDirectory()) {
			throw new IllegalArgumentException(source + " is a directory. It should "
					+ "be a readable audiofile.");
		}
		if (!sourceFile.canRead()) {
			throw new IllegalArgumentException(source
					+ " can not be read, check file permissions. It should be a readable audiofile.");
		}
		// if transcoding is enabled transcode
		if (Configuration.getBoolean(ConfKey.transcode_audio)) {
			try {
				final Encoder e = new Encoder();
				final EncodingAttributes attributes = new EncodingAttributes();
				final AudioAttributes audioAttributes = new AudioAttributes();
				audioAttributes.setChannels(channels);
				audioAttributes.setSamplingRate(samplingRate);
				audioAttributes.setCodec(TARGET_CODEC);
				attributes.setAudioAttributes(audioAttributes);
				attributes.setFormat(TARGET_FORMAT);
				e.encode(sourceFile, targetFile, attributes);
				LOG.info("Successfully transcoded " + source + " to " + target);
			} catch (final IllegalArgumentException e1) {
				LOG.severe("Incorrect encoding parameters");
				e1.printStackTrace();
			} catch (final InputFormatException e1) {
				LOG.severe("Unknown input file format: " + source);
				e1.printStackTrace();
			} catch (final EncoderException e1) {
				LOG.severe("Problems occured while transcoding " + source + ". Check output: " + target
						+ " message " + e1.getMessage());
			}
		} else { // if transcoding is disabled: copy the audio
			FileUtils.cp(source, target);
		}
	}

	/**
	 * Checks if transcoding is required: it fetches information about the file
	 * 'target' and checks if the file has the expected format, number of
	 * channels and sampling rate. If not then transcoding is required. If
	 * target is not found then transcoding is also required.
	 * 
	 * @param target
	 *            the path to the transcoded file or file to transcode
	 * @return false if the file is already transcoded as per the requested
	 *         parameters, false otherwise.
	 */
	public static boolean transcodingRequired(final String target) {
		return transcodingRequired(target, DEFAULT_NUMBER_OF_CHANNELS, DEFAULT_SAMPLING_RATE);
	}

	/**
	 * Checks if transcoding is required: it fetches information about the file
	 * 'target' and checks if the file has the expected format, number of
	 * channels and sampling rate. If not then transcoding is required. If
	 * target is not found then transcoding is also required.
	 * 
	 * @param target
	 *            the path to the transcoded file or file to transcode
	 * @param channels
	 *            Defines the number of channels the transcoded file should
	 *            have.
	 * @param samplingRate
	 *            Defines the samplingrate the transcoded file should have.
	 * @return false if the file is already transcoded as per the requested
	 *         parameters, false otherwise.
	 */
	public static boolean transcodingRequired(final String target, final Integer channels,
			final Integer samplingRate) {
		final File targetFile = new File(target);
		// if the file does not exist transcoding is always required
		boolean transcodingRequired = !targetFile.exists();
		// if the file exists, check the format
		// or skip the check (depending on the configuration)
		if (targetFile.exists() && !Configuration.getBoolean(ConfKey.skip_transcoded_audio_format_check)) {
			final AudioInfo info = getInfo(target);
			final int currentSamplingRate = info.getSamplingRate();
			final int currentNumberOfChannels = info.getChannels();
			final String currentDecoder = info.getDecoder();
			final boolean samplingRateMatches = currentSamplingRate == samplingRate;
			final boolean numberOfChannelsMatches = currentNumberOfChannels == channels;
			final boolean codecMatches = currentDecoder.equalsIgnoreCase(TARGET_CODEC);
			transcodingRequired = !(samplingRateMatches && numberOfChannelsMatches && codecMatches);
		}
		return transcodingRequired;
	}

	/**
	 * Returns information about an audio file: the sampling rate, the number of
	 * channels, the decoder, ...
	 * 
	 * @param file
	 *            the file to get the info for
	 * @return the info for the file.
	 */
	public static AudioInfo getInfo(final String file) {
		AudioInfo info = null;
		try {
			final Encoder e = new Encoder();
			info = e.getInfo(new File(file)).getAudio();
		} catch (final InputFormatException e1) {
			LOG.severe("Unknown input file format: " + file);
			e1.printStackTrace();
		} catch (final EncoderException e1) {
			LOG.warning("Could not get information about:" + file);
		}
		return info;
	}
}
