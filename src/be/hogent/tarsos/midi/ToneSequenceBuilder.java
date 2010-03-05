package be.hogent.tarsos.midi;


import jass.generators.LoopBuffer;
import jass.render.SourcePlayer;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import javax.sound.sampled.AudioFileFormat;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;

import be.hogent.tarsos.pitch.PitchFunctions;
import be.hogent.tarsos.util.FileUtils;



/**
 * @author joren.six@hogent.be
 * Create a sequence of tones. 
 * Tones are in this case a sine wave of a certain frequency (in Hertz) starting at a certain
 * time (in seconds) the current tone stops when another tone starts: this class generates only
 * one tone at the time (monophonic).   
 */
public class ToneSequenceBuilder {
	
	/**
	 * A list of frequencies
	 */
	private List<Double> frequencies;
	/**
	 * A list of starting times, frequencies.size() == realTimes.size(); 
	 */
	private final List<Double> realTimes;
	
	/**
	 * Initializes the lists of frequencies and times.
	 */
	public ToneSequenceBuilder(){
		frequencies = new ArrayList<Double>();
		realTimes = new ArrayList<Double>();		
	}

	/**
	 * Add a tone with a certain frequency (in Hertz) starting at a certain time (seconds).
	 * <p>
	 * The tone stops when the next tone starts. The last tone has a duration of 0 seconds.
	 * The entries should be added <b>chronologically</b>! Strange things will happen if you ignore
	 * this rule.
	 * </p>
	 * 
	 * @param frequency the frequency in Hertz of the tone to add
	 * @param realTime the starttime in seconds of the tone. The tone stops when the next one starts.
	 * The last tone is never played. 
	 * 
	 */
	public void addTone(double frequency, double realTime) {
		frequencies.add(frequency);
		realTimes.add(realTime);
	}
	
	/**
	 * Clears the frequencies and times. When this method finishes the object is in the same state as a new instance of {@link ToneSequenceBuilder}.
	 */
	public void clear(){
		frequencies.clear();
		realTimes.clear();
	}
	
	/**
	 * Returns a URL to a sine wave WAV file. If the file is not found it 
	 * unpacks a sine wave WAV file from the jar file to a temporary directory (java.io.tmpdir). 
	 * @return the URL to the audio file. 
	 * @throws IOException when the file is not accessible or when the user has no rights to write
	 * in the temporary directory.
	 */
	private URL sineWaveURL() throws IOException{		
		File sineWaveFile = new File(FileUtils.combine(System.getProperty("java.io.tmpdir"),"sin20ms.wav"));
		if(!sineWaveFile.exists()){
			InputStream in = this.getClass().getResourceAsStream("sin20ms.wav");
			OutputStream out = new FileOutputStream(sineWaveFile);
            byte[] buffer = new byte[4096];
            int r;
            while ((r = in.read(buffer)) != -1){
                out.write(buffer, 0, r);
            }
            out.close();
            in.close();
		}
		return sineWaveFile.toURI().toURL();
	}
	
	/**
	 * Write a WAV-file (sample rate 44.1 kHz) containing the tones and their respective durations (start times).
	 * @param fileName the name of the file to render. e.g. "out.wav". Also temporary .raw file is generated: e.g. out.wav.raw. It can not be deleted using
	 * java because the library jass keeps the file open until garbage collection. A delete on exit is requested but can fail. 
	 * Manually deleting the raw files is advised. So beware when generating a lot of files: temporarily you need twice the amount of hard disk space.
	 * @param smootFilterWindowSize to prevent (very) sudden changes in the frequency of tones a smoothing function can be applied.
	 * The window size of the smoothing function defines what an unexpected value is and if it is smoothed. When no smoothing is 
	 * required <strong>set it to zero</strong>. Otherwise a value between 5 and 50 is normal. ( 50 x 10 ms = 500 ms = 0.5 seconds).
	 * A <em>median filter</em> is used. 
	 * @throws Exception when something goes awry.
	 */
	public void writeFile(String fileName,int smootFilterWindowSize) throws Exception{
		//invariant: at any time the lists are equal in length
		assert frequencies.size() == realTimes.size();
		
		String rawFileName = fileName + ".raw";
		if(smootFilterWindowSize > 0)
			frequencies = PitchFunctions.medianFilter(frequencies,smootFilterWindowSize);
		
		URL sine50Hz44100 = sineWaveURL();
		
		float baseFreqWavFile = 50;
        float srate = 44100.f;   	
        int bufferSize = 1024;        
        LoopBuffer tone = new LoopBuffer(srate,bufferSize,sine50Hz44100);
        
        SourcePlayer player = new SourcePlayer(bufferSize,srate,rawFileName);
        player.addSource(tone);
        tone.setSpeed(0f/baseFreqWavFile);
		for(int i = 0 ; i < frequencies.size() ; i++){
			double freq = frequencies.get(i) == -1.0 ? 0.0 : frequencies.get(i);
			tone.setSpeed((float) freq/baseFreqWavFile);
			player.advanceTime(realTimes.get(i));	
		}
		convertRawToWav(srate,rawFileName,fileName);
		new File(rawFileName).deleteOnExit();
	}
	
	/**
	 * Adds a correct header to a raw file.
	 */
	private void  convertRawToWav(double srate,String rawFileName, String wavFileName) throws Exception {
        FileInputStream inStream = new FileInputStream(new File(rawFileName));
        File out = new File(wavFileName);
        int bytesAvailable = inStream.available();
        int sampleSizeInBits = 16;
        int channels = 1;
        boolean signed = false;
        boolean bigEndian = false;
        AudioFormat audioFormat = new AudioFormat((float)srate, sampleSizeInBits, channels, signed, bigEndian); 
        AudioInputStream  audioInputStream = new AudioInputStream(inStream,audioFormat,bytesAvailable/2);
        AudioSystem.write(audioInputStream, AudioFileFormat.Type.WAVE, out);
        audioInputStream.close();
        inStream.close();
    }
	
	
	/**
	 * Read data from a CSV-File, handle it with the handler, smooth it and save it to the generated audio folder.
	 * @param csvFileName the CSV-file to process
	 * @param handler the handler
	 * @param smootFilterWindowSize the window size for the smoothing function (Median filter).
	 */
	public static void saveAsWav(String csvFileName, CSVFileHandler handler,int smootFilterWindowSize){
		try {
			ToneSequenceBuilder builder = new ToneSequenceBuilder();		
			List<String[]> rows = FileUtils.readCSVFile(csvFileName,handler.getSeparator(),handler.getNumberOfExpectedColumn());
			for(String[] row : rows){
				handler.handleRow(builder, row);
			}
			builder.writeFile("data/generated_audio/" + FileUtils.basename(csvFileName) +".wav",smootFilterWindowSize);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * 
	 *
	 */
	interface CSVFileHandler{
		void handleRow(ToneSequenceBuilder builder,String[] row);
		String getSeparator();
		int getNumberOfExpectedColumn();
	}
	
	public static CSVFileHandler AUBIO_CSVFILEHANDLER = new CSVFileHandler(){
		@Override
		public void handleRow(ToneSequenceBuilder builder, String[] row) {
			double realTime = Double.parseDouble(row[0]);	
			double frequency = Double.parseDouble(row[1]);			
			builder.addTone(frequency,realTime);			
		}
		
		@Override
		public int getNumberOfExpectedColumn() {
			return 2;
		}

		@Override
		public String getSeparator() {
			return "\t";
		}		
	};
	
	public static CSVFileHandler BOZKURT_CSVFILEHANDLER = new BozkurtCSVFileHandler();
	
	public static CSVFileHandler IPEM_CSVFILEHANDLER = new IpemCSVFileHandler();
	
	private static class BozkurtCSVFileHandler implements CSVFileHandler{		
		private final double referenceFrequency = 8.17579891564371;//Hz
		@Override
		public void handleRow(ToneSequenceBuilder builder, String[] row) {
			double realTime = Double.parseDouble(row[0])/100;//100 Hz sample frequency (every 10 ms)
			double frequency = referenceFrequency * Math.pow(2.0,Double.parseDouble(row[1])/1200);			
			builder.addTone(frequency,realTime);
		}
		@Override
		public int getNumberOfExpectedColumn() {
			return 2;
		}
		@Override
		public String getSeparator() {
			return "[\\s]+";
		}
	}
	
	private static class IpemCSVFileHandler implements CSVFileHandler{
		private int sampleNumber = 0;
		@Override
		public void handleRow(ToneSequenceBuilder builder, String[] row) {
			sampleNumber ++;
			double realTime = ((double)sampleNumber )/100.0;//100 Hz sample frequency (every 10 ms)
			double frequency = Double.parseDouble(row[1]);			
			builder.addTone(frequency,realTime);
		}
		@Override
		public int getNumberOfExpectedColumn() {
			return 0;
		}
		@Override
		public String getSeparator() {
			return " ";
		}
	}

	public static void main(String[] args) throws Exception{
		/*
		saveAsWav("data/02_hicaz_klarnet.yin.txt", BOZKURT_CSVFILEHANDLER,0);
		saveAsWav("data/05_ussaktaksim_ney.yin.txt", BOZKURT_CSVFILEHANDLER,0);
		saveAsWav("data/03_huseynitaksim_tanbur.yin.txt", BOZKURT_CSVFILEHANDLER,0);
		saveAsWav("data/raw/aubio/aubio_yin_MR.1961.4.19-6.txt", AUBIO_CSVFILEHANDLER,9);
		*/
		
		ToneSequenceBuilder builder = new ToneSequenceBuilder();
		double realTime = 0;
		int interval = 240; //cents
		int numberOfChoises = 7; // +- 1.5 octaves
		int startingCentValue = 1200 * 4 + 300;//cents relative to c0 
		Random r = new Random();
		double referenceFrequency = 8.17579891564371;//Hz
		for(int i = 0;i<60;i++){
			
			double centValue = startingCentValue + r.nextInt(numberOfChoises)*interval;
			double frequency = referenceFrequency * Math.pow(2.0, centValue/1200.0);
			realTime  +=  r.nextInt(10) * 0.05;
			builder.addTone(frequency, realTime);
			if(builder.frequencies.size() > 5){
				realTime  +=  r.nextInt(5) * 0.05; //0 -> 1 sec
				builder.addTone(builder.frequencies.get(builder.frequencies.size()-3), realTime);
			}
		}
		builder.writeFile("middentoonstemming.wav", 0);
	}

}
