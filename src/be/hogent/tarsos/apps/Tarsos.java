package be.hogent.tarsos.apps;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

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
    private final transient Map<String, AbstractTarsosApp> applications;

    /**
     * Create a new Tarsos application instance.
     */
    private Tarsos() {
        applications = new HashMap<String, AbstractTarsosApp>();
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
    public void registerApplication(final String name, final AbstractTarsosApp application) {
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

        final List<AbstractTarsosApp> applicationList = new ArrayList<AbstractTarsosApp>();
        applicationList.add(new Annotate());
        applicationList.add(new PitchTable());
        applicationList.add(new MidiToWav());
        applicationList.add(new AudioToScala());

        for (AbstractTarsosApp application : applicationList) {
            instance.registerApplication(application.name(), application);
        }

        instance.run(args);
    }

    /**
     * Prints information to standard out.
     * @param info
     *            the information to print.
     */
    void print(final String info) {
        final PrintStream standardOut = System.out;
        standardOut.println(info);
    }
}
