/**
 */
package be.hogent.tarsos.sampled;

import java.util.Random;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.SourceDataLine;
import javax.sound.sampled.UnsupportedAudioFileException;

import be.hogent.tarsos.util.FFT;

/**
 * Nobutaka Ono, Ken-ichi Miyamoto, Jonathan Le Roux, Hirokazu Kameoka, Shigeki
 * Sagayama, ``Separation of a Monaural Audio Signal into Harmonic/Percussive
 * Components by Complementary Diffusion on Spectrogram,'' Proc. of EUSIPCO,
 * Aug., 2008.
 * http://hil.t.u-tokyo.ac.jp/publications/download.php?bib=Ono2008EUSIPCO08.pdf
 * @author Joren Six
 */
public final class SpectralDiffusion implements AudioProcessor {

    /**
     * The gamma parameter. 0 < gamma <= 1. Default value gamma = 0.3
     */
    private final float gamma;
    /**
     * The alpha parameter. 0 < alpha < 1. Default value alpha = 0.3 = 0.3 was
     */
    private final float alpha;


    public SpectralDiffusion() {
        gamma = 0.3f;
        alpha = 0.3f;
    }


    /**
     * @param F
     *            2 dim array: original moduli spectrogram (will be scaled by
     *            gamma).
     * @param H
     *            2 dim array: harmonic components after separation.
     * @param P
     *            2 dim array: percussive components after separation.
     * @param N
     *            Number of iterations in the iterative process [50,100].
     * @param T
     *            Time dimension (x axis of the spectrogram).
     * @param K
     *            Frequency dimension( y axis of the spectrogram).
     */
    public void spectralDiffuse(final float[][] F, final float[][] H, final float[][] P, final int N,
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


    FFT fft;
    float[][] f, h, p;
    int blockIndex = 0;
    int numberOfBlocks = 30;
    int sampleIndex = 0;

    /*
     * (non-Javadoc)
     * @see be.hogent.tarsos.sampled.AudioProcessor#processFull(float[], byte[])
     */
    @Override
    public void processFull(final float[] audioFloatBuffer, final byte[] audioByteBuffer) {
        fft = new FFT(1024);
        f = new float[numberOfBlocks][audioFloatBuffer.length * 2];
        processOverlapping(audioFloatBuffer, audioByteBuffer);
    }

    /*
     * (non-Javadoc)
     * 
     * @see be.hogent.tarsos.sampled.AudioProcessor#processOverlapping(float[],
     * byte[])
     */
    @Override
    public void processOverlapping(final float[] audioFloatBuffer, final byte[] audioByteBuffer) {
        if (blockIndex == numberOfBlocks) {
            blockIndex = blockIndex % numberOfBlocks;
        }
        for (int i = 0; i < audioFloatBuffer.length; i++) {
            f[numberOfBlocks - 1][0] = audioFloatBuffer[i];
        }
        fft.forwardTransform(f[blockIndex]);
        h = new float[audioFloatBuffer.length * 2][numberOfBlocks];
        p = new float[audioFloatBuffer.length * 2][numberOfBlocks];
        spectralDiffuse(f, h, p, 50, blockIndex, 5);
    }

    /*
     * (non-Javadoc)
     * @see be.hogent.tarsos.sampled.AudioProcessor#processingFinished()
     */
    @Override
    public void processingFinished() {
        // TODO Auto-generated method stub
    }

    public static void main(final String... strings) {
        final double sampleRate = 44100.0;
        final double seconds = 2.0;
        final double frequency = 440.0;
        final double amplitude = 0.8;

        final double twoPiF = 2 * Math.PI * frequency;
        final float[] buffer = new float[(int) (seconds * sampleRate)];
        for (int sample = 0; sample < buffer.length; sample++) {
            final double time = sample / sampleRate;
            buffer[sample] = (float) (amplitude * Math.sin(twoPiF * time));
        }

        // create a percussive element somwhat in the middle
        final Random rand = new Random();
        for (int sample = buffer.length / 2; sample < buffer.length / 2 + 1024; sample++) {
            buffer[sample] = rand.nextFloat() * 1.9f - 1.0f;
        }

        /* convert manually to PCM 16bits Little Endian, (still 44.1kHz) */
        final byte[] byteBuffer = new byte[buffer.length * 2];
        int bufferIndex = 0;
        for (int i = 0; i < byteBuffer.length; i++) {
            final int x = (int) (buffer[bufferIndex++] * 32767.0);
            byteBuffer[i] = (byte) x;
            i++;
            byteBuffer[i] = (byte) (x >>> 8);
        }

        final AudioFormat format = new AudioFormat((float) sampleRate, 16, 1, true, false);
        SourceDataLine line;
        final DataLine.Info info = new DataLine.Info(SourceDataLine.class, format);
        try {
            line = (SourceDataLine) AudioSystem.getLine(info);
            line.open(format);
            line.start();
            line.write(byteBuffer, 0, byteBuffer.length);
            line.drain();
            line.close();
        } catch (final LineUnavailableException e1) {
            // TODO Auto-generated catch block
            e1.printStackTrace();
        }

        final AudioFormat audioFormat = new AudioFormat((float) sampleRate, 16, 1, true, false);
        try {
            final AudioDispatcher dispatcher = AudioDispatcher.fromByteArray(byteBuffer, audioFormat);
            dispatcher.addAudioProcessor(new SpectralDiffusion());
            dispatcher.run();
        } catch (final UnsupportedAudioFileException e) {

        }

    }

}
