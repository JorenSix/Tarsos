/**
 */
package be.hogent.tarsos.apps.temp;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;

import javax.sound.sampled.AudioFileFormat;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.SourceDataLine;

/**
 * @author Joren Six
 */
public final class SampledSound {

    public static void main(final String args[]) throws LineUnavailableException, IOException {

        /* Construct a buffer with sound info */
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

        /* convert manually to PCM 16bits Little Endian, (still 44.1kHz) */
        final byte[] byteBuffer = new byte[buffer.length * 2];
        int bufferIndex = 0;
        for (int i = 0; i < byteBuffer.length; i++) {
            final int x = (int) (buffer[bufferIndex++] * 32767.0);
            byteBuffer[i] = (byte) x;
            i++;
            byteBuffer[i] = (byte) (x >>> 8);
        }

        /* Write the sound to a wave file */
        final File out = new File("out.wav");
        final AudioFormat audioFormat = new AudioFormat((float) sampleRate, 16, 1, true, false);
        final ByteArrayInputStream bais = new ByteArrayInputStream(byteBuffer);
        final AudioInputStream audioInputStream = new AudioInputStream(bais, audioFormat, buffer.length);
        AudioSystem.write(audioInputStream, AudioFileFormat.Type.WAVE, out);
        audioInputStream.close();

        /* Or play it directly */
        final AudioFormat format = new AudioFormat((float) sampleRate, 16, 1, true, false);
        SourceDataLine line;
        final DataLine.Info info = new DataLine.Info(SourceDataLine.class, format);
        line = (SourceDataLine) AudioSystem.getLine(info);
        line.open(format);
        line.start();
        line.write(byteBuffer, 0, byteBuffer.length);
        line.drain();
        line.close();
    }

}
