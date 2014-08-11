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

package be.tarsos.util.histogram.peaks;

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
