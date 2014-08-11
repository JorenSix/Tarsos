package be.tarsos.ui.link.layers.featurelayers;
//package be.tarsos.ui.link;
//
//import java.awt.Color;
//import java.awt.Graphics2D;
//import java.io.File;
//import java.io.IOException;
//import java.util.Map;
//
//import javax.sound.sampled.UnsupportedAudioFileException;
//
//import be.tarsos.dsp.AudioDispatcher;
//import be.tarsos.dsp.AudioEvent;
//import be.tarsos.dsp.AudioProcessor;
//import be.tarsos.dsp.mfcc.MFCC;
//import be.tarsos.sampled.pitch.PitchUnit;
//import be.tarsos.ui.link.layers.FeatureLayer;
//
//public class MfccLayer extends FeatureLayer {
//
//	private float maxSpectralEnergy = 0;
//	private float minSpectralEnergy = 100000;
//	private int[] binStartingPointsInCents;
//	private float binWith;// in seconds
//	private float binHeight;// in seconds
//	
//	/**
//	 * The default minimum pitch, in absolute cents (+-66 Hz)
//	 */
//	private int minimumFrequencyInCents = 4000;
//	/**
//	 * The default maximum pitch, in absolute cents (+-4200 Hz)
//	 */
//	private int maximumFrequencyInCents = 10500;
//	
//	private int amountOfMelFilters = 40;
//
//	private int amountOfMfccs = 40;
//	
//	private int increment;
//	
//	public MfccLayer(LinkedFeaturePanel int frameSize, int overlap){
//		super(frameSize, overlap);
//		increment = frameSize - overlap;
//	}
//	
//	public MfccLayer(int frameSize, int overlap, int amountOfMelFilters, int amountOfMfccs){
//		this(frameSize,overlap);
//		this.amountOfMelFilters = amountOfMelFilters;
//		this.amountOfMfccs = amountOfMfccs;
//	}
//	
//	@Override
//	public void initialise() {
//		float minimumFrequencyInHertz = (float) PitchUnit
//				.absoluteCentToHertz(minimumFrequencyInCents);
//		float maximumFrequencyInHertz = (float) PitchUnit
//				.absoluteCentToHertz(maximumFrequencyInCents);
//
//		float sampleRate = LinkedFrame.getInstance().getAudioFile().fileFormat().getFormat().getSampleRate();
//		
//		final MFCC mfcc = new MFCC(this.getFrameSize(), sampleRate, this.amountOfMfccs, amountOfMelFilters, minimumFrequencyInHertz, maximumFrequencyInHertz);
//
//		binWith = increment / sampleRate;
//		binHeight = 1200 / (float) amountOfMelFilters;
//
//		int[] startingPointsInHertz = mfcc.getCenterFrequencies();
//		binStartingPointsInCents = new int[startingPointsInHertz.length];
//		for (int i = 0; i < binStartingPointsInCents.length; i++) {
//			binStartingPointsInCents[i] = (int) PitchUnit
//					.hertzToAbsoluteCent(startingPointsInHertz[i]);
//		}
//
////		final double constantQLag = this.getFrameSize() / sampleRate - binWith / 2.0;// in seconds
//		try {
//			adp = AudioDispatcher.fromFile(new File(LinkedFrame.getInstance()
//					.getAudioFile().originalPath()), this.getFrameSize(),
//					this.getOverlap());
//		} catch (UnsupportedAudioFileException | IOException e) {
//			e.printStackTrace();
//		}
//		adp.setZeroPad(true);
//		adp.addAudioProcessor(mfcc);
//		adp.addAudioProcessor(new AudioProcessor() {
//
//			public void processingFinished() {
//				float minValue = 5 / 1000000.0f;
//				for (float[] magnitudes : features.values()) {
//					for (int i = 0; i < magnitudes.length; i++) {
//						magnitudes[i] = Math.max(minValue, magnitudes[i]);
//						magnitudes[i] = (float) Math.log1p(magnitudes[i]);
//						maxSpectralEnergy = Math.max(magnitudes[i],
//								maxSpectralEnergy);
//						minSpectralEnergy = Math.min(magnitudes[i],
//								minSpectralEnergy);
//					}
//				}
//				minSpectralEnergy = Math.abs(minSpectralEnergy);
//			}
//
//			public boolean process(AudioEvent audioEvent) {
//				features.put(audioEvent.getTimeStamp(),
//						mfcc.getMFCC().clone());
//				return true;
//			}
//		});
//	}
//	
//	@Override
//	public void draw(Graphics2D graphics) {
////		ViewPort viewport = ViewPort.getInstance();
////		Map<Double, float[]> spectralInfoSubMap = features.subMap(
////				viewport.getMinTime() / 1000.0, viewport.getMaxTime() / 1000.0);
////		for (Map.Entry<Double, float[]> column : spectralInfoSubMap.entrySet()) {
////			double timeStart = column.getKey();// in seconds
////			float[] spectralEnergy = column.getValue();// in cents
////
////			// draw the pixels
////			for (int i = 0; i < spectralEnergy.length; i++) {
////				Color color = Color.black;
////				float centsStartingPoint = binStartingPointsInCents[i];
////				// only draw the visible frequency range
////				if (centsStartingPoint >= viewport.getMinFrequencyInCents()
////						&& centsStartingPoint <= viewport
////								.getMaxFrequencyInCents()) {
////					int greyValue = 255 - (int) (spectralEnergy[i]
////							/ maxSpectralEnergy * 255);
////					greyValue = Math.max(0, greyValue);
////					color = new Color(greyValue, greyValue, greyValue);
////					graphics.setColor(color);
////					graphics.fillRect((int) Math.round(timeStart * 1000),
////							Math.round(centsStartingPoint),
////							(int) Math.round(binWith * 1000),
////							(int) Math.ceil(binHeight));
////				}
////			}
////		}
//		
//	}
//
//	
//}
