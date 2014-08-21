/*
*              _______                      
*             |__   __|                     
*                | | __ _ _ __ ___  ___  ___
*                | |/ _` | '__/ __|/ _ \/ __| 
*                | | (_| | |  \__ \ (_) \__ \    
*                |_|\__,_|_|  |___/\___/|___/    
*                                                         
* -----------------------------------------------------------
*
*  Tarsos is developed by Joren Six at 
*  The School of Arts,
*  University College Ghent,
*  Hoogpoort 64, 9000 Ghent - Belgium
*  
* -----------------------------------------------------------
*
*  Info: http://tarsos.0110.be
*  Github: https://github.com/JorenSix/Tarsos
*  Releases: http://tarsos.0110.be/releases/Tarsos/
*  
*  Tarsos includes some source code by various authors,
*  for credits and info, see README.
* 
*/

package be.tarsos.legacy.ui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Toolkit;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.Timer;
import java.util.TimerTask;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.Mixer;
import javax.sound.sampled.TargetDataLine;
import javax.sound.sampled.UnsupportedAudioFileException;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JPanel;

import be.tarsos.Tarsos;
import be.tarsos.midi.MidiCommon;
import be.tarsos.sampled.pitch.PitchUnit;
import be.tarsos.util.FFT;

import com.sun.media.sound.AudioFloatInputStream;

/**
 * @author Joren Six Implementation based on the sliding buffered images idea
 *         from <a
 *         href="http://forums.sun.com/thread.jspa?threadID=5284602">this
 *         thread.</a>
 */
public final class Spectrogram extends JComponent {

    private static final long serialVersionUID = -7760501261506593771L;

    private static final int W = (int) Toolkit.getDefaultToolkit().getScreenSize().getWidth();
    private static final int H = (int) Toolkit.getDefaultToolkit().getScreenSize().getHeight();

    private int position = -1;

    private final BufferedImage buffer;
    private final Graphics2D bufferGraphics;

    private final BufferedImage imageEven;
    private final Graphics2D imageEvenGraphics;

    private final Color pitchColor = Color.RED;

    private final Timer timer;

    private static final int FFT_SIZE = 16384 / 2;
    private final float[] audioDataBuffer = new float[FFT_SIZE];

    AudioFloatInputStream afis;
    double sampleRate;
    private final FFT fft;

    double[] amplitudes = new double[H];
    String lastDetectedNote = "";

    public Spectrogram(final Mixer mixer) throws UnsupportedAudioFileException, IOException,
    LineUnavailableException {

        // the image shown on even runs trough the x axis
        imageEven = new BufferedImage(W, H, BufferedImage.TYPE_INT_RGB);
        imageEvenGraphics = imageEven.createGraphics();

        buffer = new BufferedImage(W, H, BufferedImage.TYPE_INT_RGB);
        bufferGraphics = buffer.createGraphics();
        bufferGraphics.setColor(Color.BLACK);
        bufferGraphics.clearRect(0, 0, W, H);

        sampleRate = 44100;

        final AudioFormat format = new AudioFormat((float) sampleRate, 16, 1, true, false);
        final DataLine.Info dataLineInfo = new DataLine.Info(TargetDataLine.class, format);
        final TargetDataLine line = (TargetDataLine) mixer.getLine(dataLineInfo);
        final int numberOfSamples = (int) (0.1 * sampleRate);
        line.open(format, numberOfSamples);
        line.start();
        final AudioInputStream stream = new AudioInputStream(line);

        // PipedAudioStream audioFile = new
        // PipedAudioStream(FileUtils.combine("data","transcoded_audio"
        // ,"flute.novib.mf.C5B5.wav"));
        // stream = AudioSystem.getAudioInputStream(new File(audioFile.path()));

        afis = AudioFloatInputStream.getInputStream(stream);
        // read first full buffer
        afis.read(audioDataBuffer, 0, FFT_SIZE);

        fft = new FFT(FFT_SIZE);

        timer = new Timer();
        timer.schedule(new TimerTask() {
            
            public void run() {
                try {
                    step();
                } catch (final IOException e) {
                    e.printStackTrace();
                }
            }
        }, 0, 25);

    }

    
    public void paintComponent(final Graphics g) {
        g.drawImage(buffer, 0, 0, null);
    }

    private int frequencyToBin(final double frequency) {
        final double minFrequency = 100; // Hz
        final double maxFrequency = 20000; // Hz
        int bin = 0;
        final boolean logaritmic = true;
        if (frequency != 0 && frequency > minFrequency && frequency < maxFrequency) {
            double binEstimate = 0;
            if (logaritmic) {
                final double minCent = PitchUnit.hertzToAbsoluteCent(minFrequency);
                final double maxCent = PitchUnit.hertzToAbsoluteCent(maxFrequency);
                final double absCent = PitchUnit.hertzToAbsoluteCent(frequency * 2);
                binEstimate = (absCent - minCent) / maxCent * H;
            } else {
                binEstimate = (frequency - minFrequency) / maxFrequency * H;
            }
            if (binEstimate > 700) {
                Tarsos.println(binEstimate + "");
            }
            bin = H - 1 - (int) binEstimate;
        }
        return bin;
    }

    // executes on the timer thread
    public void step() throws IOException {
        position = (1 + position) % W;
        double maxAmplitude = 0.0;
        int pitchIndex = -1;

        final boolean bufferRead = false; 
        // Yin.slideBuffer(afis,audioDataBuffer,audioDataBuffer.length - 1024);
        if (bufferRead) {

            final float pitch = 0.0f;
            if (pitch != -1) {
                pitchIndex = frequencyToBin(pitch);
            }

            final float[] transformBuffer = new float[audioDataBuffer.length * 2];
            for (int i = 0; i < audioDataBuffer.length; i++) {
                transformBuffer[i] = audioDataBuffer[i];
            }

            fft.forwardTransform(transformBuffer);

            for (int j = 0; j < audioDataBuffer.length; j++) {
                double amplitude = fft.modulus(transformBuffer, j);
                amplitude = 20 * Math.log1p(amplitude);
                final double pitchCurrentBin = j * sampleRate / FFT_SIZE / 4;
                final int pixelBin = frequencyToBin(pitchCurrentBin);
                amplitudes[pixelBin] = amplitudes[pixelBin] == 0 ? amplitude
                        : (amplitudes[pixelBin] + amplitude) / 2;
                maxAmplitude = Math.max(amplitudes[pixelBin], maxAmplitude);
            }

            for (int i = 0; i < amplitudes.length; i++) {
                Color color = Color.black;
                if (i == pitchIndex) {
                    color = pitchColor;
                } else if (maxAmplitude != 0) {
                    final int greyValue = (int) (amplitudes[i] / maxAmplitude * 255);
                    color = new Color(greyValue, greyValue, greyValue);
                }
                imageEvenGraphics.setColor(color);
                imageEvenGraphics.fillRect(position, i, 1, 1);
            }

            bufferGraphics.drawImage(imageEven, 0, 0, null);
            bufferGraphics.setColor(Color.WHITE);
            bufferGraphics.drawString((new StringBuilder("Current frequency: ")).append((int) pitch).append(
            "Hz").toString(), 20, 20);
            bufferGraphics.drawString(new StringBuilder("Last detected note: ").append(lastDetectedNote)
                    .toString(), 20, 45);
        } else {
            timer.cancel();
        }

        // paintComponent will be called on the EDT (Event Dispatch Thread)
        repaint();
    }


    public static void main(final String[] args) throws UnsupportedAudioFileException, IOException,
    LineUnavailableException {
        final JPanel panel = new JPanel(new BorderLayout());
        final Spectrogram spectogram = new Spectrogram(MidiCommon.chooseMixerDevice());
        spectogram.setPreferredSize(new Dimension(W, H / 2));
        panel.add(spectogram, BorderLayout.CENTER);
        final JFrame frame = new JFrame("Spectrogram");
        frame.getContentPane().add(panel);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setExtendedState(JFrame.MAXIMIZED_BOTH);
        //frame.setUndecorated(true);
        frame.pack();
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
    }
}
