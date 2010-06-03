package be.hogent.tarsos.util;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.SourceDataLine;
import javax.sound.sampled.UnsupportedAudioFileException;

import com.sun.media.sound.AudioFloatConverter;

/**
 * This class plays a file and sends float arrays to registered AudioProcessor
 * implementors in sync. E.g. Real time audio visualization can leverage this
 * Behavior.
 * @author Joren Six
 */
public final class RealTimeAudioProcessor implements Runnable {

    /**
     * Log messages.
     */
    private static final Logger LOG = Logger.getLogger(RealTimeAudioProcessor.class.getName());


    /**
     * The audio stream (in bytes), conversion to float happens at the last
     * moment.
     */
    private final AudioInputStream audioInputStream;
    /**
     * This buffer is reused again and again to store audio data using the float
     * data type.
     */
    private final float[] audioBuffer;
    /**
     * The line to send sound to. Is also used to keep everything in sync.
     */
    private final SourceDataLine line;
    /**
     * A list of registered audio processors. The audio processors are
     * responsible for actually doing the digital signal processing
     */
    private final List<AudioProcessor> audioProcessors;

    /**
     * Initialize the processor using a file and a size.
     * @param fileName
     *            The name of the file to process. It should be a readable
     *            supported audio file.
     * @param audioBufferSize
     *            Defines the number of floats used in the audio buffer. Floats,
     *            not bytes.
     * @throws UnsupportedAudioFileException
     *             When the audio file is not supported (transcoding beforehand
     *             is an advised).
     * @throws IOException
     *             When the audio file is not readable.
     * @throws LineUnavailableException
     *             When the output line is not available.
     */
    public RealTimeAudioProcessor(final String fileName, final int audioBufferSize)
    throws UnsupportedAudioFileException, IOException,
    LineUnavailableException {
        audioBuffer = new float[audioBufferSize];
        audioProcessors = new ArrayList<AudioProcessor>();

        audioInputStream = AudioSystem.getAudioInputStream(new File(fileName));
        final AudioFormat format = audioInputStream.getFormat();
        final DataLine.Info info = new DataLine.Info(SourceDataLine.class, format);
        line = (SourceDataLine) AudioSystem.getLine(info);
        line.open();
        line.start();
    }

    /**
     * Adds an AudioProcessor to the list of subscribers.
     * @param audioProcessor
     */
    public void addAudioProcessor(final AudioProcessor audioProcessor) {
        audioProcessors.add(audioProcessor);
    }

    @Override
    public void run() {
        try {
            final AudioFormat format = audioInputStream.getFormat();
            final AudioFloatConverter converter = AudioFloatConverter.getConverter(format);
            // 4 bytes for a float:
            final byte[] audioByteBuffer = new byte[audioBuffer.length * 2 * 2];
            int bytesRead;
            bytesRead = audioInputStream.read(audioByteBuffer);
            while (bytesRead != -1) {
                converter.toFloatArray(audioByteBuffer, audioBuffer);
                // The variable line is the Java Sound object that actually
                // makes the sound.
                // The write method on line is interesting because it blocks
                // until it is ready for more data, which in effect keeps this
                // loop in sync with what you are hearing.
                // The AudioProcessors are responsible for actually doing the
                // digital signal processing. They should be able to operate in
                // real time or process the signal on a separate thread.
                // Source:
                // JavaFX™ Special Effects
                // Taking Java™ RIA to the Extreme with Animation, Multimedia,
                // and Game Element
                // Chapter 9 page 185
                line.write(audioByteBuffer, 0, audioByteBuffer.length);
                for (final AudioProcessor processor : audioProcessors) {
                    processor.proccess(audioBuffer);
                }
                bytesRead = audioInputStream.read(audioByteBuffer);
            }
        } catch (final IOException e) {
            LOG.log(Level.SEVERE, "Error while reading data from audio stream.", e);
        }
    }

    /**
     * AudioProcessors are responsible for actually doing the digital signal
     * processing. The interface is simple: a buffer with some floats.
     * @author Joren Six
     */
    public interface AudioProcessor {
        /**
         * Do the actual signal processing on a buffer.
         * @param audioBuffer
         *            The buffer containing the audio information using floats.
         */
        void proccess(final float[] audioBuffer);
    }
}
