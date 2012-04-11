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

package be.hogent.tarsos.sampled;

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

import be.hogent.tarsos.dsp.AudioDispatcher;
import be.hogent.tarsos.dsp.AudioEvent;
import be.hogent.tarsos.dsp.AudioPlayer;
import be.hogent.tarsos.dsp.AudioProcessor;
import be.hogent.tarsos.dsp.GainProcessor;
import be.hogent.tarsos.dsp.WaveformSimilarityBasedOverlapAdd;
import be.hogent.tarsos.dsp.WaveformSimilarityBasedOverlapAdd.Parameters;

/**
 * Player plays audio. It allows adding AudioProcessor objects before the time
 * stretching step and consumers can register for the changes on the state
 * property.
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
	
	private final List<AudioProcessor> processorsBeforeTimeStretching;
	
	private double durationInSeconds;
	private double currentTime;
	private double startAt;
	
	private double gain;
	private double tempo;
	
	private Player(){
		state = PlayerState.NO_FILE_LOADED;
		gain = 1.0;
		tempo = 1.0;
		processorsBeforeTimeStretching = new ArrayList<AudioProcessor>();
	}	
	
	public void load(File file) {
		if(state != PlayerState.NO_FILE_LOADED){
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
	
	public void eject(){
		loadedFile = null;
		stop();
		setState(PlayerState.NO_FILE_LOADED);
	}
	
	public void play(){
		checkIfFileIsLoaded();
		play(startAt);
	}
	
	private void checkIfFileIsLoaded(){
		if(state == PlayerState.NO_FILE_LOADED){
			throw new IllegalStateException("Can not play when no file is loaded");
		}
	}
	
	public void play(double startTime) {
		checkIfFileIsLoaded();
		try {
			AudioFileFormat fileFormat = AudioSystem.getAudioFileFormat(loadedFile);
			AudioFormat format = fileFormat.getFormat();
			
			gainProcessor = new GainProcessor(gain);
			audioPlayer = new AudioPlayer(format);		
			wsola = new WaveformSimilarityBasedOverlapAdd(Parameters.slowdownDefaults(tempo,format.getSampleRate()));
			
			dispatcher = AudioDispatcher.fromFile(loadedFile,wsola.getInputBufferSize(),wsola.getOverlap());
			
			wsola.setDispatcher(dispatcher);
			dispatcher.skip(startTime);
			
			dispatcher.addAudioProcessor(this);
			for(AudioProcessor processor : processorsBeforeTimeStretching){
				dispatcher.addAudioProcessor(processor);
			}
			dispatcher.addAudioProcessor(wsola);
			dispatcher.addAudioProcessor(gainProcessor);
			dispatcher.addAudioProcessor(audioPlayer);

			Thread t = new Thread(dispatcher,"Audio Player Thread");
			t.start();
			setState(PlayerState.PLAYING);
		} catch (UnsupportedAudioFileException e) {
			throw new Error(e);
		} catch (IOException e) {
			throw new Error(e);
		} catch (LineUnavailableException e) {
			throw new Error(e);
		}
		
	}
	
	
	public void pauze(){
		pauze(currentTime);
	}
	
	public void pauze(double pauzeAt) {
		checkIfFileIsLoaded();
		startAt=pauzeAt;
		if(state == PlayerState.PLAYING ){
			//set the state first to stop
			setState(PlayerState.STOPPED);
			//then stop the dispatcher:
			//  to prevent recursive call in processingFinished
			dispatcher.stop();
		} else if(state != PlayerState.STOPPED){
			throw new IllegalStateException("Can not stop when nothing is playing");
		}
	}
	
	public void stop(){
		pauze(0);
	}
	
	public void setGain(double newGain){
		double oldGain = gain;
		gain = newGain;
		if(state == PlayerState.PLAYING ){
			gainProcessor.setGain(gain);
		}
		if(oldGain != newGain){
			support.firePropertyChange("gain", oldGain, newGain);
			LOG.fine(String.format("Changed gain for audio player to: %.2f", newGain));
		}
	}
	
	public void setTempo(double newTempo){
		double oldTempo = tempo;
		tempo = newTempo;
		if(state == PlayerState.PLAYING ){
			wsola.setParameters(Parameters.slowdownDefaults(tempo,dispatcher.getFormat().getSampleRate()));
		}
		if(oldTempo != newTempo){
			support.firePropertyChange("tempo", oldTempo, newTempo);
			LOG.fine(String.format("Changed tempo for audio player to: %.2f", newTempo));
		}
	}
	
	private void setState(PlayerState newState){
		PlayerState oldState = state;
		state = newState;
		support.firePropertyChange("state", oldState, newState);
		LOG.info("Changed player state from " + oldState + " to " + newState);;
	}
	
	public double getDurationInSeconds() {
		checkIfFileIsLoaded();
		return durationInSeconds;
	}
	
	public PlayerState getState() {
		return state;
	}
	
	public double getStartAt() {
		return startAt;
	}

	
	public void addProcessorBeforeTimeStrechting(AudioProcessor processor){
		processorsBeforeTimeStretching.add(processor);
	}
	
	public boolean removeProcessorBeforeTimeStretching(AudioProcessor processor){
		return processorsBeforeTimeStretching.remove(processor);
	}

	public void addPropertyChangeListener(PropertyChangeListener l) {
		support.addPropertyChangeListener(l);
	}

	public void removePropertyChangeListener(PropertyChangeListener l) {
		support.removePropertyChangeListener(l);
	}
	
	public boolean process(AudioEvent audioEvent) {
		currentTime = audioEvent.getTimeStamp();
		return true;
	}

	public void processingFinished() {
		if(state == PlayerState.PLAYING){
			stop();
		}
	}
	
	private static Player instance;
	public static Player getInstance(){
		if(instance == null){
			instance = new Player();
		}
		return instance;
	}

	/**
	 * Increase the gain by a defined percentage.
	 * @param percent to increase the gain.
	 */
	public void increaseGain(double percent) {
		double newGain = gain + percent;
		if(newGain >= 0 && newGain <= 2.0){
			setGain(newGain);
		}
	}

	public void increaseTempo(double percent) {
		double newTempo = tempo + percent;
		if(newTempo >= 0 && newTempo <= 3.0){
			setTempo(newTempo);
		}
	}
}
