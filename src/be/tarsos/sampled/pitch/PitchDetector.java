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
