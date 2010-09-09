package be.hogent.tarsos.pitch.pure;

/**
 * An implementation of the AUBIO_YIN pitch tracking algorithm. See <a href=
 * "http://recherche.ircam.fr/equipes/pcm/cheveign/ps/2002_JASA_YIN_proof.pdf"
 * >the AUBIO_YIN paper.</a> Implementation based on <a
 * href="http://aubio.org">aubio</a>
 * 
 * @author Joren Six
 */
public final class Yin implements PurePitchDetector {
	/**
	 * The default YIN threshold value. Should be around 0.10~0.15. See YIN
	 * paper for more information.
	 */
	private static final double DEFAULT_THRESHOLD = 0.15;

	/**
	 * The default size of an audio buffer (in samples).
	 */
	public static final int DEFAULT_BUFFER_SIZE = 1024;

	/**
	 * The default overlap of two consecutive audio buffers (in samples).
	 */
	public static final int DEFAULT_OVERLAP = 512;

	/**
	 * The actual YIN threshold.
	 */
	private final double threshold;

	/**
	 * The audio sample rate. Most audio has a sample rate of 44.1kHz.
	 */
	private final float sampleRate;

	/**
	 * The buffer that stores the calculated values. It is exactly half the size
	 * of the input buffer.
	 */
	private final float[] yinBuffer;

	public Yin(final float audioSampleRate, final int bufferSize) {
		this(audioSampleRate, bufferSize, DEFAULT_THRESHOLD);
	}

	public Yin(final float audioSampleRate, final int bufferSize, final double yinThreshold) {
		this.sampleRate = audioSampleRate;
		this.threshold = yinThreshold;
		yinBuffer = new float[bufferSize / 2];
	}

	/**
	 * The main flow of the AUBIO_YIN algorithm. Returns a pitch value in Hz or
	 * -1 if no pitch is detected.
	 * 
	 * @return a pitch value in Hz or -1 if no pitch is detected.
	 */
	@Override
	public float getPitch(final float[] audioBuffer) {

		int tauEstimate = -1;
		float pitchInHertz = -1;

		// step 2
		difference(audioBuffer);

		// step 3
		cumulativeMeanNormalizedDifference();

		// step 4
		tauEstimate = absoluteThreshold();

		// step 5
		if (tauEstimate != -1) {
			final float betterTau = parabolicInterpolation(tauEstimate);

			// step 6
			// TODO Implement optimization for the AUBIO_YIN algorithm.
			// 0.77% => 0.5% error rate,
			// using the data of the AUBIO_YIN paper
			// bestLocalEstimate()

			// conversion to Hz
			pitchInHertz = sampleRate / betterTau;
		}

		return pitchInHertz;
	}

	/**
	 * Implements the difference function as described in step 2 of the
	 * AUBIO_YIN paper.
	 */
	private void difference(final float[] audioBuffer) {
		int index, tau;
		float delta;
		for (tau = 0; tau < yinBuffer.length; tau++) {
			yinBuffer[tau] = 0;
		}
		for (tau = 1; tau < yinBuffer.length; tau++) {
			for (index = 0; index < yinBuffer.length; index++) {
				delta = audioBuffer[index] - audioBuffer[index + tau];
				yinBuffer[tau] += delta * delta;
			}
		}
	}

	/**
	 * The cumulative mean normalized difference function as described in step 3
	 * of the AUBIO_YIN paper. <br>
	 * <code>
	 * yinBuffer[0] == yinBuffer[1] = 1
	 * </code>
	 */
	private void cumulativeMeanNormalizedDifference() {
		int tau;
		yinBuffer[0] = 1;
		// Very small optimization in comparison with AUBIO
		// start the running sum with the correct value:
		// the first value of the yinBuffer
		float runningSum = yinBuffer[1];
		// yinBuffer[1] is always 1
		yinBuffer[1] = 1;
		// now start at tau = 2
		for (tau = 2; tau < yinBuffer.length; tau++) {
			runningSum += yinBuffer[tau];
			yinBuffer[tau] *= tau / runningSum;
		}
	}

	/**
	 * Implements step 4 of the AUBIO_YIN paper.
	 */
	private int absoluteThreshold() {
		// Uses another loop construct
		// than the AUBIO implementation
		int tau = 1;
		for (tau = 1; tau < yinBuffer.length; tau++) {
			if (yinBuffer[tau] < threshold) {
				while (tau + 1 < yinBuffer.length && yinBuffer[tau + 1] < yinBuffer[tau]) {
					tau++;
				}
				// found tau, exit loop and return
				break;
			}
		}

		// if no pitch found, tau => -1
		if (tau == yinBuffer.length || yinBuffer[tau] >= threshold) {
			tau = -1;
		}

		return tau;
	}

	/**
	 * Implements step 5 of the AUBIO_YIN paper. It refines the estimated tau
	 * value using parabolic interpolation. This is needed to detect higher
	 * frequencies more precisely. See http://fizyka.umk.pl/nrbook/c10-2.pdf
	 * 
	 * @param tauEstimate
	 *            the estimated tau value.
	 * @return a better, more precise tau value.
	 */
	private float parabolicInterpolation(final int tauEstimate) {
		final float betterTau;
		final int x0;
		final int x2;

		if (tauEstimate < 1) {
			x0 = tauEstimate;
		} else {
			x0 = tauEstimate - 1;
		}
		if (tauEstimate + 1 < yinBuffer.length) {
			x2 = tauEstimate + 1;
		} else {
			x2 = tauEstimate;
		}
		if (x0 == tauEstimate) {
			if (yinBuffer[tauEstimate] <= yinBuffer[x2]) {
				betterTau = tauEstimate;
			} else {
				betterTau = x2;
			}
		} else if (x2 == tauEstimate) {
			if (yinBuffer[tauEstimate] <= yinBuffer[x0]) {
				betterTau = tauEstimate;
			} else {
				betterTau = x0;
			}
		} else {
			float s0, s1, s2;
			s0 = yinBuffer[x0];
			s1 = yinBuffer[tauEstimate];
			s2 = yinBuffer[x2];
			// fixed AUBIO implementation, thanks to Karl Helgason:
			// (2.0f * s1 - s2 - s0) was incorrectly multiplied with -1
			betterTau = tauEstimate + (s2 - s0) / (2 * (2 * s1 - s2 - s0));
		}
		return betterTau;
	}
}
