package be.hogent.tarsos.sampled.pitch;

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
