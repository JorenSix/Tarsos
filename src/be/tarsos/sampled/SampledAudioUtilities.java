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

package be.tarsos.sampled;

import java.util.Vector;
import java.util.logging.Logger;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.Mixer;
import javax.sound.sampled.Mixer.Info;
import javax.sound.sampled.SourceDataLine;

import be.tarsos.util.ConfKey;
import be.tarsos.util.Configuration;

public final class SampledAudioUtilities {

	private SampledAudioUtilities() {
	}

	private static final Logger LOG = Logger
			.getLogger(SampledAudioUtilities.class.getName());

	/**
	 * Creates a list of Mixer.Info objects. 
	 * @param supportsPlayback Should the mixer support audio play back?
	 * @param supportsRecording Should the mixer support audio recording?
	 * @return A list of mixers that fulfill the given conditions. 
	 */
	public static Vector<Mixer.Info> getMixerInfo(
			final boolean supportsPlayback, final boolean supportsRecording) {
		final Vector<Mixer.Info> infos = new Vector<Mixer.Info>();
		final Mixer.Info[] mixers = AudioSystem.getMixerInfo();
		for (final Info mixerinfo : mixers) {
			if (supportsRecording
					&& AudioSystem.getMixer(mixerinfo).getTargetLineInfo().length != 0) {
				// Mixer capable of recording audio if target line length != 0
				infos.add(mixerinfo);
			} else if (supportsPlayback
					&& AudioSystem.getMixer(mixerinfo).getSourceLineInfo().length != 0) {
				// Mixer capable of audio play back if source line length != 0
				infos.add(mixerinfo);
			}
		}
		return infos;
	}

	/**
	 * Tries to open a line to the configured mixer. If the line can not be
	 * opened and started it falls back on the default line. If this does not
	 * work a LineUnavailableException is thrown.
	 * 
	 * @param format
	 *            the format to open the line for.
	 * @return An open and started line.
	 * @throws LineUnavailableException
	 *             if the configured mixer and the default mixer are not
	 *             available.
	 */
	public static SourceDataLine getOpenLineFromConfiguredMixer(
			final AudioFormat format) throws LineUnavailableException {
		checkMixerOutputDeviceIndex();
		final DataLine.Info info = new DataLine.Info(SourceDataLine.class,format);
		final int outputDeviceIndex = Configuration.getInt(ConfKey.mixer_output_device);
		final Mixer.Info mixer = SampledAudioUtilities.getMixerInfo(true, false).get(outputDeviceIndex);
		SourceDataLine line = (SourceDataLine) AudioSystem.getMixer(mixer)
				.getLine(info);
		try {
			line.open();
			line.start();
		} catch (LineUnavailableException e) {
			LOG.warning("Could not open line on mixer '" + mixer.getName()
					+ "' let's try again on the default mixer: "
					+ e.getMessage());
			line = (SourceDataLine) AudioSystem.getLine(info);
			line.open();
			line.start();
		}
		return line;
	}

	/**
	 * Checks the mixer output device index. If it is out of bounds it is reset to a default value.
	 */
	private static void checkMixerOutputDeviceIndex() {
		int outputDeviceIndex = Configuration.getInt(ConfKey.mixer_output_device);
		int defaultOutputDeviceIndex = 0;
		Vector<Info> mixers = SampledAudioUtilities.getMixerInfo(true, false);
		if(outputDeviceIndex < 0 || mixers.size() <= outputDeviceIndex){
			Configuration.set(ConfKey.mixer_output_device,defaultOutputDeviceIndex);
			LOG.warning("Ignored configured mixer output device index (" + outputDeviceIndex + "), reset to " + defaultOutputDeviceIndex);
		}
	}
}
