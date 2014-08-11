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

import javax.sound.midi.InvalidMidiDataException;
import javax.sound.midi.MidiMessage;
import javax.sound.midi.Receiver;
import javax.sound.midi.ShortMessage;
import javax.sound.midi.Transmitter;

/**
 * Redirects NOTE ON and OFF messages to a target channel.
 * @author Joren Six
 */
public class ChannelRedirector implements Receiver, Transmitter {
	
	private final int targetChannel;
	private final Receiver targetReceiver;
	private final Transmitter sourceTransmitter;
	
	
	/**
	 * Create a new channel redirector for a certain source and target.
	 * @param targetChannel The channel the MIDI messages should arrive on.
	 * @param target The target receiver.
	 * @param source The transmitter source.
	 */
	public ChannelRedirector(int targetChannel,final Receiver target,final Transmitter source){
		this.targetChannel= targetChannel;
		targetReceiver = target;
		sourceTransmitter = source;
	}

	
	public Receiver getReceiver() {
		return targetReceiver;
	}

	
	public void setReceiver(Receiver receiver) {
		
	}

	public void close() {
		targetReceiver.close();
		sourceTransmitter.close();
	}

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
					assert false : "Invalid MIDI data should is not possible here.";
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
