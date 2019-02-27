package be.tarsos.util;

import java.io.File;
import java.util.logging.Logger;

import be.tarsos.dsp.AudioDispatcher;
import be.tarsos.dsp.io.jvm.AudioDispatcherFactory;
import be.tarsos.dsp.io.jvm.WaveformWriter;

public class AudioTranscoder {
	
	private static final Logger LOG = Logger.getLogger(AudioTranscoder.class.getName());

	public static boolean transcodingRequired(String transcodedPath) {
		//perhaps also check RIFF header of wave file
		return !new File(transcodedPath).exists() || new File(transcodedPath).length() < 5000;
	}

	public static void transcode(String filePath, String transcodedPath) {
		LOG.fine("Transcoding to " + transcodedPath );
		AudioDispatcher adp = AudioDispatcherFactory.fromPipe(filePath, 44100, 2048, 0);
		adp.addAudioProcessor(new WaveformWriter(adp.getFormat(), transcodedPath));
		adp.run();
		long transcodedSize = new File(transcodedPath).length();
		LOG.fine("Transcoded file size (MB): " + (transcodedSize / (1024 * 1024.0)));
		if(transcodedSize == 0) {
			LOG.warning("Failed to transcode media file. Check ffmpeg");
		}
		
	}
}
