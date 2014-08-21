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

package be.tarsos.sampled.pitch;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import be.tarsos.dsp.wavelet.HaarWaveletTransform;
import be.tarsos.util.ConfKey;
import be.tarsos.util.Configuration;
import be.tarsos.util.StopWatch;

/**
 * This class is responsible for the extraction and delegation of annotations.
 * It notifies listeners of annotations.
 */
public final class AnnotationPublisher{

	private AnnotationTree tree;
	private final List<AnnotationListener> listeners;
	private final AnnotationSelection selection;
	private final List<Annotation> originalAnnotationList;
	private final PitchUnit unit;

	/**
	 * Log messages.
	 */
	private static final Logger LOG = Logger.getLogger(AnnotationPublisher.class.getName());

	/**
	 * Hides the default constructor;
	 */
	private AnnotationPublisher() {
		listeners = new ArrayList<AnnotationListener>();
		selection = new AnnotationSelection();
		originalAnnotationList = new ArrayList<Annotation>();
		unit = PitchUnit.valueOf(Configuration.get(ConfKey.pitch_contour_unit));
		tree = new AnnotationTree(unit);
	}

	/**
	 * Adds an annotation listener.
	 * 
	 * @param listener
	 *            The listener to add.
	 */
	public void addListener(final AnnotationListener listener) {
		listeners.add(listener);
	}

	/**
	 * @param annotations
	 */
	public void addAnnotations(final List<Annotation> annotations) {
		int beforeSize = tree.size();
		tree.add(annotations);
		originalAnnotationList.addAll(annotations);
		assert tree.size() == beforeSize+annotations.size();
	}
	
	
	public void applySteadyStateFilter(final double maxCentsDifference, final double minDuration){
		rebuildTree(new SteadyStateFilter(maxCentsDifference, minDuration));
	}
	
	public void applyPitchClassFilter(final double pitchClasses[],final double maxCentsDifference){
		rebuildTree(new PitchClassFilter(pitchClasses, maxCentsDifference));
	}
	
	public void applyWaveletCompressionFilter(final double maxCentsDifference){
		rebuildTree(new WaveletCompressionFilter(maxCentsDifference));
	}
	
	
	
	
	/**
	 * Filter annotations to include only annotations close to the pitch classes
	 * defined in scale.
	 * 
	 * @param annotations
	 *            The annotations;
	 * @param scale
	 *            The list of pitch classes.
	 * @param maxCentsDifference
	 *            The number of cents.
	 */

	
	/**
	 * Rebuilds the tree with the filtered annotations.
	 * @param filter The filter to apply on the original list of annotations to filter.
	 */
	private void rebuildTree(AnnotationFilter filter){
		tree = new AnnotationTree(unit);
		List<Annotation> listToFilter = new ArrayList<Annotation>(originalAnnotationList);
		filter.filter(listToFilter);
		tree.add(listToFilter);
		//clear the current state
		clear();
		//add annotations
		delegateAddAnnotations(selection.getMinProbability());
	}
	
	/**
	 * Steady state filter: only keep annotations that are repeated for x seconds within y cents.
	 * @param annotations The list of annotations to filter.
	 * @param maxCentsDifference The number of cents the next annotation may differ.
	 * @param minDuration The minimum duration of the 'note'.
	 */
	public void steadyStateAnnotationFilter(final List<Annotation> annotations,final double maxCentsDifference,final double minDuration){
		
	}

	public AnnotationTree getAnnotationTree() {
		return tree;
	}
	
	
	/**
	 * Remove all annotations from the listeners.
	 */
	public void clear(){
		for (AnnotationListener listener : listeners) {
			listener.clearAnnotations();
		}
	}
	
	public void clearTree(){
		tree = new AnnotationTree(unit);
		originalAnnotationList.clear();
	}

	/**
	 * Adds a list of annotations to listeners.
	 * 
	 * @param annotations
	 *            The annotations to add to the listeners.
	 */
	public void delegateAddAnnotations(final List<Annotation> annotations) {
		if (annotations.size() > 0) {
			for (AnnotationListener listener : listeners) {
				StopWatch watch = new StopWatch();
				for (Annotation annotation : annotations) {
					listener.addAnnotation(annotation);
				}
				if (annotations.size() > 1000) {
					LOG.fine(String.format("Adding %s annotations to %s took %s.", annotations.size(),
							listener.getClass().toString(), watch.formattedToString()));
				}
			}			
		}
		for (AnnotationListener listener : listeners) {
			listener.annotationsAdded();
		}
	}

	/**
	 * Adds annotations to listeners. The annotations are defined by a search on
	 * time.
	 * 
	 * @param startTime
	 *            The start time.
	 * @param stopTime
	 *            The stop time.
	 */
	public void delegateAddAnnotations(final double startTime, final double stopTime) {
		//create a new selection based on the current one:
		AnnotationSelection newSelection = new AnnotationSelection(selection);
		newSelection.setTimeSelection(startTime, stopTime);
		List<Annotation> annotations = tree.select(newSelection);
		delegateAddAnnotations(annotations);
	}

	/**
	 * Adds annotations to listeners. The annotations are defined by a search on
	 * time and pitch.
	 * 
	 * @param startTime
	 *            The start time.
	 * @param stopTime
	 *            The stop time.
	 * @param startPitch
	 *            The start pitch.
	 * @param stopPitch
	 *            The stop pitch.
	 */
	public void delegateAddAnnotations(final double startTime, final double stopTime,
			final double startPitch, final double stopPitch) {
		AnnotationSelection newSelection = new AnnotationSelection(selection);
		newSelection.setTimeSelection(startTime, stopTime);
		newSelection.setPitchSelection(startPitch, stopPitch);
		List<Annotation> annotations = tree.select(newSelection);
		delegateAddAnnotations(annotations);
		
	}

	public void delegateAddAnnotations(final double newMinProbability) {
		AnnotationSelection newSelection = new AnnotationSelection(selection);
		newSelection.setMinProbability(newMinProbability);
		List<Annotation> annotations = tree.select(newSelection);
		delegateAddAnnotations(annotations);
	}
	
	public void alterSelection(final double startTime, final double stopTime,
			final double startPitch, final double stopPitch){
		selection.setTimeSelection(startTime, stopTime);
		selection.setPitchSelection(startPitch, stopPitch);
		LOG.finer("New selection: " + selection.toString());
	}
	
	public void alterSelection(final double startTime, final double stopTime){
		selection.setTimeSelection(startTime, stopTime);
		LOG.finer("New selection: " + selection.toString());
	}
	
	public void alterSelection(final double newMinProbability){
		selection.setMinProbability(newMinProbability);
		LOG.finer("New selection: " + selection.toString());
	}

	public void extractionFinished(){
		for (AnnotationListener listener : listeners) {
			listener.extractionFinished();
		}
	}
	
	public void extractionStarted(){
		for (AnnotationListener listener : listeners) {
			listener.extractionStarted();
		}
	}

	public AnnotationSelection getCurrentSelection() {
		return selection;
	}
	
	public List<Annotation> getCurrentlySelectedAnnotations() {
		return tree.select(getCurrentSelection());
	}

	/**
	 * The single annotation publisher in the program: is a singleton.
	 */
	private static final AnnotationPublisher INSTANCE = new AnnotationPublisher();

	/**
	 * 
	 * @return The single instance of the annotation publisher.
	 */
	public static AnnotationPublisher getInstance() {
		return INSTANCE;
	}
	
	
	
	/************FILTERS***************/
	
	public interface AnnotationFilter{
		void filter(List<Annotation> listToFilter);
	}
	
	
	public class PitchClassFilter implements AnnotationFilter {
		private final double[] pitchClasses;
		private final double maxCentsDifference;

		public PitchClassFilter(final double[] pitchClasses,
				final double maxCentsDifference) {
			this.pitchClasses = pitchClasses;
			this.maxCentsDifference = maxCentsDifference;
		}

		
		public void filter(final List<Annotation> listToFilter) {
			for (int i = 0; i < listToFilter.size(); i++) {
				double annotationPitchClass = listToFilter.get(i).getPitch(
						PitchUnit.RELATIVE_CENTS);
				// delete all annotations (except those that are close to a
				// pitch class)
				boolean delete = true;
				for (double scalePitchClass : pitchClasses) {
					// Calculate the difference e.g. between 3 and 1193 there is
					// 1190 cents
					double normalDiff = Math.abs(scalePitchClass
							- annotationPitchClass);
					// Distance between 3 and 1193 cents is also 10 cents,
					// calculate it:
					double wrappedDiff = Math.min(
							Math.abs(scalePitchClass - annotationPitchClass
									+ 1200),
							Math.abs(scalePitchClass - annotationPitchClass
									- 1200));
					// Do not delete an annotation if it is close to a pitch
					// class
					if (normalDiff < maxCentsDifference
							|| wrappedDiff < maxCentsDifference) {
						delete = false;
					}
				}
				// delete marked annotations
				if (delete) {
					listToFilter.remove(i);
					// do not forget to evaluate the new annotation
					// on the current place
					i--;
				}
			}
		}
	}
	
	public class SteadyStateFilter implements AnnotationFilter{
		final double maxCentsDifference;
		final double minDuration;
		
		public SteadyStateFilter(final double maxCentsDifference,final double minDuration){
			this.maxCentsDifference = maxCentsDifference;
			this.minDuration = minDuration;
		}

		
		public void filter(final List<Annotation> listToFilter) {
			for(int i = 0 ; i < listToFilter.size(); i++){
				double iCentsValue = listToFilter.get(i).getPitch(PitchUnit.ABSOLUTE_CENTS);
				double iStart = listToFilter.get(i).getStart();
				boolean stable = false;
				int j = i+1;
				for( ; j  < listToFilter.size() ; j++){
					double jCentsValue = listToFilter.get(j).getPitch(PitchUnit.ABSOLUTE_CENTS);
					double jStart = listToFilter.get(j).getStart();
					double centsDifference = Math.abs(iCentsValue - jCentsValue);
					double timeDifference = jStart-iStart;
					if(centsDifference > maxCentsDifference)
						break;
					if(timeDifference > minDuration){
						stable = true;
					}
				}
				if(stable){
					i = j;
				}else{
					listToFilter.remove(i);
					i--;
				}
			}
		}
		
	}
	
	public class WaveletCompressionFilter implements AnnotationFilter{
		
		HaarWaveletTransform ht;
		private double maxCentsDifference;
		public WaveletCompressionFilter(final double maxCentsDifference){
			this.maxCentsDifference = maxCentsDifference;
			ht = new HaarWaveletTransform();
		}

		
		public void filter(final List<Annotation> listToFilter) {
			int size = nextPowerOf2(listToFilter.size());
			float[] values = new float[size];
			for(int i = 0;i<listToFilter.size();i++){
				values[i] = (float) listToFilter.get(i).getPitch(PitchUnit.ABSOLUTE_CENTS);
			}
			ht.transform(values);
			
			
			for(int i = 0;i<listToFilter.size();i++){
				if(Math.abs(values[i]) < maxCentsDifference){
					values[i]=0;
				}
			}
			
			ht.inverseTransform(values);
			
			for(int i = 0;i<listToFilter.size();i++){
				Annotation unmodified = listToFilter.get(i);
				double modifiedPitchInHz =  PitchUnit.absoluteCentToHertz(values[i]);
				Annotation modified = new Annotation(unmodified.getStart(),modifiedPitchInHz,unmodified.getSource(),unmodified.getProbability());
				listToFilter.set(i, modified);
			}
			
		}
		
		private int nextPowerOf2(final int a)
	    {
	        int b = 1;
	        while (b < a)
	        {
	            b = b << 1;
	        }
	        return b;
	    }
		
	}
}
