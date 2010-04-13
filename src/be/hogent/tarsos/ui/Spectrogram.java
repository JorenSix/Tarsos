package be.hogent.tarsos.ui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Timer;
import java.util.TimerTask;

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

	private static final int W = 640;
	private static final int H = 512;

	private static final int STEP = 1; // pixel

	private final int REFRESH_INTERVAL; // ms

	private int position = -1;

	private final BufferedImage buffer;
	private final Graphics2D bufferGraphics;

	private final BufferedImage imageEven;
	private final Graphics2D imageEvenGraphics;

	private final BufferedImage imageOdd;
	private final Graphics2D imageOddGraphics;

	private Graphics2D currentGraphics;

	private final Color[] colors;
	private final Color pitchColor = Color.RED;

	private final Timer timer;
	int readAmount = 1024;
	AudioFloatInputStream afis;
	double sampleRate;
	private final FFT fft;

	public Spectrogram(int mixerIndex) throws UnsupportedAudioFileException, IOException,
			LineUnavailableException {

		// the image shown on even runs trough the x axis
		imageEven = new BufferedImage(W, H / 2, BufferedImage.TYPE_INT_RGB);
		imageEvenGraphics = imageEven.createGraphics();

		// the image shown on odd runs trough the x axis
		imageOdd = new BufferedImage(W, H / 2, BufferedImage.TYPE_INT_RGB);
		imageOddGraphics = imageOdd.createGraphics();

		currentGraphics = imageEvenGraphics;

		buffer = new BufferedImage(W, H / 2, BufferedImage.TYPE_INT_RGB);
		bufferGraphics = buffer.createGraphics();
		bufferGraphics.setColor(Color.BLACK);
		bufferGraphics.clearRect(0, 0, W, H);

		colors = new Color[256];
		for (int i = 0; i < colors.length; i++) {
			colors[i] = new Color(i, i, i); // grayscale
		}

		javax.sound.sampled.Mixer.Info selected = AudioSystem.getMixerInfo()[mixerIndex];
		Mixer mixer = AudioSystem.getMixer(selected);
		AudioFormat format = new AudioFormat(44100, 16, 1, true, false);
		DataLine.Info dataLineInfo = new DataLine.Info(TargetDataLine.class,
				format);
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

		sampleRate = format.getSampleRate();
		REFRESH_INTERVAL = (int) (readAmount / sampleRate * 1000);

		timer = new Timer();
		timer.schedule(new TimerTask() {
			@Override
			public void run() {
				step();
			}
		}, 0, REFRESH_INTERVAL);

		fft = new FFT(readAmount/2,-1);
	}

	@Override
	public void paintComponent(Graphics g) {
		g.drawImage(buffer, 0, 0, null);
	}

	// executes on the timer thread
	public void step() {
		position = (position + STEP) % W;
		if (position == W - STEP) {
			currentGraphics = (currentGraphics == imageEvenGraphics) ? imageOddGraphics
					: imageEvenGraphics;
		}

		double[] colorIndexes = new double[H];
		int pitchIndex = -1;
		try {
			float buffer[] = new float[readAmount];
			if (afis.read(buffer, 0, readAmount) != -1) {

				float pitch = Yin.processBuffer(buffer, (float) sampleRate);
				if (pitch != -1)
					;
				pitchIndex = (int) (pitch * readAmount / sampleRate * 2 );

				fft.transform(buffer);

				double maxAmplitude = 0;
				for (int j = 0; j < buffer.length / 2; j++) {
					double amplitude = buffer[j] * buffer[j] + buffer[j + buffer.length/2] * buffer[j+ buffer.length/2];
					amplitude = Math.pow(amplitude, 0.5);
					colorIndexes[j] = amplitude;
					maxAmplitude = Math.max(amplitude, maxAmplitude);
				}

				for (int i = 0; i < colorIndexes.length; i++) {
					if (maxAmplitude == 0)
						colorIndexes[i] = 0;
					else
						colorIndexes[i] = colorIndexes[i] / maxAmplitude * 255;
				}
			} else {
				System.out.println("STOP");
				timer.cancel();
			}

		} catch (IOException e) {
			e.printStackTrace();
		}

		// no need to clear since everything is covered with opaque color
		for (int i = 0; i < H / STEP / 2; i++) {
			if (i == pitchIndex)
				currentGraphics.setColor(pitchColor);
			else
				currentGraphics.setColor(colors[(int) (255 - colorIndexes[i])]);

			currentGraphics.fillRect(position, (H / STEP /2) - i * STEP , STEP,
					STEP);
		}

		if (currentGraphics == imageEvenGraphics) {
			bufferGraphics.drawImage(imageEven, 0, 0, null);
			bufferGraphics.drawImage(imageOdd, position, 0, null);
		} else {
			bufferGraphics.drawImage(imageOdd, 0, 0, null);
			bufferGraphics.drawImage(imageEven, position, 0, null);
		}

		// paintComponent will be called on the EDT (Event Dispatch Thread)
		repaint();
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
		frame.pack();
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
