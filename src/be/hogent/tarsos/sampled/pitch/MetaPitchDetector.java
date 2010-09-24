/**
 */
package be.hogent.tarsos.sampled.pitch;

/**
 * Uses YIN and MPM in the background. Only detects Pitch if both pitch trackers
 * agree (within a small, 2%, error margin).
 * 
 * @author Joren Six
 */
public final class MetaPitchDetector implements PurePitchDetector {

	/**
	 * The default buffer size for YIN and MPM.
	 */
	private static final int DEFAULT_BUFFER_SIZE = 1024;

	/**
	 * The error margin used (2%).
	 */
	private static final double ERROR_MARGIN = 0.02;

	/**
	 * The YIN pitch tracker.
	 */
	private final PurePitchDetector yin;
	/**
	 * The MPM pitch tracker.
	 */
	private final PurePitchDetector mpm;

	/**
	 * Create a new MetaPitchDetector.
	 * 
	 * @param samplingRate
	 *            The sampling rate of the signal.
	 */
	public MetaPitchDetector(final float samplingRate) {
		yin = new Yin(samplingRate, DEFAULT_BUFFER_SIZE);
		mpm = new McLeodPitchMethod(samplingRate, DEFAULT_BUFFER_SIZE);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see be.hogent.tarsos.pitch.pure.PurePitchDetector#getPitch(float[])
	 */
	@Override
	public float getPitch(final float[] audioBuffer) {
		final float yinPitch = yin.getPitch(audioBuffer);
		final float mpmPitch = mpm.getPitch(audioBuffer);
		float pitch;
		if (yinPitch == -1 || mpmPitch == -1) {
			pitch = -1;
		} else if (Math.abs(yinPitch - mpmPitch) <= mpmPitch * ERROR_MARGIN) {
			// pitch within 2 percent => accurate
			pitch = (yinPitch + mpmPitch) / 2;
		} else {
			pitch = -1;
		}
		return pitch;
	}

}
