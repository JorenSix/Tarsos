package be.hogent.tarsos.ui.link;

import java.awt.Color;
import java.awt.Graphics2D;
import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.TreeMap;

import javax.sound.sampled.UnsupportedAudioFileException;

import be.hogent.tarsos.dsp.AudioDispatcher;
import be.hogent.tarsos.dsp.AudioEvent;
import be.hogent.tarsos.dsp.AudioProcessor;
import be.hogent.tarsos.dsp.ConstantQ;
import be.hogent.tarsos.sampled.pitch.PitchUnit;

public class ConstantQLayer implements Layer {

	/**
	 * 
	 */
	private static final long serialVersionUID = -5067038057235544900L;
	
	private final TreeMap<Double,float[]> spectralInfo;
	private float maxSpectralEnergy = 0;
	private float minSpectralEnergy = 100000;
	private float[] binStartingPointsInCents;
	private float binWith;//in seconds
	private float binHeight;//in seconds
	
	
	
	public ConstantQLayer(){
		super();
		spectralInfo = new TreeMap<Double, float[]>();
		extractConstantQSpectrogram();
		
	}
	
	private void extractConstantQSpectrogram() {
		
		/**
		 * The default minimum pitch, in absolute cents (+-66 Hz)
		 */
		int minimumFrequencyInCents = 4000; 
		/**
		 * The default maximum pitch, in absolute cents (+-4200 Hz)
		 */
		int maximumFrequencyInCents = 10500;
		/**
		 * The default number of bins per octave.
		 */
		int binsPerOctave = 48;
		
		/**
		 * The default increment in samples.
		 */
		int increment = 1536;
		float minimumFrequencyInHertz = (float) PitchUnit.absoluteCentToHertz(minimumFrequencyInCents);
		float maximumFrequencyInHertz = (float) PitchUnit.absoluteCentToHertz(maximumFrequencyInCents);
		
		final ConstantQ constantQ = new ConstantQ(44100,minimumFrequencyInHertz,maximumFrequencyInHertz,binsPerOctave);
		
		binWith = increment/44100.0f;
		binHeight = 1200/(float) binsPerOctave;
		
		float[] startingPointsInHertz = constantQ.getFreqencies();
		binStartingPointsInCents = new float[startingPointsInHertz.length];
		for(int i=0;i<binStartingPointsInCents.length;i++){
			binStartingPointsInCents[i]=(float) PitchUnit.hertzToAbsoluteCent(startingPointsInHertz[i]);
		}
		
		int size = constantQ.getFFTlength();
		final double constantQLag = size / 44100.0 - binWith / 2.0;//in seconds
		int overlap = size - increment;
		try {
			AudioDispatcher adp = AudioDispatcher.fromFile(new File("/home/joren/Desktop/08._Ladrang_Kandamanyura_10s-20s_up.wav"), size, overlap);
			adp.setZeroPad(true);
			adp.addAudioProcessor(constantQ);
			adp.addAudioProcessor( new AudioProcessor() {
				
				
				public void processingFinished() {
					
				}
				
			
				public boolean process(AudioEvent audioEvent) {
					spectralInfo.put(audioEvent.getTimeStamp()-constantQLag,constantQ.getMagnitudes().clone());
					return true;
				}
			});		
			adp.run();
		} catch (UnsupportedAudioFileException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		float minValue = 5/1000000.0f;
		for(float[] magnitudes:spectralInfo.values()){
			for (int i = 0; i < magnitudes.length; i++) {
				magnitudes[i] = Math.max(minValue, magnitudes[i]);
				magnitudes[i] = (float) Math.log1p(magnitudes[i]);
				maxSpectralEnergy = Math.max(magnitudes[i], maxSpectralEnergy);
				minSpectralEnergy = Math.min(magnitudes[i], minSpectralEnergy);
			}
		}
		minSpectralEnergy = Math.abs(minSpectralEnergy);
	}
	

	public void draw(Graphics2D graphics){
		ViewPort viewport = ViewPort.getInstance();		
		Map<Double,float[]> spectralInfoSubMap = spectralInfo.subMap(viewport.getMinTime()/1000.0, viewport.getMaxTime()/1000.0);
		for(Map.Entry<Double, float[]> column : spectralInfoSubMap.entrySet()){	
			double timeStart = column.getKey();// in seconds
			float[] spectralEnergy = column.getValue();//in cents
						
			// draw the pixels
			for (int i = 0; i < spectralEnergy.length; i++) {
				Color color = Color.black;
				float centsStartingPoint = binStartingPointsInCents[i];
				//only draw the visible frequency range
				if(centsStartingPoint >= viewport.getMinFrequencyInCents() && centsStartingPoint <= viewport.getMaxFrequencyInCents() ){
					int greyValue = 255 - (int) (spectralEnergy[i]/maxSpectralEnergy * 255);
					greyValue = Math.max(0, greyValue);
					color = new Color(greyValue, greyValue, greyValue);			
					graphics.setColor(color);
					graphics.fillRect((int) Math.round(timeStart * 1000), Math.round(centsStartingPoint), (int) Math.round(binWith * 1000), (int) Math.ceil(binHeight));
				}
			}
		}
	}

}
