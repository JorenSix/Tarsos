package be.hogent.tarsos.ui.link.layers.featurelayers;

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
import be.hogent.tarsos.ui.link.layers.BackgroundLayer;
import be.hogent.tarsos.util.AudioFile;
import be.hogent.tarsos.util.StopWatch;

public class WaveFormLayer extends FeatureLayer {

	private Color waveFormColor;
	private float[] samples;

	// private BufferedImage waveFormImage;

	public WaveFormLayer(LinkedPanel parent) {
		super(parent);
		waveFormColor = Color.black;
	}

	public WaveFormLayer(LinkedPanel parent, Color color) {
		super(parent);
		waveFormColor = color;
	}

	public void draw(Graphics2D graphics) {
		graphics.setColor(waveFormColor);
//		graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
//				RenderingHints.VALUE_ANTIALIAS_ON);
//		graphics.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS,
//				RenderingHints.VALUE_FRACTIONALMETRICS_ON);
		this.drawWaveForm(graphics);
	}

	private void drawWaveForm(Graphics2D graphics) {

		AudioFile f = LinkedFrame.getInstance().getAudioFile();
		if (samples != null && samples.length > 0) {
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
//			graphics.transform(getSaneTransform(waveFormHeight));

			final float sampleRate = f.fileFormat().getFormat().getSampleRate();

			float percentageOfSongDisplayed = (float) waveFormWidthInMs
					/ (float) f.getLengthInMilliSeconds();
			int amountOfSamples = f.fileFormat().getFrameLength();
			int amountOfSamplesDisplayed = Math.round(amountOfSamples
					* percentageOfSongDisplayed);
			int samplesPerPixel = Math.round((float) amountOfSamplesDisplayed
					/ (float) waveFormWidth);// waarom / 8 ??;

//			final float frameSizeInSec = samplesPerPixel / sampleRate; // samplesPerPixel(samples/pixel)/sampleRate(Samples/sec)

//			final int startY = (int) (waveFormHeight / 2);
			final int one = (int) (waveFormHeight / 2 * 0.85);
			// om te tekenen: wat nodig?
			// - startPixel = 0 | ok
			// - laatstePixel | ok
			// - startSample | sample waarbij T >= XMin
			// - laatsteSample | sample waarbij T <= XMax
			// - samplesPerPixel | ok
			// sampleRate = samples/sec
			// i/sampleRate = Xmin => Xmin*sampleRate = i
			// i/sampleRate = XMax => Xmax*sampleRate = i

			int startSample = Math.round((waveFormXMin/1000) * sampleRate);
			int endSample = Math.round((waveFormXMax/1000) * sampleRate);
			
			int pixelCount = 0;
			for (int i = startSample; i < endSample; i += samplesPerPixel) {
				if (i >= 0 && i < samples.length){
//					int y = (int) (samples[i] + one);
					graphics.drawLine(pixelCount, 0, pixelCount, (int)samples[i] * one);
				}
				pixelCount++;
			}
		}
	}

	private AffineTransform getSaneTransform(final float heigth) {
		return new AffineTransform(1.0, 0.0, 0.0, -1.0, 0, heigth / 2);
	}

	@Override
	public void initialise() {

		AudioFile f = LinkedFrame.getInstance().getAudioFile();
		int amountOfSamples = f.fileFormat().getFrameLength();
		try {
			adp = AudioDispatcher.fromFile(new File(f.originalPath()),
					amountOfSamples, this.getOverlap());
		} catch (UnsupportedAudioFileException e) {
			e.printStackTrace();
		} catch (IOException e2) {
			e2.printStackTrace();
		}
		adp.setZeroPad(true);

		adp.addAudioProcessor(new AudioProcessor() {

			public void processingFinished() {
			}

			public boolean process(AudioEvent audioEvent) {
				float[] audioFloatBuffer = audioEvent.getFloatBuffer();
				WaveFormLayer.this.samples = audioFloatBuffer.clone();
				return true;
			}
		});

	}

}
