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
/*
*
*  Tarsos is developed by Joren Six at 
*  The Royal Academy of Fine Arts & Royal Conservatory,
*  University College Ghent,
*  Hoogpoort 64, 9000 Ghent - Belgium
*  
*  http://tarsos.0110.be/tag/TarsosDSP
*
*/

package be.tarsos.sampled;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import javax.sound.sampled.AudioFileFormat;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.UnsupportedAudioFileException;

import be.tarsos.dsp.AudioDispatcher;
import be.tarsos.dsp.AudioEvent;
import be.tarsos.dsp.AudioProcessor;
import be.tarsos.dsp.GainProcessor;
import be.tarsos.dsp.StopAudioProcessor;
import be.tarsos.dsp.WaveformSimilarityBasedOverlapAdd;
import be.tarsos.dsp.WaveformSimilarityBasedOverlapAdd.Parameters;
import be.tarsos.dsp.io.jvm.AudioDispatcherFactory;
import be.tarsos.dsp.io.jvm.AudioPlayer;

/**
 * Player plays audio. 
 * 
 * When the state of the audio player changes a property change is fired on the 'state' property. 
 * 
 * The player allows time stretching and volume change. Adding AudioProcessor objects before the time
 * stretching step can implement any audio processing step.
 * 
 * @author Joren Six
 */
public class Player implements AudioProcessor {

	/**
	 * Logs messages.
	 */
	private static final Logger LOG = Logger.getLogger(Player.class.getName());

	private PropertyChangeSupport support = new PropertyChangeSupport(this);

	private PlayerState state;
	private File loadedFile;
	private GainProcessor gainProcessor;
	private AudioPlayer audioPlayer;
	private WaveformSimilarityBasedOverlapAdd wsola;
	private AudioDispatcher dispatcher;
	private StopAudioProcessor stopAudioProcessor;

	private final List<AudioProcessor> processorsBeforeTimeStretching;

	private double durationInSeconds;
	private double currentTime;
	private double startAt;

	private double gain;
	private double tempo;

	/**
	 * Creates a new player with no file loaded and a default gain and tempo. By
	 * default tempo and gain are unaffected (100%).
	 */
	private Player() {
		state = PlayerState.NO_FILE_LOADED;
		gain = 1.0;
		tempo = 1.0;
		processorsBeforeTimeStretching = new ArrayList<AudioProcessor>();
		stopAudioProcessor = new StopAudioProcessor(Double.MAX_VALUE);
	}

	/**
	 * At this point the sound that is played stops automatically.
	 * 
	 * @param stopAt
	 *            The time at which the player stops (in seconds).
	 */
	public void setStopAt(double stopAt) {
		if(stopAt > durationInSeconds)
			stopAt = durationInSeconds;
		
		if (stopAudioProcessor == null) {
			stopAudioProcessor = new StopAudioProcessor(stopAt);
		} else {
			stopAudioProcessor.setStopTime(stopAt);
		}
		LOG.info(String.format("Will stop playback at: %.2f seconds.", stopAt));
	}

	/**
	 * Returns the time at which the audio play back stops automatically.
	 * 
	 * @return The time at which the audio play back stops automatically.
	 */
	public double getStopAt() {
		return stopAudioProcessor.getStopAt();
	}

	/**
	 * Loads a new audio file. Throws an error if the audio format is not
	 * recognized.
	 * 
	 * @param file
	 *            The audio file to load.
	 */
	public void load(File file) {
		if (state != PlayerState.NO_FILE_LOADED) {
			eject();
		}
		loadedFile = file;
		AudioFileFormat fileFormat;
		try {
			fileFormat = AudioSystem.getAudioFileFormat(loadedFile);
		} catch (UnsupportedAudioFileException e) {
			throw new Error(e);
		} catch (IOException e) {
			throw new Error(e);
		}
		AudioFormat format = fileFormat.getFormat();
		durationInSeconds = fileFormat.getFrameLength() / format.getFrameRate();
		startAt = 0;
		currentTime = 0;
		setState(PlayerState.STOPPED);
	}

	/**
	 * Ejects the currently loaded file.
	 */
	public void eject() {
		loadedFile = null;
		stop();
		setState(PlayerState.NO_FILE_LOADED);
	}

	/**
	 * Plays the currently loaded file. Play back is started at the time of last
	 * If no file is loaded thow a illegal state exception.
	 */
	public void play() {
		checkIfFileIsLoaded();
		play(startAt);
	}

	private void checkIfFileIsLoaded() {
		if (state == PlayerState.NO_FILE_LOADED) {
			throw new IllegalStateException(
					"Can not play when no file is loaded");
		}
	}

	public void play(double startTime) {
		checkIfFileIsLoaded();

		try {
			AudioFileFormat fileFormat = AudioSystem
					.getAudioFileFormat(loadedFile);
			AudioFormat format = fileFormat.getFormat();

			gainProcessor = new GainProcessor(gain);
			audioPlayer = new AudioPlayer(format);
			wsola = new WaveformSimilarityBasedOverlapAdd(
					Parameters.slowdownDefaults(tempo, format.getSampleRate()));

			dispatcher = AudioDispatcherFactory.fromFile(loadedFile,
					wsola.getInputBufferSize(), wsola.getOverlap());

			wsola.setDispatcher(dispatcher);
			dispatcher.skip(startTime);

			

			dispatcher.addAudioProcessor(this);

			for (AudioProcessor processor : processorsBeforeTimeStretching) {
				dispatcher.addAudioProcessor(processor);
			}
			dispatcher.addAudioProcessor(stopAudioProcessor);
			dispatcher.addAudioProcessor(wsola);
			dispatcher.addAudioProcessor(gainProcessor);
			dispatcher.addAudioProcessor(audioPlayer);

			Thread t = new Thread(dispatcher, "Audio Player Thread");
			t.start();
			
			LOG.info(String.format("Started playback from: %.2f seconds.",startTime));
			setState(PlayerState.PLAYING);
		} catch (UnsupportedAudioFileException e) {
			throw new Error(e);
		} catch (IOException e) {
			throw new Error(e);
		} catch (LineUnavailableException e) {
			throw new Error(e);
		}

	}

	/**
	 * Stops the play back at current time.
	 */
	public void pauze() {
		pauze(currentTime);
	}

	/**
	 * Stops the play back. When play is called, play back is resumed at the
	 * indicated time.
	 * 
	 * @param pauzeAt
	 *            The time at which to resume playing after calling play (in
	 *            seconds).
	 */
	public void pauze(double pauzeAt) {
		checkIfFileIsLoaded();
		startAt = pauzeAt;
		if (state == PlayerState.PLAYING) {
			// set the state first to stop
			setState(PlayerState.STOPPED);
			// then stop the dispatcher:
			// to prevent recursive call in processingFinished
			dispatcher.stop();
		} else if (state != PlayerState.STOPPED) {
			throw new IllegalStateException(
					"Can not stop when nothing is playing");
		}
	}

	/**
	 * Stops the play back. When play is called, play back is resumed at 0. This
	 * is the same as pauze(0).
	 */
	public void stop() {
		pauze(0);
	}

	/**
	 * Set the gain for the current stream. 1 is no change, 0.8 is 80% of the
	 * volume and 2 double (watch out for clipping when using values > 1). If no
	 * stream is playing the next stream gets this gain.
	 * 
	 * @param newGain
	 *            The gain to apply to the current stream, 1 is no change, 0.8
	 *            is 80% of the volume and 2 double (watch out for clipping when
	 *            using values > 1)
	 */
	public void setGain(double newGain) {
		double oldGain = gain;
		gain = newGain;
		if (state == PlayerState.PLAYING) {
			gainProcessor.setGain(gain);
		}
		if (oldGain != newGain) {
			support.firePropertyChange("gain", oldGain, newGain);
			LOG.fine(String.format("Changed gain for audio player to: %.2f",
					newGain));
		}
	}

	/**
	 * Set the tempo for the current stream. 1 is no change, 2 is double tempo, 0.5 half. If no stream is playing the next stream gets this new tempo assigned.
	 * @param newTempo  The new tempo for the current stream. 1 is no change, 2 is double tempo, 0.5 half.
	 */
	public void setTempo(double newTempo) {
		double oldTempo = tempo;
		tempo = newTempo;
		if (state == PlayerState.PLAYING) {
			wsola.setParameters(Parameters.slowdownDefaults(tempo, dispatcher
					.getFormat().getSampleRate()));
		}
		if (oldTempo != newTempo) {
			support.firePropertyChange("tempo", oldTempo, newTempo);
			LOG.fine(String.format("Changed tempo for audio player to: %.2f",
					newTempo));
		}
	}

	/**
	 * Set the new state and fire the property change handler.
	 * @param newState
	 */
	private void setState(PlayerState newState) {
		PlayerState oldState = state;
		state = newState;
		support.firePropertyChange("state", oldState, newState);
		LOG.info("Changed player state from " + oldState + " to " + newState);
	}

	/**
	 * If no file is loaded a illegal state exception is fired.
	 * @return the duration in seconds of the loaded stream.
	 */
	public double getDurationInSeconds() {
		checkIfFileIsLoaded();
		return durationInSeconds;
	}

	/**
	 * @return The current state of the player. 
	 */
	public PlayerState getState() {
		return state;
	}

	/**
	 * @return at which time the stream will start, in seconds, when play() is called, or at which time the current stream started. 
	 */
	public double getStartAt() {
		return startAt;

	}

	/**
	 * 
	 * @return The time in seconds of currently played buffer, or when the last
	 *         stream stopped.
	 */
	public double getCurrentTime() {
		return currentTime;
	}

	/**
	 * Adds an audio processor to the chain before time stretching kicks in.
	 * @param processor the processor to add.
	 */
	public void addProcessorBeforeTimeStrechting(AudioProcessor processor) {
		processorsBeforeTimeStretching.add(processor);
	}

	/**
	 * Removes an audio processor to the chain before time stretching kicks in.
	 * @param processor the processor to remove.
	 * @return True if the specified processor is removed, false otherwise. 
	 */
	public boolean removeProcessorBeforeTimeStretching(AudioProcessor processor) {
		return processorsBeforeTimeStretching.remove(processor);
	}

	/**
	 * Adds a change listener.
	 * @param l The listener to add.
	 */
	public void addPropertyChangeListener(PropertyChangeListener l) {
		support.addPropertyChangeListener(l);
	}

	/**
	 * Removes a change listener.
	 * @param l The listener to remove.
	 */
	public void removePropertyChangeListener(PropertyChangeListener l) {
		support.removePropertyChangeListener(l);
	}

	public boolean process(AudioEvent audioEvent) {
		currentTime = audioEvent.getTimeStamp();
		return true;
	}

	public void processingFinished() {
		if (state == PlayerState.PLAYING) {
			stop();
		}
	}

	private static Player instance;
	/**
	 * @return the player instance. Singleton design pattern.
	 */
	public static Player getInstance() {
		if (instance == null) {
			instance = new Player();
		}
		return instance;
	}

	/**
	 * Increase the gain by a defined percentage. Negative percentages can be used.
	 * 
	 * @param percent
	 *            To increase the gain.
	 */
	public void increaseGain(double percent) {
		double newGain = gain + percent;
		if (newGain >= 0 && newGain <= 2.0) {
			setGain(newGain);
		}
	}

	/**
	 * Increase the tempo by a defined percentage. Negative percentages can be used.
	 * 
	 * @param percent
	 *            To increase the tempo.
	 */
	public void increaseTempo(double percent) {
		double newTempo = tempo + percent;
		if (newTempo >= 0 && newTempo <= 3.0) {
			setTempo(newTempo);
		}
	}

}
