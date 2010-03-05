package be.hogent.tarsos.util;

import java.util.ArrayList;
import java.util.List;



/**
 * @author Joren Six
 * Represents an audio file. 
 * Facilitates transcoding, handling of path names and data sets.
 */
public class AudioFile {
	
	/**
	 * Where to save the transcoded files
	 */
	public static final String TRANSCODE_DIRECTORY = FileUtils.combine("data", "transcoded_audio");
	private static final String ORIGINAL_AUDIO_DIRECTORY = "audio";
	
	private final String path;
	/**
	 * Create and transcode an audio file.
	 * @param path the path for the audio file
	 */
	public AudioFile(String path){
		this.path = path;
		if(AudioTranscoder.transcodingRequired(transcodedPath()))
			AudioTranscoder.transcode(path, transcodedPath());
	}
	
	/**
	 * @return the path of the transcoded audio file.
	 */
	public String transcodedPath(){
		String baseName = FileUtils.basename(FileUtils.sanitizedFileName(path));
		return FileUtils.combine(TRANSCODE_DIRECTORY,baseName + ".wav");
	}
	
	/**
	 * @return the path of the original file
	 */
	public String path(){
		return this.path;
	}
	
	public String toString(){
		return FileUtils.basename(path);
	}
	
	/**
	 * @return the name of
	 */
	public String basename() {
		return this.toString();
	}	
	
	
	
	public static List<AudioFile> audioFiles(String... datasets){
		List<AudioFile> files = new ArrayList<AudioFile>();
		for(String dataset : datasets)
			for(String originalFile : FileUtils.glob(FileUtils.combine(ORIGINAL_AUDIO_DIRECTORY, dataset),".*\\..*"))
				files.add(new AudioFile(originalFile));
		return files;
	}


}
