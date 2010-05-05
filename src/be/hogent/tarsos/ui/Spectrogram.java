package be.hogent.tarsos.ui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Toolkit;
import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Timer;
import java.util.TimerTask;

import javax.sound.midi.InvalidMidiDataException;
import javax.sound.midi.MidiDevice;
import javax.sound.midi.MidiUnavailableException;
import javax.sound.midi.ShortMessage;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.Mixer;
import javax.sound.sampled.TargetDataLine;
import javax.sound.sampled.UnsupportedAudioFileException;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JPanel;

import be.hogent.tarsos.apps.PlayAlong;
import be.hogent.tarsos.pitch.Pitch;
import be.hogent.tarsos.pitch.PitchConverter;
import be.hogent.tarsos.pitch.PitchUnit;
import be.hogent.tarsos.pitch.Yin;
import be.hogent.tarsos.util.FFT;

import com.sun.media.sound.AudioFloatInputStream;

/**
 * @author Joren Six Implementation based on the sliding buffered images idea
 *         from <a
 *         href="http://forums.sun.com/thread.jspa?threadID=5284602">this
 *         thread.</a>
 * 
 */
public class Spectrogram extends JComponent {

	private static final long serialVersionUID = -7760501261506593771L;

	private static final int W = (int) Toolkit.getDefaultToolkit().getScreenSize().getWidth();
	private static final int H = (int) Toolkit.getDefaultToolkit().getScreenSize().getHeight();

	private int position = -1;

	private final BufferedImage buffer;
	private final Graphics2D bufferGraphics;

	private final BufferedImage imageEven;
	private final Graphics2D imageEvenGraphics;

	private final Color pitchColor = Color.RED;

	private final Timer timer;

	private final int fftSize = 16384 / 2;
	private final float audioDataBuffer[] = new float[fftSize];

	AudioFloatInputStream afis;
	double sampleRate;
	private final FFT fft;
	private final MidiDevice outputDevice;
	int currentKeyDown;
	double[] amplitudes = new double[H];
	String lastDetectedNote = "";

	public Spectrogram(int mixerIndex) throws UnsupportedAudioFileException, IOException, LineUnavailableException {

		outputDevice = PlayAlong.chooseDevice(false, true);
		try {
			outputDevice.open();
			ShortMessage sm = new ShortMessage();
			sm.setMessage(ShortMessage.PROGRAM_CHANGE, VirtualKeyboard.CHANNEL, 72, 0);
			outputDevice.getReceiver().send(sm, -1);
		} catch (MidiUnavailableException e) {
			// Unable to open midi device
		} catch (InvalidMidiDataException e) {
			e.printStackTrace();
		}

		// the image shown on even runs trough the x axis
		imageEven = new BufferedImage(W, H, BufferedImage.TYPE_INT_RGB);
		imageEvenGraphics = imageEven.createGraphics();

		buffer = new BufferedImage(W, H, BufferedImage.TYPE_INT_RGB);
		bufferGraphics = buffer.createGraphics();
		bufferGraphics.setColor(Color.BLACK);
		bufferGraphics.clearRect(0, 0, W, H);

		sampleRate = 44100;

		javax.sound.sampled.Mixer.Info selected = AudioSystem.getMixerInfo()[mixerIndex];
		Mixer mixer = AudioSystem.getMixer(selected);
		AudioFormat format = new AudioFormat((float) sampleRate, 16, 1, true, false);
		DataLine.Info dataLineInfo = new DataLine.Info(TargetDataLine.class, format);
		TargetDataLine line = (TargetDataLine) mixer.getLine(dataLineInfo);
		int numberOfSamples = (int) (0.1 * sampleRate);
		line.open(format, numberOfSamples);
		line.start();
		AudioInputStream stream = new AudioInputStream(line);

		// AudioFile audioFile = new
		// AudioFile(FileUtils.combine("data","transcoded_audio"
		// ,"flute.novib.mf.C5B5.wav"));
		// stream = AudioSystem.getAudioInputStream(new File(audioFile.path()));

		afis = AudioFloatInputStream.getInputStream(stream);
		// read first full buffer
		afis.read(audioDataBuffer, 0, fftSize);

		fft = new FFT(fftSize);

		timer = new Timer();
		timer.schedule(new TimerTask() {
			@Override
			public void run() {
				try {
					step();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}, 0, 25);

	}

	@Override
	public void paintComponent(Graphics g) {
		g.drawImage(buffer, 0, 0, null);
	}

	private int frequencyToBin(double frequency) {
		double minFrequency = 100; // Hz
		double maxFrequency = 20000;// Hz
		int bin = 0;
		boolean logaritmic = true;
		if (frequency != 0 && frequency > minFrequency && frequency < maxFrequency) {
			double binEstimate = 0;
			if (logaritmic) {
				minFrequency = PitchConverter.hertzToAbsoluteCent(minFrequency);
				maxFrequency = PitchConverter.hertzToAbsoluteCent(maxFrequency);
				frequency = PitchConverter.hertzToAbsoluteCent(frequency * 2);
				binEstimate = (frequency - minFrequency) / maxFrequency * H;
			} else {
				binEstimate = (frequency - minFrequency) / maxFrequency * H;
			}
			if (binEstimate > 700)
				System.out.println(binEstimate);

			bin = H - 1 - (int) binEstimate;
		}

		return bin;
	}

	// executes on the timer thread
	public void step() throws IOException {
		position = (1 + position) % W;
		double maxAmplitude = 0.0;
		int pitchIndex = -1;

		boolean bufferRead = Yin.slideBuffer(afis, audioDataBuffer, audioDataBuffer.length - 1024);
		if (bufferRead) {

			float pitch = detectPitch();
			if (pitch != -1) {
				pitchIndex = frequencyToBin(pitch);
				pitchToMidiOut(pitch);
			}

			float[] transformBuffer = new float[audioDataBuffer.length * 2];
			for (int i = 0; i < audioDataBuffer.length; i++) {
				transformBuffer[i] = audioDataBuffer[i];
			}

			fft.forwardTransform(transformBuffer);

			for (int j = 0; j < audioDataBuffer.length; j++) {
				double amplitude = fft.modulus(transformBuffer, j);
				amplitude = 20 * Math.log1p(amplitude);
				double pitchCurrentBin = j * sampleRate / fftSize / 4;
				int pixelBin = frequencyToBin(pitchCurrentBin);
				amplitudes[pixelBin] = amplitudes[pixelBin] == 0 ? amplitude : (amplitudes[pixelBin] + amplitude) / 2;
				maxAmplitude = Math.max(amplitudes[pixelBin], maxAmplitude);
			}

			for (int i = 0; i < amplitudes.length; i++) {
				Color color = Color.black;
				if (i == pitchIndex) {
					color = pitchColor;
				} else if (maxAmplitude != 0) {
					int greyValue = (int) (amplitudes[i] / maxAmplitude * 255);
					color = new Color(greyValue, greyValue, greyValue);
				}
				imageEvenGraphics.setColor(color);
				imageEvenGraphics.fillRect(position, i, 1, 1);
			}

			bufferGraphics.drawImage(imageEven, 0, 0, null);
			bufferGraphics.setColor(Color.WHITE);
			bufferGraphics.drawString((new StringBuilder("Current frequency: ")).append(((int) pitch)).append("Hz").toString(), 20, 20);
			bufferGraphics.drawString((new StringBuilder("Last detected note: ").append(lastDetectedNote).toString()), 20, 45);
		} else {
			timer.cancel();
		}

		// paintComponent will be called on the EDT (Event Dispatch Thread)
		repaint();
	}

	/**
	 * Sends a NOTE_ON or NOTE_OFF message on the requested key.
	 * 
	 * @param midiKey
	 *            The midi key to send the message for
	 *            [0,VirtualKeyboard.NUMBER_OF_MIDI_KEYS[
	 * @param sendOnMessage
	 *            <code>true</code> for NOTE_ON messages, <code>false</code> for
	 *            NOTE_OFF
	 */
	private void sendNoteMessage(int midiKey, boolean sendOnMessage) {
		// do not send note on messages to pressed keys
		if (sendOnMessage && currentKeyDown == midiKey)
			return;
		// do not send note off messages to keys that are not pressed
		if (!sendOnMessage && currentKeyDown != midiKey)
			return;

		try {
			ShortMessage sm = new ShortMessage();
			int command = sendOnMessage ? ShortMessage.NOTE_ON : ShortMessage.NOTE_OFF;
			sm.setMessage(command, VirtualKeyboard.CHANNEL, midiKey, 125);
			outputDevice.getReceiver().send(sm, -1);
		} catch (InvalidMidiDataException e1) {
			e1.printStackTrace();
		} catch (MidiUnavailableException e) {
			e.printStackTrace();
		}
		// mark key correctly
		currentKeyDown = sendOnMessage ? midiKey : -1;
	}

	private void pitchToMidiOut(double pitch) {
		double midiCentValue = PitchConverter.hertzToMidiCent(pitch);
		int newKeyDown = -1;
		// 'musical' pitch detected ?
		if (Math.abs(midiCentValue - (int) midiCentValue) < 0.3 && midiCentValue < 128 && midiCentValue >= 0) {
			newKeyDown = (int) midiCentValue;
			lastDetectedNote = "Name: " + Pitch.getInstance(PitchUnit.HERTZ, pitch).noteName() + "\nFrequency: " + ((int) pitch) + "Hz \t"
					+ " MIDI note:" + PitchConverter.hertzToMidiCent(pitch);
		}
		// if no pitch detected
		// send note off
		if (newKeyDown == -1 && currentKeyDown != -1) {
			sendNoteMessage(currentKeyDown, false);
		} else if (currentKeyDown != newKeyDown) {
			// if different pitch than previous detected send note off and on
			if (currentKeyDown != -1)
				sendNoteMessage(currentKeyDown, false);
			sendNoteMessage(newKeyDown, true);
			currentKeyDown = newKeyDown;
		}
	}

	private final float yinBuffer[] = new float[1024];

	private float detectPitch() {
		for (int i = 0; i < 1024; i++)
			yinBuffer[i] = audioDataBuffer[i];
		return Yin.processBuffer(yinBuffer, (float) sampleRate);
	}

	public static void main(String[] args) throws UnsupportedAudioFileException, IOException, LineUnavailableException {
		final JPanel panel = new JPanel(new BorderLayout());

		Spectrogram spectogram = new Spectrogram(chooseDevice());
		spectogram.setPreferredSize(new Dimension(W, H / 2));
		panel.add(spectogram, BorderLayout.CENTER);
		final JFrame frame = new JFrame("Spectrogram");
		frame.getContentPane().add(panel);
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.setExtendedState(JFrame.MAXIMIZED_BOTH);
		frame.setUndecorated(true);
		// frame.pack();
		frame.setLocationRelativeTo(null);
		frame.setVisible(true);
	}

	/**
	 * Choose a Mixer device using CLI.
	 */
	public static int chooseDevice() {
		try {
			javax.sound.sampled.Mixer.Info mixers[] = AudioSystem.getMixerInfo();
			for (int i = 0; i < mixers.length; i++) {
				javax.sound.sampled.Mixer.Info mixerinfo = mixers[i];
				if (AudioSystem.getMixer(mixerinfo).getTargetLineInfo().length != 0)
					System.out.println(i + " " + mixerinfo.toString());
			}
			// choose MIDI input device
			System.out.print("Choose the Mixer device: ");
			BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
			int deviceIndex = Integer.parseInt(br.readLine());
			return deviceIndex;
		} catch (NumberFormatException e) {
			System.out.println("Invalid number, please try again");
			return chooseDevice();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return -1;
	}
}
