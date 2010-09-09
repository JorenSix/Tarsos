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
 * 
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
	 * @param ff
	 *            2 dim array: original moduli spectrogram (will be scaled by
	 *            gamma).
	 * @param hh
	 *            2 dim array: harmonic components after separation.
	 * @param pp
	 *            2 dim array: percussive components after separation.
	 * @param nn
	 *            Number of iterations in the iterative process [50,100].
	 * @param tt
	 *            Time dimension (x axis of the spectrogram).
	 * @param kk
	 *            Frequency dimension( y axis of the spectrogram).
	 */
	public void spectralDiffuse(final float[][] ff, final float[][] hh, final float[][] pp, final int nn,
			final int tt, final int kk) {

		float[][] ww; // A range compressed version of the power spectrogram.
		ww = new float[tt][kk];

		int k, t, n;
		float delta, hhh;

		for (t = 0; t < tt; t++) {
			for (k = 0; k < kk; k++) {
				ww[t][k] = (float) Math.pow(ff[t][k], 2.0 * gamma);
				hh[t][k] = ww[t][k] / 2.0f;
				pp[t][k] = hh[t][k]; // P[t][k] = W[t][k] / 2.0f;
			}
		}

		for (n = 0; n < nn; n++) {
			for (t = 2; t < tt - 1; t++) {
				for (k = 1; k < kk - 1; k++) {
					delta = alpha * (hh[t - 1][k] - 2.0f * hh[t][k] + hh[t + 1][k]) / 4.0f - (1.0f - alpha)
							* (pp[t][k - 1] - 2.0f * pp[t][k] + pp[t][k - 1]) / 4.0f;
					hhh = hh[t][k] + delta;
					if (hhh > ww[t][k]) {
						hhh = ww[t][k];
					}
					if (hhh < 0.0) {
						hhh = 0.0f;
					}
					hh[t][k] = hhh;
					pp[t][k] = ww[t][k] - hhh;
				}
			}
		}
	}

	private FFT fft;
	private float[][] f, p, h;
	private int blockIndex = 0;
	private final int numberOfBlocks = 30;

	/*
	 * (non-Javadoc)
	 * 
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
		for (float element : audioFloatBuffer) {
			f[numberOfBlocks - 1][0] = element;
		}
		fft.forwardTransform(f[blockIndex]);
		h = new float[audioFloatBuffer.length * 2][numberOfBlocks];
		p = new float[audioFloatBuffer.length * 2][numberOfBlocks];
		spectralDiffuse(f, h, p, 50, blockIndex, 5);
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
