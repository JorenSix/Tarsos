package be.hogent.tarsos.ui;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.IOException;

import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.UnsupportedAudioFileException;
import javax.swing.JFrame;

import be.hogent.tarsos.util.AudioFile;
import be.hogent.tarsos.util.FFT;
import be.hogent.tarsos.util.RealTimeAudioProcessor;
import be.hogent.tarsos.util.RealTimeAudioProcessor.AudioProcessor;

/**
 * Shows a spectrum for a file.
 * @author Joren Six
 */
public final class Spectrum extends JFrame implements AudioProcessor {

    private static final int BAR_WIDTH = 1;// pixels
    private static final int BAR_MAX_HEIGHT = 400;// pixels
    /**
     */
    private static final long serialVersionUID = 2799646800090116812L;

    private final BufferedImage buffer;
    private final Graphics2D bufferGraphics;
    private final BufferedImage imageEven;
    private final Graphics2D imageEvenGraphics;

    private final FFT fft;
    private final float[] amplitudes;
    private final float[] hightWaterMarks;

    public Spectrum(final AudioFile audioFile, final int bins) throws UnsupportedAudioFileException,
    IOException, LineUnavailableException {


        this.setSize(new Dimension(bins * BAR_WIDTH / 2, BAR_MAX_HEIGHT));
        this.setVisible(true);
        this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        imageEven = new BufferedImage(getWidth(), getHeight(), BufferedImage.TYPE_INT_RGB);
        imageEvenGraphics = imageEven.createGraphics();

        buffer = new BufferedImage(getWidth(), getHeight(), BufferedImage.TYPE_INT_RGB);
        bufferGraphics = buffer.createGraphics();
        bufferGraphics.setColor(Color.BLACK);
        bufferGraphics.clearRect(0, 0, getWidth(), getHeight());

        fft = new FFT(bins * 2);
        amplitudes = new float[bins * 2];
        hightWaterMarks = new float[bins * 2];

        final RealTimeAudioProcessor rtap = new RealTimeAudioProcessor(audioFile.transcodedPath(), bins * 4);
        rtap.addAudioProcessor(this);
        new Thread(rtap).start();
    }

    @Override
    public void paint(final Graphics g) {
        g.drawImage(buffer, 0, 0, null);
    }

    @Override
    public void proccess(final float[] audioBuffer) {
        imageEvenGraphics.setColor(Color.BLACK);
        imageEvenGraphics.clearRect(0, 0, getWidth(), getHeight());

        fft.forwardTransform(audioBuffer);
        fft.modulus(audioBuffer, amplitudes);
        imageEvenGraphics.setColor(Color.BLUE);

        for (int i = 0; i < amplitudes.length / 2; i++) {
            imageEvenGraphics.setColor(Color.BLUE);
            final int height = (int) (Math.log1p(amplitudes[i]) * 50);
            hightWaterMarks[i] = Math.max(height, hightWaterMarks[i]);

            imageEvenGraphics.fillRect(i * BAR_WIDTH + BAR_WIDTH, BAR_MAX_HEIGHT - height, BAR_WIDTH,
                    height);
            imageEvenGraphics.setColor(Color.RED);
            imageEvenGraphics.fillRect(i * BAR_WIDTH + BAR_WIDTH,
                    (int) (BAR_MAX_HEIGHT - hightWaterMarks[i]),
                    BAR_WIDTH, 2);
            hightWaterMarks[i] = hightWaterMarks[i] - 1;
        }
        bufferGraphics.drawImage(imageEven, 0, 0, null);
        // bufferGraphics.setColor(Color.WHITE);
        repaint();
    }

    public static void main(final String... args) throws UnsupportedAudioFileException, IOException,
    LineUnavailableException {
        final AudioFile f = new AudioFile("flute.novib.mf.C5B5.wav");
        new Spectrum(f, 1024);
    }
}
