package be.hogent.tarsos.apps;

import java.util.List;

import be.hogent.tarsos.util.AudioFile;
import be.hogent.tarsos.util.SignalPowerExtractor;

public class PowerExtractor {

	private PowerExtractor(){}

	/**
	 * Extracts power features from some files.
	 * @param args
	 */
	public static void main(String... args){
		String[] globDirectories = {"makam","maghreb"};
		List<AudioFile> files = AudioFile.audioFiles(globDirectories);
		for(AudioFile file:files){
			System.out.println(file.basename());
			SignalPowerExtractor spex = new SignalPowerExtractor(file);
			spex.savePowerPlot("data/tests/power_" + file.basename() + ".png",-40);
			spex.saveTextFile("data/tests/power_" + file.basename() + ".txt");
			spex.saveWaveFormPlot("data/tests/waveform_" + file.basename() + ".png");
		}
	}
}
