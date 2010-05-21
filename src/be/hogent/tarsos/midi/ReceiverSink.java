package be.hogent.tarsos.midi;

import javax.sound.midi.MidiMessage;
import javax.sound.midi.Receiver;

/**
 * ReceiverSink acts as a sink for MIDI messages. It is a Receiver and sends
 * messages to each registered <code>Receiver</code>. It can be used to send
 * messages to a synthesizer while monitoring the events by writing them to the
 * command line, a log file,... or to build a MIDI file from any input
 * @author Joren Six
 */
public class ReceiverSink implements Receiver {

    private final Receiver[] receivers;
    private final boolean ignoreTiming;

    /**
     * @param receivers
     *            the list of <code>Receiver</code>s to send messages to
     */
    public ReceiverSink(final boolean ignoreTiming, final Receiver... receivers) {
        this.receivers = receivers;
        this.ignoreTiming = ignoreTiming;
    }

    @Override
    public void close() {
        for (Receiver receiver : receivers) {
            receiver.close();
        }
    }

    @Override
    public void send(final MidiMessage message, final long timeStamp) {
        long actualTimeStamp = timeStamp;
        if (ignoreTiming) {
            actualTimeStamp = -1;
        }
        for (Receiver receiver : receivers) {
            receiver.send(message, actualTimeStamp);
        }
    }
}
