/**
 */
package be.hogent.tarsos.sampled.pitch;

/**
 * A pure pitch detector is capable of analyzing a buffer with audio information
 * and return a pitch. Depending on the underlying algorithm the buffer has to
 * have a certain length.
 * 
 * @author Joren Six
 */
public interface PurePitchDetector {
	
	/**
	 * Analyzes a buffer with audio information and estimates a pitch in Hz.
	 * 
	 * @param audioBuffer
	 *            The buffer with audio information. The information in the
	 *            buffer is not modified so it can be (re)used for e.g. FFT
	 *            analysis.
	 * @return An estimation of the pitch in Hz or -1 if no pitch is detected.
	 */
	float getPitch(final float[] audioBuffer);

	/**
	 * Some algorithms can calculate a probability (noisiness, aperiodicity or
	 * clarity measure) for the detected pitch. This is somewhat similar to the
	 * term voiced which is used in speech recognition. This probability should
	 * be calculated together with the pitch but is returned using a call to
	 * this method. So if you want the probability of a buffer: first call
	 * getPitch(buffer) and then getProbability().
	 * 
	 * @return A probability
	 */
	float getProbability();
	
	// TODO Implement a cleaner way to return the probability!
}
