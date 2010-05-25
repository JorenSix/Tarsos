package be.hogent.tarsos.apps;

import java.io.IOException;
import java.util.Arrays;
import java.util.logging.Level;
import java.util.logging.Logger;

import joptsimple.OptionException;
import joptsimple.OptionParser;
import joptsimple.OptionSet;

/**
 * @author Joren Six
 */
public abstract class AbstractTarsosApp {

    /**
     * Log messages.
     */
    private static final Logger LOG = Logger.getLogger(AbstractTarsosApp.class.getName());

    /**
     * @param args
     *            The arguments to start the program.
     */
    public void run(final String... args) {

    }

    /**
     * @return The name of the parameter used to start the application.
     */
    abstract String name();

    /**
     * @return The short description of the application. What purpose it serves.
     */
    abstract String description();

    /**
     * Parses arguments, adds and checks for help option an prints command line
     * help for an application.
     * @param args
     *            The command line arguments (options).
     * @param parser
     *            The argument parser.
     * @param application
     *            The application that needs the parameters.
     * @return null if the arguments could not be parsed by parser. An OptionSet
     *         otherwise.
     */
    protected final OptionSet parse(final String[] args, final OptionParser parser,
            final AbstractTarsosApp application) {
        Tarsos instance = Tarsos.getInstance();
        parser.acceptsAll(Arrays.asList("h", "?", "help"), "Show help");
        OptionSet options = null;
        try {
            options = parser.parse(args);
        } catch (OptionException e) {
            final String message = e.getMessage();
            instance.print(message);
            instance.print("");
            printHelp(parser);
        }
        return options;
    }

    /**
     * Checks if the OptionSet contains the help argument.
     * @param options
     *            The options to check.
     * @return True if options is null or options contain help.
     */
    protected final boolean isHelpOptionSet(final OptionSet options) {
        return options == null || options.has("help");
    }

    /**
     * Prints command line help for an application.
     * @param parser
     *            The command line argument parser.
     */
    protected final void printHelp(final OptionParser parser) {
        Tarsos instance = Tarsos.getInstance();
        instance.print("Application description");
        instance.print("-----------------------");
        instance.print(description());
        instance.print("");
        try {
            parser.printHelpOn(System.out);
        } catch (IOException e1) {
            LOG.log(Level.SEVERE, "Could not print to STD OUT. How quaint.", e1);
        }
    }

}
