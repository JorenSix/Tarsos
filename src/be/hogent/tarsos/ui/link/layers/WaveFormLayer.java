package be.hogent.tarsos.ui.link.layers;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

import javax.sound.sampled.UnsupportedAudioFileException;

import be.hogent.tarsos.dsp.AudioDispatcher;
import be.hogent.tarsos.dsp.AudioEvent;
import be.hogent.tarsos.dsp.AudioProcessor;
import be.hogent.tarsos.ui.link.LinkedFrame;
import be.hogent.tarsos.ui.link.LinkedPanel;
import be.hogent.tarsos.ui.link.coordinatessystems.CoordinateSystem;
import be.hogent.tarsos.util.AudioFile;
import be.hogent.tarsos.util.StopWatch;

public class WaveFormLayer extends BackgroundLayer {

	private Color waveFormColor;
//	private BufferedImage waveFormImage;

	public WaveFormLayer(LinkedPanel parent, Color color) {
		super(parent, color);
		waveFormColor = new Color(255 - color.getRed(), 255 - color.getGreen(),
				255 - color.getBlue());
	}

	public void draw(Graphics2D graphics) {
		super.draw(graphics);
		graphics.setColor(waveFormColor);
		graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
				RenderingHints.VALUE_ANTIALIAS_ON);
		graphics.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS,
				RenderingHints.VALUE_FRACTIONALMETRICS_ON);
		this.createWaveFormImage(graphics);

		// graphics.fillRect(
		// Math.round(cs.getMin(CoordinateSystem.X_AXIS)),
		// Math.round(cs.getMin(CoordinateSystem.Y_AXIS)),
		// Math.round(cs.getDelta(CoordinateSystem.X_AXIS)),
		// Math.round(cs.getDelta(CoordinateSystem.Y_AXIS)));
	}

	private void createWaveFormImage(final Graphics2D graphics) {
		final StopWatch watch = new StopWatch();

		try {
			CoordinateSystem cs = parent.getCoordinateSystem();
			final int waveFormYMin = (int) cs.getMin(CoordinateSystem.Y_AXIS);
			final int waveFormYMax = (int) cs.getMax(CoordinateSystem.Y_AXIS);
			final int waveFormXMin = (int) cs.getMin(CoordinateSystem.X_AXIS);
			final int waveFormXMax = (int) cs.getMax(CoordinateSystem.X_AXIS);

			final int waveFormHeightInCents = (int) cs
					.getDelta(CoordinateSystem.Y_AXIS);
			final int waveFormWidthInMs = (int) cs
					.getDelta(CoordinateSystem.X_AXIS);

			final int waveFormWidth = parent.getWidth();
			final int waveFormHeight = parent.getHeight();
//			waveFormImage = new BufferedImage(waveFormWidth, waveFormHeight,
//					BufferedImage.TYPE_INT_RGB);

			AudioFile f = LinkedFrame.getInstance().getAudioFile();
			
			graphics.transform(getSaneTransform(waveFormHeight));
			
			final float sampleRate = f.fileFormat().getFormat().getSampleRate();
			
			float percentageOfSongDisplayed = waveFormWidthInMs/f.getLengthInMilliSeconds();
			int amountOfSamples = f.fileFormat().getFrameLength();
			int amountOfSamplesDisplayed = Math.round(amountOfSamples*percentageOfSongDisplayed);
			int samplesPerPixel = Math.round((float)amountOfSamplesDisplayed/(float)waveFormWidth);// waarom / 8 ??;
			
			float frameSizeInSec = sampleRate(Samples/sec)
					
					
//			final int one = (int) (waveFormHeight / 2 * 0.85);

//			final double secondsToX;
//			secondsToX = 1000 * waveFormWidth
//					/ (float) f.getLengthInMilliSeconds();

//			float[] samples = f.fileFormat().getFormat().
			
			AudioDispatcher adp = AudioDispatcher.fromFile(
					new File(f.transcodedPath()), samplesPerPixel, 0);
			adp.addAudioProcessor(new AudioProcessor() {

				private int frame = 0;

				public void processingFinished() {
				}

				public boolean process(AudioEvent audioEvent) {
					
					float[] audioFloatBuffer = audioEvent.getFloatBuffer();
					double seconds = frame / frameRate;
					frame += audioFloatBuffer.length;
					int x = (int) (secondsToX * seconds);
					int y = (int) (audioFloatBuffer[0] * one);
					graphics.drawLine(x, 0, x, y);
					return true;
				}
			});

			new Thread(adp, "Waveform image builder").start();
		} catch (UnsupportedAudioFileException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	private AffineTransform getSaneTransform(final float heigth) {
		return new AffineTransform(1.0, 0.0, 0.0, -1.0, 0, heigth / 2);
	}

}
