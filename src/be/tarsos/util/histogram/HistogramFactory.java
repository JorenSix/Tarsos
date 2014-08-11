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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.logging.Logger;

import be.tarsos.sampled.pitch.Annotation;
import be.tarsos.sampled.pitch.PitchUnit;
import be.tarsos.util.ConfKey;
import be.tarsos.util.Configuration;
import be.tarsos.util.KernelDensityEstimate;
import be.tarsos.util.ScalaFile;
import be.tarsos.util.TimeUnit;
import be.tarsos.util.KernelDensityEstimate.GaussianKernel;

/**
 * This class creates different kinds of histograms from either a list of annotations 
 * or a list of peak positions.
 * 
 * @author Joren Six
 */
public class HistogramFactory {
	
	private static final Logger LOG = Logger.getLogger(HistogramFactory.class.getName());
	
	private HistogramFactory(){	
	}
	
	/*------------------Pitch Class Histogram-------------------------------------------------*/
	
	/**
	 * Creates a pitch class histogram directly from a list of annotations (without kernels).
	 * @param annotations The list of annotations.
	 * @return A pitch class histogram with the annotations added.
	 */
	public static PitchClassHistogram createPitchClassHistogram(final List<Annotation> annotations) {
		final PitchClassHistogram histogram = new PitchClassHistogram();
		for (Annotation annotation : annotations) {
			histogram.add(annotation.getPitch(PitchUnit.RELATIVE_CENTS));
		}
		assert histogram.getAbsoluteSumFreq() == annotations.size();
		return histogram;
	}
	
	/**
	 * Creates a pitch class histogram directly from a list of annotations (without kernels).
	 * @param kde
	 * @return A pitch class histogram with the annotations added.
	 */
	public static PitchClassHistogram createPitchClassHistogram(KernelDensityEstimate kde) {
		final PitchClassHistogram histogram = new PitchClassHistogram();
		for (int i = 0 ; i < kde.size() ; i++) {
			histogram.setCount(i,(long) kde.getValue(i));
		}
		return histogram;
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
	 * @param width The with of the kernel in cents.
	 * @return A histogram build with Gaussian kernels.
	 */
	public static PitchClassHistogram createPitchClassHistogram(final List<Annotation> annotations,
			final double width) {
		KernelDensityEstimate kde = createPichClassKDE(annotations,width);
		PitchClassHistogram pitchClassHistogram = new PitchClassHistogram();
		for (int i = 0; i < 1200; i++) {
			pitchClassHistogram.setCount(i, (long) kde.getValue(i));
		}
		return pitchClassHistogram;
	}
	

	/**
	 * Builds a pitch class histogram using the pitches defined in a scala
	 * file. It uses Gaussian kernels.
	 * @param scalaFile 
	 * @return A PitchClassHistogram built with Gaussian kernels on the pitches defined in the file.
	 */
	public static PitchClassHistogram createPitchClassHistogram(final ScalaFile scalaFile) {
		return PitchClassHistogram.createToneScale(scalaFile.getPitches().clone());
	}
	
	
	
	/**
	 * Create a pitch class histogram by taking the maximum number of
	 * annotations for a defined time window. This should make lesser used pitch
	 * classes more prominently visible.
	 * 
	 * @param annotations
	 *            The original list of annotations.
	 * @param duration
	 *            The duration for one window (define in the unit)
	 * @param unit
	 *            The uint for the duration.
	 * @return A pitch class histogram containing the maximum in a window.
	 */
	public static PitchClassHistogram createMaxPitchClassHistogram(final List<Annotation> annotations,final double duration,final TimeUnit unit){
		double durationInSeconds = TimeUnit.SECONDS.convert(duration, unit);
		double currentPositionInSeconds = durationInSeconds;
		PitchClassHistogram maxPitchClassHistogram = new PitchClassHistogram();
		//make sure the annotations are ordered by time
		Collections.sort(annotations);
		List<Annotation> tempAnnotations = new ArrayList<Annotation>();
		for(Annotation annotation : annotations){
			if(annotation.getStart() > currentPositionInSeconds){
				PitchClassHistogram tempHisto = createPitchClassHistogram(tempAnnotations);
				maxPitchClassHistogram.max(tempHisto);
				tempAnnotations.clear();
				currentPositionInSeconds += durationInSeconds;
			}
			tempAnnotations.add(annotation);			
		}
		return maxPitchClassHistogram;
	}
	
	/*------------------Pitch Histogram-------------------------------------------------*/

	/**
	 * Create a pitch histogram based on a list of annotations.
	 * @param annotations A list of annotations.
	 * @return a pitch class histogram with the annotations added.
	 */
	public static PitchHistogram createPitchHistogram(final List<Annotation> annotations) {
		final PitchHistogram pitchHistogram = new PitchHistogram();
		for (final Annotation annotation : annotations) {
			pitchHistogram.add(annotation.getPitch(PitchUnit.ABSOLUTE_CENTS));
		}
		return pitchHistogram;
	}
	
	public static PitchHistogram createPitchHistogram(final List<Annotation> annotations,
			final double width) {
		int start = Configuration.getInt(ConfKey.pitch_histogram_start); 
		int stop = Configuration.getInt(ConfKey.pitch_histogram_stop);
		int size = stop - start;
		KernelDensityEstimate kde = new KernelDensityEstimate(new GaussianKernel(width),size);
		for (Annotation annotation : annotations) {
			double pitch = annotation.getPitch(PitchUnit.ABSOLUTE_CENTS);
			if(pitch >= start && pitch <= stop){
				kde.add(pitch);
			}else{
				LOG.finer("Ignored pitch: " + pitch + " not between " + start + " and " + stop);
			}
		}
		PitchHistogram pitchHistogram = new PitchHistogram();
		for (int i = start; i < size; i++) {
			pitchHistogram.setCount(i, (long) kde.getValue(i));
		}
		return pitchHistogram;
	}
	
	
	/*-----------------------KDE----------------------*/
	
	/**
	 * Creates a pitch class {@link KernelDensityEstimate} for a list of annotations.
	 * @param annotations A list of annotations.
	 * @param width The width of the Gaussian kernel. 
	 * @return A kernel density estimate with the annotations added.
	 */
	public static KernelDensityEstimate createPichClassKDE(final List<Annotation> annotations,
			final double width){
		KernelDensityEstimate kde = new KernelDensityEstimate(new GaussianKernel(width),1200);
		for (Annotation annotation : annotations) {
			double pitch = annotation.getPitch(PitchUnit.RELATIVE_CENTS);
			kde.add(pitch);
		}
		return kde;
	}

	/**
	 * Creates a pitch class {@link KernelDensityEstimate} for a list of pitches
	 * defined by a Scala file.
	 * 
	 * @param scalaFile
	 *            A Scala file, defining a list of pitches.
	 * @param width
	 *            The width of the Gaussian kernel.
	 * @return A kernel density estimate with the annotations added.
	 */
	public static KernelDensityEstimate createPichClassKDE(
			final ScalaFile scalaFile, final double width) {
		KernelDensityEstimate kde = new KernelDensityEstimate(
				new GaussianKernel(width), 1200);
		for (double pitch : scalaFile.getPitches()) {
			kde.add(pitch);
		}
		return kde;
	}
	
	/**
	 * Creates a pitch class {@link KernelDensityEstimate} for a list of pitches
	 * defined by the annotations.
	 * 
	 * @param width
	 *            The width of the Gaussian kernel.
	 * @param annotations
	 *            A list of annotations.
	 * @param windowDuration
	 *            The duration for one window in seconds.
	 * @return A kernel density estimate with the annotations added.
	 */
	public static KernelDensityEstimate createPichClassKDE(final List<Annotation> annotations,
			final double width,final double windowDuration) {
		KernelDensityEstimate maxKde = new KernelDensityEstimate(
				new GaussianKernel(width), 1200);
		KernelDensityEstimate currentKde = new KernelDensityEstimate(
				new GaussianKernel(width), 1200);
		//sort by time
		Collections.sort(annotations);
		int startWindowIndex = -1;
		for(int i = 0 ; i < annotations.size() ; i++){
			currentKde.add(annotations.get(i).getPitch(PitchUnit.RELATIVE_CENTS));
			if(startWindowIndex >= 0){
				currentKde.remove(annotations.get(startWindowIndex).getPitch(PitchUnit.RELATIVE_CENTS));
				startWindowIndex++;
				maxKde.max(currentKde);
			} else if(annotations.get(i).getStart() - annotations.get(0).getStart() > windowDuration) {
				startWindowIndex=0;
			}
		}
		return maxKde;
	}

	
}
