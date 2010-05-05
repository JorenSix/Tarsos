package be.hogent.tarsos.pitch;

import java.io.File;
import java.io.IOException;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.UnsupportedAudioFileException;

import be.hogent.tarsos.util.SimplePlot;

import com.sun.media.sound.AudioFloatInputStream;

/**
 * 
 * An implementation of the YIN pitch tracking algorithm. See <a href=
 * "http://recherche.ircam.fr/equipes/pcm/cheveign/ps/2002_JASA_YIN_proof.pdf"
 * >the YIN paper.</a>
 * 
 * Implementation based on <a href="http://aubio.org">aubio</a>
 * 
 * @author Joren Six
 */
public class Yin {

    /**
     * Used to start and stop real time annotations.
     */
    private static Yin yinInstance;

    /**
     * The YIN threshold value
     */
    private final double threshold = 0.15;

    private final int bufferSize;
    private final int overlapSize;
    private final float sampleRate;
    /**
     * A boolean to start and stop the algorithm. Practical for real time
     * processing of data.
     */
    private volatile boolean running;

    /**
     * The buffer with audio information. The information in the buffer is not
     * modified so it can be (re)used for e.g. FFT analysis.
     */
    private final float[] inputBuffer;

    /**
     * The buffer that stores the calculated values. It is exactly half the size
     * of the input buffer.
     */
    private final float[] yinBuffer;

    private Yin(float sampleRate, int bufferSize) {
        this.sampleRate = sampleRate;
        this.bufferSize = bufferSize;
        // half of each buffer overlaps
        overlapSize = bufferSize / 2;
        running = true;
        inputBuffer = new float[bufferSize];
        yinBuffer = new float[bufferSize / 2];
    }

    /**
     * Implements the difference function as described in step 2 of the YIN
     * paper
     */
    private void difference() {
        int j, tau;
        float delta;
        for (tau = 0; tau < yinBuffer.length; tau++) {
            yinBuffer[tau] = 0;
        }
        for (tau = 1; tau < yinBuffer.length; tau++) {
            for (j = 0; j < yinBuffer.length; j++) {
                delta = inputBuffer[j] - inputBuffer[j + tau];
                yinBuffer[tau] += delta * delta;
            }
        }
    }

    /**
     * The cumulative mean normalized difference function as described in step 3
     * of the YIN paper <br>
     * <code>
	 * yinBuffer[0] == yinBuffer[1] = 1
	 * </code>
     * 
     */
    private void cumulativeMeanNormalizedDifference() {
        int tau;
        yinBuffer[0] = 1;
        // Very small optimization in comparison with AUBIO
        // start the running sum with the correct value:
        // the first value of the yinBuffer
        float runningSum = yinBuffer[1];
        // yinBuffer[1] is always 1
        yinBuffer[1] = 1;
        // now start at tau = 2
        for (tau = 2; tau < yinBuffer.length; tau++) {
            runningSum += yinBuffer[tau];
            yinBuffer[tau] *= tau / runningSum;
        }
    }

    /**
     * Implements step 4 of the YIN paper
     */
    private int absoluteThreshold() {
        // Uses another loop construct
        // than the AUBIO implementation
        for (int tau = 1; tau < yinBuffer.length; tau++) {
            if (yinBuffer[tau] < threshold) {
                while (tau + 1 < yinBuffer.length && yinBuffer[tau + 1] < yinBuffer[tau])
                    tau++;
                return tau;
            }
        }
        // no pitch found
        return -1;
    }

    /**
     * Implements step 5 of the YIN paper. It refines the estimated tau value
     * using parabolic interpolation. This is needed to detect higher
     * frequencies more precisely. See http://fizyka.umk.pl/nrbook/c10-2.pdf
     * 
     * @param tauEstimate
     *            the estimated tau value.
     * @return a better, more precise tau value.
     */
    private float parabolicInterpolation(int tauEstimate) {
        float s0, s1, s2;
        int x0 = (tauEstimate < 1) ? tauEstimate : tauEstimate - 1;
        int x2 = (tauEstimate + 1 < yinBuffer.length) ? tauEstimate + 1 : tauEstimate;
        if (x0 == tauEstimate)
            return (yinBuffer[tauEstimate] <= yinBuffer[x2]) ? tauEstimate : x2;
        if (x2 == tauEstimate)
            return (yinBuffer[tauEstimate] <= yinBuffer[x0]) ? tauEstimate : x0;
        s0 = yinBuffer[x0];
        s1 = yinBuffer[tauEstimate];
        s2 = yinBuffer[x2];
        // fixed AUBIO implementation, thanks to Karl Helgason:
        // (2.0f * s1 - s2 - s0) was incorrectly multiplied with -1
        return tauEstimate + 0.5f * (s2 - s0) / (2.0f * s1 - s2 - s0);
    }

    /**
     * The main flow of the YIN algorithm. Returns a pitch value in Hz or -1 if
     * no pitch is detected.
     * 
     * @return a pitch value in Hz or -1 if no pitch is detected.
     */
    private float getPitch() {
        int tauEstimate = -1;
        float pitchInHertz = -1;

        // step 2
        difference();

        // step 3
        cumulativeMeanNormalizedDifference();

        // step 4
        tauEstimate = absoluteThreshold();

        // step 5
        if (tauEstimate != -1) {
            float betterTau = parabolicInterpolation(tauEstimate);

            // step 6
            // TODO Implement optimization for the YIN algorithm.
            // 0.77% => 0.5% error rate,
            // using the data of the YIN paper
            // bestLocalEstimate()

            // conversion to Hz
            pitchInHertz = sampleRate / betterTau;
        }

        return pitchInHertz;
    }

    /**
     * The interface to use to react to detected pitches.
     * 
     * @author Joren Six
     * 
     */
    public interface DetectedPitchHandler {
        /**
         * Use this method to react to detected pitches. The handleDetectedPitch
         * is called for every sample even when there is no pitch detected: in
         * that case -1 is the pitch value.
         * 
         * @param time
         *            in seconds
         * @param pitch
         *            in Hz
         */
        void handleDetectedPitch(float time, float pitch);
    }

    /**
     * Annotate a file wit pitch information.
     * 
     * @param fileName
     *            the file to annotate.
     * @param detectedPitchHandler
     *            handles the pitch information.
     * @throws UnsupportedAudioFileException
     *             Currently only WAVE files with one channel (MONO) are
     *             supported.
     * @throws IOException
     *             If there is an error reading the file.
     */
    public static void processFile(String fileName, DetectedPitchHandler detectedPitchHandler)
            throws UnsupportedAudioFileException, IOException {
        AudioInputStream ais = AudioSystem.getAudioInputStream(new File(fileName));
        AudioFloatInputStream afis = AudioFloatInputStream.getInputStream(ais);
        Yin.processStream(afis, detectedPitchHandler);
    }

    /**
     * Annotate an audio stream: useful for real-time pitch tracking.
     * 
     * @param afis
     *            The audio stream.
     * @param detectedPitchHandler
     *            Handles the pitch information.
     * @throws UnsupportedAudioFileException
     *             Currently only WAVE streams with one channel (MONO) are
     *             supported.
     * @throws IOException
     *             If there is an error reading the stream.
     */
    public static void processStream(AudioFloatInputStream afis, DetectedPitchHandler detectedPitchHandler)
            throws UnsupportedAudioFileException, IOException {
        AudioFormat format = afis.getFormat();
        float sampleRate = format.getSampleRate();
        double frameSize = format.getFrameSize();
        double frameRate = format.getFrameRate();
        float time = 0;
        // number of bytes / frameSize * frameRate gives the number of seconds
        // because we use float buffers there is a factor 2: 2 bytes per float?
        // Seems to be correct but a float uses 4 bytes: confused programmer is
        // confused.
        float timeCalculationDivider = (float) (frameSize * frameRate / 2);
        long floatsProcessed = 0;
        yinInstance = new Yin(sampleRate, 2048);
        int bufferStepSize = yinInstance.bufferSize - yinInstance.overlapSize;

        // read full buffer
        boolean hasMoreBytes = afis.read(yinInstance.inputBuffer, 0, yinInstance.bufferSize) != -1;
        floatsProcessed += yinInstance.inputBuffer.length;
        while (hasMoreBytes && yinInstance.running) {
            float pitch = yinInstance.getPitch();
            time = floatsProcessed / timeCalculationDivider;
            if (detectedPitchHandler != null)
                detectedPitchHandler.handleDetectedPitch(time, pitch);

            // slide buffer with predefined overlap
            hasMoreBytes = Yin.slideBuffer(afis, yinInstance.inputBuffer, yinInstance.overlapSize);

            floatsProcessed += bufferStepSize;
        }
    }

    /**
     * Slides a buffer with an overlap and reads new data from the stream. to
     * the correct place in the buffer. E.g. with a buffer size of 9 and overlap
     * of 3.
     * 
     * <pre>
     *      | 8 | 7 | 6 | 5 | 4 | 3 | 2 | 1 | 0 |
     *                        |
     *                Slide (9 - 3 = 6)
     *                        |
     *                        v
     *      | _ | _ | _ | _ | _ | _ | 8 | 7 | 6 |
     *                        |
     *        Fill from 0 to 9 - 3 = 6 exclusive
     *                        |
     *                        v
     *      | 14| 13| 12| 11| 10| 9 | 8 | 7 | 6 |
     * 
     * </pre>
     * 
     * @param audioFloatInputStream
     *            The stream to read audio data from.
     * @param audioBuffer
     *            The buffer to read audio data to.
     * @param overlap
     *            The overlap: the number of elements that remain in the buffer
     *            after this method is finished.
     * 
     * @return True if the stream can deliver more data, false otherwise.
     * @throws IOException
     *             When something goes wrong while reading the stream. In
     *             particular, an IOException is thrown if the input stream has
     *             been closed.
     */
    public static boolean slideBuffer(AudioFloatInputStream audioFloatInputStream, float audioBuffer[],
            int overlap) throws IOException {
        assert overlap < audioBuffer.length;

        int slideSize = audioBuffer.length - overlap;

        for (int i = 0; i < slideSize; i++) {
            audioBuffer[i + overlap] = audioBuffer[i];
        }

        boolean hasMoreBytes = audioFloatInputStream.read(audioBuffer, 0, slideSize) != -1;
        return hasMoreBytes;
    }

    /**
     * Process one and only one buffer and return the pitch. Useful for
     * applications where multiple actions are taken on the same buffer.
     * 
     * @param buffer
     *            The audio information.
     * @return a pitch in Hz or -1 if no pitch is found.
     * @exception Error
     *                when the buffer has an incorrect length.
     */
    public static float processBuffer(float[] buffer, float sampleRate) {
        if (yinInstance == null)
            yinInstance = new Yin(sampleRate, buffer.length);

        if (buffer.length != yinInstance.inputBuffer.length)
            throw new Error("Buffer and yin buffer should have the same length!");

        for (int i = 0; i < buffer.length; i++)
            yinInstance.inputBuffer[i] = buffer[i];

        return yinInstance.getPitch();
    }

    /**
     * Stops real time annotation.
     */
    public static void stop() {
        if (yinInstance != null)
            yinInstance.running = false;
    }

    public static void main(String... args) throws UnsupportedAudioFileException, IOException {
        final SimplePlot p = new SimplePlot("Pitch tracking");
        Yin.processFile("../Tarsos/audio/pitch_check/flute.novib.mf.C5B5.wav", new DetectedPitchHandler() {
            @Override
            public void handleDetectedPitch(float time, float pitch) {
                System.out.println(time + "\t" + pitch);
                if (pitch == -1)
                    pitch = 0;
                p.addData(time, pitch);
            }
        });
        p.save();
    }

    public static DetectedPitchHandler PRINT_DETECTED_PITCH_HANDLER = new DetectedPitchHandler() {
        @Override
        public void handleDetectedPitch(float time, float pitch) {
            System.out.println(time + "\t" + pitch);
        }
    };
}
