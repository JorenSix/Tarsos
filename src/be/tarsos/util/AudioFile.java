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
import java.io.FileFilter;
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

import be.tarsos.dsp.AudioEvent;
import be.tarsos.dsp.io.TarsosDSPAudioFloatConverter;
import be.tarsos.dsp.io.jvm.AudioPlayer;
import be.tarsos.dsp.io.jvm.JVMAudioInputStream;
import be.tarsos.sampled.pitch.Annotation;
import be.tarsos.sampled.pitch.PitchDetectionMode;
import be.tarsos.sampled.pitch.PitchDetector;
import be.tarsos.sampled.pitch.PitchUnit;
import be.tarsos.transcoder.ffmpeg.EncoderException;
import be.tarsos.util.Configuration.ConfigChangeListener;

/**
 * Represents an audio file. Facilitates transcoding, handling of originalPath
 * names and data sets.
 * 
 * @author Joren Six
 */
public final class AudioFile {

	/**
	 * Log messages.
	 */
	private static final Logger LOG = Logger.getLogger(AudioFile.class.getName());

	private final String originalPath;
	private final long lengthInMilliSeconds;
	private String md5;
	private final String transcodedPath;
	private final String transcodedDirectory;
	
	
	/**
	 * Initializes and maintains a list of files that are transcoded and analysed. 
	 * The md5 hash of each file is stored together with the file name corresponding with the
	 * first occurrence of the hash. Binary duplicates are redirected using the md5 to the first file name. 
	 * @author joren
	 */
	private static class AudioFileList implements ConfigChangeListener{
		
		private static AudioFileList instance;
		/**
		 * A hashmap with an MD5 hash as a key and the first detected name of the file as a value. 
		 * A hash could look like this:
		 * [53da1e8f42af35ca => MR.1964.1.1-28,0554af3e017912f0 => MR.1968.24.2-A2]
		 */
		private final Map<String, String> currentFiles;
		
		public static AudioFileList getInstance(){
			if(instance==null){
				instance = new AudioFileList();
			}
			return instance;
		}
		
		private AudioFileList(){
			currentFiles = new HashMap<String, String>();
			initializeCurrentFiles();
		}
		
		private void initializeCurrentFiles() {
			String path = Configuration.get(ConfKey.data_directory);
			File[] children = new File(path).listFiles(new FileFilter() {		
				
				public boolean accept(File pathname) {
					return pathname.isDirectory() && pathname.getName().length() > 17;
				}
			});
			for(File child : children){
				String childFileName = child.getName(); 
				String md5 = childFileName.substring(childFileName.length()-16);
				String name = childFileName.replace("_"+md5,"");
				currentFiles.put(md5, name);
			}
		}
		
		public void configurationChanged(ConfKey key) {
			if(key == ConfKey.data_directory){
				initializeCurrentFiles();
			}			
		}
		
		public boolean containsFile(String md5){
			return currentFiles.containsKey(md5);
		}
		
		public void addFile(String md5,AudioFile file){
			currentFiles.put(md5, FileUtils.basename(file.transcodedPath()));
		}
		
		public String getBaseName(String md5){
			return currentFiles.get(md5);
		}
	}


	/**
	 * Create and transcode an audio file.
	 * 
	 * @param filePath
	 *            the originalPath for the audio file
	 * @throws EncoderException
	 *             If FFMPEG fails to transcode the audio an
	 *             UnsupportedAudioFileException is generated.
	 */
	public AudioFile(final String filePath) throws EncoderException {
		this.originalPath = new File(filePath).getAbsolutePath();
		try{
			md5 = FileUtils.getMD5Checksum(originalPath).substring(16);
		}catch(final StringIndexOutOfBoundsException e){
			md5 = StringUtils.messageDigestFive(originalPath).substring(16);
		}
		AudioFileList list = AudioFileList.getInstance();
		

		// The aim of the MD5 hash is twofold:
		//
		// 1) To prevent name clashes: track 01.mp3 and track
		//    01.mp3 in different folders have other hashes. Only half of the hash
		//    is used because a MD5 hash name clash of half the length is also really
		//    improbable and you get shorter path names.
		// 2) To detect byte precise doubles: if two files with the same content are introduced
		//    to the system transcoding and analysis is only done one. Even if the
		//    files have a different name.
		
		final String baseName;
		if(list.containsFile(md5)){
			baseName = list.getBaseName(md5);
		} else {
			// 01. qsdflj.mp3 => 01._qsdfj
			baseName = FileUtils.basename(StringUtils.sanitize(originalPath));
		}
				
		// Configured data directory
		final String dataFolder = Configuration.get(ConfKey.data_directory);
		// Sub folder of data directory where annotations are stored =>
		// /01._qsdfj_MD5HASH
		final String subFolder = baseName + "_" + md5;
		// create the directory if it is not already there
		FileUtils.mkdirs(FileUtils.combine(dataFolder, subFolder));
		// return the name where the transcoded file should go
		transcodedDirectory = FileUtils.combine(dataFolder, subFolder);
				
		// 01._qsdfj => 01._qsdfj_transcoded.wav
		final String fileName = baseName + "_transcoded."
				+ AudioTranscoder.TARGET_ENCODING.getAttributes().getFormat();
		// return the name where the transcoded file should go
		transcodedPath = FileUtils.combine(transcodedDirectory, fileName);
		
		if (AudioTranscoder.transcodingRequired(transcodedPath())) {
			try{
				AudioTranscoder.transcode(filePath, transcodedPath());
			}catch(EncoderException e){
				//try to continue if the transcoded file exists
				LOG.warning("Transcoding probably failed: " + e.getMessage());
				if(!FileUtils.exists(transcodedPath())){
					throw e;
				}
			}
		}
		if(!list.containsFile(md5)){
			list.addFile(baseName, this);
		}
		lengthInMilliSeconds = calculateLengthInMilliSeconds();
	}

	/**
	 * @return the path of the transcoded audio file.
	 */
	public String transcodedPath() {
		return transcodedPath;
	}

	/**
	 * @return the directory where the transcoded audio file resides.
	 */
	public String transcodedDirectory() {
		return transcodedDirectory;
	}

	/**
	 * @return the path of the original file
	 */
	public String originalPath() {
		return this.originalPath;
	}

	
	public String toString() {
		return FileUtils.basename(originalPath);
	}

	/**
	 * @return the name of the file (without extension)
	 */
	public String originalBasename() {
		return this.toString();
	}
	
	/**
	 * @return the name of the file (without extension)
	 */
	public String transcodedBasename() {
		return FileUtils.basename(transcodedPath);
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
				try {
					files.add(new AudioFile(originalFile));
				} catch (EncoderException e) {
					LOG.severe(String.format("Transcoding failed: %s is not supported.", originalFile));
				}
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
		return lengthInMilliSeconds;
	}
	
	public double getLengthIn(TimeUnit unit) {
		return unit.convert(lengthInMilliSeconds, TimeUnit.MILLISECONDS);
	}

	private long calculateLengthInMilliSeconds() {
		long length = -1;
		try {
			AudioFileFormat fileFormat;
			fileFormat = AudioSystem.getAudioFileFormat(new File(transcodedPath()));
			int frames = fileFormat.getFrameLength();
			float frameRate = fileFormat.getFormat().getFrameRate();
			length = (long) (frames / frameRate * 1000);
			LOG.finest(String.format("Determined the lenght of %s: %s s", originalBasename(), lengthInMilliSeconds));
		} catch (UnsupportedAudioFileException e) {
			LOG.log(Level.WARNING, "Could not determine audio file length.", e);
		} catch (IOException e) {
			LOG.log(Level.SEVERE, "Could not determine audio file length.", e);
		}
		return length;
	}

	public double getMicrosecondsPositionOfFrame(final long frame) {
		long lengtInMicroSeconds = -1;
		try {
			AudioFileFormat fileFormat;
			fileFormat = AudioSystem.getAudioFileFormat(new File(transcodedPath()));
			float frameRate = fileFormat.getFormat().getFrameRate();
			lengtInMicroSeconds = (long) (frame / frameRate * 1000);
			LOG.finest(String.format("Determined the lenght of %s: %s �s", originalBasename(), lengtInMicroSeconds));
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
		AudioFileFormat fileFormat = fileFormat();
		int frames = fileFormat.getFrameLength();
		int frameSize = fileFormat.getFormat().getFrameSize();
		long size = frames * frameSize;
		LOG.finest(String.format("Determined size of audio data for %s: %s bytes.", originalBasename(), size));
		return size;
	}

	/**
	 * @return The file format info of the transcoded audio data.
	 */
	public AudioFileFormat fileFormat() {
		AudioFileFormat fileFormat = null;
		try {
			fileFormat = AudioSystem.getAudioFileFormat(new File(transcodedPath()));
			LOG.finest(String.format("Fileformat determined for %s", originalBasename()));
		} catch (UnsupportedAudioFileException e) {
			LOG.log(Level.WARNING, "Could not determine audio file length.", e);
		} catch (IOException e) {
			LOG.log(Level.SEVERE, "Could not determine audio file length.", e);
		}
		return fileFormat;
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
			AudioPlayer player = new AudioPlayer(stream.getFormat());
			TarsosDSPAudioFloatConverter converter = TarsosDSPAudioFloatConverter.getConverter(JVMAudioInputStream.toTarsosDSPFormat(stream.getFormat()));

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
				AudioEvent event = new AudioEvent(JVMAudioInputStream.toTarsosDSPFormat(stream.getFormat()),-1);
				float[] floatBuffer = new float[numberOfBytes/frameSize];
				converter.toFloatArray(audioInfo, floatBuffer);
				event.setFloatBuffer(floatBuffer);
				player.process(event);
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
		final List<Annotation> samples = pitchDetector.getAnnotations();
		List<Double> startPositions = new ArrayList<Double>();
		for (Annotation sample : samples) {
			double pitch = sample.getPitch(PitchUnit.RELATIVE_CENTS);
			if (pitch > from && pitch < to) {
				startPositions.add(sample.getStart());
			}
		}

		HashMap<Double, Double> selections = new HashMap<Double, Double>();
		if (startPositions.size() > 1) {
			int last = startPositions.size() - 1;
			double startSegment = startPositions.get(0);
			selections.put(startSegment, startPositions.get(last));
			for (int i = 0; i < startPositions.size() - 1; i++) {
				double start = startPositions.get(i);
				double nextStart = startPositions.get(i + 1);
				if (nextStart - start > 0.1) {
					selections.put(startSegment, start);
					startSegment = nextStart;
					selections.put(startSegment, startPositions.get(last));
				}
			}
		}

		double[] selectionsDouble = new double[selections.size() * 2];
		int index = 0;
		for (Map.Entry<Double, Double> entry : selections.entrySet()) {
			selectionsDouble[index++] = entry.getKey();
			selectionsDouble[index++] = entry.getValue();
		}
		Arrays.sort(selectionsDouble);
		playSelections(selectionsDouble);
	}
}
