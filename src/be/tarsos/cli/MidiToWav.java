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
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.sound.midi.InvalidMidiDataException;
import javax.sound.midi.MidiUnavailableException;

import joptsimple.OptionParser;
import joptsimple.OptionSet;
import joptsimple.OptionSpec;
import be.tarsos.midi.MidiToWavRenderer;
import be.tarsos.util.ScalaFile;

/**
 * @author Joren Six
 */
public final class MidiToWav extends AbstractTarsosApp {

    /**
     * Log messages.
     */
    private static final Logger LOG = Logger.getLogger(MidiToWav.class.getName());

    /*
     * (non-Javadoc)
     * @see be.tarsos.exp.cli.AbstractTarsosApp#description()
     */
    
    public String description() {
        return "Creates a WAV file using a scala file and a MIDI file as input. "
        + "This is usefull to create WAV-files in a certain tone scale.";
    }
    
    public String synopsis(){
		return "[option] ..."; 
	}

    /*
     * (non-Javadoc)
     * @see be.tarsos.exp.cli.AbstractTarsosApp#run(java.lang.String[])
     */
    
    public void run(final String... args) {
        File midiFile;
        File outFile;
        File sclFile;
        double[] tuning;

        final OptionParser parser = new OptionParser();

        final OptionSpec<File> midiFileSpec = parser.accepts("midi", "The input MIDI file.")
        .withRequiredArg()
        .ofType(File.class);
        final OptionSpec<File> sclFileSpec = parser.accepts("scala", "The scala file.").withRequiredArg()
        .ofType(File.class);

        final OptionSpec<File> outFileSpec = parser.accepts("out", "The outpu WAV file.").withRequiredArg()
        .ofType(File.class).defaultsTo(new File("out.wav"));

        final OptionSet options = parse(args, parser, this);

        if (!isHelpOptionSet(options) && options.has(midiFileSpec) && options.has(sclFileSpec)) {
            midiFile = options.valueOf(midiFileSpec);
            sclFile = options.valueOf(sclFileSpec);
            outFile = options.valueOf(outFileSpec);

            tuning = new ScalaFile(sclFile.getAbsolutePath()).getPitches();
            try {
                MidiToWavRenderer renderer;
                renderer = new MidiToWavRenderer();
                renderer.setTuning(tuning);
                renderer.createWavFile(midiFile, outFile);
            } catch (MidiUnavailableException e) {
                LOG.log(Level.SEVERE, "MIDI synth unavailable.", e);
            } catch (InvalidMidiDataException e) {
                LOG.log(Level.SEVERE, "Invalid MIDI data sent.", e);
            } catch (IOException e) {
                LOG.log(Level.SEVERE, "IOException while converting MIDI to WAV.", e);
            }
        } else {
            printHelp(parser);
        }
    }
}
