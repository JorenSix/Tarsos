package be.hogent.tarsos.sampled.pitch;

/**
 * An interface to react to detected pitches.
 * 
 * @author Joren Six
 */
public interface DetectedPitchHandler {
	/**
	 * Use this method to react to detected pitches.
	 */
	void handleDetectedPitch(Annotation annotation);
}
