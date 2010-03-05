package be.hogent.tarsos.util;

import java.util.Date;

/**
 * @author Joren Six
 * StopWatch counts the ticks passed between
 * initialization and invocation of ticksPassed.
 */
public class StopWatch {
	private long ticks;
	
	public StopWatch(){
		ticks = new Date().getTime();
	}
	
	/**
	 * @return The number of ticks passed between initialization
	 * and the call to <code>ticksPassed</code>
	 */
	public long ticksPassed(){
		return new Date().getTime() - ticks;
	}
}
