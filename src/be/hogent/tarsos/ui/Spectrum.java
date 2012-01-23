/**
*
*  Tarsos is developed by Joren Six at 
*  The Royal Academy of Fine Arts & Royal Conservatory,
*  University College Ghent,
*  Hoogpoort 64, 9000 Ghent - Belgium
*
**/
package be.hogent.tarsos.ui;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.UnsupportedAudioFileException;
import javax.swing.JFrame;

import be.hogent.tarsos.sampled.AudioDispatcher;
import be.hogent.tarsos.sampled.AudioProcessor;
import be.hogent.tarsos.sampled.BlockingAudioPlayer;
import be.hogent.tarsos.transcoder.ffmpeg.EncoderException;
import be.hogent.tarsos.util.AudioFile;
import be.hogent.tarsos.util.FFT;

/**
 * Shows a spectrum for a file.
 * 
 * @author Joren Six
 */
public final class Spectrum extends JFrame implements AudioProcessor {

	/**
     */
	private static final long serialVersionUID = 2799646800090116812L;

	private BufferedImage buffer;
	private Graphics2D bufferGraphics;

	private final FFT fft;
	private final float[] amplitudes;
	private final float[] hightWaterMarks;
	private float highestWaterMark;

	private int barWidth; // pixels
	private int barMaxHeight;

	public Spectrum(final AudioFile audioFile, final int bins) throws EncoderException, IOException,
			LineUnavailableException, UnsupportedAudioFileException {

		this.setSize(new Dimension(640, 400));
		barWidth = getWidth() / bins;
		barMaxHeight = getHeight(); // pixels
		this.setVisible(true);
		this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

		buffer = new BufferedImage(getWidth(), getHeight(), BufferedImage.TYPE_INT_RGB);
		bufferGraphics = buffer.createGraphics();
		bufferGraphics.setColor(Color.BLACK);
		bufferGraphics.clearRect(0, 0, getWidth(), getHeight());

		fft = new FFT(bins * 2);
		amplitudes = new float[bins * 2];
		hightWaterMarks = new float[bins * 2];
		highestWaterMark = 1;

		final int bufferSize = bins * 4;
		final int overlap = 0;

		final AudioInputStream stream = AudioSystem.getAudioInputStream(new File(audioFile.transcodedPath()));
		final AudioDispatcher rtap = new AudioDispatcher(stream, bufferSize, overlap);
		rtap.addAudioProcessor(new BlockingAudioPlayer(stream.getFormat(), bufferSize, overlap));
		rtap.addAudioProcessor(this);
		new Thread(rtap).start();

		addComponentListener(new ComponentAdapter() {

			@Override
			public void componentResized(final ComponentEvent e) {
				frameWasResized(e);
			}

			private void frameWasResized(final ComponentEvent e) {
				barWidth = getWidth() / bins;
				barMaxHeight = getHeight();
				buffer = new BufferedImage(getWidth(), getHeight(), BufferedImage.TYPE_INT_RGB);
				bufferGraphics = buffer.createGraphics();
				bufferGraphics.setColor(Color.BLACK);
				bufferGraphics.clearRect(0, 0, getWidth(), getHeight());
			}
		});
	}

	@Override
	public void paint(final Graphics g) {
		g.drawImage(buffer, 0, 0, null);
	}

	public static void main(final String... args) throws EncoderException, IOException,
			LineUnavailableException, UnsupportedAudioFileException {
		final AudioFile f = new AudioFile("audio/MR.1975.26.43-4.wav");
		new Spectrum(f, 128);
	}

	public void processOverlapping(final float[] audioBuffer, final byte[] audioByteBuffer) {
		bufferGraphics.setColor(Color.BLACK);
		bufferGraphics.clearRect(0, 0, getWidth(), getHeight());
		final float[] audioBufferClone = audioBuffer.clone();
		fft.forwardTransform(audioBufferClone);
		fft.modulus(audioBufferClone, amplitudes);

		for (int i = 0; i < amplitudes.length / 2; i++) {
			bufferGraphics.setColor(Color.BLUE);

			final int height = (int) (20 * Math.log1p(amplitudes[i]) * getHeight() / highestWaterMark);
			hightWaterMarks[i] = Math.max(height, hightWaterMarks[i]);
			highestWaterMark = Math.max(highestWaterMark, hightWaterMarks[i]);

			bufferGraphics.fillRect(i * barWidth + barWidth, barMaxHeight - height, barWidth, height);
			bufferGraphics.setColor(Color.RED);
			bufferGraphics.fillRect(i * barWidth + barWidth, (int) (barMaxHeight - hightWaterMarks[i]),
					barWidth, 2);
			hightWaterMarks[i] = hightWaterMarks[i] - 1;
		}

		System.out.println(highestWaterMark + "");

		repaint();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @seebe.hogent.tarsos.util.RealTimeAudioProcessor.AudioProcessor#
	 * processingFinished()
	 */
	public void processingFinished() {
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * be.hogent.tarsos.util.RealTimeAudioProcessor.AudioProcessor#processFull
	 * (float[], byte[])
	 */
	public void processFull(final float[] audioFloatBuffer, final byte[] audioByteBuffer) {
		processOverlapping(audioFloatBuffer, audioByteBuffer);
	}
}
