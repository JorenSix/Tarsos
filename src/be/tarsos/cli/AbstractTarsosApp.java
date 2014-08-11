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

package be.tarsos.cli;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.logging.Level;
import java.util.logging.Logger;

import joptsimple.OptionException;
import joptsimple.OptionParser;
import joptsimple.OptionSet;
import joptsimple.OptionSpec;
import be.tarsos.Tarsos;
import be.tarsos.sampled.pitch.PitchDetectionMode;
import be.tarsos.util.FileUtils;
import be.tarsos.util.StringUtils;

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
    public abstract void run(final String... args);

    /**
     * The name of the application is based on the class name. If the class is called PitchToMidi the name
     * is pitch_to_midi.
     * @return The name of the parameter used to start the application.
     */
    public final String name(){
    	//fully qualified name
    	String name = this.getClass().getCanonicalName();
    	//unqualified name
    	name = name.substring(name.lastIndexOf('.')+1);
    	//lower case first letter
    	name = name.substring(0,1).toLowerCase() + name.substring(1);
    	//split on upper case
    	String[] parts = name.split("(?=\\p{Upper})");
    	//lower case every part
    	for(int i = 0; i < parts.length ; i++){
    		parts[i] = parts[i].toLowerCase();
    	}
    	//join with parts with an underscore
    	return StringUtils.join(Arrays.asList(parts), "_");
    }

    /**
     * @return The short description of the application. What purpose it serves.
     */
    public abstract String description();
    
    /**
     * The synopsis is a short description of the required or optional arguments.
     * @return The command line synopsis. E.g. [options] input_file output_file. Is printed as 'java -jar tarsos.jar name [options] input_file output_file'."
     */
    public String synopsis(){return null;}


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
        parser.acceptsAll(Arrays.asList("h", "?", "help"), "Show help");
        OptionSet options = null;
        try {
            options = parser.parse(args);
        } catch (final OptionException e) {
            final String message = e.getMessage();
            Tarsos.println(message);
            Tarsos.println("");
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
     * An argument is a 
     * @param argument
     */
    protected final boolean isValidAudioFileOrDirectory(String argument){
    	return FileUtils.exists(argument) && (FileUtils.isDirectory(argument) || FileUtils.isAudioFile(new File(argument)));
    }

    /**
     * Creates an optionspec for a pitch detector.
     * @param parser
     *            The parser to add an option to.
     * @return An OptionSpec with a correct name and a clear description.
     */
    protected final OptionSpec<PitchDetectionMode> createDetectionModeSpec(final OptionParser parser) {
        final StringBuilder names = new StringBuilder();
        for (final PitchDetectionMode modes : PitchDetectionMode.values()) {
            names.append(modes.name()).append(" | ");
        }
        final String descr = "The detector to use [" + names.toString() + "]";
        return parser.accepts("detector", descr
        ).withRequiredArg().ofType(PitchDetectionMode.class).defaultsTo(
                PitchDetectionMode.TARSOS_YIN);
    }

    /**
     * Prints command line help for an application.
     * @param parser
     *            The command line argument parser.
     */
	protected final void printHelp(final OptionParser parser) {
		Tarsos.printTarsosAsciiArt();
		Tarsos.printSeparator();
		Tarsos.println("");
		Tarsos.println("Application description");
		Tarsos.printSeparator();
		Tarsos.println(description());
		Tarsos.println("");
        
        if(synopsis() != null){
        	Tarsos.println("Synopsis");
        	Tarsos.printSeparator();
        	Tarsos.println("java -jar tarsos.jar " + name() + " " + synopsis());
        	Tarsos.println("");
        }        
        
        try {
            parser.printHelpOn(System.out);
        } catch (final IOException e1) {
            LOG.log(Level.SEVERE, "Could not print to STD OUT. How quaint.", e1);
        }
    }
    
    protected final void printError(final OptionParser parser,String message){
    	printHelp(parser);
    	Tarsos.println("");
    	Tarsos.println("Check your command line arguments");
    	Tarsos.printSeparator();
    	Tarsos.println("There is something wrong with your command line argumens. Please check the following:");
    	Tarsos.println("");
    	Tarsos.println(message);
    	Tarsos.println("For more info, execute\\n  java -jar tarsos.jar " + name() + " --help");
    }
}
