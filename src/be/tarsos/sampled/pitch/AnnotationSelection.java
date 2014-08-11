/*
*              _______                      
*             |__   __|                     
*                | | __ _ _ __ ___  ___  ___
*                | |/ _` | '__/ __|/ _ \/ __| 
*                | | (_| | |  \__ \ (_) \__ \    
*                |_|\__,_|_|  |___/\___/|___/    
*                                                         
* -----------------------------------------------------------
*
*  Tarsos is developed by Joren Six at 
*  The School of Arts,
*  University College Ghent,
*  Hoogpoort 64, 9000 Ghent - Belgium
*  
* -----------------------------------------------------------
*
*  Info: http://tarsos.0110.be
*  Github: https://github.com/JorenSix/Tarsos
*  Releases: http://tarsos.0110.be/releases/Tarsos/
*  
*  Tarsos includes some source code by various authors,
*  for credits and info, see README.
* 
*/

package be.tarsos.sampled.pitch;

/**
 * Represents the current selection of annotations.
 * 
 */
public final class AnnotationSelection {

	private double startPitch, stopPitch;
	private double startTime, stopTime;
	private double minProbability;

	private static final double MINIMUM_PITCH = 0;
	private static final double MAXIMUM_PITCH = 10000000;
	private static final double MINIMUM_TIME = 0;
	private static final double MAXIMUM_TIME = 10000000;
	private static final double DEFAULT_MIN_PROBABILITY = 0.0;
	public static final double MAX_PROBABILITY = 1.0;

	public AnnotationSelection() {
		setSelection(MINIMUM_TIME, MAXIMUM_TIME, MINIMUM_PITCH, MAXIMUM_PITCH, DEFAULT_MIN_PROBABILITY);
	}
	
	public AnnotationSelection(AnnotationSelection selectionToCopy){
		setSelection(selectionToCopy.getStartTime(), selectionToCopy.getStopTime(), selectionToCopy.getStartPitch(), selectionToCopy.getStopPitch(), selectionToCopy.getMinProbability());
	}

	public void setSelection(final double newStartTime, final double newStopTime, final double newStartPitch,
			final double newStopPitch, final double newMinProbability) {
		assert stopTime >= startTime;
		assert stopPitch >= startPitch;

		assert startTime >= MINIMUM_TIME;
		assert stopTime >= MINIMUM_TIME;
		assert startTime <= MAXIMUM_TIME;
		assert stopTime <= MAXIMUM_TIME;

		assert minProbability <= MAX_PROBABILITY : String.format(
				"%s should be smaller than max probability %s .", minProbability, MAX_PROBABILITY);
		
		minProbability = newMinProbability;
		startTime = newStartTime;
		stopTime = newStopTime;
		startPitch = newStartPitch;
		stopPitch = newStopPitch;
	}

	public void setSelection(final double newStartTime, final double newStopTime, final double newStartPitch,
			final double newStopPitch) {
		setSelection(newStartTime, newStopTime, newStartPitch, newStopPitch, minProbability);
	}

	public void setTimeSelection(final double newStartTime, final double newStopTime) {
		setSelection(newStartTime, newStopTime, startPitch, stopPitch, minProbability);
	}

	public void setMinProbability(final double newMinProbability) {
		setSelection(startTime, stopTime, startPitch, stopPitch, newMinProbability);
	}

	public void setPitchSelection(final double newStartPitch, final double newStopPitch) {
		setSelection(startTime, stopTime, newStartPitch, newStopPitch, minProbability);
	}

	public double getStartPitch() {
		return startPitch;
	}

	public double getStopPitch() {
		return stopPitch;
	}

	public double getStartTime() {
		return startTime;
	}

	public double getStopTime() {
		return stopTime;
	}

	public double getMinProbability() {
		return minProbability;
	}

	public double getTimeSpan() {
		return stopTime - startTime;
	}
	
	public String toString() {
		return String
				.format("Selection in time %ss-%ss, in pitch %s-%s, in probability %s-1",
						startTime, stopTime, startPitch, stopPitch,
						minProbability);
	}

}
