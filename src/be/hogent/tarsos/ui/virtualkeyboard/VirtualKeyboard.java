package be.hogent.tarsos.ui.virtualkeyboard;

import java.awt.Point;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.IOException;

import javax.sound.midi.InvalidMidiDataException;
import javax.sound.midi.MidiDevice;
import javax.sound.midi.MidiMessage;
import javax.sound.midi.MidiSystem;
import javax.sound.midi.MidiUnavailableException;
import javax.sound.midi.Receiver;
import javax.sound.midi.ShortMessage;
import javax.sound.midi.Transmitter;
import javax.swing.JComponent;

import be.hogent.tarsos.apps.PlayAlong;
import be.hogent.tarsos.midi.LogReceiver;
import be.hogent.tarsos.midi.MidiCommon;
import be.hogent.tarsos.midi.MidiUtils;
import be.hogent.tarsos.midi.ReceiverSink;

/**
 * An abstract class to represent a keyboard.
 * <p>
 * Uses refactored code from the gervill package licensed under the GPL with the
 * classpath exception.
 * </p>
 * <p>
 * <a href="https://gervill.dev.java.net/source/browse/gervill/src.demos/">
 * Gervill source code</a>
 * </p>
 * 
 * @author Joren Six
 */
public abstract class VirtualKeyboard extends JComponent implements Transmitter, Receiver {
	/**
     */
	private static final long serialVersionUID = -8109877572069108012L;
	/**
	 * Channel to send MIDI events to.
	 */
	public static final int CHANNEL = 0; // channel zero.. bad to the bone
	/**
	 * The velocity of NOTE_ON events.
	 */
	public static final int VELOCITY = 85;
	/**
	 * The number of MIDI keys in total.
	 */
	public static final int NUMBER_OF_MIDI_KEYS = 128;

	// on a azerty (Belgian) keyboard)
	private static String mappedKeys = "qsdfghjklmazertyuiop&й\"'(§и!за";

	private final int numberOfKeysPerOctave;
	private Receiver recveiver = null;

	/**
	 * The (one and only) MIDI key currently pressed using the mouse.
	 */
	private int currentlyPressedMidiNote;

	/**
	 * Lowest MIDI key assigned to a keyboard shortcut.
	 */
	private int lowestAssignedKey;

	/**
	 * Number of keys used in the representation (smaller than
	 * NUMBER_OF_MIDI_KEYS).
	 */
	private final int numberOfKeys;

	/**
	 * Remember which of the keys are pressed (using the mouse, MIDI or
	 * keyboard).
	 */
	private final boolean[] keyDown;

	/**
	 * Create a new keyboard that spans 7 octaves and uses the specified number
	 * of keys per octave.
	 * 
	 * @param numberOfKeysInOctave
	 *            the number of keys per octave
	 */
	public VirtualKeyboard(final int numberOfKeysInOctave) {
		// default: 7 octaves
		// number of keys smaller than VirtualKeyboard.NUMBER_OF_MIDI_KEYS
		this(numberOfKeysInOctave, numberOfKeysInOctave * 3);
	}

	/**
	 * Create a new keyboard that has a number of keys per octave and a
	 * specified total number of keys.
	 * 
	 * @param numberOfKeysInOctave
	 *            the number of keys per octave
	 * @param totalOfKeys
	 *            the total number of keys used. E.g. 12 keys per octave and 4
	 *            octaves = 48 keys
	 */
	public VirtualKeyboard(final int numberOfKeysInOctave, final int totalOfKeys) {
		super();
		setFocusable(true);

		if (totalOfKeys > NUMBER_OF_MIDI_KEYS) {
			throw new IllegalArgumentException(String.format("Total number of keys (%s) "
					+ "should be lower than (%s).", totalOfKeys, NUMBER_OF_MIDI_KEYS));
		}

		this.numberOfKeys = totalOfKeys;
		this.numberOfKeysPerOctave = numberOfKeysInOctave;
		this.currentlyPressedMidiNote = -1;
		setLowestAssignedKey(3 * numberOfKeysInOctave); // start at octave 3

		keyDown = new boolean[VirtualKeyboard.NUMBER_OF_MIDI_KEYS];

		addMouseListener(new MouseAdapter() {
			@Override
			public void mousePressed(final MouseEvent e) {
				grabFocus();
				final Point p = e.getPoint();
				currentlyPressedMidiNote = getMidiNote(p.x, p.y);
				sendNoteMessage(currentlyPressedMidiNote, true);
			}

			@Override
			public void mouseReleased(final MouseEvent e) {
				sendNoteMessage(currentlyPressedMidiNote, false);
				currentlyPressedMidiNote = -1;
			}
		});

		addFocusListener(new FocusListener() {
			@Override
			public void focusGained(final FocusEvent e) {
				repaint();
			}

			@Override
			public void focusLost(final FocusEvent e) {
				allKeysOff(); // is this behavior wanted?
				repaint();
			}
		});

		addKeyListener(new KeyListener() {
			@Override
			public void keyPressed(final KeyEvent e) {
				final int pressedKeyChar = e.getKeyChar();
				for (int i = 0; i < VirtualKeyboard.getMappedKeys().length(); i++) {
					if (VirtualKeyboard.getMappedKeys().charAt(i) == pressedKeyChar) {
						final int midiKey = i + getLowestAssignedKey();
						if (midiKey < VirtualKeyboard.this.getNumberOfKeys() && !keyDown[midiKey]) {
							sendNoteMessage(midiKey, true);
						}
						return;
					}
				}
			}

			@Override
			public void keyReleased(final KeyEvent e) {
				final char pressedKeyChar = e.getKeyChar();
				for (int i = 0; i < VirtualKeyboard.getMappedKeys().length(); i++) {
					if (VirtualKeyboard.getMappedKeys().charAt(i) == pressedKeyChar) {
						final int midiKey = i + getLowestAssignedKey();
						if (keyDown[midiKey]) {
							sendNoteMessage(midiKey, false);
						}
						return;
					}
				}
			}

			@Override
			public void keyTyped(final KeyEvent e) {
				if (e.getKeyChar() == '-') {
					setLowestAssignedKey(getLowestAssignedKey() - getNumberOfKeysPerOctave());
					if (getLowestAssignedKey() < 0) {
						setLowestAssignedKey(0);
					}
					repaint();
				}
				if (e.getKeyChar() == '+') {
					setLowestAssignedKey(getLowestAssignedKey() + getNumberOfKeysPerOctave());
					if (getLowestAssignedKey() > 127) {
						setLowestAssignedKey(getLowestAssignedKey() - getNumberOfKeysPerOctave());
					}
					repaint();
				}
			}
		});
	}

	protected final boolean isKeyDown(final int midiKey) {
		// midikey should be smaller than 128
		if (midiKey >= VirtualKeyboard.NUMBER_OF_MIDI_KEYS) {
			throw new IllegalArgumentException("Requested invalid midi key: " + midiKey);
		}

		return keyDown[midiKey];
	}

	/**
	 * Sends a NOTE_ON or NOTE_OFF message on the requested key.
	 * 
	 * @param midiKey
	 *            The midi key to send the message for
	 *            [0,VirtualKeyboard.NUMBER_OF_MIDI_KEYS[
	 * @param sendOnMessage
	 *            <code>true</code> for NOTE_ON messages, <code>false</code> for
	 *            NOTE_OFF
	 */
	protected final void sendNoteMessage(final int midiKey, final boolean sendOnMessage) {
		// do not send note on messages to pressed keys
		if (sendOnMessage && keyDown[midiKey]) {
			return;
		}
		// do not send note off messages to keys that are not pressed
		if (!sendOnMessage && !keyDown[midiKey]) {
			return;
		}

		try {
			final ShortMessage sm = new ShortMessage();

			final int command;
			final int velocity;

			if (sendOnMessage) {
				command = ShortMessage.NOTE_ON;
				velocity = VirtualKeyboard.VELOCITY;
			} else {
				command = ShortMessage.NOTE_OFF;
				velocity = 0;
			}

			sm.setMessage(command, VirtualKeyboard.CHANNEL, midiKey, velocity);

			send(sm, -1);
		} catch (final InvalidMidiDataException e1) {
			e1.printStackTrace();
		}
		// mark key correctly
		keyDown[midiKey] = sendOnMessage;
	}

	/**
	 * Converts x and y coordinate into a MIDI note number.
	 * 
	 * @param x
	 *            the x coordinate
	 * @param y
	 *            the y coordinate
	 * @return the corresponding MIDI note number
	 */
	protected abstract int getMidiNote(int x, int y);

	protected final void allKeysOff() {
		for (int midiKey = 0; midiKey < VirtualKeyboard.NUMBER_OF_MIDI_KEYS; midiKey++) {
			sendNoteMessage(midiKey, false);
		}
	}

	@Override
	public final void setReceiver(final Receiver receiver) {
		this.recveiver = receiver;
	}

	@Override
	public abstract void close();

	@Override
	public final Receiver getReceiver() {
		return this.recveiver;
	}

	@Override
	public final void send(final MidiMessage message, final long timeStamp) {
		// acts as a "MIDI cable" sends the received messages trough
		if (recveiver != null) {
			recveiver.send(message, timeStamp);
		}

		// implements Receiver send: makes sure the right keys are marked
		if (message instanceof ShortMessage) {
			final ShortMessage sm = (ShortMessage) message;
			final boolean correctChannel = sm.getChannel() == VirtualKeyboard.CHANNEL;
			final boolean noteOnOrOff = sm.getCommand() == ShortMessage.NOTE_ON
					|| sm.getCommand() == ShortMessage.NOTE_OFF;
			if (correctChannel && noteOnOrOff) {
				keyDown[sm.getData1()] = sm.getCommand() == ShortMessage.NOTE_ON && sm.getData2() != 0;
				repaint();
			}
		}
	}

	/**
	 * Creates a virtual keyboard using the best representation available. For
	 * the moment there are two special keyboards: one with 12 keys (a normal
	 * keyboard) and one with 19 keys. The rest use the
	 * {@link UniversalVirtualKeyboard} class.
	 * 
	 * @param numberOfKeysPerOctave
	 *            requested number of keys for each octave.
	 * @return a <code>VirtualKeyboard</code> using the best representation
	 *         available.
	 */
	public static VirtualKeyboard createVirtualKeyboard(final int numberOfKeysPerOctave) {
		VirtualKeyboard keyboard = null;
		switch (numberOfKeysPerOctave) {
		case 12:
			keyboard = new VirtualKeyboard12();
			break;
		case 19:
			keyboard = new VirtualKeyboard19();
			break;
		default:
			keyboard = new UniversalVirtualKeyboard(numberOfKeysPerOctave);
			break;
		}
		return keyboard;
	}

	/**
	 * Connects the virtual keyboard to the default Gervill synthesizer.
	 * 
	 * @param tuning
	 *            The tuning for one octave defined in cents.
	 */
	public void connectToTunedSynth(double[] tuning) {
		final MidiDevice.Info synthInfo = MidiCommon.getMidiDeviceInfo("Gervill", true);
		MidiDevice synthDevice;
		try {
			synthDevice = MidiSystem.getMidiDevice(synthInfo);
			synthDevice.open();

			Receiver recv;
			recv = new ReceiverSink(true, synthDevice.getReceiver(), new LogReceiver());
			setReceiver(recv);

			final double[] rebasedTuning = PlayAlong.tuningFromPeaks(tuning);

			MidiUtils.sendTunings(recv, 0, 2, "tuning", rebasedTuning);
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

	public static void setMappedKeys(String mappedKeys) {
		VirtualKeyboard.mappedKeys = mappedKeys;
	}

	public static String getMappedKeys() {
		return mappedKeys;
	}

	protected int getNumberOfKeysPerOctave() {
		return numberOfKeysPerOctave;
	}

	protected void setLowestAssignedKey(int lowestAssignedKey) {
		this.lowestAssignedKey = lowestAssignedKey;
	}

	protected int getLowestAssignedKey() {
		return lowestAssignedKey;
	}

	protected int getNumberOfKeys() {
		return numberOfKeys;
	}

}
