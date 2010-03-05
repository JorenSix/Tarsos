package be.hogent.tarsos.ui;

import javax.sound.midi.Receiver;
import javax.sound.midi.Transmitter;

public interface VirtualKeyboard extends Transmitter, Receiver {
	/**
	 * Channel to send MIDI events to
	 */
	public static final int CHANNEL = 0;//channel zero.. bad to the bone
	/**
	 * The velocity of NOTE_ON events
	 */
	public static final int VELOCITY = 85;
	/**
	 * The number of keys on the keyboard
	 */
	public static final int NUMBER_OF_KEYS = 128;
	
    // on a azerty (Belgian) keyboard)
	public static final String mappedKeys = "qsdfghjklmazertyuiop&й\"'(§и!за";
	
	
	void setReceiver(Receiver receiver);
}
