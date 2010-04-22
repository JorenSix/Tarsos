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
import be.hogent.tarsos.pitch.PitchFunctions;
import be.hogent.tarsos.pitch.Yin;
import be.hogent.tarsos.util.FFT;

import com.sun.media.sound.AudioFloatInputStream;

/**
 * @author Joren Six
 * Implementation based on the sliding buffered images idea
 * from <a href="http://forums.sun.com/thread.jspa?threadID=5284602">this thread.</a>
 *
 */
public class Spectrogram extends JComponent {

	private static final long serialVersionUID = -7760501261506593771L;

	private static final int W = (int) Toolkit.getDefaultToolkit().getScreenSize().getWidth();
	private static final int H = (int) Toolkit.getDefaultToolkit().getScreenSize().getHeight();

	private static final int STEP = 1; // pixel


	private int position = -1;

	private final BufferedImage buffer;
	private final Graphics2D bufferGraphics;

	private final BufferedImage imageEven;
	private final Graphics2D imageEvenGraphics;

	private final Color pitchColor = Color.RED;

	private final Timer timer;

	private final int fftSize = 16384;
	private final int readStepSize = 1024;
	private final float audioDataBuffer[] = new float[fftSize];

	AudioFloatInputStream afis;
	double sampleRate;
	private final FFT fft;
	private final MidiDevice outputDevice;

	private int currentKeyDown;

	/**
	 * Sends a NOTE_ON or NOTE_OFF message on the requested key.
	 * @param midiKey The midi key to send the message for [0,VirtualKeyboard.NUMBER_OF_MIDI_KEYS[
	 * @param sendOnMessage <code>true</code> for NOTE_ON messages, <code>false</code> for NOTE_OFF
	 */
	private void sendNoteMessage(int midiKey, boolean sendOnMessage){
		//do not send note on messages to pressed keys
		if(sendOnMessage && currentKeyDown == midiKey)
			return;
		//do not send note off messages to keys that are not pressed
		if(!sendOnMessage && currentKeyDown != midiKey)
			return;

        try {
        	ShortMessage sm = new ShortMessage();
        	int command = sendOnMessage ? ShortMessage.NOTE_ON : ShortMessage.NOTE_OFF;
        	sm.setMessage(command, VirtualKeyboard.CHANNEL ,midiKey, 125);
            outputDevice.getReceiver().send(sm,-1);
        } catch (InvalidMidiDataException e1) {
            e1.printStackTrace();
        } catch (MidiUnavailableException e) {
			e.printStackTrace();
		}
        //mark key correctly
        currentKeyDown =  sendOnMessage ? midiKey : -1;
	}

	private void pitchToMidiOut(double pitch){
		double midiCentValue = PitchFunctions.convertHertzToMidiCent(pitch);
		int newKeyDown = -1;
		//'musical' pitch detected ?
		if( Math.abs(midiCentValue - (int) midiCentValue) < 0.25  && midiCentValue < 128 && midiCentValue >= 0){
			 newKeyDown = (int) midiCentValue;
			 lastDetectedNote = "Name: " + PitchFunctions.noteName(pitch) + "\nFrequency: " + ((int) pitch) + "Hz \t" + " MIDI note:" + PitchFunctions.convertHertzToMidiCent(pitch);
		}
		//if no pitch detected
		//send note off
		if(newKeyDown == -1 && currentKeyDown != -1){
			sendNoteMessage(currentKeyDown, false);
		} else if(currentKeyDown != newKeyDown){
			//if different pitch than previous detected send note off and on
			if(currentKeyDown != -1)
				sendNoteMessage(currentKeyDown, false);
			sendNoteMessage(newKeyDown, true);
			currentKeyDown = newKeyDown;
		}
	}



	public Spectrogram(int mixerIndex) throws UnsupportedAudioFileException, IOException,
			LineUnavailableException {

		outputDevice = PlayAlong.chooseDevice(false, true);
		try {
			outputDevice.open();
			ShortMessage sm = new ShortMessage();
            sm.setMessage(ShortMessage.PROGRAM_CHANGE,VirtualKeyboard.CHANNEL,72,0);
            outputDevice.getReceiver().send(sm,-1);
		} catch (MidiUnavailableException e) {
			//Unable to open midi device
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

		javax.sound.sampled.Mixer.Info selected = AudioSystem.getMixerInfo()[mixerIndex];
		Mixer mixer = AudioSystem.getMixer(selected);
		AudioFormat format = new AudioFormat(44100, 16, 1, true, false);
		DataLine.Info dataLineInfo = new DataLine.Info(TargetDataLine.class,format);
		TargetDataLine line = (TargetDataLine) mixer.getLine(dataLineInfo);
		int numberOfSamples = (int) (0.1 * 44100);
		line.open(format, numberOfSamples);
		line.start();
		AudioInputStream stream = new AudioInputStream(line);

		/*
		 * AudioFile audioFile = new
		 * AudioFile(FileUtils.combine("data","transcoded_audio"
		 * ,"flute.novib.mf.C5B5.wav")); AudioInputStream stream =
		 * AudioSystem.getAudioInputStream(new File(audioFile.path()));
		 * AudioFormat format = stream.getFormat();
		 */

		afis = AudioFloatInputStream.getInputStream(stream);
		//read first full buffer
		afis.read(audioDataBuffer, 0, fftSize);

		sampleRate = format.getSampleRate();

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

		fft = new FFT(fftSize);
	}

	@Override
	public void paintComponent(Graphics g) {
		g.drawImage(buffer, 0, 0, null);
	}

	double[] amplitudes = new double[H];
	String lastDetectedNote = "";

	private int frequencyToBin(double frequency){
		if(frequency == 0.0)
			return 0;
		int bin = H - 1 - (int) (frequency * H / sampleRate);
		return bin;
	}

	// executes on the timer thread
	public void step() throws IOException {
		position = (position + STEP) % W;
		double maxAmplitude = 0.0;
		int pitchIndex = -1;

		// slide buffer
		int slideSize = audioDataBuffer.length - readStepSize;
		for (int i = 0; i < readStepSize; i++) {
			audioDataBuffer[i + slideSize] = audioDataBuffer[i];
		}

		if (afis.read(audioDataBuffer, 0, readStepSize) != -1) {

			float pitch = detectPitch();
			if (pitch != -1) {
				pitchIndex = frequencyToBin(pitch);
				pitchToMidiOut(pitch);
			}

			double[] transformBuffer = new double[fftSize*2];
			for (int i = 0; i < fftSize; i++)
				transformBuffer[i] = audioDataBuffer[i];

			new com.sun.media.sound.FFT(audioDataBuffer.length,-1).transform(transformBuffer);

			for (int j = 0; j < audioDataBuffer.length / 2 ; j ++ ) {
				double amplitude = transformBuffer[j] * transformBuffer[j] + transformBuffer[j + audioDataBuffer.length/2] * transformBuffer[j + audioDataBuffer.length/2];
				amplitude = 20.0 * Math.log1p(amplitude);
				double pitchCurrentBin = j * sampleRate / fftSize /2 ;
				int pixelBin =  frequencyToBin(pitchCurrentBin);
				amplitudes[pixelBin] = amplitudes[pixelBin] == 0 ? amplitude : (amplitudes[pixelBin] + amplitude) / 2 ;
				maxAmplitude = Math.max(amplitudes[pixelBin],maxAmplitude);
			}

			Color colors[] = new Color[H];
			for(int i = 0; i < amplitudes.length; i++) {
				 if (maxAmplitude == 0){
					 colors[i] = Color.black;
				 }else{
					 int greyValue = (int) (amplitudes[i] / maxAmplitude * 255);
					 colors[i] = new Color(greyValue,greyValue,greyValue);
				 }
			}

			//no need to clear since everything is covered with opaque color
			for (int i = 0  ; i < H ; i++) {
				Color color = i == pitchIndex ? pitchColor :colors[i];
				imageEvenGraphics.setColor(color);
				imageEvenGraphics.fillRect(position,i, 1,1);
			}

			bufferGraphics.drawImage(imageEven, 0, 0, null);

			bufferGraphics.setColor(Color.WHITE);

			bufferGraphics.drawString((new StringBuilder("Current detected frequency: ")).append(((int)pitch)).append("Hz").toString(), 20, 20);
			bufferGraphics.drawString((new StringBuilder("Last detected note: ").append(lastDetectedNote).toString()), 20, 40);
		} else {
			timer.cancel();
		}

		// paintComponent will be called on the EDT (Event Dispatch Thread)
		repaint();
	}

	private final float yinBuffer[] = new float[readStepSize];
	private float detectPitch(){
		for (int i = 0; i < readStepSize; i++)
			yinBuffer[i] = audioDataBuffer[i];
		return Yin.processBuffer(yinBuffer, (float) sampleRate);
	}

	public static void main(String[] args)
			throws UnsupportedAudioFileException, IOException,
			LineUnavailableException {
		final JPanel panel = new JPanel(new BorderLayout());

		Spectrogram spectogram = new Spectrogram(chooseDevice());
		spectogram.setPreferredSize(new Dimension(W, H/2));
		panel.add(spectogram, BorderLayout.CENTER);
		final JFrame frame = new JFrame("Spectrogram");
		frame.getContentPane().add(panel);
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.setExtendedState(JFrame.MAXIMIZED_BOTH);
		frame.setUndecorated(true);
		//frame.pack();
		frame.setLocationRelativeTo(null);
		frame.setVisible(true);
	}

	/**
	 * Choose a Mixer device using CLI.
	 */
	public static int chooseDevice(){
		try {
			javax.sound.sampled.Mixer.Info mixers[] = AudioSystem.getMixerInfo();
			for (int i = 0; i < mixers.length; i++) {
				javax.sound.sampled.Mixer.Info mixerinfo = mixers[i];
				if (AudioSystem.getMixer(mixerinfo).getTargetLineInfo().length != 0)
					System.out.println(i + " " + mixerinfo.toString());
			}
			//choose MIDI input device
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
