package be.hogent.tarsos.ui;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;

import javax.swing.JComponent;
import javax.swing.JFrame;

import be.hogent.tarsos.util.AudioFile;
import be.hogent.tarsos.util.SignalPowerExtractor;
import be.hogent.tarsos.util.SignalPowerExtractor.WaveFormDataAggregator;

public final class WaveForm extends JComponent {

	/**
	 * 
	 */
	private static final long serialVersionUID = 3730361987954996673L;

	final AudioFile audioFile;

	BufferedImage image;
	Graphics2D graphics;

	public WaveForm(AudioFile file) {
		this.audioFile = file;
		setSize(new Dimension(1000, 280));
		image = new BufferedImage(getWidth(), getHeight(), BufferedImage.TYPE_INT_BGR);
		graphics = image.createGraphics();
		graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		graphics.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS,
				RenderingHints.VALUE_FRACTIONALMETRICS_ON);
		draw(graphics);
	}

	@Override
	public void paint(final Graphics g) {
		graphics.setBackground(Color.WHITE);
		graphics.clearRect(0, 0, getWidth(), getHeight());
		draw(graphics);
		g.drawImage(image, 0, 0, null);
	}

	public void draw(final Graphics2D graphics) {
		final int width = 1000;
		final int height = 280;

		graphics.setColor(Color.BLACK);

		final double secondsToX;
		final double powerToY;

		secondsToX = 1000 * width / (float) audioFile.getLengthInMilliSeconds();
		powerToY = height / 2.0;
		graphics.drawLine(0, (int) powerToY, width, (int) powerToY);
		SignalPowerExtractor spex = new SignalPowerExtractor(audioFile);

		spex.waveFormPlot(new WaveFormDataAggregator() {
			int prevx, prevy;

			@Override
			public void addDataPoint(double seconds, double power) {
				System.out.println("[" + seconds + "," + power + "]");
				int x = (int) (secondsToX * seconds);
				int y = (int) (Math.log10(power) * powerToY + powerToY);
				System.out.println("[" + x + "," + y + "]");
				graphics.drawLine(prevx, prevy, x, y);
				prevx = x;
				prevy = y;
			}
		});

	}

	public static void main(final String... strings) {
		JFrame f = new JFrame();
		String fileName = "C:\\Users\\jsix666\\eclipse_workspace\\Tarsos\\audio\\dekkmma_voice_all\\MR.1964.1.4-31.wav";

		AudioFile audioFile = new AudioFile(fileName);
		f.add(new WaveForm(audioFile));
		f.setVisible(true);
	}

}
