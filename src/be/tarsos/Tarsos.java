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

package be.tarsos;

import java.awt.Color;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.Logger;

import javax.swing.JOptionPane;
import javax.swing.UIManager;

import be.tarsos.cli.AbstractTarsosApp;
import be.tarsos.cli.Annotate;
import be.tarsos.cli.AnnotationSynth;
import be.tarsos.cli.AudioToScala;
import be.tarsos.cli.DetectPitch;
import be.tarsos.cli.HistogramToScala;
import be.tarsos.cli.MidiToWav;
import be.tarsos.cli.PitchHistogramRating;
import be.tarsos.cli.PitchTable;
import be.tarsos.cli.PitchToHistogram;
import be.tarsos.cli.PitchToMidi;
import be.tarsos.cli.PowerExtractor;
import be.tarsos.cli.PrintScalaIntervals;
import be.tarsos.cli.Rank;
import be.tarsos.cli.TimbreAndScaleRelation;
import be.tarsos.cli.TuneMidiSynth;
import be.tarsos.ui.TarsosFrame;
import be.tarsos.util.ConfKey;
import be.tarsos.util.Configuration;
import be.tarsos.util.FileUtils;

/**
 * This is the starting point of the Tarsos application suite. It's main task is
 * to start other applications and to maintain state. Creating the needed
 * directories, checking the runtime, configure logging... also passing, minimal
 * parsing and checking arguments are tasks for this class.
 * 
 * @author Joren Six
 */
public final class Tarsos {

	/**
	 * A map of applications, maps the name of the application to the instance.
	 */
	private final transient Map<String, AbstractTarsosApp> applications;

	/**
	 * Properties file that defines the logging behavior.
	 */
	private static final String LOG_PROPS = "/be/tarsos/util/logging.properties";

	/**
	 * A reconfigurable logger.
	 */
	private Logger log;

	/**
	 * Create a new Tarsos application instance.
	 */
	private Tarsos() {
		// Configure logging.
		configureLogging();
		//Check configuration and write default values
		Configuration.checkForConfigurationAndWriteDefaults();
		// Configure directories.
		Tarsos.configureDirectories(log);
		// Initialize the CLI application list.
		applications = new HashMap<String, AbstractTarsosApp>();
	}

	/**
	 * Checks the configured directories and creates them if they are not present.
	 * @param log The logger to report to.
	 */
	public static void configureDirectories(Logger log) {
		// replace java.io.tmpdir with actual temp dir.
		for (final ConfKey confKey : ConfKey.values()) {
			String directory = Configuration.get(confKey);
			if (directory.contains("java.io.tmpdir")) {
				directory = directory.replace("java.io.tmpdir", FileUtils.temporaryDirectory());
				Configuration.set(confKey, directory);
			}
		}

		// if the runtime directory is writable use it as base directory
		// otherwise use the temporary directory
		final String baseDirectory;
		if (new File(FileUtils.runtimeDirectory()).canWrite()) {
			baseDirectory = FileUtils.runtimeDirectory();
		} else {
			baseDirectory = FileUtils.temporaryDirectory();
		}
		// Check and create required directories. If relative use baseDirectory.
		for (final ConfKey confKey : ConfKey.values()) {
			if (confKey.isRequiredDirectory()) {
				String directory = Configuration.get(confKey);
				if (!new File(directory).isAbsolute()) {
					directory = FileUtils.combine(baseDirectory, directory);
				}
				if (FileUtils.mkdirs(directory)) {
					log.info("Created directory: " + Configuration.get(confKey));
				}
				// Check if the directory is writable
				if (!new File(directory).canWrite()) {
					String message = "Required directory " + directory + " is not writable!\n Please configure another directory for '" + confKey + "'.";
					log.severe(message);
					JOptionPane.showMessageDialog(null, message, "Directory error", JOptionPane.ERROR_MESSAGE);
					//System.exit(-1);
				}
				if(FileUtils.exists(directory)){
					log.fine(String.format("%s files in %s", new File(directory).list().length, directory));
				}
			}
		}
	}

	/**
	 * Configure the logging facility.
	 */
	private void configureLogging() {
		// a default (not configured) logger
		log = Logger.getLogger(Tarsos.class.getName());
		try {
			final InputStream stream = Tarsos.class.getResourceAsStream(LOG_PROPS);
			LogManager.getLogManager().readConfiguration(stream);
			// a configured logger
			log = Logger.getLogger(Tarsos.class.getName());
		} catch (final SecurityException e) {
			log.log(Level.SEVERE, e.getLocalizedMessage(), e);
		} catch (final IOException e) {
			log.log(Level.SEVERE, e.getLocalizedMessage(), e);
		}
	}

	/**
	 * Colors used by ptplot.
	 */
	public static final Color[] COLORS = { 
			new Color(0xff0000), // red
			new Color(0x0000ff), // blue
			new Color(0x00aaaa), // cyan-ish
			new Color(0x000000), // black
			new Color(0xffa500), // orange
			new Color(0x53868b), // cadetblue4
			new Color(0xff7f50), // coral
			new Color(0x45ab1f), // dark green-ish
			new Color(0x90422d), // sienna-ish
			new Color(0xa0a0a0), // grey-ish
			new Color(0x14ff14), // green-ish
			new Color(0xcc02de), // purple
			new Color(0x6e4272), // dark purple
			new Color(0x552209), // brown
	};

	/**
	 * Register a Tarsos application.
	 * 
	 * @param name
	 *            The name (parameter) of the Tarsos application. The parameter
	 *            is used in <code>java -jar tarsos.jar application</code>.
	 * @param application
	 *            The instance that represents the Tarsos application.
	 */
	private void registerApplication(final String name, final AbstractTarsosApp application) {
		applications.put(name, application);
	}

	/**
	 * @param arguments
	 *            The arguments for the program.
	 */
	private void run(final String... arguments) {
		if (arguments.length == 0) {
			startUserInterface();
		} else {
			startCommandLineApplication(arguments);
		}
	}

	private void startUserInterface() {
		// Schedule a job for the event-dispatching thread:
		// creating and showing this application's GUI.
		javax.swing.SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				try {
					if(Tarsos.isMac()){
						//System.setProperty("apple.laf.useScreenMenuBar", "true");
						//System.setProperty("com.apple.mrj.application.apple.menu.about.name", "Tarsos");
					}
					UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
				} catch (Exception e) {
					log.log(Level.WARNING, "Unable to set system L&F, continue with default L&F", e);
				}
				final TarsosFrame tarsosFrame = TarsosFrame.getInstance();
				tarsosFrame.setVisible(true);
			}
		});

	}
	
	public static boolean isMac(){		 
		String os = System.getProperty("os.name").toLowerCase();
		//Mac
	    return os.indexOf("mac") >= 0; 
	}
	
	public static boolean isWindows(){		 
		String os = System.getProperty("os.name").toLowerCase();
		//Mac
		return os.indexOf("windows") >= 0; 
	}

	/**
	 * Starts a command line application.
	 * 
	 * @param arguments
	 *            The command line arguments.
	 */
	private void startCommandLineApplication(final String... arguments) {

		registerApplications();

		final String subcommand = arguments[0];
		String[] subcommandArgs;
		if (arguments.length > 1) {
			subcommandArgs = new String[arguments.length - 1];
			for (int i = 1; i < arguments.length; i++) {
				subcommandArgs[i - 1] = arguments[i];
			}
		} else {
			subcommandArgs = new String[0];
		}
		if (applications.containsKey(subcommand)) {
			applications.get(subcommand).run(subcommandArgs);
		} else {
			 printTarsosAsciiArt();
			 printSeparator();
			 
			print("Unknown subcommand. Valid subcommands:");
			for (final String key : applications.keySet()) {
				print("\t" + key);
			}
		}
	}
	
	public static void printTarsosAsciiArt(){
		Tarsos.println("   _______                           ");
		Tarsos.println("  |__   __|                          ");
		Tarsos.println("     | | __ _ _ __ ___  ___  ___     ");
		Tarsos.println("     | |/ _` | '__/ __|/ _ \\/ __| ");
		Tarsos.println("     | | (_| | |  \\__ \\ (_) \\__ \\");
		Tarsos.println("     |_|\\__,_|_|  |___/\\___/|___/  ");
		Tarsos.println("");
	}
	
	public static void printSeparator(){
		Tarsos.println("-------------------------------------");
	}

	/**
	 * Registers a list of Tarsos CLI applications.
	 */
	private void registerApplications() {
		final List<AbstractTarsosApp> applicationList = new ArrayList<AbstractTarsosApp>();
		applicationList.add(new Annotate());
		applicationList.add(new PitchTable());
		applicationList.add(new MidiToWav());
		applicationList.add(new AudioToScala());
		applicationList.add(new DetectPitch());
		applicationList.add(new AnnotationSynth());
		applicationList.add(new PowerExtractor());
		applicationList.add(new TuneMidiSynth());
		applicationList.add(new Rank());
		applicationList.add(new PitchToMidi());
		applicationList.add(new PitchToHistogram());
		applicationList.add(new HistogramToScala());
		applicationList.add(new TimbreAndScaleRelation());
		applicationList.add(new PitchHistogramRating());
		applicationList.add(new PrintScalaIntervals());
		
		for (final AbstractTarsosApp application : applicationList) {
			registerApplication(application.name(), application);
		}
	}

	/**
	 * The only instance of Tarsos.
	 */
	private static Tarsos tarsosInstance;

	/**
	 * Thread safe singleton implementation.
	 * 
	 * @return The only instance of the Tarsos application.
	 */
	private static Tarsos getInstance() {
		synchronized (Tarsos.class) {
			if (tarsosInstance == null) {
				tarsosInstance = new Tarsos();
			}
		}
		return tarsosInstance;
	}

	/**
	 * Prints information to standard out.
	 * 
	 * @param info
	 *            the information to print.
	 */
	private void print(final String info) {
		final PrintStream standardOut = System.out;
		standardOut.println(info);
	}

	/**
	 * Starts Tarsos. A command line application is started when command line
	 * arguments are present, otherwise the UI is used.
	 * 
	 * @param args
	 *            The arguments consist of a subcommand and options for the
	 *            subcommand. E.g.
	 * 
	 *            <pre>
	 * java -jar annotate --in blaat.wav
	 * java -jar annotate -bufferCount blaat.wav
	 * </pre>
	 */
	public static void main(final String... args) {
		final Tarsos instance = Tarsos.getInstance();
		instance.run(args);
	}

	/**
	 * Prints info to a stream (console).
	 * 
	 * @param info
	 *            The information to print.
	 */
	public static void println(final String info) {
		getInstance().print(info);
	}
}
