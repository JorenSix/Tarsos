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
 * 
 * An utility class to calculate and access the power of an audio file at any
 * given time.
 * 
 * @author Joren Six
 */
public class SignalPowerExtractor {

    private final AudioFile audioFile;
    private final double readWindow = 0.01; // seconds
    private double[] linearPowerArray;
    double maxLinearPower = -1;
    double minLinearPower = Double.MAX_VALUE;

    /**
     * Create a new power extractor
     * 
     * @param audioFile
     *            The audio file to extract power from.
     */
    public SignalPowerExtractor(AudioFile audioFile) {
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
    public double powerAt(double seconds, boolean relative) {
        if (linearPowerArray == null)
            extractPower();
        double power = linearPowerArray[secondsToIndex(seconds)];
        if (relative) {
            double powerDifference = maxLinearPower - minLinearPower;
            power = (power - minLinearPower) / powerDifference;
        } else {
            power = linearToDecibel(power);
        }
        return power;
    }

    /**
     * Calculates an index value for the power array from a number of seconds
     */
    private int secondsToIndex(double seconds) {
        return (int) (seconds / readWindow);
    }

    /**
     * Fills the power array with linear power values. Also stores the min and
     * max linear power.
     */
    private void extractPower() {
        File inputFile = new File(audioFile.path());
        AudioInputStream ais;
        try {
            ais = AudioSystem.getAudioInputStream(inputFile);
            AudioFloatInputStream afis = AudioFloatInputStream.getInputStream(ais);
            AudioFormat format = ais.getFormat();

            double sampleRate = format.getSampleRate();
            double frameSize = format.getFrameSize();
            double frameRate = format.getFrameRate();
            double audioFileLengtInSeconds = inputFile.length() / (frameSize * frameRate);

            linearPowerArray = new double[secondsToIndex(audioFileLengtInSeconds) + 1];
            int readAmount = (int) (readWindow * sampleRate);
            float buffer[] = new float[readAmount];

            int index = 0;
            while (afis.read(buffer, 0, readAmount) != -1) {
                double power = SignalPowerExtractor.localEnergy(buffer);
                minLinearPower = Math.min(power, minLinearPower);
                maxLinearPower = Math.max(power, maxLinearPower);
                linearPowerArray[index] = power;
                index++;
            }
        } catch (UnsupportedAudioFileException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Creates a wave from plot.
     * 
     * @param waveFormPlotFileName
     *            The file to save to.
     */
    public void saveWaveFormPlot(String waveFormPlotFileName) {
        try {
            File inputFile = new File(audioFile.path());
            AudioInputStream ais;
            ais = AudioSystem.getAudioInputStream(inputFile);
            AudioFormat format = ais.getFormat();
            double frameSize = format.getFrameSize();
            double frameRate = format.getFrameRate();
            double timeFactor = 2.0 / (frameSize * frameRate);

            RandomAccessFile file = new RandomAccessFile(new File(audioFile.path()), "r");
            SimplePlot p = new SimplePlot("Waveform " + audioFile.basename());
            p.setSize(4000, 500);
            // skip header (44 bytes, fixed length)
            for (int i = 0; i < 44; i++) {
                file.read();
            }
            int i1, index = 0;
            while ((i1 = file.read()) != -1) {
                byte b1 = (byte) i1;
                byte b2 = (byte) file.read();
                if (index % 3 == 0) {// write the power only every 10 bytes
                    double power = (b2 << 8 | b1 & 0xFF) / 32767.0;
                    double seconds = index * timeFactor;
                    p.addData(seconds, power);
                }
                index++;
            }
            p.save(waveFormPlotFileName);
        } catch (IOException e) {
            e.printStackTrace();
        } catch (UnsupportedAudioFileException e) {
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
    public void saveTextFile(String textFileName) {
        if (linearPowerArray == null)
            extractPower();

        StringBuilder sb = new StringBuilder("Time (in seconds);Power\n");
        for (int index = 0; index < linearPowerArray.length; index++) {
            sb.append(index * readWindow).append(";").append(linearPowerArray[index]).append("\n");
        }
        FileUtils.writeFile(sb.toString(), textFileName);
    }

    /**
     * Creates a 'power plot' of the signal.
     * 
     * @param powerPlotFileName
     *            where to save the plot.
     */
    public void savePowerPlot(String powerPlotFileName, double silenceTreshold) {
        if (linearPowerArray == null)
            extractPower();

        SimplePlot plot = new SimplePlot("Powerplot for " + audioFile.basename());
        for (int index = 0; index < linearPowerArray.length; index++) {
            // prevents negative infinity
            double power = linearToDecibel(linearPowerArray[index] == 0.0 ? 0.00000000000001
                    : linearPowerArray[index]);
            double timeInSeconds = index * readWindow;
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
    public static double localEnergy(float buffer[]) {
        double power = 0.0D;
        for (int i = 0; i < buffer.length; i++)
            power += buffer[i] * buffer[i];
        return power;
    }

    /**
     * Returns the dBSPL for a buffer.
     * 
     * @param buffer
     *            The buffer with audio information.
     * @return The dBSPL level for the buffer.
     */
    public static double soundPressureLevel(float buffer[]) {
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
    private static double linearToDecibel(double value) {
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
    public static boolean isSilence(float buffer[], double silenceThreshold) {
        return (soundPressureLevel(buffer) < silenceThreshold);
    }
}
