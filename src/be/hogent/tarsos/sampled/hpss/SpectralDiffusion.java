/**
 */
package be.hogent.tarsos.sampled.hpss;

import java.io.File;
import java.io.IOException;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.UnsupportedAudioFileException;

import be.hogent.tarsos.sampled.AudioProcessor;
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
public final class SpectralDiffusion implements AudioProcessor, FrameListener {

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
	
	public void processingFinished() {
		// TODO Auto-generated method stub
	}

	public static void main(final String... strings) throws IOException, UnsupportedAudioFileException {
		String fileName = "C:\\Users\\jsix666\\eclipse_workspace\\Tarsos\\audio\\dekkmma_voice_all\\MR.1964.1.4-30.wav";
		AudioFormat decodedFormat = AudioSystem.getAudioFileFormat(new File(fileName)).getFormat();
		AudioReader input = AudioReaderFactory.getAudioReader(fileName, decodedFormat);
		STFT stft = new STFT(input, 1024, 512, 2048);
		stft.addFrameListener(new SpectralDiffusion());
		stft.start();

	}

	
	public void newFrame(STFT stft, long frAddr) {

		System.out.println(stft.getFrame(frAddr).length);

	}
}
