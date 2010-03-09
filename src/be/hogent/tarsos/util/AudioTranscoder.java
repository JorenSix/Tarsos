package be.hogent.tarsos.util;

import it.sauronsoftware.jave.AudioAttributes;
import it.sauronsoftware.jave.AudioInfo;
import it.sauronsoftware.jave.Encoder;
import it.sauronsoftware.jave.EncoderException;
import it.sauronsoftware.jave.EncodingAttributes;
import it.sauronsoftware.jave.InputFormatException;

import java.io.File;
import java.util.logging.Logger;

import be.hogent.tarsos.util.Configuration.Config;


/**
 * @author Joren Six
 * 
 * Transcodes audio. Uses ffmpeg: a vast number of formats are supported.
 * A compiled ffmpeg binary is provided for Windows and Linux (32bit). 
 * If support on another platform is required check the 
 * <a href="http://www.sauronsoftware.it/projects/jave/manual.php">documentation of JAVE</a> 
 * or disable transcoding (trough configuration) and perform it manually .
 */
public class AudioTranscoder
{
	private static Logger log = Logger.getLogger(AudioTranscoder.class.getName());
	
	//makes sure no instances of AudioTranscoder are created.
	private AudioTranscoder(){};
	
	/**
	 * Defines the target codec: signed 16 bit little endian pcm.
	 */
	private static final String  TARGET_CODEC = Configuration.get(Config.transcoded_audio_codec);
	/**
	 * Defines the target format: wav.
	 */
	private static final String  TARGET_FORMAT =  Configuration.get(Config.transcoded_audio_format);
	/**
	 * The default sampling rate is used when no sampling rate is specified.
	 * The default is set to 44.1kHz
	 */
	private static final Integer DEFAULT_SAMPLING_RATE = Configuration.getInt(Config.transcoded_audio_sampling_rate);//Hertz
	
	/**
	 * The number of channels used int he transcoded file.
	 */
	private static final Integer DEFAULT_NUMBER_OF_CHANNELS = Configuration.getInt(Config.transcoded_audio_number_of_channels);//Mono
	
	
	/**
	 * Transcode the source file to target using the requested number of channels and sampling rate.
	 * @param source The path of the source file.
	 * @param target The path of the target file.
	 * A stereo stream can be down mixed to a mono stream. 
	 * Converting a mono stream to a stereo stream results in a file with two channels with the same data. 
	 * @throws IllegalArgumentException if the source file can not be read or the target file is not writable.
	 */
	public static void transcode(String source, String target){
		transcode(source, target, DEFAULT_NUMBER_OF_CHANNELS, DEFAULT_SAMPLING_RATE);
	}
	
	/**
	 * Transcode the source file to target using the requested number of channels and sampling rate.
	 * @param source The path of the source file.
	 * @param target The path of the target file.
	 * @param channels The number of channels target should have. 
	 * A stereo stream can be down mixed to a mono stream. 
	 * Converting a mono stream to a stereo stream results in a file with two channels with the same data. 
	 * @param samplingRate The sampling rate the target file should have;
	 * @throws IllegalArgumentException if the source file can not be read or the target file is not writable.
	 */
	public static void transcode(String source, String target,Integer channels,Integer samplingRate){
		File sourceFile = new File(source);
		File targetFile = new File(target);		
		//sanity checks
		if(! sourceFile.exists())
			throw new IllegalArgumentException(source  + " does not exist. It should be a readable audiofile.");		
		if( sourceFile.isDirectory())
			throw new IllegalArgumentException(source  + " is a directory. It should be a readable audiofile.");		
		if( ! sourceFile.canRead())
			throw new IllegalArgumentException(source  + " can not be read, check file permissions. It should be a readable audiofile.");		
		if( ! sourceFile.canWrite())
			throw new IllegalArgumentException(target  + " can not be written, check file permissions. It should be in a writable location.");
		//if transcoding is enabled transcode
		if(Configuration.getBoolean(Config.transcode_audio)){
			try {
				Encoder e = new Encoder();			
				EncodingAttributes attributes = new EncodingAttributes();
				AudioAttributes audioAttributes = new AudioAttributes();
				audioAttributes.setChannels(channels);
				audioAttributes.setSamplingRate(samplingRate);
				audioAttributes.setCodec(TARGET_CODEC);			
				attributes.setAudioAttributes(audioAttributes);
				attributes.setFormat(TARGET_FORMAT);			
				e.encode(sourceFile, targetFile, attributes );			
			} catch (IllegalArgumentException e1) {
				log.severe("Incorrect encoding parameters");
				e1.printStackTrace();
			} catch (InputFormatException e1) {
				log.severe("Unknown input file format: " + source );
				e1.printStackTrace();
			} catch (EncoderException e1) {
				log.severe("Problems occured while transcoding " + source + ". Check output: " + target);
			}
		}else{//if transcoding is disabled: copy the audio			
			FileUtils.cp(source,target);
		}
	}
	
	/**
	 * Checks if transcoding is required: it fetches information about the file 'target' and 
	 * checks if the file has the expected format, number of channels and sampling rate. If not 
	 * then transcoding is required. If target is not found then transcoding is also required.
	 * @param target the path to the transcoded file or file to transcode
	 * @return false if the file is already transcoded as per the requested parameters, false otherwise.
	 */
	public static boolean transcodingRequired(String target){
		return transcodingRequired(target,DEFAULT_NUMBER_OF_CHANNELS,DEFAULT_SAMPLING_RATE);
	}
	
	/**
	 * Checks if transcoding is required: it fetches information about the file 'target' and 
	 * checks if the file has the expected format, number of channels and sampling rate. If not 
	 * then transcoding is required. If target is not found then transcoding is also required.
	 * @param target the path to the transcoded file or file to transcode
	 * @param channels Defines the number of channels the transcoded file should have.
	 * @param samplingRate Defines the samplingrate the transcoded file should have.
	 * @return false if the file is already transcoded as per the requested parameters, false otherwise.
	 */
	public static boolean transcodingRequired(String target,Integer channels,Integer samplingRate){
		File targetFile = new File(target);
		boolean transcodingRequired = true;
		if(targetFile.exists()){
			AudioInfo info = getInfo(target);
			int currentSamplingRate = info.getSamplingRate();
			int currentNumberOfChannels = info.getChannels();
			String currentDecoder = info.getDecoder(); 		
			transcodingRequired = !(currentSamplingRate == samplingRate && currentNumberOfChannels ==  channels && currentDecoder.equalsIgnoreCase(TARGET_CODEC));
		}
		return transcodingRequired;
	}
	
	
	/**
	 * Returns information about an audio file: the sampling rate, the number of channels, 
	 * the decoder, ...
	 * @param file the file to get the info for
	 * @return the info for the file.
	 */
	public static AudioInfo getInfo(String file){
		AudioInfo info = null;
		try {
			Encoder e = new Encoder();
			info = e.getInfo(new File(file)).getAudio();						
		} catch (InputFormatException e1) {
			log.severe("Unknown input file format: " + file );
			e1.printStackTrace();
		} catch (EncoderException e1) {
			log.warning("Could not get information about:" + file);
		}
		return info;
	}
}
