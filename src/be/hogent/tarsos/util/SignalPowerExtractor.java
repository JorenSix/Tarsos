package be.hogent.tarsos.util;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.UnsupportedAudioFileException;

import com.sun.media.sound.AudioFloatInputStream;

/**
 * An utility class to calculate and access the power of an audio file at any
 * given time.
 * 
 * @author Joren Six
 */
public final class SignalPowerExtractor {

    private final AudioFile audioFile;
    private static final double READ_WINDOW = 0.01; // seconds
    private double[] linearPowerArray;
    double maxLinearPower = -1;
    double minLinearPower = Double.MAX_VALUE;

    /**
     * Create a new power extractor.
     * 
     * @param audioFile
     *            The audio file to extract power from.
     */
    public SignalPowerExtractor(final AudioFile audioFile) {
        this.audioFile = audioFile;
    }

    /**
     * Returns the relative power [0.0;1.0] at the given time.
     * 
     * @param seconds
     *            the time to get the relative power for.
     * @return A number between 0 and 1 inclusive that shows the relative power
     *         at the given time
     * @exception IndexOutOfBoundsException
     *                when the number of seconds is not between the start and
     *                end of the song.
     */
    public double powerAt(final double seconds, final boolean relative) {
        if (linearPowerArray == null) {
            extractPower();
        }
        double power = linearPowerArray[secondsToIndex(seconds)];
        if (relative) {
            final double powerDifference = maxLinearPower - minLinearPower;
            power = (power - minLinearPower) / powerDifference;
        } else {
            power = linearToDecibel(power);
        }
        return power;
    }

    /**
     * Calculates an index value for the power array from a number of seconds.
     */
    private int secondsToIndex(final double seconds) {
        return (int) (seconds / READ_WINDOW);
    }

    /**
     * Fills the power array with linear power values. Also stores the min and
     * max linear power.
     */
    private void extractPower() {
        final File inputFile = new File(audioFile.path());
        AudioInputStream ais;
        try {
            ais = AudioSystem.getAudioInputStream(inputFile);
            final AudioFloatInputStream afis = AudioFloatInputStream.getInputStream(ais);
            final AudioFormat format = ais.getFormat();

            final double sampleRate = format.getSampleRate();
            final double frameSize = format.getFrameSize();
            final double frameRate = format.getFrameRate();
            final double audioFileLengtInSeconds = inputFile.length() / (frameSize * frameRate);

            linearPowerArray = new double[secondsToIndex(audioFileLengtInSeconds) + 1];
            final int readAmount = (int) (READ_WINDOW * sampleRate);
            final float[] buffer = new float[readAmount];

            int index = 0;
            while (afis.read(buffer, 0, readAmount) != -1) {
                final double power = SignalPowerExtractor.localEnergy(buffer);
                minLinearPower = Math.min(power, minLinearPower);
                maxLinearPower = Math.max(power, maxLinearPower);
                linearPowerArray[index] = power;
                index++;
            }
        } catch (final UnsupportedAudioFileException e) {
            e.printStackTrace();
        } catch (final IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Creates a wave from plot.
     * 
     * @param waveFormPlotFileName
     *            The file to save to.
     */
    public void saveWaveFormPlot(final String waveFormPlotFileName) {
        try {
            final File inputFile = new File(audioFile.path());
            AudioInputStream ais;
            ais = AudioSystem.getAudioInputStream(inputFile);
            final AudioFormat format = ais.getFormat();
            final double frameSize = format.getFrameSize();
            final double frameRate = format.getFrameRate();
            final double timeFactor = 2.0 / (frameSize * frameRate);

            final RandomAccessFile file = new RandomAccessFile(new File(audioFile.path()), "r");
            final SimplePlot p = new SimplePlot("Waveform " + audioFile.basename());
            p.setSize(4000, 500);
            // skip header (44 bytes, fixed length)
            for (int i = 0; i < 44; i++) {
                file.read();
            }
            int i1, index = 0;
            while ((i1 = file.read()) != -1) {
                final byte b1 = (byte) i1;
                final byte b2 = (byte) file.read();
                if (index % 3 == 0) { // write the power only every 10 bytes
                    final double power = (b2 << 8 | b1 & 0xFF) / 32767.0;
                    final double seconds = index * timeFactor;
                    p.addData(seconds, power);
                }
                index++;
            }
            p.save(waveFormPlotFileName);
        } catch (final IOException e) {
            e.printStackTrace();
        } catch (final UnsupportedAudioFileException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    /**
     * Creates a text file with relative power values for each sample.
     * 
     * @param textFileName
     *            where to save the text file?
     */
    public void saveTextFile(final String textFileName) {
        if (linearPowerArray == null) {
            extractPower();
        }

        final StringBuilder sb = new StringBuilder("Time (in seconds);Power\n");
        for (int index = 0; index < linearPowerArray.length; index++) {
            sb.append(index * READ_WINDOW).append(";").append(linearPowerArray[index]).append("\n");
        }
        FileUtils.writeFile(sb.toString(), textFileName);
    }

    /**
     * Creates a 'power plot' of the signal.
     * 
     * @param powerPlotFileName
     *            where to save the plot.
     */
    public void savePowerPlot(final String powerPlotFileName, final double silenceTreshold) {
        if (linearPowerArray == null) {
            extractPower();
        }

        final SimplePlot plot = new SimplePlot("Powerplot for " + audioFile.basename());
        for (int index = 0; index < linearPowerArray.length; index++) {
            // prevents negative infinity
            final double power = linearToDecibel(linearPowerArray[index] == 0.0 ? 0.00000000000001
                    : linearPowerArray[index]);
            final double timeInSeconds = index * READ_WINDOW;
            plot.addData(0, timeInSeconds, power);
            plot.addData(1, timeInSeconds, silenceTreshold);
        }
        plot.save(powerPlotFileName);
    }

    /**
     * Calculates the local (linear) energy of an audio buffer.
     * 
     * @param buffer
     *            The audio buffer.
     * @return The local (linear) energy of an audio buffer.
     */
    public static double localEnergy(final float[] buffer) {
        double power = 0.0D;
        for (int i = 0; i < buffer.length; i++) {
            power += buffer[i] * buffer[i];
        }
        return power;
    }

    /**
     * Returns the dBSPL for a buffer.
     * 
     * @param buffer
     *            The buffer with audio information.
     * @return The dBSPL level for the buffer.
     */
    public static double soundPressureLevel(final float[] buffer) {
        double value = Math.pow(localEnergy(buffer), 0.5);
        value = value / buffer.length;
        return linearToDecibel(value);
    }

    /**
     * Converts a linear to a dB value.
     * 
     * @param value
     *            The value to convert.
     * @return The converted value.
     */
    private static double linearToDecibel(final double value) {
        return 20.0 * Math.log10(value);
    }

    /**
     * Checks if the dBSPL level in the buffer falls below a certain threshold.
     * 
     * @param buffer
     *            The buffer with audio information.
     * @param silenceThreshold
     *            The threshold in dBSPL
     * @return
     */
    public static boolean isSilence(final float[] buffer, final double silenceThreshold) {
        return soundPressureLevel(buffer) < silenceThreshold;
    }
}
