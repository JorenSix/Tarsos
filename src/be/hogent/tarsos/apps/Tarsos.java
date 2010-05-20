package be.hogent.tarsos.apps;

import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import joptsimple.OptionException;
import joptsimple.OptionParser;
import joptsimple.OptionSet;
import be.hogent.tarsos.util.Configuration;

/**
 * This is the starting point of the Tarsos application suite. It's main task is
 * to start other applications and to maintain state. Creating the needed
 * directories, checking the runtime,... and also passing, minimal parsing and
 * checking arguments are tasks for this class.
 * @author Joren Six
 */
public final class Tarsos {

    /**
     * Logs messages.
     */
    private static final Logger LOG = Logger.getLogger(Tarsos.class.getName());

    /**
     * A map of applications, maps the name of the application to the instance.
     */
    private final transient Map<String, TarsosApplication> applications;

    /**
     * Create a new Tarsos application instance.
     */
    private Tarsos() {
        applications = new HashMap<String, TarsosApplication>();
        Configuration.createRequiredDirectories();
        // default confiuration
        // logging configuration, ...
        // create needed directories, ...
    }

    /**
     * Register a Tarsos application.
     * @param name
     *            The name (parameter) of the Tarsos application. The parameter
     *            is used in <code>java -jar tarsos.jar application</code>.
     * @param application
     *            The instance that represents the Tarsos application.
     */
    public void registerApplication(final String name, final TarsosApplication application) {
        applications.put(name, application);
        LOG.fine("Registering " + name);
    }

    /**
     * @param arguments
     *            The arguments for the program.
     */
    public void run(final String... arguments) {
        if (arguments.length == 0) {
            print("java -jar tarsos.jar subcommand [subcommand options]");
        } else {
            final String subcommand = arguments[0];
            String[] subcommandArgs;
            if (arguments.length > 1) {
                subcommandArgs = Arrays.copyOfRange(arguments, 1, arguments.length);
            } else {
                subcommandArgs = new String[0];
            }
            if (applications.containsKey(subcommand)) {
                applications.get(subcommand).run(subcommandArgs);
            } else {
                print("Unknown subcommand. Valid subcommands:");
                for (String key : applications.keySet()) {
                    print("\t" + key);
                }
            }
        }
    }

    /**
     * The only instance of Tarsos.
     */
    private static Tarsos tarsosInstance;

    /**
     * Thread safe singleton implementation.
     * @return The only instance of the Tarsos application.
     */
    public static Tarsos getInstance() {
        synchronized (Tarsos.class) {
            if (tarsosInstance == null) {
                tarsosInstance = new Tarsos();
            }
        }

        return tarsosInstance;
    }

    /**
     * @param args
     *            The arguments consist of a subcommand and options for the
     *            subcommand. E.g.
     *            <pre>
     * java -jar annotate --in blaat.wav
     * java -jar annotate -i blaat.wav
     * </pre>
     */
    public static void main(final String... args) {
        final Tarsos instance = Tarsos.getInstance();

        final List<TarsosApplication> applicationList = new ArrayList<TarsosApplication>();
        applicationList.add(new Annotate());
        applicationList.add(new PitchTable());
        applicationList.add(new MidiToWav());
        applicationList.add(new AudioToScala());

        for (TarsosApplication application : applicationList) {
            instance.registerApplication(application.name(), application);
        }

        instance.run(args);
    }

    /**
     * Prints information to standard out.
     * @param info
     *            the information to print.
     */
    private void print(final String info) {
        final PrintStream standardOut = System.out;
        standardOut.println(info);
    }

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
    public static OptionSet parse(final String[] args, final OptionParser parser,
            final TarsosApplication application) {
        parser.acceptsAll(Arrays.asList("h", "?", "help"), "Show help");
        OptionSet options = null;
        try {
            options = parser.parse(args);
        } catch (OptionException e) {
            final String message = e.getMessage();
            tarsosInstance.print(message);
            tarsosInstance.print("");
            printHelp(parser, application);
        }
        return options;
    }

    /**
     * Checks if the OptionSet contains the help argument.
     * @param options
     *            The options to check.
     * @return True if options is null or options contain help.
     */
    public static boolean isHelpOptionSet(final OptionSet options) {
        return options == null || options.has("help");
    }

    /**
     * Prints command line help for an application.
     * @param parser
     *            The command line argument parser.
     * @param application
     *            The application.
     */
    public static void printHelp(final OptionParser parser, final TarsosApplication application) {
        Tarsos instance = Tarsos.getInstance();
        instance.print("Application description");
        instance.print("-----------------------");
        instance.print(application.description());
        instance.print("");
        try {
            parser.printHelpOn(System.out);
        } catch (IOException e1) {
            LOG.log(Level.SEVERE, "Could not print to STD OUT. How quaint.", e1);
        }
    }
}
