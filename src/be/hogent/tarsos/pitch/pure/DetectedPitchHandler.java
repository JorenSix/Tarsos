package be.hogent.tarsos.pitch.pure;

/**
 * An interface to react to detected pitches.
 * @author Joren Six
 */
public interface DetectedPitchHandler {
    /**
     * Use this method to react to detected pitches. The handleDetectedPitch
     * is called for every sample even when there is no pitch detected: in
     * that case -1 is the pitch value.
     * @param time
     *            in seconds
     * @param pitch
     *            in Hz
     */
    void handleDetectedPitch(float time, float pitch);
}
