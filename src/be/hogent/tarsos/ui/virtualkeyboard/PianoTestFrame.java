package be.hogent.tarsos.ui.virtualkeyboard;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.io.IOException;

import javax.sound.midi.InvalidMidiDataException;
import javax.sound.midi.MidiDevice;
import javax.sound.midi.MidiSystem;
import javax.sound.midi.MidiUnavailableException;
import javax.sound.midi.Receiver;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;

import be.hogent.tarsos.apps.PlayAlong;
import be.hogent.tarsos.midi.DumpReceiver;
import be.hogent.tarsos.midi.MidiCommon;
import be.hogent.tarsos.midi.MidiUtils;
import be.hogent.tarsos.midi.ReceiverSink;

public class PianoTestFrame extends JFrame {

	private static final long serialVersionUID = 6063312726815482475L;

	public PianoTestFrame(final VirtualKeyboard keyboard, final double[] tuning) {
		// setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		setLayout(new BorderLayout());
		final Dimension dimension = new Dimension(650, 100);
		setPreferredSize(dimension);
		setMinimumSize(dimension);
		setMaximumSize(dimension);

		final JPanel keyboardPanel = new JPanel(new BorderLayout());
		keyboardPanel.setBorder(new EmptyBorder(10, 20, 10, 5));
		keyboardPanel.add(keyboard, BorderLayout.CENTER);

		this.add(keyboard, BorderLayout.CENTER);

		final MidiDevice.Info synthInfo = MidiCommon.getMidiDeviceInfo("Gervill", true);
		MidiDevice synthDevice;
		try {
			synthDevice = MidiSystem.getMidiDevice(synthInfo);
			synthDevice.open();

			Receiver recv;
			recv = new ReceiverSink(true, synthDevice.getReceiver(), new DumpReceiver(System.out));
			keyboard.setReceiver(recv);

			final double[] rebasedTuning = PlayAlong.tuningFromPeaks(tuning);

			MidiUtils.sendTunings(recv, 0, 2, "african", rebasedTuning);
			MidiUtils.sendTuningChange(recv, VirtualKeyboard.CHANNEL, 2);
		} catch (MidiUnavailableException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (InvalidMidiDataException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

	public static void main(final String... strings) {
		final double[] tuning = { 0, 100, 200, 300, 400, 500, 600 };
		new PianoTestFrame(VirtualKeyboard.createVirtualKeyboard(7), tuning).setVisible(true);
	}
}
