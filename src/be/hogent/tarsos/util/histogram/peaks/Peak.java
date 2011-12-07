package be.hogent.tarsos.util.histogram.peaks;

/**
 * Defines a Peak in a histogram.
 * 
 * @author Joren Six
 */
public final class Peak implements Comparable<Peak> {
	/**
	 * The position in cents
	 */
	private double position;
	/**
	 * The peak height
	 */
	private double height;

	/**
	 * Creates a new peak.
	 * 
	 * @param pos
	 *            the position in cents
	 * @param peakHeight
	 *            the height of the peak
	 */
	public Peak(final double pos, final double peakHeight) {
		this.position = pos;
		this.height = peakHeight;
	}

	/**
	 * The height of the peak (number of occurrences).
	 * 
	 * @return the height of the peak
	 */
	public double getHeight() {
		return height;
	}

	/**
	 * The position of the peak in cents.
	 * 
	 * @return The position of the peak in cents
	 */
	public double getPosition() {
		return position;
	}

	/**
	 * @param newPosition
	 */
	public void setPosition(final double newPosition) {
		this.position = newPosition;

	}

	/**
	 * 
	 * @param newHeight
	 */
	public void setHeight(final double newHeight) {
		this.height = newHeight;
	}

	
	public int compareTo(Peak o) {
		return Double.valueOf(height).compareTo(o.height);
	}

}
