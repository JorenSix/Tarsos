package be.hogent.tarsos.ui;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.Timer;
import java.util.TimerTask;

import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.UnsupportedAudioFileException;
import javax.swing.JFrame;

import be.hogent.tarsos.apps.AutoTune.Speaker;
import be.hogent.tarsos.util.AudioFile;
import be.hogent.tarsos.util.FFT;

import com.sun.media.sound.AudioFloatInputStream;

/**
 * Shows a spectrum for a file.
 * @author Joren Six
 */
public final class Spectrum extends JFrame {

    private static final int BAR_WIDTH = 10;// pixels
    private static final int BAR_MAX_HEIGHT = 100;// pixels

    private final BufferedImage buffer;
    private final Graphics2D bufferGraphics;

    private final BufferedImage imageEven;
    private final Graphics2D imageEvenGraphics;

    /**
     */
    private static final long serialVersionUID = 2799646800090116812L;


    private final int fftBins;
    private final Timer timer;
    private final AudioFloatInputStream afis;
    private final float[] audioBuffer;
    private final FFT fft;
    private final float[] amplitudes;
    private final Speaker speaker;

    public Spectrum(final AudioFile audioFile, final int bins) throws UnsupportedAudioFileException,
    IOException {

        this.setSize(new Dimension(bins * BAR_WIDTH, BAR_MAX_HEIGHT));
        this.setVisible(true);
        this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        fftBins = bins;

        imageEven = new BufferedImage(getWidth(), getHeight(), BufferedImage.TYPE_INT_RGB);
        imageEvenGraphics = imageEven.createGraphics();

        buffer = new BufferedImage(getWidth(), getHeight(), BufferedImage.TYPE_INT_RGB);
        bufferGraphics = buffer.createGraphics();
        bufferGraphics.setColor(Color.BLACK);
        bufferGraphics.clearRect(0, 0, getWidth(), getHeight());

        final File transcodedFile = new File(audioFile.transcodedPath());
        final AudioInputStream ais = AudioSystem.getAudioInputStream(transcodedFile);
        afis = AudioFloatInputStream.getInputStream(ais);
        audioBuffer = new float[fftBins * 2];
        fft = new FFT(fftBins);
        amplitudes = new float[fftBins];

        speaker = new Speaker();

        timer = new Timer();
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                step();
            }
        }, 0, 5);
    }

    public void step() {
        int bytesRead;
        try {
            bytesRead = afis.read(audioBuffer);
        } catch (final IOException e) {
            bytesRead = -1;
        }
        if (bytesRead != -1) {
            speaker.write(audioBuffer, 0, audioBuffer.length);
            imageEvenGraphics.setColor(Color.BLACK);
            imageEvenGraphics.clearRect(0, 0, getWidth(), getHeight());

            fft.forwardTransform(audioBuffer);
            fft.modulus(audioBuffer, amplitudes);
            imageEvenGraphics.setColor(Color.BLUE);

            for (int i = 0; i < amplitudes.length / 2; i++) {
                final int height = (int) (Math.log1p(amplitudes[i]) * BAR_MAX_HEIGHT);
                System.out.println(height);
                imageEvenGraphics.fillRect(i * BAR_WIDTH, BAR_MAX_HEIGHT - height, BAR_WIDTH, height);
            }
            bufferGraphics.drawImage(imageEven, 0, 0, null);
            // bufferGraphics.setColor(Color.WHITE);
            repaint();
        } else {
            timer.cancel();
            System.out.println("done");
        }

    }

    @Override
    public void paint(final Graphics g) {
        g.drawImage(buffer, 0, 0, null);
    }

    public static void main(final String... args) throws UnsupportedAudioFileException, IOException {
        final AudioFile f = new AudioFile("flute.novib.mf.C5B5.wav");
        new Spectrum(f, 64);
    }
}
