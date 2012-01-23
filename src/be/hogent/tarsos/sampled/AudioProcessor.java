/**
*
*  Tarsos is developed by Joren Six at 
*  The Royal Academy of Fine Arts & Royal Conservatory,
*  University College Ghent,
*  Hoogpoort 64, 9000 Ghent - Belgium
*
**/
package be.hogent.tarsos.sampled;

/**
 * AudioProcessors are responsible for actually doing digital signal processing.
 * The interface is simple: a buffer with some floats and the same information
 * in raw bytes.
 * @author Joren Six
 */
public interface AudioProcessor {

    /**
     * Process the first (complete) buffer. Once the first complete buffer is
     * processed the remaining buffers are overlapping buffers and processed
     * using the processOverlapping method (Even if overlap is zero).
     * @param audioFloatBuffer
     *            The buffer to process using the float data type.
     * @param audioByteBuffer
     *            The buffer to process using raw bytes.
     */
    void processFull(final float[] audioFloatBuffer, final byte[] audioByteBuffer);

    /**
     * Do the actual signal processing on an overlapping buffer. Once the
     * first complete buffer is processed the remaining buffers are
     * overlapping buffers and are processed using the processOverlapping
     * method. Even if overlap is zero.
     * @param audioFloatBuffer
     *            The buffer to process using the float data type.
     * @param audioByteBuffer
     *            The buffer to process using raw bytes.
     */
    void processOverlapping(final float[] audioFloatBuffer, final byte[] audioByteBuffer);

    /**
     * Notify the AudioProcessor that no more data is available and processing
     * has finished. Can be used to deallocate resources or cleanup.
     */
    void processingFinished();
}
