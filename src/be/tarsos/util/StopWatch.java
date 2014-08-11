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

package be.tarsos.util;

import java.util.Locale;

/**
 * StopWatch counts the ticks (ns) passed between initialization and invocation
 * of ticksPassed.
 * 
 * @author Joren Six
 */
public final class StopWatch {

	/**
	 * Number of ticks between start and stop in ns (10^-9 s).
	 */
	private transient long ticks;

	/**
	 * Create and start the stop watch.
	 */
	public StopWatch() {
		ticks = System.nanoTime();
	}

	/**
	 * @return The number of ticks passed between initialization and the call to
	 *         <code>ticksPassed</code>. In milliseconds or 10^-3 seconds.
	 */
	public long ticksPassed() {
		return (long) timePassed(TimeUnit.MILLISECONDS);
	}

	/**
	 * @return The number of ticks passed between initialization and the call to
	 *         <code>ticksPassed</code>. In nanoseconds or 10^-9 seconds.
	 */
	public long nanoTicksPassed() {
		return Math.abs(System.nanoTime() - ticks);
	}
	
	/**
	 * Calculates and returns the time passed in the requested unit.
	 * @param unit The requested time unit.
	 * @return The time passed in the requested unit.
	 */
	public double timePassed(TimeUnit unit){
		return unit.convert(nanoTicksPassed(), TimeUnit.NANOSECONDS);
	}

	/**
	 * Starts or restarts the watch.
	 */
	public void start() {
		ticks = System.nanoTime();
	}

	@Override
	public String toString() {
		return ticksPassed() + "ms";
	}

	public String formattedToString() {
		long ticksPassed = ticksPassed();
		long nanoTicksPassed = nanoTicksPassed();
		final String formatString;
		final double value;
		if (ticksPassed >= 1000) {
			formatString = "%.2f s";
			value = ticksPassed / 1000.0;
		} else if (ticksPassed >= 1) {
			formatString = "%.2f ms";
			value = ticksPassed;
		} else if (nanoTicksPassed >= 1000) {
			formatString = "%.2f µs";
			value = nanoTicksPassed / 1000.0;
		} else {
			formatString = "%.2f ns";
			value = nanoTicksPassed;
		}
		return String.format(Locale.US, formatString, value);
	}
}
