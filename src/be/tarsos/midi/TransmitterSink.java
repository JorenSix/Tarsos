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

/**
 */
package be.tarsos.midi;

import javax.sound.midi.Receiver;
import javax.sound.midi.Transmitter;

/**
 * Sends messages to a list of Transmitters. Can be used to send MIDI messages
 * to several receivers. Each transmitter in the sink should have the same
 * receiver (or ReceiverSink)
 * 
 * @author Joren Six
 */
public final class TransmitterSink implements Transmitter {

    private final Transmitter[] transmitters;

    public TransmitterSink(final Transmitter... transmitterList) {
        this.transmitters = transmitterList;
    }

    
    public void close() {
        for (final Transmitter transmitter : transmitters) {
            transmitter.close();
        }
    }

    
    public Receiver getReceiver() {
        Receiver receiver = null;
        if (transmitters.length != 0) {
            receiver = transmitters[0].getReceiver();
            for (int i = 1; i < transmitters.length; i++) {
                if (transmitters[i].getReceiver() != receiver) {
                    throw new AssertionError(
                    "Each Transmitter in the TransmitterSink should have the same Receiver");
                }
            }
        }
        return receiver;
    }

    
    public void setReceiver(final Receiver receiver) {
        for (final Transmitter transmitter : transmitters) {
            transmitter.setReceiver(receiver);
        }
    }
}
