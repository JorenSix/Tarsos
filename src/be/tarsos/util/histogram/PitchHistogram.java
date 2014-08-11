/*
*              _______                      
*             |__   __|                     
*                | | __ _ _ __ ___  ___  ___
*                | |/ _` | '__/ __|/ _ \/ __| 
*                | | (_| | |  \__ \ (_) \__ \    
*                |_|\__,_|_|  |___/\___/|___/    
*                                                         
* -----------------------------------------------------------
*
*  Tarsos is developed by Joren Six at 
*  The School of Arts,
*  University College Ghent,
*  Hoogpoort 64, 9000 Ghent - Belgium
*  
* -----------------------------------------------------------
*
*  Info: http://tarsos.0110.be
*  Github: https://github.com/JorenSix/Tarsos
*  Releases: http://tarsos.0110.be/releases/Tarsos/
*  
*  Tarsos includes some source code by various authors,
*  for credits and info, see README.
* 
*/

package be.tarsos.util.histogram;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.imageio.ImageIO;

import ptolemy.plot.Plot;
import be.tarsos.sampled.pitch.Annotation;
import be.tarsos.sampled.pitch.PitchUnit;
import be.tarsos.util.ConfKey;
import be.tarsos.util.Configuration;
import be.tarsos.util.FileUtils;
import be.tarsos.util.SimplePlot;

/**
 * The PitchHistogram accepts values from 0 to 9600 cents or +- from 16Hz to
 * 40000Hz: the human hearing range is completely covered.<br>
 * The start and stop values can be configured.<br>
 * Values outside the defined range are ignored!
 * 
 * @author Joren Six
 */
public final class PitchHistogram extends Histogram {

	/**
	 * Log messages.
	 */
	private static final Logger LOG = Logger.getLogger(PitchHistogram.class.getName());

	private final List<PitchClassHistogram> toneScaleHistogramPerOctave = new ArrayList<PitchClassHistogram>();

	public PitchHistogram() {
		super(Configuration.getInt(ConfKey.pitch_histogram_start), Configuration.getInt(ConfKey.pitch_histogram_stop),
				(Configuration.getInt(ConfKey.pitch_histogram_stop) - Configuration.getInt(ConfKey.pitch_histogram_start))
						/ Configuration.getInt(ConfKey.histogram_bin_width), false, // does
				// not
				// wrap
				true// ignore values outside human hearing range
		);

		// initialize the list of tone scales
		for (int value = Configuration.getInt(ConfKey.pitch_histogram_start); value < Configuration
				.getInt(ConfKey.pitch_histogram_stop); value += 1200) {
			toneScaleHistogramPerOctave.add(new PitchClassHistogram());
		}

	}

	@Override
	public void valueAddedHook(final double value) {
		// keep a histogram for each octave
		final int octaveIndex = (int) (value / 1200);
		if (toneScaleHistogramPerOctave.size() > octaveIndex && octaveIndex >= 0) {
			toneScaleHistogramPerOctave.get(octaveIndex).add(value);
		}
	}

	/**
	 * @param numberOfOctaves
	 *            The number of energy rich octaves
	 * @return a PitchClassHistogram containing only samples from the
	 *         numberOfOctaves most energy rich octaves.
	 */
	public PitchClassHistogram mostEnergyRichOctaves(final int numberOfOctaves) {
		final PitchClassHistogram h = new PitchClassHistogram();
		final List<Integer> octavesOrderedByEnergy = octavesOrderedByEnergy();
		for (int i = 0; i < numberOfOctaves; i++) {
			final int octaveIndex = octavesOrderedByEnergy.get(i);
			h.add(toneScaleHistogramPerOctave.get(octaveIndex));
		}
		return h;
	}

	/**
	 * @return a list of octave indexes ordered by energy: the most energy rich
	 *         first. The energy is defined by the number of collected samples
	 *         in the interval E.g. [4,3,2,5,7,6,0,1]
	 */
	private List<Integer> octavesOrderedByEnergy() {
		final List<Integer> octaves = new ArrayList<Integer>();

		for (int i = 0; i < toneScaleHistogramPerOctave.size(); i++) {
			octaves.add(i);
		}

		Collections.sort(octaves, new Comparator<Integer>() {
			public int compare(final Integer o1, final Integer o2) {
				final Long energyFirst;
				energyFirst = PitchHistogram.this.toneScaleHistogramPerOctave.get(o1).getSumFreq();
				final Long energySecond;
				energySecond = PitchHistogram.this.toneScaleHistogramPerOctave.get(o2).getSumFreq();
				return energySecond.compareTo(energyFirst);
			}
		});
		return octaves;
	}

	/**
	 * @return the ambitus folded to one octave (1200 cents)
	 */
	public PitchClassHistogram pitchClassHistogram() {
		final PitchClassHistogram summedToneScaleHistogram = new PitchClassHistogram();
		for (final PitchClassHistogram histogram : toneScaleHistogramPerOctave) {
			summedToneScaleHistogram.add(histogram);
		}
		return summedToneScaleHistogram;
	}

	/**
	 * Plot the complete ambitus.
	 * 
	 * @param fileName
	 */
	public void plotAmbitusHistogram(final String fileName) {
		this.plotAmbitusHistogram(fileName, (int) getStart(), (int) getStop());
	}

	/**
	 * Plot the ambitus but use only values between start and stop (cents).
	 * 
	 * @param fileName
	 *            the plot is saved to this location.
	 * @param start
	 *            the starting value
	 * @param stop
	 *            stopping value
	 */
	public void plotAmbitusHistogram(final String fileName, final int start, final int stop) {
		final SimplePlot plot = new SimplePlot();
		for (double current = start; current <= stop; current += this.getClassWidth()) {
			plot.addData(0, current, this.getCount(current));
		}
		plot.save(fileName);
	}
	
	public PitchClassHistogram mostEnergyRitchOctave(){
		PitchClassHistogram pch = new PitchClassHistogram();
		int currentCount = 0;
		int maxCount = 0;
		Double startKey = 0.0;
		for(Double key : this.keySet()){
			if(key >= 1200)
				currentCount -= this.getCount(key-1200);
			currentCount += this.getCount(key);
			if(currentCount>maxCount){
				startKey = key;
				maxCount = currentCount;
			}		
		}
		double stopKey = startKey + 1200;
		for(;startKey < stopKey ; startKey += Configuration.getDouble(ConfKey.histogram_bin_width) )
			pch.setCount(startKey % 1200, this.getCount(startKey));
		
		return pch;
	}

	/**
	 * Saves a tone scale histogram plot with each octave separately or just the
	 * total.
	 * 
	 * @param fileName
	 *            the plot is saved to this location.
	 * @param splitOctaves
	 *            if true each octave is separated, otherwise only the total is
	 *            shown.
	 */
	public void plotToneScaleHistogram(final String fileName, final boolean splitOctaves) {
		final PitchClassHistogram summedToneScaleHistogram = pitchClassHistogram();

		if (splitOctaves) {
			final Plot h = new Plot();
			h.setXRange(0, 1200);
			h.setYLabel("Number of Annotations (#)");
			h.setXLabel("Pitch Class (cent)");
			for (int dataset = 0; dataset < toneScaleHistogramPerOctave.size(); dataset++) {
				final PitchClassHistogram currentToneScaleHistogram = toneScaleHistogramPerOctave.get(dataset);
				for (final double key : currentToneScaleHistogram.keySet()) {
					long count = currentToneScaleHistogram.getCount(key);
					for (int i = 0; i < dataset; i++) {
						count += toneScaleHistogramPerOctave.get(i).getCount(key);
					}
					h.addPoint(dataset, key, count, true);
				}
			}
			for (int dataset = 0; dataset < toneScaleHistogramPerOctave.size(); dataset++) {
				h.addLegend(dataset, "C" + dataset + "-B" + dataset + " [" + dataset * 1200 + "-"
						+ (dataset * 1200 + 1199) + "]");
			}
			h.setSize(1680, 1050);
			h.setTitle(FileUtils.basename(fileName));
			try {
				Thread.sleep(60);
				final BufferedImage image = h.exportImage();
				ImageIO.write(image, "png", new File(fileName));
			} catch (final IOException e) {
				LOG.log(Level.SEVERE, "Could not write plot.", e);
			} catch (final InterruptedException e) {
				LOG.log(Level.SEVERE, "Thread interrupted.", e);
			}
		} else {
			final SimplePlot plot = new SimplePlot();
			plot.addData(0, summedToneScaleHistogram);
			plot.setXLabel("Pitch (cent)");
			plot.setYLabel("Number of Annotations (#)");
			plot.save(fileName);
		}
	}

	@Override
	public void plot(final String fileName, final String title) {
		final SimplePlot plot = new SimplePlot(title);
		double startingValue = getStart();
		double stoppingValue = getStop();
		boolean valuesStarted = false;
		for (final double key : keySet()) {
			final long count = getCount(key);
			if (count != 0L) {
				stoppingValue = key;
			}
			if (!valuesStarted && count != 0L) {
				valuesStarted = true;
			}
			if (!valuesStarted) {
				startingValue = key;
			}
		}
		plot.setXRange(startingValue, stoppingValue);
		for (final double current : keySet()) {
			if (current >= startingValue && current <= stoppingValue) {
				plot.addData(0, current, getCount(current));
			}
		}
		plot.save(fileName);
	}

	/**
	 * Create a tone scale histogram using a kernel instead of an ordinary
	 * count. This construction uses a paradigm described here:
	 * http://en.wikipedia.org/wiki/Kernel_density_estimation
	 * 
	 * It uses Gaussian kernels of a defined width. The width should be around
	 * the just noticeable threshold of about 7 cents.
	 * 
	 * @param annotations
	 *            A list of annotations.s
	 * @param width
	 *            The width of each kernel.
	 * @return A histogram build with Gaussian kernels.
	 */
	public static PitchHistogram createPitchHistogram(final List<Annotation> annotations,
			final double width) {
		int pitchHistogramMaximum = Configuration.getInt(ConfKey.pitch_histogram_stop);
		int pitchHistogramMinimum = Configuration.getInt(ConfKey.pitch_histogram_start);
		int octaves = (int) Math.ceil((pitchHistogramMaximum - pitchHistogramMinimum)/1200.0);
		
		// 1200 pitch classes should be enough for everybody!
		double[] accumulator = new double[1200 * octaves];

		double calculationAria = 5 * width;// hehe aria, not area
		double halfwit = width / 2.0; //hehe halfwit, not halfwidth, hehe
		
		//Compute a kernel: a lookup table with e.g. a gaussian curve
		double kernel[] = new double[(int)calculationAria*2+1];
		double difference =  - calculationAria;
		for(int i = 0 ; i < kernel.length; i++){
			double power = Math.pow(difference / halfwit, 2.0);
			kernel[i] = Math.pow(Math.E, -0.5 * power);
			difference++;
		}

		/*
		 * Add the kernel to an accumulator for each annotation.
		 */
	
		for (Annotation annotation : annotations) {
			double pitch = annotation.getPitch(PitchUnit.ABSOLUTE_CENTS);
			int start = (int) (pitch + - calculationAria);
			int stop = (int) (pitch  + calculationAria);
			int kernelIndex = 0;
			for (int i = start; i < stop && i < pitchHistogramMaximum; i++) {
				if(i > pitchHistogramMinimum){					
					accumulator[i] += kernel[kernelIndex];
				}
				kernelIndex++;
			}
		}

		PitchHistogram histo = new PitchHistogram();
		for (int i = 0; i < accumulator.length; i++) {
			histo.setCount(i + pitchHistogramMinimum, (long) accumulator[i]);
		}
		return histo;
	}
	
	public static double[] createAccumulator(final List<Annotation> annotations,
			final double width) {
		int pitchHistogramMaximum = Configuration.getInt(ConfKey.pitch_histogram_stop);
		int pitchHistogramMinimum = Configuration.getInt(ConfKey.pitch_histogram_start);
		int octaves = (int) Math.ceil((pitchHistogramMaximum - pitchHistogramMinimum)/1200.0);
		
		// 1200 pitch classes should be enough for everybody!
		double[] accumulator = new double[1200 * octaves];

		double calculationAria = 5 * width;// hehe aria, not area
		double halfWidth = width / 2.0;
		
		//Compute a kernel: a lookup table with e.g. a gaussian curve
		double kernel[] = new double[(int)calculationAria*2+1];
		double difference =  - calculationAria;
		for(int i = 0 ; i < kernel.length; i++){
			double power = Math.pow(difference / halfWidth, 2.0);
			kernel[i] = Math.pow(Math.E, -0.5 * power);
			difference++;
		}

		/*
		 * Add the kernel to an accumulator for each annotation.
		 */
	
		for (Annotation annotation : annotations) {
			double pitch = annotation.getPitch(PitchUnit.ABSOLUTE_CENTS);
			int start = (int) (pitch + - calculationAria);
			int stop = (int) (pitch  + calculationAria);
			int kernelIndex = 0;
			for (int i = start; i < stop && i < pitchHistogramMaximum; i++) {
				if(i > pitchHistogramMinimum){					
					accumulator[i] += kernel[kernelIndex];
				}
				kernelIndex++;
			}
		}

		return accumulator;
	}
}
