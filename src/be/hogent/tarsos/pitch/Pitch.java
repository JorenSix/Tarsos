package be.hogent.tarsos.pitch;

import be.hogent.tarsos.pitch.Sample.PitchUnit;

/**
 * A class representing pitch.
 * Can be used to convert pitch units or to base
 * pitch interval calculations on.
 *
 * @author Joren Six
 *
 */
public class Pitch {

	/**
	 * The pitch in Hz
	 */
	private final double pitchInHertz;

	/**
	 * Create a new pitch object
	 * with a certain pitch
	 * @param pitchInHertz
	 */
	private Pitch(double pitchInHertz){
		this.pitchInHertz = pitchInHertz;
	}

	/**
	 * Get the pitch value in a given unit.
	 *
	 * @param unit
	 * @return
	 */
	public double getPitch(PitchUnit unit){
		double value = 0.0;
		switch (unit) {
			case ABSOLUTE_CENTS:
				value = PitchConverter.hertzToAbsoluteCent(pitchInHertz);
				break;
			case RELATIVE_CENTS:
				value = PitchConverter.hertzToRelativeCent(pitchInHertz);
				break;
			case MIDI_KEY:
				value = PitchConverter.hertzToMidiKey(pitchInHertz);
				break;
			case MIDI_CENT:
				value = PitchConverter.hertzToMidiCent(pitchInHertz);
				break;
			case HERTZ:
				value = pitchInHertz;
				break;
			default:
				throw new Error("Unsupported unit: " + unit.name());
		}
		return value;
	}

	public int octaveIndex(){
		int midiKey = (int) getPitch(PitchUnit.MIDI_KEY);
		return (midiKey / 12) - 1;
	}

	/**
	 * Returns the name of the MIDI key corresponding to the given hertzValue.
	 * The MIDI key is the key returned by the convertHertzToMidiKey method.
	 *
	 * @param hertzValue
	 *            The pitch in Hz.
	 * @return A note name like C3, A4 or A3#/B3b.
	 * @exception IllegalArgumentException When the hertzValue is outside
	 * the valid MIDI key range.
	 */
	public String noteName() {
		String name = "";
		String[] noteNames = { "Cx", "C#x/Dbx", "Dx", "D#x/Ebx", "Ex", "Fx",
				"F#x/Gbx", "Gx", "G#x/Abx", "Ax", "A#x/Bbx", "Bx" };
		int midiKey = PitchConverter.hertzToMidiKey(pitchInHertz);
		int noteIndex = (midiKey) % 12;
		int octaveIndex = octaveIndex();
		name = noteNames[noteIndex].replace("x", "" + octaveIndex);
		return name;
	}



	public static Pitch getInstance(PitchUnit unit,double value){
		double hertzValue = Double.MAX_VALUE;
		switch (unit) {
			case ABSOLUTE_CENTS:
				hertzValue = PitchConverter.absoluteCentToHertz(value);
				break;
			case RELATIVE_CENTS:
				throw new IllegalArgumentException("Cannot convert relative cent value to absolute frequency. Pitch object creation failed.");
			case MIDI_KEY:
				hertzValue = PitchConverter.midiKeyToHertz((int) value);
				break;
			case MIDI_CENT:
				hertzValue = PitchConverter.midiCentToHertz(value);
				break;
			case HERTZ:
				hertzValue = value;
				break;
			default:
				throw new Error("Unsupported unit: " + unit.name());
		}
		return new Pitch(hertzValue);
	}

}
