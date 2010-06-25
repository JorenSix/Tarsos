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
/**
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
    private final float[] audioFloatBuffer;

    /**
     * This buffer is reused again and again to store audio data using the byte
     * data type.
     */
    private final byte[] audioByteBuffer;

    /**
     * The line to send sound to. Is also used to keep everything in sync.
     */
    private SourceDataLine line;
    /**
     * A list of registered audio processors. The audio processors are
     * responsible for actually doing the digital signal processing
     */
    private final List<AudioProcessor> audioProcessors;

    private final boolean realTime;

    final AudioFloatConverter converter;

    /**
     * The floatOverlap: the number of elements that are copied in the buffer
     * from the previous buffer. Overlap should be smaller (strict) than the
     * buffer size and can be zero.
     */
    private final int floatOverlap, floatStepSize;

    private final int byteOverlap, byteStepSize;

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
        this(AudioSystem.getAudioInputStream(new File(fileName)), audioBufferSize, 0, true);
    }

    public RealTimeAudioProcessor(final AudioInputStream stream, final int audioBufferSize,
            final int bufferOverlap, final boolean play)
    throws UnsupportedAudioFileException, IOException, LineUnavailableException {


        audioProcessors = new ArrayList<AudioProcessor>();
        audioInputStream = stream;
        realTime = play;

        final AudioFormat format = audioInputStream.getFormat();
        final int numberOfBytesForFloat = format.getFrameSize();

        converter = AudioFloatConverter.getConverter(format);

        audioFloatBuffer = new float[audioBufferSize];
        floatOverlap = bufferOverlap;
        floatStepSize = audioFloatBuffer.length - floatOverlap;

        audioByteBuffer = new byte[audioFloatBuffer.length * numberOfBytesForFloat];
        byteOverlap = floatOverlap * numberOfBytesForFloat;
        byteStepSize = floatStepSize * numberOfBytesForFloat;

        if (play) {
            final DataLine.Info info = new DataLine.Info(SourceDataLine.class, format);
            line = (SourceDataLine) AudioSystem.getLine(info);
            line.open();
            line.start();
        }
    }

    /**
     * Adds an AudioProcessor to the list of subscribers.
     * @param audioProcessor
     */
    public void addAudioProcessor(final AudioProcessor audioProcessor) {
        audioProcessors.add(audioProcessor);
        LOG.fine("Added an audioprocessor to the list of processors: " + audioProcessor.toString());
    }

    @Override
    public void run() {
        try {
            int bytesRead;

            // read and play (if wanted) the first full buffer.
            bytesRead = audioInputStream.read(audioByteBuffer);
            converter.toFloatArray(audioByteBuffer, audioFloatBuffer);
            if (realTime) {
                // converter.toFloatArray(in_buff, out_buff, out_offset,
                // out_len)

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
            }
            while (bytesRead != -1) {
                for (final AudioProcessor processor : audioProcessors) {
                    processor.proccess(audioFloatBuffer);
                }

                bytesRead = slideBuffer();
                if (realTime) {
                    line.write(audioByteBuffer, byteOverlap, byteStepSize);
                }
            }
            for (final AudioProcessor processor : audioProcessors) {
                processor.processingFinished();
            }
            line.close();
        } catch (final IOException e) {
            LOG.log(Level.SEVERE, "Error while reading data from audio stream.", e);
        }
    }

    /**
     * Slides a buffer with an floatOverlap and reads new data from the stream.
     * to the correct place in the buffer. E.g. with a buffer size of 9 and
     * floatOverlap of 3.
     * 
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
     * 
     * @return The number of bytes read.
     * @throws IOException
     *             When something goes wrong while reading the stream. In
     *             particular, an IOException is thrown if the input stream has
     *             been closed.
     */
    private int slideBuffer() throws IOException {
        assert floatOverlap < audioFloatBuffer.length;

        for (int i = 0; i < floatOverlap; i++) {
            audioFloatBuffer[i] = audioFloatBuffer[i + floatStepSize];
        }

        final int bytesRead = audioInputStream.read(audioByteBuffer, byteOverlap, byteStepSize);
        converter.toFloatArray(audioByteBuffer, byteOverlap, audioFloatBuffer, floatOverlap,
                floatStepSize);

        return bytesRead;
    }

    /**
     * AudioProcessors are responsible for actually doing the digital signal
     * processing. The interface is simple: a buffer with some floats.
     * @author Joren Six
     */
    public interface AudioProcessor {
        /**
         * Do the actual signal processing on a buffer.
         * 
         * @param audioFloatBuffer
         *            The buffer containing the audio information using floats.
         */
        void proccess(final float[] audioBuffer);

        /**
         * Notify the AudioProcessor that no more data is available.
         */
        void processingFinished();
    }
}
