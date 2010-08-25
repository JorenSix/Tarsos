/**
 */
package be.hogent.tarsos.sampled;


/**
 * Nobutaka Ono, Ken-ichi Miyamoto, Jonathan Le Roux, Hirokazu Kameoka, Shigeki
 * Sagayama, ``Separation of a Monaural Audio Signal into Harmonic/Percussive
 * Components by Complementary Diffusion on Spectrogram,'' Proc. of EUSIPCO,
 * Aug., 2008.
 * http://hil.t.u-tokyo.ac.jp/publications/download.php?bib=Ono2008EUSIPCO08.pdf
 * @author Joren Six
 */
public class SpectralDiffusion implements AudioProcessor {

    /**
     * The gamma parameter. 0 < gamma <= 1. Default value gamma = 0.3
     */
    private final float gamma;
    /**
     * The alpha parameter. 0 < alpha < 1. Default value alpha = 0.3 = 0.3 was
     */
    private final float alpha;
    /**
     * The number of iterations. This is called k in the HPSS article. The
     * normal range for iterations is [30,80]. 30 is the default.
     */
    private final int iterations;

    /**
     */
    float f[][];

    public SpectralDiffusion() {
        gamma = 0.3f;
        alpha = 0.3f;
        iterations = 30;
    }

    /**
     * @param f
     *            The STFT of an input signal f(t)
     * @param h
     *            A buffer used to store the calculated STFT of the harmonic
     *            component of the signal.
     * @param p
     *            A buffer used to store the calculated STFT of the percussive
     *            component of the signal.
     */
    public final void spectralDiffuse(final float[][] F, final float[][] H, final float[][] P, final int N,
            final int T, final int K) {

        float[][] W; // A range compressed version of the power spectrogram.
        W = new float[T][K];

        int k, t, n;
        float delta, h;

        for (t = 0; t < T; t++) {
            for (k = 0; k < K; k++) {
                W[t][k] = (float) Math.pow(F[t][k], 2.0 * gamma);
                H[t][k] = W[t][k] / 2.0f;
                P[t][k] = H[t][k]; // P[t][k] = W[t][k] / 2.0f;
            }
        }

        for (n = 0; n < N; n++) {
            for (t = 2; t < T - 1; t++) {
                for (k = 1; k < K - 1; k++) {
                    delta = alpha * (H[t - 1][k] - 2.0f * H[t][k] + H[t + 1][k]) / 4.0f - (1.0f - alpha)
                    * (P[t][k - 1] - 2.0f * P[t][k] + P[t][k - 1]) / 4.0f;
                    h = H[t][k] + delta;
                    if (h > W[t][k]) {
                        h = W[t][k];
                    }
                    if (h < 0.0) {
                        h = 0.0f;
                    }
                    H[t][k] = h;
                    P[t][k] = W[t][k] - h;
                }
            }
        }
    }



    /*
     * (non-Javadoc)
     * 
     * @see be.hogent.tarsos.sampled.AudioProcessor#processFull(float[], byte[])
     */
    @Override
    public void processFull(final float[] audioFloatBuffer, final byte[] audioByteBuffer) {
        f = new float[audioFloatBuffer.length * 2][30];
        for (int i = 0; i < audioFloatBuffer.length; i++) {
            f[i][0] = audioFloatBuffer[i];
        }
        // final FFT fft = new FFT(1024);
        // fft.forwardTransform(f);
    }

    /*
     * (non-Javadoc)
     * 
     * @see be.hogent.tarsos.sampled.AudioProcessor#processOverlapping(float[],
     * byte[])
     */
    @Override
    public void processOverlapping(final float[] audioFloatBuffer, final byte[] audioByteBuffer) {
        // TODO Auto-generated method stub

    }

    /*
     * (non-Javadoc)
     * 
     * @see be.hogent.tarsos.sampled.AudioProcessor#processingFinished()
     */
    @Override
    public void processingFinished() {
        // TODO Auto-generated method stub
    }

}
