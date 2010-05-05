package be.hogent.tarsos.pitch;

import java.util.List;

/**
 * 
 * A PitchDetector is able to annotate a song with pitches.
 * 
 * @author Joren Six
 */
public interface PitchDetector {

	/**
	 * Execute the pitch detection process. Should take care of caching
	 * annotations: e.g. execute the annotation process once per song, write a
	 * CSV-file with annotations and read that file when needed
	 */
	public void executePitchDetection();

	/**
	 * @return a list of annotated samples
	 */
	public List<Sample> getSamples();

	/**
	 * @return the name of the detector possibly with parameters e.g. aubio_YIN
	 */
	public String getName();
}
