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
import java.io.PrintStream;

import joptsimple.OptionParser;
import joptsimple.OptionSet;
import joptsimple.OptionSpec;
import be.tarsos.sampled.pitch.Pitch;
import be.tarsos.sampled.pitch.PitchUnit;
import be.tarsos.util.ConfKey;
import be.tarsos.util.Configuration;
import be.tarsos.util.FileUtils;
import be.tarsos.util.ScalaFile;

/**
 * Generates a table with pitches in various units.
 * @author Joren Six
 */
public final class PitchTable extends AbstractTarsosApp {

    /*
     * (non-Javadoc)
     * @see be.tarsos.exp.cli.AbstractTarsosApp#run(java.lang.String[])
     */
    
    public void run(final String... args) {
        final OptionParser parser = new OptionParser();
        final OptionSpec<Double> refFrequencySpec =  parser.accepts("ref-frequency", "The reference frequency to calculate absolute cents, defaults to C-1 with A4 tuned to 440Hz (8.176Hz)."
        ).withRequiredArg().ofType(Double.class).defaultsTo(8.175798915643707);
        final OptionSpec<File> scalaFileSpec =  parser.accepts("scala", "A scala file to print values for, by default the western tone scale is used."
        ).withRequiredArg().ofType(File.class);
        
        final OptionSet options = parse(args, parser, this);
        
        if (isHelpOptionSet(options)) {
            printHelp(parser);
        } else {
        	//change and restore configuration.
        	ConfKey key = ConfKey.absolute_cents_reference_frequency;
        	double tempRefFreq = Configuration.getDouble(key);
        	Configuration.set(key, options.valueOf(refFrequencySpec));
        	ScalaFile scalaFile=null;
        	if(options.valueOf(scalaFileSpec)==null){
        		scalaFile = ScalaFile.westernTuning();
        	}else{
        		String scalaFilePath = options.valueOf(scalaFileSpec).getAbsolutePath();
        		if(!FileUtils.exists(scalaFilePath)){
        			printError(parser, "The scala file was not found. Please provide an existing scala file and not " + scalaFilePath);
        		} else {
        			scalaFile = new ScalaFile(scalaFilePath);
        		}
        	}   
        	printTable(scalaFile);	
            Configuration.set(key, tempRefFreq);
        }
    }
    
    private void printTable(ScalaFile scalaFile) {
    	final PrintStream stdOut = System.out;
    	 stdOut.printf("%4s %10s %16s %14s %15s %10s\n", "MIDICENT", "NAME", "FREQUENCY", "ABS CENTS","REL CENTS", "OCTAVE");
         stdOut.println("---------------------------------------------------------------------------");
         
         boolean noNames = scalaFile.getPitchNames() == null;
         
         for(int i = 0; i < 10 ; i ++){
        	 for(int j = 0 ; j < scalaFile.getPitches().length ; j++){
	        	 final Pitch pitch = Pitch.getInstance(PitchUnit.ABSOLUTE_CENTS, i * 1200 + scalaFile.getPitches()[j]);
	             final double frequency = pitch.getPitch(PitchUnit.HERTZ);
	             final double absoluteCents = pitch.getPitch(PitchUnit.ABSOLUTE_CENTS);
	             final double relativeCents = pitch.getPitch(PitchUnit.RELATIVE_CENTS);
	             final double midiCents = pitch.getPitch(PitchUnit.MIDI_CENT);
	             
	             stdOut.printf("%8.2f %15s %14.4fHz %14.0f  %14.0f %10d\n", midiCents, noNames ? "" : scalaFile.getPitchNames()[j] , frequency,
	                     absoluteCents, relativeCents, i - 1);
        	 }
         }
    }

    /*
     * (non-Javadoc)
     * @see be.tarsos.exp.cli.AbstractTarsosApp#description()
     */
    
    public String description() {
        return "Prints a table with the pitch classes of a certain tone scale in different units for 10 octaves. There is an option to define the reference frequency on which the cent values are based.";
    }
    
    public String synopsis(){
    	return "[--scala scala_file.scl] [--ref-frequency x]";
    }
}
