package be.hogent.tarsos.apps;

/**
 * @author Joren Six
 */
public interface TarsosApplication {
    /**
     * @param args
     *            The arguments to start the program.
     */
    void run(final String... args);

    /**
     * @return The name of the parameter used to start the application.
     */
    String name();

    /**
     * @return The short description of the application. What purpose it serves.
     */
    String description();
}
