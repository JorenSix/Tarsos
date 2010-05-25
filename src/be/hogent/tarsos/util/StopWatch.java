package be.hogent.tarsos.util;

/**
 * StopWatch counts the ticks (ns) passed between initialization and invocation
 * of ticksPassed.
 * @author Joren Six
 */
public final class StopWatch {

    /**
     * Conversion factor between ns and ms.
     */
    private static final int NS_TO_MS = 1000;

    /**
     * Number of ticks between start and stop in ns (10^-9 s).
     */
    private final transient long ticks;

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
}
