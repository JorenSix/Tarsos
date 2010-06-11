package be.hogent.tarsos.ui;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
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


    /**
     */
    private static final long serialVersionUID = 2799646800090116812L;

    private BufferedImage buffer;
    private Graphics2D bufferGraphics;

    private final FFT fft;
    private final float[] amplitudes;
    private final float[] hightWaterMarks;

    private int barWidth;// pixels
    private int barMaxHeight;

    public Spectrum(final AudioFile audioFile, final int bins) throws UnsupportedAudioFileException,
    IOException, LineUnavailableException {

        this.setSize(new Dimension(640, 400));
        barWidth = getWidth() / bins;
        barMaxHeight = getHeight();// pixels
        this.setVisible(true);
        this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

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

    @Override
    public void proccess(final float[] audioBuffer) {
        bufferGraphics.setColor(Color.BLACK);
        bufferGraphics.clearRect(0, 0, getWidth(), getHeight());

        fft.forwardTransform(audioBuffer);
        fft.modulus(audioBuffer, amplitudes);

        for (int i = 0; i < amplitudes.length / 2; i++) {
            bufferGraphics.setColor(Color.BLUE);
            final int height = (int) (Math.log1p(amplitudes[i]) * 50);
            hightWaterMarks[i] = Math.max(height, hightWaterMarks[i]);

            bufferGraphics.fillRect(i * barWidth + barWidth, barMaxHeight - height, barWidth,
                    height);
            bufferGraphics.setColor(Color.RED);
            bufferGraphics.fillRect(i * barWidth + barWidth, (int) (barMaxHeight - hightWaterMarks[i]),
                    barWidth, 2);
            hightWaterMarks[i] = hightWaterMarks[i] - 1;
        }
        // bufferGraphics.setColor(Color.WHITE);
        repaint();
    }

    public static void main(final String... args) throws UnsupportedAudioFileException, IOException,
    LineUnavailableException {
        final AudioFile f = new AudioFile("audio/MR.1975.26.43-4.wav");
        new Spectrum(f, 32);
    }
}
