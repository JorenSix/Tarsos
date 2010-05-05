package be.hogent.tarsos.util;

import java.util.Date;

/**
 * StopWatch counts the ticks passed between initialization and invocation of
 * ticksPassed.
 * 
 * @author Joren Six
 */
public class StopWatch {
    private final long ticks;

    public StopWatch() {
        ticks = new Date().getTime();
    }

    /**
     * @return The number of ticks passed between initialization and the call to
     *         <code>ticksPassed</code>
     */
    public long ticksPassed() {
        return new Date().getTime() - ticks;
    }
}
