/**
*
*  Tarsos is developed by Joren Six at 
*  The Royal Academy of Fine Arts & Royal Conservatory,
*  University College Ghent,
*  Hoogpoort 64, 9000 Ghent - Belgium
*
**/
package be.hogent.tarsos.sampled.pitch;

import java.util.List;

/**
 * A PitchDetector is able to annotate a song with pitches.
 * 
 * @author Joren Six
 */
public interface PitchDetector {

	/**
	 * Execute the pitch detection process.
	 * @return Returns a list of detected pitches, encapsulated in an annotation object.
	 */
	List<Annotation> executePitchDetection();
	
	/**
	 * Calculate and return an indicator for progress.
	 * @return A value between 0.0 and 100. Indicating the progress made in
	 *         percentage. Or -1 if the task has an indeterminate duration.
	 */
	double progress();

	/**
	 * @return a list of annotated samples
	 */
	List<Annotation> getAnnotations();

	/**
	 * @return the name of the detector possibly with parameters e.g. aubio_YIN
	 */
	String getName();
}
