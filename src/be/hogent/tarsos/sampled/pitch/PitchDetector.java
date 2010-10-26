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
	 */
	List<Annotation> executePitchDetection();

	/**
	 * @return a list of annotated samples
	 */
	List<Annotation> getAnnotations();

	/**
	 * @return the name of the detector possibly with parameters e.g. aubio_YIN
	 */
	String getName();
}
