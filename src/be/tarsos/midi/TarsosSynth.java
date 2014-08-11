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

package be.tarsos.midi;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Vector;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.sound.midi.Instrument;
import javax.sound.midi.InvalidMidiDataException;
import javax.sound.midi.MidiChannel;
import javax.sound.midi.MidiDevice;
import javax.sound.midi.MidiEvent;
import javax.sound.midi.MidiSystem;
import javax.sound.midi.MidiUnavailableException;
import javax.sound.midi.Patch;
import javax.sound.midi.Receiver;
import javax.sound.midi.ShortMessage;
import javax.sound.midi.Synthesizer;

import ptolemy.kernel.util.InvalidStateException;
import be.tarsos.midi.MidiCommon.MoreMidiInfo;
import be.tarsos.sampled.pitch.PitchUnit;
import be.tarsos.util.ConfKey;
import be.tarsos.util.Configuration;
import be.tarsos.util.Configuration.ConfigChangeListener;

/**
 * A singleton that interfaces with a real underlying synthesizer, it acts as an interface that distributes
 * MIDI messages.
 * 
 * It sends pitch bends and note on events on one channel (PITCH_BEND_MIDI_CHANNEL) and MIDI tuning dumps on an other channel
 * (TUNED_MIDI_CHANNEL). This is to keep them from interfering with each other.
 * 
 * This class also listens to changes in the configured synthesizer or instrument. 
 */
public class TarsosSynth implements ConfigChangeListener {
	
	private static final Logger LOG = Logger.getLogger(TarsosSynth.class.getName());

	/**
	 * The octave used to send relative pitch messages: If you want to hear 0
	 * cents this translates to a C in the 4th octave: C4. But as we all know,
	 * you should not play with C4.
	 */
	private static final int BASE_OCTAVE = 4;
	
	/**
	 * 1200 = the number of cents in an octave.
	 */
	private static final int CENTS_IN_OCTAVE = 1200;

	/**
	 * 127 = the maximum MIDI velocity.
	 */
	private static final int MAX_VELOCITY = 127;
	
	/**
	 * 127 is the max midi key.
	 */
	private static final int MAX_MIDI_KEY = 127;

	/**
	 * Send note off after 250ms.
	 */
	private static final int NOTE_OFF_AFTER = 250;

	/**
	 * The receiver used to send messages to. The receiver acts as a
	 * "MIDI cable" and sends the received messages trough.
	 */
	private ReceiverSink receiver;
	
	/**
	 * The synthesizer device used to render the MIDI, it is connected to the receiver.
	 */
	private final List<MidiDevice> synthDevices;

	
	/**
	 * The channel used to tune a keyboard and send messages.
	 */
	public static final int TUNED_MIDI_CHANNEL = 1;
	
	
	/**
	 * The channel used send pitch bend messages and note on/off events.
	 */
	private static final int PITCH_BEND_MIDI_CHANNEL = 0;
	
	/**
	 * Listen to configuration changes.
	 */
	public TarsosSynth(){
		synthDevices = new ArrayList<MidiDevice>();
		Configuration.addListener(this);
	}
	
	public void setDefaultReceiver(){
				
		try {
			final MoreMidiInfo synthInfo = MidiCommon.listDevices(false,true).get(0);
			MidiDevice synthDevice = MidiSystem.getMidiDevice(synthInfo.getInfo());
			synthDevice.open();
			receiver = new ReceiverSink(true, synthDevice.getReceiver(), new LogReceiver());
			
			synthDevices.add(synthDevice);
			
			// configure the instrument as well
			setConfiguredInstrument();
			//configure midi input device.
			connectMidiInputDevice();
			LOG.info(String.format("Configured %s as MIDI OUT.", synthInfo.toString()));
		} catch (MidiUnavailableException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}

	

	/**
	 * Play a pitch bended note defined by relative cents and a velocity.
	 * 
	 * @param relativeCents
	 *            The relative cents pitch to play. This could be a number
	 *            between [0,1200] but it is actually possible to play any
	 *            <del>note</del> pitch. By default 0 = C4, to play B3 a
	 *            relative cents value of -100 works.
	 * @param velocity
	 *            The MIDI key velocity. A value in [0,127].
	 */
	public void playRelativeCents(final double relativeCents, final int velocity) {
		final double absoluteCent = BASE_OCTAVE * CENTS_IN_OCTAVE + relativeCents;
		playAbsoluteCents(absoluteCent, velocity);
	}

	/**
	 * Send a MIDI pitch bend and a MIDI note ON OFF pair to render a certain
	 * value in absolute cents.
	 * 
	 * @param absoluteCent
	 *            The value in absolute cents.
	 * @param velocity
	 *            The velocity of the note on message.
	 */
	public void playAbsoluteCents(final double absoluteCent, final int velocity) {
			final double pitchInHertz = PitchUnit.absoluteCentToHertz(absoluteCent);
			final double pitchInMidiCent = PitchUnit.hertzToMidiCent(pitchInHertz);
			final int pitchInMidiKey = (int) Math.round(pitchInMidiCent);
			final double deviationInCents = 100 * (pitchInMidiCent - pitchInMidiKey);
						
			pitchBend(deviationInCents);
			LOG.fine(String.format("NOTE_ON  message %.2f abs cents, %s midi key, %s velocity, %.2fHz ",
					absoluteCent, pitchInMidiKey, velocity, pitchInHertz));
			
			noteOn(pitchInMidiKey,velocity,PITCH_BEND_MIDI_CHANNEL);
			final Runnable noteOffThread = new Runnable() {
				public void run() {
					try {
						Thread.sleep(NOTE_OFF_AFTER);
					} catch (final InterruptedException e) {
						LOG.log(Level.WARNING, "Failed to sleep before sending NOTE OFF", e);
					} finally {
						noteOff(pitchInMidiKey,PITCH_BEND_MIDI_CHANNEL);
						LOG.fine(String.format(
								"NOTE_OFF message %.2f abs cents, %s midi key, %s velocity, %.2fHz",
								absoluteCent, pitchInMidiKey, velocity, pitchInHertz));
					}
				}
			};
			new Thread(noteOffThread, "MIDI note off message").start();
	}

	/**
	 * Send a pitch bend message to the channel.
	 * 
	 * @param deviationInCents
	 *            The deviation in cents. Can be positive or negative but some
	 *            synthesizers only support positive pitch bend messages.
	 */
	public void pitchBend(double deviationInCents){
		final MidiEvent pitchBendEvent = MidiSequenceBuilder.createPitchBendEvent(deviationInCents,PITCH_BEND_MIDI_CHANNEL, -1);
		receiver.send(pitchBendEvent.getMessage(), -1);
	}
	
	
	/**
	 * Send a Note ON message to a MIDI key with a velocity.
	 * 
	 * @param midiKey
	 *            The MIDI key number.
	 * @param velocity
	 *            The velocity of the note on message.
	 * @param channel
	 *            The MIDI channel (0-16).
	 */
	public void noteOn(final int midiKey, final int velocity,final int channel) {
		sendNoteMessage(midiKey, velocity, channel, true);
	}

	/**
	 * Send a note off message to the MIDI key.
	 * 
	 * @param midiKey
	 *            The MIDI key to send the note off message to.
	 * @param channel
	 *            The MIDI channel (0-16).
	 */
	public void noteOff(final int midiKey,final int channel) {
		sendNoteMessage(midiKey, 0, channel, false);
	}
	
	/**
	 * Sends a NOTE_ON or NOTE_OFF message on the requested key.
	 * 
	 * @param midiKey
	 *            The midi key to send the message for
	 *            [0,VirtualKeyboard.NUMBER_OF_MIDI_KEYS[
	 * @param sendOnMessage
	 *            <code>true</code> for NOTE_ON messages, <code>false</code> for
	 *            NOTE_OFF
	 */
	private final void sendNoteMessage(final int midiKey, final int velocity, final int channel, final boolean sendOnMessage) {
		if (velocity < 0 || velocity > MAX_VELOCITY) {
			throw new IllegalArgumentException("Velocity should be in [0,127]");
		}
		if (midiKey < 0 || midiKey > MAX_MIDI_KEY) {
			throw new IllegalArgumentException("Velocity should be in [0,127]");
		}
		try {
			final ShortMessage sm = new ShortMessage();
			final int command;
			if (sendOnMessage) {
				command = ShortMessage.NOTE_ON;				
			} else {
				command = ShortMessage.NOTE_OFF;
			}
			sm.setMessage(command, channel, midiKey, velocity);
			receiver.send(sm, -1);
		} catch (final InvalidMidiDataException e) {
			LOG.log(Level.SEVERE, "Invalid midi data for constucted MIDI", e);
		}
	}
	
	
	
	/**
	 * Tune the receiver with the MIDI Tuning Standard.
	 * @param tuning
	 */
	public void tune(double tuning[]){
		try {
			if (tuning.length != 0) {
				final double[] rebasedTuning = MidiCommon.tuningFromPeaks(tuning);
				MidiUtils.sendTunings(receiver, 0, 2, "tuning", rebasedTuning);
				MidiUtils.sendTuningChange(receiver, TUNED_MIDI_CHANNEL, 2);
			}
		} catch (IOException e) {
			LOG.log(Level.WARNING, "Tuning failed: MIDI device threw an I/O error.", e);
		} catch (InvalidMidiDataException e) {
			LOG.log(Level.WARNING, "Tuning failed: MIDI tuning message incorrectly constructed.", e);
			throw new InvalidStateException("MIDI tuning message incorrectly constructed");
		}
	}
	
	
	/**
	 * Adds a receiver to the underlying list of receivers.
	 * 
	 * @param receiver
	 *            The receiver to add.
	 */
	public void addReceiver(final Receiver receiver){
		this.receiver.addReceiver(receiver);		
	}
	

	public void configurationChanged(final ConfKey key) {
		// change the instrument
		if (key == ConfKey.midi_instrument_index) {
			setConfiguredInstrument();
		} else if (key == ConfKey.midi_output_devices) {
			try {
				setReceiver();
			} catch (MidiUnavailableException e) {
				LOG.log(Level.SEVERE, "Unable to set MIDI device.", e);
			}
		} else if (key == ConfKey.midi_input_device){
			try {
				connectMidiInputDevice();
			} catch (MidiUnavailableException e) {
				LOG.log(Level.SEVERE, "Unable to set MIDI device.", e);
			}
		}
	}
	
	/**
	 * Configure the receiver: it uses a configured synthesizer (or other MIDI
	 * out) and connects a receiver to it. It also configures the instrument.
	 * 
	 * @throws MidiUnavailableException
	 *             If the configured device is not available.
	 */
	private void setReceiver() throws MidiUnavailableException {
		checkMidiOutputDeviceIndex();
		
		for(MidiDevice synthDevice : synthDevices){
			if (synthDevice != null) {
				//synthDevice.close();
			}
		}
		
		final List<String> midiDeviceIndexes = Configuration.getList(ConfKey.midi_output_devices);
		for(String midiDeviceIndex : midiDeviceIndexes){
			final MoreMidiInfo synthInfo = MidiCommon.listDevices(false,true).get(Integer.valueOf(midiDeviceIndex));
			MidiDevice synthDevice = MidiSystem.getMidiDevice(synthInfo.getInfo());
			synthDevice.open();
			synthDevices.add(synthDevice);
			LOG.info(String.format("Configured %s as MIDI OUT.", synthInfo.toString()));
		}
		
		Receiver[] receivers = new Receiver[synthDevices.size()+1];
		for(int i = 0 ; i < synthDevices.size() ; i++){
			receivers[i] = synthDevices.get(i).getReceiver();
		}
		receivers[synthDevices.size()] = new LogReceiver();
		receiver = new ReceiverSink(true,receivers);
		// configure the instrument as well
		setConfiguredInstrument();
		//configure midi input device.
		connectMidiInputDevice();

	}
	
	/**
	 * Checks the MIDI output device index and sets it to a default value if the device index is out of bounds.
	 */
	private void checkMidiOutputDeviceIndex() {
		List<String> deviceIndexes = Configuration.getList(ConfKey.midi_output_devices);
		int defaultDeviceIndex = 0;
		
		Vector<MoreMidiInfo> devices = MidiCommon.listDevices(false,true);
		if(deviceIndexes.size()== 1 && deviceIndexes.get(0).isEmpty()){
			Configuration.set(ConfKey.midi_output_devices, defaultDeviceIndex);
		} else {
			for(String deviceIndexString : deviceIndexes){
				int deviceIndex = Integer.valueOf(deviceIndexString);
				if(deviceIndex < 0 || deviceIndex >= devices.size()){
					Configuration.set(ConfKey.midi_output_devices, defaultDeviceIndex);
					LOG.warning("Ignored index out of bounds exception, reconfigured the midi device index, from " + deviceIndex + " to " + defaultDeviceIndex);
				}	
			}	
		}
	}

	private void setFallbackReceiver(){
		try {
			synthDevices.clear();
			MidiDevice synthDevice = MidiSystem.getSynthesizer();
			synthDevice.open();
			receiver = new ReceiverSink(true);
			receiver.addReceiver(new LogReceiver());
			receiver.addReceiver(synthDevice.getReceiver());
			LOG.warning("Using not the configured synth but: " + synthDevice.toString());
			synthDevices.add(synthDevice);
		} catch (MidiUnavailableException e) {
			LOG.severe("Could not initialize a synth");
		}		
	}
	
	
	private void connectMidiInputDevice() throws MidiUnavailableException{
		final int midiDeviceIndex = Configuration.getInt(ConfKey.midi_input_device);
		MoreMidiInfo synthInfo;
		try{
			synthInfo = MidiCommon.listDevices(true,false).get(midiDeviceIndex);
		}catch(ArrayIndexOutOfBoundsException e){
			synthInfo = MidiCommon.listDevices(true,false).get(0);
			LOG.warning("Could not find configured MIDI INPUT device");
		}		
		LOG.info(String.format("Configuring %s as MIDI IN.", synthInfo.toString()));
		MidiDevice midiInDevice = MidiSystem.getMidiDevice(synthInfo.getInfo());
		midiInDevice.open();
		//changes the channel of note on and off events to the target channel
		ChannelRedirector redirector = new ChannelRedirector(TUNED_MIDI_CHANNEL, receiver, midiInDevice.getTransmitter());
		midiInDevice.getTransmitter().setReceiver(redirector);
	}
	
	
	public List<String> availableInstruments(){
		List<String> instruments = new ArrayList<String>();
		for(MidiDevice synthDevice : synthDevices){
			if (synthDevice instanceof Synthesizer) {
				for(Instrument instrument : ((Synthesizer) synthDevice).getAvailableInstruments()){
					instruments.add(instrument.getName());
				}
			}
		}
		return instruments;
	}
	
	private void setConfiguredInstrument() {
		for(MidiDevice synthDevice : synthDevices){
			if (synthDevice instanceof Synthesizer) {
				Synthesizer synth = (Synthesizer) synthDevice;
				Instrument[] available = synth.getAvailableInstruments();
				int instrumentIndex = Configuration.getInt(ConfKey.midi_instrument_index);
				
				//check and reset instrument index
				int defaultInstrumentIndex = 0;
				if(instrumentIndex >= available.length){
					LOG.warning("Ignored configured instrument index (" + instrumentIndex + ") reset to "  + defaultInstrumentIndex);
					Configuration.set(ConfKey.midi_instrument_index, defaultInstrumentIndex);
					instrumentIndex = Configuration.getInt(ConfKey.midi_instrument_index);
					instrumentIndex = defaultInstrumentIndex;
				}
				Instrument configuredInstrument = available[instrumentIndex];
	
				MidiChannel channel = synth.getChannels()[PITCH_BEND_MIDI_CHANNEL];
				Patch patch = configuredInstrument.getPatch();
				channel.programChange(patch.getBank(), patch.getProgram());
				
				channel = synth.getChannels()[TUNED_MIDI_CHANNEL];
				patch = configuredInstrument.getPatch();
				channel.programChange(patch.getBank(), patch.getProgram());
				
				LOG.info(String.format("Configured synth with %s.", configuredInstrument.getName()));
			}
		}
	}
	
	
	public void close() {
		for(MidiDevice synthDevice : synthDevices){
			if (synthDevice != null) {
				synthDevice.close();
			}
		}
		if(receiver != null){
			receiver.close();
		}
	}


	private static TarsosSynth tarsosSynth;
	public synchronized static TarsosSynth getInstance() {
		if(tarsosSynth==null){
			try {
				tarsosSynth = new TarsosSynth();
				tarsosSynth.setReceiver();
			} catch (MidiUnavailableException e) {
				LOG.severe("The configured Synth is not available:" + e.getMessage());
				tarsosSynth.setFallbackReceiver();
			}
		}
		return tarsosSynth;
	}

}
