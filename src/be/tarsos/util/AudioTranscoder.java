package be.tarsos.util;

import java.io.File;

import be.tarsos.dsp.AudioDispatcher;
import be.tarsos.dsp.io.jvm.AudioDispatcherFactory;
import be.tarsos.dsp.io.jvm.WaveformWriter;

public class AudioTranscoder {

	public static boolean transcodingRequired(String transcodedPath) {
		//perhaps also check RIFF header of wave file
		return !new File(transcodedPath).exists() || new File(transcodedPath).length() < 5000;
	}

	public static void transcode(String filePath, String transcodedPath) {
		AudioDispatcher adp = AudioDispatcherFactory.fromPipe(filePath, 44100, 2048, 0);
		adp.addAudioProcessor(new WaveformWriter(adp.getFormat(), transcodedPath));
		adp.run();
	}
}
