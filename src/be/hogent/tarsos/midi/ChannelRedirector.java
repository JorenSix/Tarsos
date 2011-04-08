package be.hogent.tarsos.midi;

import javax.sound.midi.InvalidMidiDataException;
import javax.sound.midi.MidiMessage;
import javax.sound.midi.Receiver;
import javax.sound.midi.ShortMessage;
import javax.sound.midi.Transmitter;

public class ChannelRedirector implements Receiver, Transmitter {
	
	private final int targetChannel;
	private final Receiver targetReceiver;
	private final Transmitter sourceTransmitter;
	public ChannelRedirector(int targetChannel,final Receiver target,final Transmitter source){
		this.targetChannel= targetChannel;
		targetReceiver = target;
		sourceTransmitter = source;
	}

	@Override
	public Receiver getReceiver() {
		return targetReceiver;
	}

	@Override
	public void setReceiver(Receiver receiver) {
		
	}

	@Override
	public void close() {
		targetReceiver.close();
		sourceTransmitter.close();
	}

	@Override
	public void send(MidiMessage message, long timeStamp) {
		final MidiMessage newMessage;
		if (message instanceof ShortMessage) {
			final ShortMessage sm = (ShortMessage) message;
			final boolean correctChannel = sm.getChannel() == targetChannel;
			final boolean noteOnOrOff = sm.getCommand() == ShortMessage.NOTE_ON
					|| sm.getCommand() == ShortMessage.NOTE_OFF;
			
			if (!correctChannel && noteOnOrOff) {
				newMessage = new ShortMessage();
				try {
					((ShortMessage)newMessage).setMessage(sm.getCommand(), targetChannel,
							sm.getData1(), sm.getData2());
				} catch (InvalidMidiDataException e) {
					//should not happen.
				}
			} else {
				newMessage = sm;
			}
		}else {
			newMessage = message;
		}
		targetReceiver.send(newMessage, timeStamp);		
	}
}
