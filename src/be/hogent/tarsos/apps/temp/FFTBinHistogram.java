package be.hogent.tarsos.apps.temp;

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

    /**
     * @param args
     * @throws IOException
     * @throws UnsupportedAudioFileException
     */
    public static void main(String[] args) {
        AudioFile audioFile = new AudioFile(FileUtils.combine("src", "be", "hogent", "tarsos", "test",
                "data", "power_test.wav"));
        AudioInputStream stream = null;
        try {
            stream = AudioSystem.getAudioInputStream(new File(audioFile.path()));
            AudioFloatInputStream afis = AudioFloatInputStream.getInputStream(stream);

            int readAmount = 16384 / 2;
            float[] buffer = new float[readAmount];
            AudioFormat format = stream.getFormat();
            double sampleRate = format.getSampleRate();
            double[] spectrum = new double[readAmount];

            FFT fft = new FFT(format.getFrameSize());

            try {
                while (afis.read(buffer, 0, readAmount) != -1) {
                    fft.forwardTransform(buffer);

                    for (int j = 0; j < buffer.length / 2; j += 2) {
                        double amplitude = buffer[j] * buffer[j] + buffer[j + 1] * buffer[j + 1];

                        spectrum[j] = +amplitude;
                    }
                }
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }

            AmbitusHistogram ambitusHistogram = new AmbitusHistogram();

            for (int i = 0; i < buffer.length / 2; i++) {
                double amplitude = spectrum[i];
                amplitude = 20.0 * Math.log1p(amplitude);
                double pitch = i * sampleRate / buffer.length / 2;
                ambitusHistogram.setCount(pitch, (long) (amplitude * 10000000));
            }
            ambitusHistogram.plot("fft amb.png", "amb fft");
            ambitusHistogram.toneScaleHistogram().plot("fft tone scale.png", "fft tone scale test");

        } catch (UnsupportedAudioFileException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

    }
}
