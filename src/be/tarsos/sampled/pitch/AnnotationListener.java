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


/**
 * Elements interested in representing annotations should implement this
 * interface.
 * 
 * The implementor should be able to clear all annotations and reset a subset of
 * annotations rather quickly.
 */
public interface AnnotationListener {
	/**
	 * Add an annotation to the element.
	 * 
	 * @param annotation
	 *            The annotation to add: this method is called a lot. After a
	 *            clearAnnotations() call a very large number of annotations is
	 *            possible. So efficiently adding annotations should be possible
	 *            or threaded.
	 */
	void addAnnotation(Annotation annotation);

	/**
	 * Clears all annotations.
	 */
	void clearAnnotations();

	/**
	 * Is called after a list of annotations is added.
	 */
	void annotationsAdded();
	
	/**
	 * A hook to react to annotation extraction. This method is called when
	 * pitch annotations starts on an audio file.
	 */
	void extractionStarted();

	/**
	 * A hook to react to annotation extraction. This method is called when
	 * pitch annotations is completed.
	 */
	void extractionFinished();
}
