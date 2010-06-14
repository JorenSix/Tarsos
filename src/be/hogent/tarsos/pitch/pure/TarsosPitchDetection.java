package be.hogent.tarsos.pitch.pure;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.UnsupportedAudioFileException;

import be.hogent.tarsos.apps.Tarsos;
import be.hogent.tarsos.pitch.PitchDetectionMode;
import be.hogent.tarsos.pitch.PitchDetector;
import be.hogent.tarsos.pitch.Sample;
import be.hogent.tarsos.util.AudioFile;
import be.hogent.tarsos.util.SimplePlot;
import be.hogent.tarsos.util.StopWatch;

import com.sun.media.sound.AudioFloatInputStream;

public final class TarsosPitchDetection implements PitchDetector {

    private static final Logger LOG = Logger.getLogger(TarsosPitchDetection.class.getName());

    private final AudioFile file;
    private final List<Sample> samples;

    private final PitchDetectionMode detectionMode;

    public TarsosPitchDetection(final AudioFile audioFile, final PitchDetectionMode pitchDetectionMode) {
        this.file = audioFile;
        this.samples = new ArrayList<Sample>();
        this.detectionMode = pitchDetectionMode;
    }


    @Override
    public void executePitchDetection() {
        try {
            processFile(file.path(), new DetectedPitchHandler() {
                @Override
                public void handleDetectedPitch(final float time, final float pitch) {
                    final long start = (long) (time * 1000);
                    final Sample s = pitch == -1 ? new Sample(start) : new Sample(start, pitch);
                    samples.add(s);
                }
            }, detectionMode);
        } catch (final UnsupportedAudioFileException e) {
            LOG.log(Level.SEVERE, "Unsupported audio file: " + file.basename() + " " + e.getMessage(), e);
        } catch (final IOException e) {
            LOG.log(Level.SEVERE, "Exception while reading audio file: " + file.basename() + " "
                    + e.getMessage(), e);
        }
    }

    @Override
    public String getName() {
        final String name;
        if (PitchDetectionMode.TARSOS_MPM == detectionMode) {
            name = "tarsos_mpm";
        } else {
            name = "tarsos_yin";
        }
        return name;
    }

    @Override
    public List<Sample> getSamples() {
        return samples;
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
    public static void processFile(final String fileName, final DetectedPitchHandler detectedPitchHandler,
            final PitchDetectionMode detectionMode)
    throws UnsupportedAudioFileException, IOException {
        final AudioInputStream ais = AudioSystem.getAudioInputStream(new File(fileName));
        final AudioFloatInputStream afis = AudioFloatInputStream.getInputStream(ais);
        processStream(afis, detectedPitchHandler,detectionMode);
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
    public static void processStream(final AudioFloatInputStream afis,
            final DetectedPitchHandler detectedPitchHandler, final PitchDetectionMode detectionMode)
    throws UnsupportedAudioFileException,
    IOException {
        final AudioFormat format = afis.getFormat();
        final float sampleRate = format.getSampleRate();
        final double frameSize = format.getFrameSize();
        final double frameRate = format.getFrameRate();
        float time = 0;
        // number of bytes / frameSize * frameRate gives the number of seconds
        // because we use float buffers there is a factor 2: 2 bytes per float?
        // Seems to be correct but a float uses 4 bytes: confused programmer is
        // confused.
        final float timeCalculationDivider = (float) (frameSize * frameRate / 2);
        long floatsProcessed = 0;

        final PurePitchDetector pureDetector;
        final int bufferSize;
        final int overlapSize;

        if (PitchDetectionMode.TARSOS_MPM == detectionMode) {
            pureDetector = new McLeodPitchMethod(sampleRate);
            bufferSize = McLeodPitchMethod.BUFFER_SIZE;
            overlapSize = McLeodPitchMethod.OVERLAP;
        } else {
            pureDetector = new Yin(sampleRate);
            bufferSize = Yin.BUFFER_SIZE;
            overlapSize = Yin.OVERLAP;
        }

        final int bufferStepSize = bufferSize - overlapSize;
        final float[] audioBuffer = new float[bufferSize];

        // read full buffer
        boolean hasMoreBytes = afis.read(audioBuffer, 0, bufferSize) != -1;
        floatsProcessed += audioBuffer.length;
        while (hasMoreBytes) {
            final float pitch = pureDetector.getPitch(audioBuffer);
            time = floatsProcessed / timeCalculationDivider;
            if (detectedPitchHandler != null) {
                detectedPitchHandler.handleDetectedPitch(time, pitch);
            }

            // slide buffer with predefined overlap
            hasMoreBytes = slideBuffer(afis, audioBuffer, overlapSize);

            floatsProcessed += bufferStepSize;
        }
    }

    /**
     * Slides a buffer with an overlap and reads new data from the stream. to
     * the correct place in the buffer. E.g. with a buffer size of 9 and overlap
     * of 3.
     * <pre>
     *      | 0 | 1 | 3 | 3 | 4  | 5  | 6  | 7  | 8  |
     *                        |
     *                Slide (9 - 3 = 6)
     *                        |
     *                        v
     *      | 6 | 7 | 8 | _ | _  | _  | _  | _  | _  |
     *                        |
     *        Fill from 3 to (3+6) exclusive
     *                        |
     *                        v
     *      | 6 | 7 | 8 | 9 | 10 | 11 | 12 | 13 | 14 |
     * </pre>
     * @param audioInputStream
     *            The stream to read audio data from.
     * @param audioBuffer
     *            The buffer to read audio data to. If consecutive buffers are
     *            read and an overlap is wanted it should contain the previous
     *            window.
     * @param overlap
     *            The overlap: the number of elements that are copied in the
     *            buffer from the previous buffer. Overlap should be smaller
     *            (strict) than the buffer size and can be zero.
     * @return True if the stream can deliver more data, false otherwise.
     * @throws IOException
     *             When something goes wrong while reading the stream. In
     *             particular, an IOException is thrown if the input stream has
     *             been closed.
     */
    public static boolean slideBuffer(final AudioFloatInputStream audioInputStream,
            final float[] audioBuffer, final int overlap) throws IOException {
        assert overlap < audioBuffer.length;

        final int bufferStepSize = audioBuffer.length - overlap;

        for (int i = 0; i < bufferStepSize; i++) {
            audioBuffer[i] = audioBuffer[i + overlap];
        }

        return audioInputStream.read(audioBuffer, overlap, bufferStepSize) != -1;
    }

    public static void main(final String... args) throws UnsupportedAudioFileException, IOException {
        final StopWatch start = new StopWatch();
        final SimplePlot p = new SimplePlot("Pitch tracking");
        processFile("../Tarsos/audio/pitch_check/flute.novib.mf.C5B5.wav", new DetectedPitchHandler() {
            @Override
            public void handleDetectedPitch(final float time, final float pitch) {
                Tarsos.println(time + "\t" + pitch);
                double plotPitch = pitch;
                if (plotPitch == -1) {
                    plotPitch = 0;
                }
                p.addData(time, plotPitch);
            }
        }, PitchDetectionMode.TARSOS_MPM);
        p.save();
        Tarsos.println(" " + start.ticksPassed() / 1000.0);
    }

    public static final DetectedPitchHandler PRINT_DETECTED_PITCH_HANDLER = new DetectedPitchHandler() {
        @Override
        public void handleDetectedPitch(final float time, final float pitch) {
            Tarsos.println(time + "\t" + pitch);
        }
    };
}
