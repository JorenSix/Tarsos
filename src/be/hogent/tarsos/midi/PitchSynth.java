/**
 */
package be.hogent.tarsos.midi;

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

import be.hogent.tarsos.sampled.pitch.PitchConverter;
import be.hogent.tarsos.util.ConfKey;
import be.hogent.tarsos.util.Configuration;
import be.hogent.tarsos.util.Configuration.ConfigChangeListener;

/**
 * @author Joren Six
 */
public final class PitchSynth implements ConfigChangeListener {

	/**
	 * Log messages.
	 */
	private static final Logger LOG = Logger.getLogger(PitchSynth.class.getName());

	/**
	 * Default used in constructor.
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
	 * The channel used to send messages on.
	 */
	private static final int MIDI_CHANNEL = 0;

	/**
	 * Send note off after 250ms.
	 */
	private static final int NOTE_OFF_AFTER = 250;

	/**
	 * The octave used to send relative pitch messages: If you want to hear 0
	 * cents this translates to a C in the 4th octave: C4. But as we all know,
	 * you should not play with C4.
	 */
	private final int baseOctave;

	/**
	 * The receiver used to send messages to. The receiver acts as a
	 * "MIDI cable" and sends the received messages trough.
	 */
	private Receiver receiver;

	/**
	 * The synth device used (connected to the receiver).
	 */
	private MidiDevice synthDevice;

	/**
	 * Create a new synth.
	 * 
	 * @throws MidiUnavailableException
	 *             If the gervill synth is not available.
	 */
	public PitchSynth() throws MidiUnavailableException {
		this(BASE_OCTAVE);
	}

	/**
	 * Creates a new synth.
	 * 
	 * @param useDefaultSynth
	 *            If true uses the default (Gervill) synth, otherwise the user
	 *            is asked for a preference.
	 * @param dumpMessages
	 *            If true all MIDI messages are dumped to the console, practical
	 *            for debugging purposes.
	 * @param relBaseOctave
	 *            The octave used to send relative pitch messages: If you want
	 *            to hear 0 cents this translates to a C in the specified
	 *            octave.
	 * @throws MidiUnavailableException
	 *             If the (chosen?) synth is not available.
	 */
	public PitchSynth(final int relBaseOctave) throws MidiUnavailableException {
		this.baseOctave = relBaseOctave;
		setReceiver();
		Configuration.addListener(this);
	}

	private void setReceiver() throws MidiUnavailableException {
		final int midiDeviceIndex = Configuration.getInt(ConfKey.midi_output_device);
		if (synthDevice != null) {
			synthDevice.close();
		}
		final MidiDevice.Info synthInfo = MidiSystem.getMidiDeviceInfo()[midiDeviceIndex];
		LOG.info(String.format("Configuring %s as MIDI OUT.", synthInfo.getName()));
		synthDevice = MidiSystem.getMidiDevice(synthInfo);
		synthDevice.open();
		receiver = synthDevice.getReceiver(); // new ReceiverSink(true,
												// synthDevice.getReceiver(),
												// new LogReceiver());
		// configure the instrument as well
		setConfiguredInstrument();
		LOG.info(String.format("Configured %s as MIDI OUT.", synthInfo.getName()));
	}

	private void setConfiguredInstrument() {
		if (synthDevice instanceof Synthesizer) {
			Synthesizer synth = (Synthesizer) synthDevice;
			Instrument[] available = synth.getAvailableInstruments();
			Instrument configuredInstrument = available[Configuration.getInt(ConfKey.midi_instrument_index)];
			// synth.loadInstrument(configuredInstrument);
			MidiChannel channel = synth.getChannels()[MIDI_CHANNEL];
			Patch patch = configuredInstrument.getPatch();
			channel.programChange(patch.getBank(), patch.getProgram());
			LOG.info(String.format("Configured synth with %s.", configuredInstrument.getName()));
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
	public void play(final double relativeCents, final int velocity) {

		if (velocity < 0 || velocity > MAX_VELOCITY) {
			throw new IllegalArgumentException("Velocity should be in [0,127]");
		}

		try {
			final double absoluteCent = baseOctave * CENTS_IN_OCTAVE + relativeCents;
			final double pitchInHertz = PitchConverter.absoluteCentToHertz(absoluteCent);
			final double pitchInMidiCent = PitchConverter.hertzToMidiCent(pitchInHertz);
			final int pitchInMidiKey = (int) Math.round(pitchInMidiCent);
			final double deviationInCents = 100 * (pitchInMidiCent - pitchInMidiKey);
			final MidiEvent pitchBendEvent = MidiSequenceBuilder.createPitchBendEvent(deviationInCents, -1);

			receiver.send(pitchBendEvent.getMessage(), -1);

			final ShortMessage noteOnMessage = new ShortMessage();
			noteOnMessage.setMessage(ShortMessage.NOTE_ON, MIDI_CHANNEL, pitchInMidiKey, velocity);

			final ShortMessage noteOffMessage = new ShortMessage();
			noteOffMessage.setMessage(ShortMessage.NOTE_OFF, MIDI_CHANNEL, pitchInMidiKey, 0);

			receiver.send(noteOnMessage, -1);

			final Runnable noteOffThread = new Runnable() {
				@Override
				public void run() {
					try {
						Thread.sleep(NOTE_OFF_AFTER);
					} catch (final InterruptedException e) {
						LOG.log(Level.WARNING, "Failed to sleep before sending NOTE OFF", e);
					} finally {
						receiver.send(noteOffMessage, -1);
					}
				}
			};
			new Thread(noteOffThread, "MIDI note off message").start();
		} catch (final InvalidMidiDataException e) {
			LOG.log(Level.SEVERE, "Invalid midi data for constucted MIDI", e);
		}
	}

	@Override
	public void configurationChanged(final ConfKey key) {
		// change the instrument
		if (key == ConfKey.midi_instrument_index) {
			setConfiguredInstrument();
		} else if (key == ConfKey.midi_output_device) {
			try {
				setReceiver();
			} catch (MidiUnavailableException e) {
				LOG.log(Level.SEVERE, "Unable to set MIDI device.", e);
			}
		}
	}
}
