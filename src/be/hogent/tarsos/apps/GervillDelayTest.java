package be.hogent.tarsos.apps;

import javax.sound.midi.MidiDevice;
import javax.sound.midi.Receiver;
import javax.sound.midi.ShortMessage;
import javax.sound.midi.Transmitter;
import javax.swing.JFrame;

import be.hogent.tarsos.midi.DumpReceiver;
import be.hogent.tarsos.midi.MidiUtils;
import be.hogent.tarsos.midi.ReceiverSink;

public final class GervillDelayTest {

    public static void main(String[] args) throws Exception {
        Receiver recv;
        MidiDevice outputDevice = PlayAlong.chooseDevice(false, true);
        outputDevice.open();
        recv = outputDevice.getReceiver();
        MidiDevice midiInputDevice = PlayAlong.chooseDevice(true, false);
        midiInputDevice.open();
        Transmitter midiInputTransmitter = midiInputDevice.getTransmitter();

        recv = new ReceiverSink(true, recv, new DumpReceiver(System.out));
        midiInputTransmitter.setReceiver(recv);

        ShortMessage msg = new ShortMessage();

        double[] tunings = new double[128];
        for (int i = 1; i < 128; i++) {
            tunings[i] = tunings[i - 1] + 240;
        }

        MidiUtils.sendTunings(recv, 0, 0, "test", tunings);
        MidiUtils.sendTuningChange(recv, 0, 0);
        msg.setMessage(ShortMessage.NOTE_ON, 0, 69, 100);
        recv.send(msg, -1);
        msg.setMessage(ShortMessage.NOTE_OFF, 0, 69, 100);
        recv.send(msg, -1);
        new JFrame().setVisible(true);
    }
}
