package be.tarsos.ui.link.layers.featurelayers;

import java.awt.Color;
import java.awt.Graphics2D;
import java.io.File;
import java.io.IOException;
import java.util.Map;

import javax.sound.sampled.UnsupportedAudioFileException;

import be.tarsos.dsp.AudioDispatcher;
import be.tarsos.dsp.AudioEvent;
import be.tarsos.dsp.AudioProcessor;
import be.tarsos.dsp.io.jvm.AudioDispatcherFactory;
import be.tarsos.dsp.util.fft.FFT;
import be.tarsos.sampled.pitch.PitchUnit;
import be.tarsos.ui.link.LinkedFrame;
import be.tarsos.ui.link.LinkedPanel;
import be.tarsos.ui.link.coordinatessystems.ICoordinateSystem;

public class FFTLayer extends FeatureLayer {

	/**
	 * 
	 */
	private static final long serialVersionUID = -5067038057235544900L;

	
	private float binWith;// in seconds
	
	private float maxSpectralEnergy = 0;
	private float minSpectralEnergy = 100000;
	private float[] binStartingPointsInCents;
	private float[] binHeightsInCents;

	/**
	 * The default increment in samples.
	 */
	private int increment;

	public FFTLayer(final LinkedPanel parent, int frameSize, int overlap) {
		super(parent, frameSize, overlap);
		increment = frameSize - overlap;
		this.name = "FFT layer";
	}

	public FFTLayer(final LinkedPanel parent, int frameSize, int overlap, int binsPerOctave) {
		super(parent, frameSize, overlap);
		increment = frameSize - overlap;
	}

	public void draw(Graphics2D graphics) {
		ICoordinateSystem cs = parent.getCoordinateSystem();
		Map<Double, float[]> spectralInfoSubMap = features.subMap(
				cs.getMin(ICoordinateSystem.X_AXIS) / 1000.0, cs.getMax(ICoordinateSystem.X_AXIS) / 1000.0);
		for (Map.Entry<Double, float[]> column : spectralInfoSubMap.entrySet()) {
			double timeStart = column.getKey();// in seconds
			float[] spectralEnergy = column.getValue();// in cents

			// draw the pixels
			for (int i = 0; i < spectralEnergy.length; i++) {
				Color color = Color.black; 
				float centsStartingPoint = binStartingPointsInCents[i];
				// only draw the visible frequency range
				if (centsStartingPoint >= cs.getMin(ICoordinateSystem.Y_AXIS)
						&& centsStartingPoint <= cs.getMax(ICoordinateSystem.Y_AXIS)) {
					
					int greyValue = 255 - (int) (spectralEnergy[i]
							/ maxSpectralEnergy * 255);
					greyValue = Math.max(0, greyValue);
					color = new Color(greyValue, greyValue, greyValue);
					graphics.setColor(color);
					graphics.fillRect((int) Math.round(timeStart * 1000),
							Math.round(centsStartingPoint),
							(int) Math.round(binWith * 1000),
							(int) Math.ceil(binHeightsInCents[i]));
				}
			}
		}
	}

	@Override
	public void initialise() {
		float sampleRate = LinkedFrame.getInstance().getAudioFile().fileFormat().getFormat().getSampleRate();
		
		

		binWith = increment / sampleRate;

		final FFT fft = new FFT(getFrameSize());
		
		binStartingPointsInCents = new float[getFrameSize()];
		binHeightsInCents = new float[getFrameSize()];
		for (int i = 1; i < getFrameSize(); i++) {
			binStartingPointsInCents[i] = (float) PitchUnit.hertzToAbsoluteCent(fft.binToHz(i,sampleRate));
			binHeightsInCents[i] = binStartingPointsInCents[i] - binStartingPointsInCents[i-1];
		}
		
		final double lag =  this.getFrameSize() / sampleRate - binWith / 2.0;// in seconds
		try {
			adp = AudioDispatcherFactory.fromFile(new File(LinkedFrame.getInstance()
					.getAudioFile().originalPath()), this.getFrameSize(),
					this.getOverlap());
		} catch (UnsupportedAudioFileException e) {
			e.printStackTrace();
		} catch (IOException e2){
			e2.printStackTrace();
		}		
		adp.addAudioProcessor(new AudioProcessor() {

			public void processingFinished() {
				float minValue = 5 / 1000000.0f;
				for (float[] magnitudes : features.values()) {
					for (int i = 0; i < magnitudes.length; i++) {
						magnitudes[i] = Math.max(minValue, magnitudes[i]);
						magnitudes[i] = (float) Math.log1p(magnitudes[i]);
						maxSpectralEnergy = Math.max(magnitudes[i],
								maxSpectralEnergy);
						minSpectralEnergy = Math.min(magnitudes[i],
								minSpectralEnergy);
					}
				}
				minSpectralEnergy = Math.abs(minSpectralEnergy);
			}

			public boolean process(AudioEvent audioEvent) {
				float[] buffer = audioEvent.getFloatBuffer().clone();
				float[] amplitudes = new float[buffer.length/2];
				fft.forwardTransform(buffer);
				fft.modulus(buffer, amplitudes);
				features.put(audioEvent.getTimeStamp() - lag,amplitudes);
				return true;
			}
		});
	}
}
