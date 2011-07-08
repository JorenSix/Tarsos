package be.hogent.tarsos.cli.temp;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.Mixer;
import javax.sound.sampled.SourceDataLine;
import javax.sound.sampled.TargetDataLine;

import be.hogent.tarsos.Tarsos;
import be.hogent.tarsos.util.FFT;

import com.sun.media.sound.AudioFloatConverter;
import com.sun.media.sound.AudioFloatInputStream;

public final class AutoTune {

	private AutoTune() {
	}

	/**
	 * Log messages.
	 */
	private static final Logger LOG = Logger.getLogger(AutoTune.class.getName());

	/**
	 * Choose a Mixer device using CLI.
	 * @return An integer representing the device index.
	 */
	public static int chooseDevice() {
		int deviceIndex = -1;
		try {
			final javax.sound.sampled.Mixer.Info[] mixers = AudioSystem.getMixerInfo();
			for (int i = 0; i < mixers.length; i++) {
				final javax.sound.sampled.Mixer.Info mixerinfo = mixers[i];
				if (AudioSystem.getMixer(mixerinfo).getTargetLineInfo().length != 0) {
					Tarsos.println(i + " " + mixerinfo.toString());
				}
			}
			// choose MIDI input device
			Tarsos.println("Choose the Mixer device: ");
			final BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
			deviceIndex = Integer.parseInt(br.readLine());
		} catch (final NumberFormatException e) {
			Tarsos.println("Invalid number, please try again");
			deviceIndex = chooseDevice();
		} catch (final IOException e) {
			LOG.log(Level.SEVERE, "Exception while reading from STD IN.", e);
		}
		return deviceIndex;
	}

	public static void main(final String... args) throws LineUnavailableException {
		new Thread(new AudioProcessor(chooseDevice())).start();
	}

	public static final class Speaker {
		private static final float SAMPLERATE = 44100;
		private static final int CHANNELS = 1;
		private static final int BITS = 16;

		private final AudioFormat format = new AudioFormat(SAMPLERATE, BITS, CHANNELS, true, false);
		private final AudioFloatConverter converter = AudioFloatConverter.getConverter(format);

		private SourceDataLine line;

		public Speaker() {
			final DataLine.Info info = new DataLine.Info(SourceDataLine.class, format);

			// Obtain and open the line.
			try {
				if (!AudioSystem.isLineSupported(info)) {
					throw new LineUnavailableException("Line not supported");
				}
				line = (SourceDataLine) AudioSystem.getLine(info);
				line.open(format);
				line.start();
			} catch (final LineUnavailableException ex) {
				LOG.log(Level.SEVERE, "Line not available", ex);
			}
		}

		public void write(final float[] originalData, final int start, final int end) {
			final byte[] convertedData = new byte[originalData.length * 2];
			converter.toByteArray(originalData, convertedData);
			line.write(convertedData, start, end - start);
		}
	}

	private static final class AudioProcessor implements Runnable {

		AudioFloatInputStream afis;
		float[] audioBuffer;
		FFT fft;
		Speaker speaker;

		float sampleRate = 44100;

		private AudioProcessor(final int inputDevice) throws LineUnavailableException {
			speaker = new Speaker();
			final javax.sound.sampled.Mixer.Info selected = AudioSystem.getMixerInfo()[inputDevice];
			final Mixer mixer = AudioSystem.getMixer(selected);
			final AudioFormat format = new AudioFormat(sampleRate, 16, 1, true, false);
			final DataLine.Info dataLineInfo = new DataLine.Info(TargetDataLine.class, format);
			final TargetDataLine line = (TargetDataLine) mixer.getLine(dataLineInfo);
			final int numberOfSamples = (int) (0.1 * sampleRate);
			line.open(format, numberOfSamples);
			line.start();
			final AudioInputStream stream = new AudioInputStream(line);
			afis = AudioFloatInputStream.getInputStream(stream);

			audioBuffer = new float[2048];
			fft = new FFT(1024);
		}

		public void run() {

			try {
				boolean hasMoreBytes = afis.read(audioBuffer, 0, audioBuffer.length) != -1;
				while (hasMoreBytes) {

					// float pitch = Yin.processBuffer(audioBuffer, SAMPLERATE);

					// if(pitch > 440 && pitch < 3520){
					// System.out.println(pitch);

					// calculate fft
					fft.forwardTransform(audioBuffer);

					// scale pitch
					/*
					 * int originalBin = (int) (pitch * audioBuffer.length /
					 * SAMPLERATE); int newBin = (int) (1760 audioBuffer.length
					 * / SAMPLERATE); int diff = newBin - originalBin; if(diff >
					 * 0) for(int bufferCount = audioBuffer.length - 1;
					 * bufferCount >= 0 ; bufferCount--){
					 * audioBuffer[bufferCount] = bufferCount - diff >= 0 ?
					 * audioBuffer[bufferCount-diff] : 0; } else for(int
					 * bufferCount = 0; bufferCount < audioBuffer.length ;
					 * bufferCount++){ audioBuffer[bufferCount] =
					 * bufferCount-diff < audioBuffer.length ?
					 * audioBuffer[bufferCount-diff] : 0; }
					 */
					// inverse fft
					fft.backwardsTransform(audioBuffer);

					// play resulting audio
					speaker.write(audioBuffer, 0, 1024);
					// }

					for (int i = 0; i < 1024; i++) {
						audioBuffer[i] = audioBuffer[1024 + i];
					}
					hasMoreBytes = afis.read(audioBuffer, audioBuffer.length - 1024, 1024) != -1;
				}
			} catch (final IOException e) {
				e.printStackTrace();
			}
		}
	}
}
