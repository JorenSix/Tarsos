package be.hogent.tarsos.pitch;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.List;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.UnsupportedAudioFileException;

import be.hogent.tarsos.util.AudioFile;
import be.hogent.tarsos.util.FileUtils;
import be.hogent.tarsos.util.SimplePlot;

import com.sun.media.sound.AudioFloatInputStream;

public class SignalPowerExtractor {

	private final AudioFile audioFile;
	private final double  readWindow = 0.01; //seconds
	private double[] powerArray;


	public SignalPowerExtractor(AudioFile audioFile){
		this.audioFile = audioFile;
	}

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
			powerArray = new double[(int) (audioFileLengtInSeconds/readWindow) + 1];
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
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	private void createWaveFormPlot() {
		try {
			RandomAccessFile file = new RandomAccessFile(new File(audioFile.path()),"r");
			SimplePlot p = new SimplePlot("waveform " + audioFile.basename());
			p.setSize(3000, 300);
			//skip header
			for(int i = 0 ; i < 44 ; i++){
				file.read();
			}
			int i1,index=0;
			while((i1 = file.read()) != -1){
				byte b1 = (byte) i1;
				byte b2 = (byte) file.read();
				if(index % 10 == 0){
					double power = (b2 << 8 | b1 & 0xFF) / 32767.0;
					p.addData(index,power);
				}
				index++;
			}
			p.save();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}


	public void saveTextFile(String fileName){
		if(powerArray == null)
			extractPower();

		StringBuilder sb = new StringBuilder("Time (in seconds);Power\n");
		for(int index = 0;index < powerArray.length;index++){
			sb.append(index * readWindow)
			  .append(";")
			  .append(powerArray[index])
			  .append("\n");
		}
		FileUtils.writeFile(sb.toString(), fileName);
	}

	public void savePlot(String fileName){
		if(powerArray == null)
			extractPower();

		SimplePlot plot = new SimplePlot("Powerplot for " + audioFile.basename());
		for(int index = 0;index < powerArray.length;index++){
			plot.addData(index * readWindow, powerArray[index]);
		}
		plot.save(fileName);
	}

	public static void main(String... args){
		String[] globDirectories = {"makam","maghreb"};
		List<AudioFile> files = AudioFile.audioFiles(globDirectories);
		for(AudioFile file:files){
			System.out.println(file.basename());
			SignalPowerExtractor spex = new SignalPowerExtractor(file);
			spex.savePlot("data/tests/power_" + file.basename() + ".png");
			spex.saveTextFile("data/tests/power_" + file.basename() + ".txt");
			spex.createWaveFormPlot();
		}
	}


}
