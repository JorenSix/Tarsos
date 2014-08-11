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

package be.tarsos.exp.cli;

import java.util.Random;

import javax.sound.midi.MidiDevice;
import javax.sound.midi.Receiver;
import javax.sound.midi.ShortMessage;
import javax.sound.midi.Transmitter;
import javax.swing.JFrame;

import be.tarsos.midi.LogReceiver;
import be.tarsos.midi.MidiCommon;
import be.tarsos.midi.MidiUtils;
import be.tarsos.midi.ReceiverSink;

public final class GervillDelayTest {

	private GervillDelayTest() {
	}

	public static void main(final String[] args) throws Exception {
		Receiver recv;
		final MidiDevice outputDevice = MidiCommon.chooseMidiDevice(false, true);
		outputDevice.open();
		recv = outputDevice.getReceiver();
		final MidiDevice midiInputDevice = MidiCommon.chooseMidiDevice(true, false);
		midiInputDevice.open();
		final Transmitter midiInputTransmitter = midiInputDevice.getTransmitter();

		recv = new ReceiverSink(true, recv, new LogReceiver());
		midiInputTransmitter.setReceiver(recv);

		final ShortMessage msg = new ShortMessage();

		final Random rnd = new Random();
		final double[] tunings = new double[128];
		for (int i = 1; i < 128; i++) {
			tunings[i] = i * 100 + rnd.nextDouble() * 400;
		}

		MidiUtils.sendTunings(recv, 0, 0, "test", tunings);
		MidiUtils.sendTuningChange(recv, 0, 0);
		msg.setMessage(ShortMessage.NOTE_ON, 0, 69, 100);
		recv.send(msg, -1);
		msg.setMessage(ShortMessage.NOTE_OFF, 0, 69, 0);
		recv.send(msg, -1);
		new JFrame().setVisible(true);
	}
}
