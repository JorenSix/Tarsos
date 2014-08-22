package be.tarsos.ui.link.layers.featurelayers;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.io.File;
import java.io.IOException;

import javax.sound.sampled.UnsupportedAudioFileException;

import be.tarsos.dsp.AudioEvent;
import be.tarsos.dsp.AudioProcessor;
import be.tarsos.dsp.io.jvm.AudioDispatcherFactory;
import be.tarsos.ui.link.LinkedFrame;
import be.tarsos.ui.link.LinkedPanel;
import be.tarsos.ui.link.coordinatessystems.ICoordinateSystem;
import be.tarsos.util.AudioFile;

public class WaveFormLayer extends FeatureLayer {

	private Color waveFormColor;
	private float[] samples;

	// private BufferedImage waveFormImage;

	public WaveFormLayer(LinkedPanel parent) {
		super(parent);
		waveFormColor = Color.black;
		this.name = "Waveform layer";
	}

	public WaveFormLayer(LinkedPanel parent, Color color) {
		super(parent);
		waveFormColor = color;
	}

	public void draw(Graphics2D graphics) {
		graphics.setColor(waveFormColor);
		this.drawWaveForm(graphics);
	}

	private void drawWaveForm(Graphics2D graphics) {
		
		ICoordinateSystem cs = parent.getCoordinateSystem();
		final int waveFormXMin = (int) cs.getMin(ICoordinateSystem.X_AXIS);
		final int waveFormXMax = (int) cs.getMax(ICoordinateSystem.X_AXIS);
		graphics.setColor(Color.GRAY);
		graphics.drawLine(waveFormXMin, 0, waveFormXMax,0);
		graphics.setColor(Color.BLACK);
		if (samples != null && samples.length > 0) {
			graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);
			// - X as overlopen
			// - Waveform tekenen waar i>=0 && i<=lengteLiedjeInMilisec &&
			// i>=XMin && i<=XMax
			// -> tekenen in milliseconden en maal factor
			// waveFormHeightInUnits/2
			// - Samplewaarden * waveFormHeightInUnits tekenen
			// -> sampleindices uitrekenen aan de hand van de seconden
			// -> Stel: milliseconden i -> i/lengteLiedjeInMilisec =
			// x/aantalSamples
			// -> x = i*aantalSamples/lengteLiedjeInMilisec

			AudioFile f = LinkedFrame.getInstance().getAudioFile();
			
		
			final int waveFormHeightInUnits = (int) cs
					.getDelta(ICoordinateSystem.Y_AXIS);
			final float lengthInMs = f.getLengthInMilliSeconds();
			final int amountOfSamples = samples.length;
			float sampleCalculateFactor = amountOfSamples / lengthInMs;
			int amplitudeFactor = waveFormHeightInUnits / 2;
			
			for (int i = Math.max(0, waveFormXMin); i < Math.min(waveFormXMax, lengthInMs); i++) {
				int index = (int) (i * sampleCalculateFactor);
				if (index < samples.length) {
					graphics.drawLine(i, 0, i,
							(int) (samples[index] * amplitudeFactor));
				}
			}
			graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_OFF);
		}
	}

	@Override
	public void initialise() {

		AudioFile f = LinkedFrame.getInstance().getAudioFile();
		int amountOfSamples = f.fileFormat().getFrameLength();
		try {
			adp = AudioDispatcherFactory.fromFile(new File(f.originalPath()),
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

//	@Override
//	protected void setProperties() {
//		// TODO Auto-generated method stub
//		
//	}

}
