package be.hogent.tarsos.apps;

import java.io.File;
import java.io.IOException;

import javax.sound.midi.InvalidMidiDataException;
import javax.sound.midi.MidiDevice;
import javax.sound.midi.MidiDevice.Info;
import javax.sound.midi.MidiSystem;
import javax.sound.midi.MidiUnavailableException;

import joptsimple.OptionParser;
import joptsimple.OptionSet;
import joptsimple.OptionSpec;
import be.hogent.tarsos.midi.LogReceiver;
import be.hogent.tarsos.midi.MidiUtils;
import be.hogent.tarsos.midi.ReceiverSink;
import be.hogent.tarsos.ui.virtualkeyboard.VirtualKeyboard;
import be.hogent.tarsos.util.ScalaFile;

/**
 * Sends MIDI Tuning messages to the requested port using a scala file as tone
 * scale.
 * 
 * @author Joren Six
 */
public final class TuneMidiSynth extends AbstractTarsosApp {

	@Override
	public String description() {
		return "Sends MIDI Tuning messages to the requested port using a scala file as tone scale.";
	}

	@Override
	public String name() {
		return "tune_midi_synth";
	}

	@Override
	public void run(final String... args) {

		final File sclFile;

		final OptionParser parser = new OptionParser();

		final OptionSpec<Integer> midiOutSpec = parser
				.accepts("midi-out", "The MIDI device index to send tuning messages to.").withRequiredArg()
				.ofType(Integer.class);
		final OptionSpec<Integer> midiInSpec = parser
				.accepts("midi-in", "The MIDI device index to receive messages from.").withRequiredArg()
				.ofType(Integer.class);
		final OptionSpec<File> sclFileSpec = parser.accepts("scala", "The scala file.").withRequiredArg()
				.ofType(File.class);

		final OptionSet options = parse(args, parser, this);

		if (!isHelpOptionSet(options) && options.has(sclFileSpec)) {

			try {
				final MidiDevice synth = getMidiDevice(midiInSpec, options, false, true);

				sclFile = options.valueOf(sclFileSpec);
				final double[] tuning = new ScalaFile(sclFile.getAbsolutePath()).getPitches();

				final double[] rebasedTuning = PlayAlong.tuningFromPeaks(tuning);

				synth.open();
				Tarsos.println("t");

				final MidiDevice midiInputDevice;

				midiInputDevice = getMidiDevice(midiOutSpec, options, true, false);

				midiInputDevice.open();

				final ReceiverSink sink = new ReceiverSink(true, synth.getReceiver(), new LogReceiver());

				midiInputDevice.getTransmitter().setReceiver(sink);

				MidiUtils.sendTunings(sink, 0, 2, "african", rebasedTuning);
				MidiUtils.sendTuningChange(sink, VirtualKeyboard.CHANNEL, 2);

				Tarsos.println("Press enter to stop");
				System.in.read();

			} catch (final MidiUnavailableException e) {
				Tarsos.println("Midi device not available choose another device.");
				printHelp(parser);
			} catch (final InvalidMidiDataException e) {
				e.printStackTrace();
			} catch (final IOException e) {
				e.printStackTrace();
			}

		} else {
			printHelp(parser);
		}
	}

	/**
	 * Returns a MIDI device using either CLI input or predefined option.
	 * 
	 * @param midiSpec
	 *            The option specification.
	 * @param options
	 *            The command line options.
	 * @param inputDevice
	 *            Is the requested device used for input?
	 * @param outputDevice
	 *            Is the requested device used for output?
	 * @return A MIDI device.
	 * @throws MidiUnavailableException
	 *             If the MIDI device is not available.
	 */
	private MidiDevice getMidiDevice(final OptionSpec<Integer> midiSpec, final OptionSet options,
			final boolean inputDevice, final boolean outputDevice) throws MidiUnavailableException {
		final MidiDevice midiDevice;
		if (options.has(midiSpec)) {
			final int midiDeviceIndex = options.valueOf(midiSpec);
			final Info deviceInfo = MidiSystem.getMidiDeviceInfo()[midiDeviceIndex];
			midiDevice = MidiSystem.getMidiDevice(deviceInfo);
		} else {
			midiDevice = Tarsos.chooseMidiDevice(inputDevice, outputDevice);
		}
		return midiDevice;
	}

}
