/**
 */
package be.hogent.tarsos.pitch.pure;

/**
 * A pure pitch detector is capable of analysing a buffer with audio information
 * and return a pitch. Depending on the underlying algorithm the buffer has to
 * have a certain lenght.
 * @author Joren Six
 */
public interface PurePitchDetector {

    /**
     * The buffer with audio information. The information in the buffer is not
     * modified so it can be (re)used for e.g. FFT analysis.
     */
    float getPitch(final float[] audioBuffer);

}
