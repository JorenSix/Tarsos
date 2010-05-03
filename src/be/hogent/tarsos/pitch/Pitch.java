package be.hogent.tarsos.pitch;

import be.hogent.tarsos.pitch.Sample.PitchUnit;
import be.hogent.tarsos.util.ConfKey;
import be.hogent.tarsos.util.Configuration;

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

	/**
	 * Converts pitch from one unit to another (and back (and back (and back ...))).
	 * @author Joren Six
	 */
	public static class PitchConverter{
		private static final double reference_frequency =
			Configuration.getDouble(ConfKey.absolute_cents_reference_frequency);// C-1 =
																// 16.35 Hz
		private static final double log_two = Math.log(2.0);

		/**
		 * A MIDI key is an integer between 0 and 127, inclusive.
		 * Within a certain range every pitch is mapped to
		 * @param hertzValue
		 * @return
		 */
		public static int hertzToMidiKey(Double hertzValue) {
			int midiKey = (int) Math.round(hertzToMidiCent(hertzValue));
			if (midiKey < 0 || midiKey > 127)
				throw new IllegalArgumentException("MIDI is only defined between [" + midiKeyToHertz(0) + ","+ midiKeyToHertz(127) +"] " + hertzValue + "does not map to a MIDI key.");
			return midiKey;
		}

		public static double midiKeyToHertz(int midiKey){
			if (midiKey < 0 || midiKey > 127)
				throw new IllegalArgumentException("MIDI keys are values from 0 to 127, inclusive " + midiKey + " is invalid.");
			return midiCentToHertz(midiKey);
		}

		/**
		 * Folds the pitch values to one octave. E.g. 1203 becomes 3 and 956 remains
		 * 956, -3 is 1197
		 *
		 * @param pitchValuesInCent
		 *            a list of double values in cent
		 */
		public static double hertzToRelativeCent(double hertzValue) {
			double absoluteCentValue = hertzToAbsoluteCent(hertzValue);
			// make absoluteCentValue positive
			absoluteCentValue = absoluteCentValue >= 0 ? absoluteCentValue : Math
					.abs(1200 + absoluteCentValue);
			// so it can be folded to one octave
			return absoluteCentValue % 1200.0;
		}


		/**
		 * This method is not really practical.
		 * Maybe I will need it someday.
		 * @param relativeCent
		 * @return
		public static double relativeCentToHertz(double relativeCent){
			if (relativeCent < 0 || relativeCent >= 1200)
				throw new IllegalArgumentException("Relative cent values are values from 0 to 1199, inclusive " + relativeCent + " is invalid.");
			int defaultOctave = 5;
			int offset = defaultOctave * 1200;
			return absoluteCentToHertz(relativeCent + offset);
		}
		*/


		/**
		 * The reference frequency is configured. This is 16.35Hz is C0 on a piano
		 * keyboard with A4 tuned to 440 Hz. This means that 0 cents is C0; 1200 is
		 * C1; 2400 is C2; ... also -1200 cents is C-1
		 *
		 * @param hertzValue
		 * @return the converted value using the configured reference frequency
		 */
		/**
		 * @param hertzValue
		 * @return
		 */
		public static double hertzToAbsoluteCent(double hertzValue) {
			double pitchValueInAbsoluteCent = 0.0;
			if (hertzValue != 0)
				pitchValueInAbsoluteCent = 1200 * Math.log(hertzValue / reference_frequency) / log_two;
			return pitchValueInAbsoluteCent;
		}

		public static double absoluteCentToHertz(double absoluteCent){
			return reference_frequency * Math.pow(2, absoluteCent / 1200.0);
		}

		/**
		 * Converts a frequency in Hz to a MIDI CENT value using
		 * <code>(12 × log2 (f / 440)) + 69</code> <br>
		 * E.g.<br>
		 * <code>69.168 MIDI CENTS = MIDI NOTE 69  + 16,8 cents</code><br>
		 * <code>69.168 MIDI CENTS = 440Hz + x Hz</code>
		 *
		 * @param hertzValue
		 *            The pitch in Hertz.
		 * @return The pitch in MIDI cent.
		 */
		public static double hertzToMidiCent(double hertzValue) {
			double pitchValueInMidiCent = 0.0;
			if (hertzValue != 0)
				pitchValueInMidiCent = (12 * Math.log(hertzValue / 440) / log_two) + 69;
			return pitchValueInMidiCent;
		}

		public static double midiCentToHertz(double midiCent){
			return 440 * Math.pow(2, (midiCent - 69) / 12d );
		}
	}

}
