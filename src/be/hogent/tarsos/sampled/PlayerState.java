package be.hogent.tarsos.sampled;

/**
 * Defines the state of the audio player.
 * @author Joren Six
 */
public enum PlayerState{
	/**
	 * No file is loaded.
	 */
	NO_FILE_LOADED,
	/**
	 * The file is playing
	 */
	PLAYING,
	/**
	 * Audio play back is stopped. 
	 */
	STOPPED
}