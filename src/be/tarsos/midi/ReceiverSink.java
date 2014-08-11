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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.sound.midi.MidiMessage;
import javax.sound.midi.Receiver;

/**
 * ReceiverSink acts as a sink for MIDI messages. It is a Receiver and sends
 * messages to each registered <code>Receiver</code>. It can be used to send
 * messages to a synthesizer while monitoring the events by writing them to the
 * command line, a LOG file,... or to build a MIDI file from any input
 * 
 * @author Joren Six
 */
public final class ReceiverSink implements Receiver {

	private final List<Receiver> receivers;
	private final boolean ignoreTiming;

	
	/**
	 * @param ignoreTimingData A boolean that defines if timing messages should be ignored.
	 * @param receiverList The list of <code>Receiver</code>s to send messages to.
	 */
	public ReceiverSink(final boolean ignoreTimingData, final Receiver... receiverList) {
		this.receivers = new ArrayList<Receiver>(Arrays.asList(receiverList));
		this.ignoreTiming = ignoreTimingData;
	}

	
	public void close() {
		for (final Receiver receiver : receivers) {
			receiver.close();
		}
	}
	
	public void send(final MidiMessage message, final long timeStamp) {
		long actualTimeStamp = timeStamp;
		if (ignoreTiming) {
			actualTimeStamp = -1;
		}
		for (final Receiver receiver : receivers) {
			receiver.send(message, actualTimeStamp);
		}
	}
	
	/**
	 * Adds a receiver to the sink (list of receivers).
	 * 
	 * @param receiver The receiver to add. 
	 */
	public void addReceiver(final Receiver receiver){
		receivers.add(receiver);
	}
}
