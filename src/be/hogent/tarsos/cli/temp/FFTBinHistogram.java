package be.hogent.tarsos.cli.temp;

import java.io.File;
import java.io.IOException;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.UnsupportedAudioFileException;

import be.hogent.tarsos.util.AudioFile;
import be.hogent.tarsos.util.FFT;
import be.hogent.tarsos.util.FileUtils;
import be.hogent.tarsos.util.histogram.AmbitusHistogram;

import com.sun.media.sound.AudioFloatInputStream;

public final class FFTBinHistogram {

    private FFTBinHistogram() {
    }
    /**
     * @param args
     * @throws IOException
     * @throws UnsupportedAudioFileException
     */
    public static void main(final String[] args) {
        final AudioFile audioFile = new AudioFile(FileUtils.combine("src", "be", "hogent", "tarsos", "test",
                "data", "power_test.wav"));
        AudioInputStream stream = null;
        try {
            stream = AudioSystem.getAudioInputStream(new File(audioFile.originalPath()));
            final AudioFloatInputStream afis = AudioFloatInputStream.getInputStream(stream);

            final int readAmount = 16384 / 2;
            final float[] buffer = new float[readAmount];
            final AudioFormat format = stream.getFormat();
            final double sampleRate = format.getSampleRate();
            final double[] spectrum = new double[readAmount];

            final FFT fft = new FFT(format.getFrameSize());

            try {
                while (afis.read(buffer, 0, readAmount) != -1) {
                    fft.forwardTransform(buffer);

                    for (int j = 0; j < buffer.length / 2; j += 2) {
                        final double amplitude = buffer[j] * buffer[j] + buffer[j + 1] * buffer[j + 1];

                        spectrum[j] = +amplitude;
                    }
                }
            } catch (final IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }

            final AmbitusHistogram ambitusHistogram = new AmbitusHistogram();

            for (int i = 0; i < buffer.length / 2; i++) {
                double amplitude = spectrum[i];
                amplitude = 20.0 * Math.log1p(amplitude);
                final double pitch = i * sampleRate / buffer.length / 2;
                ambitusHistogram.setCount(pitch, (long) (amplitude * 10000000));
            }
            ambitusHistogram.plot("fft amb.png", "amb fft");
            ambitusHistogram.toneScaleHistogram().plot("fft tone scale.png", "fft tone scale test");

        } catch (final UnsupportedAudioFileException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (final IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

    }
}
