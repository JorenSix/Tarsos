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

package be.tarsos.ui.virtualkeyboard;

import java.awt.Point;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.sound.midi.MidiMessage;
import javax.sound.midi.Receiver;
import javax.sound.midi.ShortMessage;
import javax.swing.JComponent;

import be.tarsos.midi.MidiCommon;
import be.tarsos.midi.TarsosSynth;

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
public abstract class VirtualKeyboard extends JComponent implements Receiver {
	/**
     */
	private static final long serialVersionUID = -8109877572069108012L;
	/**
	 * The velocity of NOTE_ON events.
	 */
	public static final int VELOCITY = 85;
	/**
	 * The number of MIDI keys in total.
	 */
	public static final int NUMBER_OF_MIDI_KEYS = 128;

	// on a azerty (Belgian) keyboard)
	private static String mappedKeys = "qsdfghjklmazertyuiop";

	private final int numberOfKeysPerOctave;

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
		setLowestAssignedKey(5 * numberOfKeysInOctave); // start at octave 3

		keyDown = new boolean[VirtualKeyboard.NUMBER_OF_MIDI_KEYS];

		addMouseListener(new MouseAdapter() {

			
			public void mousePressed(final MouseEvent e) {
				grabFocus();
				final Point p = e.getPoint();
				currentlyPressedMidiNote = getMidiNote(p.x, p.y);
				sendNoteMessage(currentlyPressedMidiNote, true);
			}

			
			public void mouseReleased(final MouseEvent e) {
				sendNoteMessage(currentlyPressedMidiNote, false);
				currentlyPressedMidiNote = -1;
			}
		});

		addFocusListener(new FocusListener() {

			public void focusGained(final FocusEvent e) {
				repaint();
			}

			public void focusLost(final FocusEvent e) {
				allKeysOff(); // is this behavior wanted?
				repaint();
			}
		});

		addKeyListener(new KeyListener() {

			public void keyPressed(final KeyEvent e) {
				final int pressedKeyChar = e.getKeyChar();
				for (int i = 0; i < VirtualKeyboard.getMappedKeys().length(); i++) {
					if (VirtualKeyboard.getMappedKeys().charAt(i) == pressedKeyChar) {
						final int midiKey = i + getLowestAssignedKey();
						if (midiKey < 128 && !keyDown[midiKey]) {
							sendNoteMessage(midiKey, true);
						}
						return;
					}
				}
			}

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

		TarsosSynth.getInstance().addReceiver(this);
	}
	
	private void sendNoteMessage(int midiKey, boolean noteOn) {
		if(noteOn){
			TarsosSynth.getInstance().noteOn(midiKey,VELOCITY,TarsosSynth.TUNED_MIDI_CHANNEL);
		}else{
			TarsosSynth.getInstance().noteOff(midiKey,TarsosSynth.TUNED_MIDI_CHANNEL);
		}
		
	}

	protected final boolean isKeyDown(final int midiKey) {
		// midikey should be smaller than 128
		if (midiKey >= VirtualKeyboard.NUMBER_OF_MIDI_KEYS) {
			throw new IllegalArgumentException("Requested invalid midi key: " + midiKey);
		}
		return keyDown[midiKey];
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

	public final void send(final MidiMessage message, final long timeStamp) {
		// implements Receiver send: makes sure the right keys are marked
		if (message instanceof ShortMessage) {
			final ShortMessage sm = (ShortMessage) message;
			final boolean correctChannel = sm.getChannel() == TarsosSynth.TUNED_MIDI_CHANNEL;
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


	

	private double tuning[];

	protected double[] getTuning() {
		return tuning;
	}

	/**
	 * Connects the virtual keyboard to the default Gervill synthesizer.
	 * 
	 * @param tuning
	 *            The tuning for one octave defined in cents.
	 */
	public void connectToTunedSynth(double[] tuning) {
		TarsosSynth.getInstance().tune(tuning);
		this.tuning = MidiCommon.tuningFromPeaks(tuning);
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

	protected float getNumberOfOctaves() {
		return numberOfKeys / (float) numberOfKeysPerOctave;
	}
	
	
	public void close() {
	}

}
