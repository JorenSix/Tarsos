package be.tarsos.ui.link.layers.featurelayers;

import java.awt.Color;
import java.awt.Graphics2D;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import javax.sound.sampled.UnsupportedAudioFileException;

import be.tarsos.dsp.AudioDispatcher;
import be.tarsos.dsp.AudioEvent;
import be.tarsos.dsp.AudioProcessor;
import be.tarsos.dsp.ConstantQ;
import be.tarsos.dsp.io.jvm.AudioDispatcherFactory;
import be.tarsos.sampled.pitch.PitchUnit;
import be.tarsos.ui.link.LinkedFrame;
import be.tarsos.ui.link.LinkedPanel;
import be.tarsos.ui.link.ViewPort;
import be.tarsos.ui.link.coordinatessystems.ICoordinateSystem;
import be.tarsos.ui.link.layers.LayerProperty;
import be.tarsos.util.AudioFile;

public class ConstantQLayer extends FeatureLayer {

	/**
	 * 
	 */
	
	private static final long serialVersionUID = -5067038057235544900L;

	private float maxSpectralEnergy = 0;
	private float minSpectralEnergy = 100000;
	private float[] binStartingPointsInCents;
//	private float binWith;// in seconds
	private float binHeight;// in seconds

	/**
	 * The default minimum pitch, in absolute cents (+-66 Hz)
	 */
	private int minimumFrequencyInCents = 4000;
	/**
	 * The default maximum pitch, in absolute cents (+-4200 Hz)
	 */
	private int maximumFrequencyInCents = 10500;
	/**
	 * The default number of bins per octave.
	 */
	private int binsPerOctave = 48;

	/**
	 * The default increment in samples.
	 */
//	private int increment;

	public ConstantQLayer(final LinkedPanel parent, int overlap) {
		super(parent, -1, overlap);
//		increment = 0;//frameSize - overlap;
		this.name = "CQT layer";
	}

	public ConstantQLayer(final LinkedPanel parent, int overlap, int binsPerOctave) {
		this(parent, overlap);
		this.binsPerOctave = binsPerOctave;
	}

	public ConstantQLayer(final LinkedPanel parent, int overlap, int minFreqInCents,
			int maxFreqInCents, int binsPerOctave) {
		this(parent, overlap, binsPerOctave);
		this.minimumFrequencyInCents = minFreqInCents;
		this.maximumFrequencyInCents = maxFreqInCents;
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
//					graphics.fillRect((int) Math.round(timeStart * 1000),
//							Math.round(centsStartingPoint),
////							(int) Math.round(binWith * 1000),
//							(int) Math.ceil(binHeight));
				}
			}
		}
	}

	@Override
	public void initialise() {
		float minimumFrequencyInHertz = (float) PitchUnit
				.absoluteCentToHertz(minimumFrequencyInCents);
		float maximumFrequencyInHertz = (float) PitchUnit
				.absoluteCentToHertz(maximumFrequencyInCents);

		final ConstantQ constantQ = new ConstantQ(
				LinkedFrame.getInstance().getAudioFile().fileFormat()
						.getFormat().getSampleRate(), minimumFrequencyInHertz,
				maximumFrequencyInHertz, binsPerOctave);

//		binWith = increment
//				/ LinkedFrame.getInstance().getAudioFile().fileFormat()
//						.getFormat().getSampleRate();
		binHeight = 1200 / (float) binsPerOctave;

		float[] startingPointsInHertz = constantQ.getFreqencies();
		binStartingPointsInCents = new float[startingPointsInHertz.length];
		for (int i = 0; i < binStartingPointsInCents.length; i++) {
			binStartingPointsInCents[i] = (float) PitchUnit
					.hertzToAbsoluteCent(startingPointsInHertz[i]);
		}

		int size = constantQ.getFFTlength();
//		final double constantQLag = size / 44100.0 - binWith / 2.0;// in seconds
		try {
			adp = AudioDispatcherFactory.fromFile(new File(LinkedFrame.getInstance()
					.getAudioFile().originalPath()), this.getFrameSize(),
					this.getOverlap());
		} catch (UnsupportedAudioFileException e) {
			e.printStackTrace();
		} catch (IOException e2){
			e2.printStackTrace();
		}
		adp.setZeroPad(true);
		adp.addAudioProcessor(constantQ);
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
//				features.put(audioEvent.getTimeStamp() - constantQLag,
//						constantQ.getMagnitudes().clone());
				return true;
			}
		});
	}
}
