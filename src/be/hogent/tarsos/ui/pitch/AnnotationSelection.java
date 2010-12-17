package be.hogent.tarsos.ui.pitch;

/**
 * Represents the current selection of annotations.
 * 
 */
public final class AnnotationSelection {

	private double startPitch, stopPitch;
	private double startTime, stopTime;

	private static final double MINIMUM_PITCH = 0;
	private static final double MAXIMUM_PITCH = 10000000;
	private static final double MINIMUM_TIME = 0;
	private static final double MAXIMUM_TIME = 10000000;

	protected AnnotationSelection() {
		setSelection(MINIMUM_TIME, MAXIMUM_TIME, MINIMUM_PITCH, MAXIMUM_PITCH);
	}

	public void setSelection(final double newStartTime, final double newStopTime, final double newStartPitch,
			final double newStopPitch) {
		assert stopTime >= startTime;
		assert stopPitch >= startPitch;

		assert startTime >= MINIMUM_TIME;
		assert stopTime >= MINIMUM_TIME;
		assert startTime <= MAXIMUM_TIME;
		assert stopTime <= MAXIMUM_TIME;

		startTime = newStartTime;
		stopTime = newStopTime;
		startPitch = newStartPitch;
		stopPitch = newStopPitch;
	}

	public void setTimeSelection(final double newStartTime, final double newStopTime) {
		setSelection(newStartTime, newStopTime, startPitch, stopPitch);
	}

	public void setPitchSelection(final double newStartPitch, final double newStopPitch) {
		setSelection(startTime, stopTime, newStartPitch, newStopPitch);
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

	public double getTimeSpan() {
		return stopTime - startTime;
	}

}
