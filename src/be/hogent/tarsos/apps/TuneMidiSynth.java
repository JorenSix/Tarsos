package be.hogent.tarsos.apps;

import java.io.File;
import java.io.IOException;

import javax.sound.midi.InvalidMidiDataException;
import javax.sound.midi.MidiDevice;
import javax.sound.midi.MidiSystem;
import javax.sound.midi.MidiUnavailableException;
import javax.sound.midi.MidiDevice.Info;

import joptsimple.OptionParser;
import joptsimple.OptionSet;
import joptsimple.OptionSpec;
import be.hogent.tarsos.midi.DumpReceiver;
import be.hogent.tarsos.midi.MidiUtils;
import be.hogent.tarsos.midi.ReceiverSink;
import be.hogent.tarsos.ui.VirtualKeyboard;
import be.hogent.tarsos.util.ScalaFile;

public class TuneMidiSynth extends AbstractTarsosApp {

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
        final int midiDeviceIndex;
        final File sclFile;

        final OptionParser parser = new OptionParser();

        final OptionSpec<Integer> midiFileSpec = parser.accepts("midideviceindex",
        "The MIDI device to send tuning messages to.").withRequiredArg().ofType(Integer.class);
        final OptionSpec<File> sclFileSpec = parser.accepts("scala", "The scala file.").withRequiredArg()
        .ofType(File.class);

        final OptionSet options = parse(args, parser, this);

        if (!isHelpOptionSet(options) && options.has(sclFileSpec)) {

            try {
                final MidiDevice synth;
                if (options.has(midiFileSpec)) {
                    midiDeviceIndex = options.valueOf(midiFileSpec);
                    final Info midiDeviceInfo = MidiSystem.getMidiDeviceInfo()[midiDeviceIndex];
                    synth = MidiSystem.getMidiDevice(midiDeviceInfo);
                } else {
                    synth = Tarsos.chooseDevice(false, true);
                }

                sclFile = options.valueOf(sclFileSpec);
                final double[] tuning = new ScalaFile(sclFile.getAbsolutePath()).getPitches();

                final double[] rebasedTuning = PlayAlong.tuningFromPeaks(tuning);

                synth.open();
                Tarsos.println("t");

                MidiDevice virtualMidiInputDevice;

                virtualMidiInputDevice = Tarsos.chooseDevice(true, false);

                virtualMidiInputDevice.open();

                final ReceiverSink sink = new ReceiverSink(true, synth.getReceiver(), new DumpReceiver(
                        System.out));

                virtualMidiInputDevice.getTransmitter().setReceiver(sink);

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

}
