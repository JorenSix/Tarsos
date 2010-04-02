package be.hogent.tarsos.util;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.List;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.UnsupportedAudioFileException;


import com.sun.media.sound.AudioFloatInputStream;

/**
 * @author Joren Six
 * An utility class to calculate and access the power of an audio file at any given time.
 */
public class SignalPowerExtractor {

	private final AudioFile audioFile;
	private final double  readWindow = 0.01; //seconds
	private double[] powerArray;


	/**
	 * Create a new power extractor
	 * @param audioFile the audio file to extract power from.
	 */
	public SignalPowerExtractor(AudioFile audioFile){
		this.audioFile = audioFile;
	}

	/**
	 * Returns the relative power [0.0;1.0] at the given time.
	 * @param seconds the time to get the relative power for.
	 * @return A number between 0 and 1 inclusive that shows the relative power at the given time
	 * @exception IndexOutOfBoundsException when the number of seconds is not between the start and end
	 * of the song.
	 */
	public double powerAt(double seconds){
		if(powerArray == null)
			extractPower();
		return powerArray[secondsToIndex(seconds)];
	}

	/**
	 * Calculates an index value for the power array from a number of seconds
	 */
	private int secondsToIndex(double seconds){
		return (int) (seconds/readWindow);
	}

	/**
	 * Fills the power array with relative power values.
	 */
	private void extractPower(){
		File inputFile = new File(audioFile.path());
		AudioInputStream ais;
		try {
			ais = AudioSystem.getAudioInputStream(inputFile);
			AudioFloatInputStream afis = AudioFloatInputStream.getInputStream(ais);
			AudioFormat format = ais.getFormat();

			double sampleRate = format.getSampleRate();
			double frameSize = format.getFrameSize();
			double frameRate = format.getFrameRate();
			double audioFileLengtInSeconds =  inputFile.length() / (frameSize * frameRate);

			powerArray = new double[secondsToIndex(audioFileLengtInSeconds) + 1];
			int readAmount = (int)(readWindow * sampleRate);
			float buffer[] = new float[readAmount];
			double maxPower = -1;
			double minPower = Double.MAX_VALUE;
			int index = 0;
			while(afis.read(buffer,0, readAmount) != -1) {
				double power = 0.0D;
				for(int i = 0; i < buffer.length; i++)
					power += buffer[i] * buffer[i];
				minPower = Math.min(power,minPower);
				maxPower = Math.max(power,maxPower);
				powerArray[index]=power;
				index++;
			}

			double powerDifference = maxPower - minPower;
			for(index = 0;index < powerArray.length;index++){
				powerArray[index] = (powerArray[index] - minPower)/powerDifference;
			}

		} catch (UnsupportedAudioFileException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	/**
	 * Creates a plot from a
	 * @param waveFormPlotFileName
	 */
	public void saveWaveFormPlot(String waveFormPlotFileName) {
		try {
			File inputFile = new File(audioFile.path());
			AudioInputStream ais;
			ais = AudioSystem.getAudioInputStream(inputFile);
			AudioFormat format = ais.getFormat();
			double frameSize = format.getFrameSize();
			double frameRate = format.getFrameRate();
			double timeFactor = 2.0 / (frameSize * frameRate);

			RandomAccessFile file = new RandomAccessFile(new File(audioFile.path()),"r");
			SimplePlot p = new SimplePlot("Waveform " + audioFile.basename());
			p.setSize(4000, 500);
			//skip header (44 bytes, fixed length)
			for(int i = 0 ; i < 44 ; i++){
				file.read();
			}
			int i1,index=0;
			while((i1 = file.read()) != -1){
				byte b1 = (byte) i1;
				byte b2 = (byte) file.read();
				if(index % 3 == 0){//write the power only every 10 bytes
					double power = (b2 << 8 | b1 & 0xFF) / 32767.0;
					double seconds = index * timeFactor;
					p.addData(seconds,power);
				}
				index++;
			}
			p.save(waveFormPlotFileName);
		} catch (IOException e) {
			e.printStackTrace();
		} catch (UnsupportedAudioFileException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}


	/**
	 * Creates a text file with relative power values for each sample.
	 * @param textFileName where to save the text file?
	 */
	public void saveTextFile(String textFileName){
		if(powerArray == null)
			extractPower();

		StringBuilder sb = new StringBuilder("Time (in seconds);Power\n");
		for(int index = 0;index < powerArray.length;index++){
			sb.append(index * readWindow)
			  .append(";")
			  .append(powerArray[index])
			  .append("\n");
		}
		FileUtils.writeFile(sb.toString(), textFileName);
	}

	/**
	 * Creates a 'power plot' of the signal.
	 * @param powerPlotFileName where to save the plot.
	 */
	public void savePowerPlot(String powerPlotFileName){
		if(powerArray == null)
			extractPower();

		SimplePlot plot = new SimplePlot("Powerplot for " + audioFile.basename());
		for(int index = 0;index < powerArray.length;index++){
			plot.addData(index * readWindow, powerArray[index]);
		}
		plot.save(powerPlotFileName);
	}

	public static void main(String... args){
		String[] globDirectories = {"makam","maghreb"};
		List<AudioFile> files = AudioFile.audioFiles(globDirectories);
		for(AudioFile file:files){
			System.out.println(file.basename());
			SignalPowerExtractor spex = new SignalPowerExtractor(file);
			spex.savePowerPlot("data/tests/power_" + file.basename() + ".png");
			spex.saveTextFile("data/tests/power_" + file.basename() + ".txt");
			spex.saveWaveFormPlot("data/tests/waveform_" + file.basename() + ".png");
		}
	}
}
