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
import java.util.ArrayList;
import java.util.List;

import joptsimple.OptionParser;
import joptsimple.OptionSet;
import joptsimple.OptionSpec;
import be.tarsos.transcoder.ffmpeg.EncoderException;
import be.tarsos.util.AudioFile;
import be.tarsos.util.SignalPowerExtractor;

/**
 * Extracts power from a file.
 * 
 * @author Joren Six
 */
public final class PowerExtractor extends AbstractTarsosApp {

	/**
	 * The default silence level used in the power plot.
	 */
	private static final int SILENCELEVEL = -40;

	@Override
	public String description() {
		return "Extracts power features from one or more files.";
	}

	@Override
	public void run(final String... args) {
		final OptionParser parser = new OptionParser();

		final OptionSpec<File> inputSpec = parser.accepts("in", "Input audio file(s).").withRequiredArg()
				.ofType(File.class).withValuesSeparatedBy(' ');

		final OptionSet options = parse(args, parser, this);

		if (isHelpOptionSet(options)) {
			printHelp(parser);
		} else {
			final List<AudioFile> audioFiles = new ArrayList<AudioFile>();
			for (final File inputFile : options.valuesOf(inputSpec)) {
				try {
					audioFiles.add(new AudioFile(inputFile.getAbsolutePath()));
				} catch (EncoderException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
			SignalPowerExtractor spex;
			for (final AudioFile file : audioFiles) {
				spex = new SignalPowerExtractor(file);
				spex.savePowerPlot("power_" + file.originalBasename() + ".png", SILENCELEVEL);
				spex.saveTextFile("power_" + file.originalBasename() + ".txt", true);
				// spex.saveWaveFormPlot("waveform_" + file.basename() +
				// ".png");
			}
		}
	}
}
