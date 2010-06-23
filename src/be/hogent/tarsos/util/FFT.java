package be.hogent.tarsos.util;

import edu.emory.mathcs.jtransforms.fft.FloatFFT_1D;

/**
 * Wrapper for calling a hopefully Fast Fourier transform. Makes it easy to
 * switch FFT algorithm with minimal overhead.
 * @author Joren Six
 */
public final class FFT {

    final FloatFFT_1D fft;

    public FFT(final int size) {
        fft = new FloatFFT_1D(size);
    }

    /**
     * Computes forward DFT.
     * @param data
     *            data to transform.
     */
    public void forwardTransform(final float[] data) {
        fft.complexForward(data);
    }

    /**
     * Computes inverse DFT.
     * @param data
     *            data to transform
     */
    public void backwardsTransform(final float[] data) {
        fft.complexInverse(data, true);
    }

    /**
     * Returns the modulus of the element at index i. The modulus, magnitude or
     * absolute value is (a²+b²) ^ 0.5 with a being the real part and b the
     * imaginary part of a complex number.
     * @param data
     *            The FFT transformed data.
     * @param index
     *            The index of the element.
     * @return The modulus, magnitude or absolute value of the element at index
     *         i
     */
    public float modulus(final float[] data, final int index) {
        final int realIndex = index * 2;
        final int imgIndex = index * 2 + 1;
        final float modulus = data[realIndex] * data[realIndex] + data[imgIndex] * data[imgIndex];
        return (float) Math.pow(modulus, 0.5);
    }

    /**
     * Calculates the the modulus for each element in data and stores the result
     * in amplitudes.
     * @param data
     *            The input data.
     * @param amplitudes
     *            The output modulus info or amplitude.
     */
    public void modulus(final float[] data, final float[] amplitudes) {
        assert data.length / 2 == amplitudes.length;
        for (int i = 0; i < amplitudes.length; i++) {
            amplitudes[i] = modulus(data, i);
        }
    }
}
