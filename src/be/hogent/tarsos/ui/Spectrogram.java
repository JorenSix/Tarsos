package be.hogent.tarsos.ui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.IOException;
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

import org.apache.commons.math.complex.Complex;
import org.apache.commons.math.transform.FastFourierTransformer;

import be.hogent.tarsos.pitch.Yin;

import com.sun.media.sound.AudioFloatInputStream;

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

	public Spectrogram() throws UnsupportedAudioFileException, IOException,
			LineUnavailableException {

		// the image shown on even runs trough the x axis
		imageEven = new BufferedImage(W, H, BufferedImage.TYPE_INT_RGB);
		imageEvenGraphics = imageEven.createGraphics();

		// the image shown on odd runs trough the x axis
		imageOdd = new BufferedImage(W, H, BufferedImage.TYPE_INT_RGB);
		imageOddGraphics = imageOdd.createGraphics();

		currentGraphics = imageEvenGraphics;

		buffer = new BufferedImage(W, H, BufferedImage.TYPE_INT_RGB);
		bufferGraphics = buffer.createGraphics();
		bufferGraphics.setColor(Color.BLACK);
		bufferGraphics.clearRect(0, 0, W, H);

		colors = new Color[256];
		for (int i = 0; i < colors.length; i++) {
			colors[i] = new Color(i, i, i); // grayscale
		}

		javax.sound.sampled.Mixer.Info selected = AudioSystem.getMixerInfo()[5];
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
			double bufferD[] = new double[readAmount];
			if (afis.read(buffer, 0, readAmount) != -1) {

				float pitch = Yin.processBuffer(buffer, (float) sampleRate);
				if (pitch != -1)
					;
				pitchIndex = (int) (pitch * readAmount / sampleRate);

				for (int i = 0; i < buffer.length; i++)
					bufferD[i] = buffer[i];

				Complex[] data = new FastFourierTransformer()
						.transform(bufferD);

				double maxAmplitude = 0;
				for (int j = 0; j < data.length / 2; j++) {
					double amplitude = data[j].getReal() * data[j].getReal()
							+ data[j].getImaginary() * data[j].getImaginary();
					amplitude = Math.pow(amplitude, 0.5) / data.length;
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
			currentGraphics.fillRect(position, (H / STEP) - i * STEP * 2, STEP,
					STEP * 2);
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
		Spectrogram spectogram = new Spectrogram();
		spectogram.setPreferredSize(new Dimension(W, H));
		panel.add(spectogram, BorderLayout.CENTER);
		final JFrame frame = new JFrame("Scroller");
		frame.getContentPane().add(panel);
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.pack();
		frame.setLocationRelativeTo(null);
		frame.setVisible(true);
	}
}
