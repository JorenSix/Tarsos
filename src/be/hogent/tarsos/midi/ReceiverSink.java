/**
 * 
 */
package be.hogent.tarsos.midi;

import javax.sound.midi.MidiMessage;
import javax.sound.midi.Receiver;


/**
 * @author Joren Six
 *
 * ReceiverSink acts sends messages to each registered <code>Receiver</code>
 */
public class ReceiverSink implements Receiver{
	
	Receiver[] receivers;
	boolean ignoreTiming;
	/**
	 * @param receivers the list of <code>Receiver<code>s to send messages to
	 */
	public ReceiverSink(boolean ignoreTiming,Receiver... receivers){
		this.receivers =  receivers;
		this.ignoreTiming = ignoreTiming;
	}

	@Override
	public void close() {
		for(Receiver receiver : receivers){
			receiver.close();
		}
	}

	@Override
	public void send(MidiMessage message, long timeStamp) {
		timeStamp = ignoreTiming ? -1 : timeStamp;
		for(Receiver receiver : receivers){
			receiver.send(message, timeStamp);
		}
	}
}