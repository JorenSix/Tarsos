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
* Tarsos is developed by Joren Six at IPEM, University Ghent
*  
* -----------------------------------------------------------
*
*  Info: http://tarsos.0110.be
*  Github: https://github.com/JorenSix/Tarsos
*  Releases: http://0110.be/releases/Tarsos/
*  
*  Tarsos includes some source code by various authors,
*  for credits, license and info: see README.
* 
*/



package be.tarsos.sampled.pitch;

/**
 * An interface to react to annotations.
 * 
 * @author Joren Six
 */
public interface AnnotationHandler {
	/**
	 * Use this method to react to annotations.
	 * @param annotation The annotation to handle.
	 */
	void handleAnnotation(Annotation annotation);
}
