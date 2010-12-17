package be.hogent.tarsos.util;

import java.util.Locale;

/**
 * StopWatch counts the ticks (ns) passed between initialization and invocation
 * of ticksPassed.
 * 
 * @author Joren Six
 */
public final class StopWatch {

	/**
	 * Conversion factor between ns and ms.
	 */
	private static final int NS_TO_MS = 1000000;

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
		return Math.abs((System.nanoTime() - ticks) / NS_TO_MS);
	}

	/**
	 * @return The number of ticks passed between initialization and the call to
	 *         <code>ticksPassed</code>. In nanoseconds or 10^-9 seconds.
	 */
	public long nanoTicksPassed() {
		return Math.abs(System.nanoTime() - ticks);
	}

	/**
	 * Starts or restarts the stop watch.
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
