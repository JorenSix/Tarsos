package be.hogent.tarsos.pitch;

/**
 * Defines the unit of the pitch value.
 */
public enum PitchUnit {
	/**
	 * Oscillations per second.
	 */
	HERTZ("Hertz"),

	/**
	 * Number of cents compared to the "absolute zero" a configured, low
	 * frequency. By default C0 (16Hz) is used.
	 */
	ABSOLUTE_CENTS("Absolute cents"),

	/**
	 * Number of cents (between 0 and 1200) relative to the start of the octave.
	 * The first octave starts at "absolute zero" a configured, low frequency.
	 */
	RELATIVE_CENTS("Relative cents"),

	/**
	 * An integer from 0 to 127 that represents the closest MIDI key. All Hz
	 * values above 13289.7300 Hz are mapped to 127, all values below a certain
	 * value are mapped to 0.
	 */
	MIDI_KEY("MIDI key"),

	/**
	 * An double from 0 to 127 that represents the closest MIDI key. All values
	 * above (13289.7300 Hz + 99,99... cents) are mapped to 127,99999... All
	 * values below a certain value are mapped to 0.00.
	 */
	MIDI_CENT("MIDI cent");

	private final String humanName;

	/**
	 * Creates a new pitch unit with a human name.
	 * 
	 * @param name
	 *            The human name.
	 */
	private PitchUnit(final String name) {
		humanName = name;
	}

	/**
	 * 
	 * @return A nicer description of the name of the unit.
	 */
	public String getHumanName() {
		return humanName;
	}

	/**
	 * Converts a pitch in hertz to the current unit.
	 * 
	 * @param pitchInHertz
	 *            The pitch in hertz.
	 * @return A converted pitch value;
	 */
	public double convertFromHertz(final double hertzValue) {
		final double convertedPitch;
		switch (this) {
		case ABSOLUTE_CENTS:
			convertedPitch = PitchConverter.hertzToAbsoluteCent(hertzValue);
			break;
		case HERTZ:
			convertedPitch = hertzValue;
			break;
		case MIDI_CENT:
			convertedPitch = PitchConverter.hertzToMidiCent(hertzValue);
			break;
		case MIDI_KEY:
			convertedPitch = PitchConverter.hertzToMidiKey(hertzValue);
			break;
		case RELATIVE_CENTS:
			convertedPitch = PitchConverter.hertzToRelativeCent(hertzValue);
			break;
		default:
			throw new AssertionError("Unknown pitch unit: " + getHumanName());
		}
		return convertedPitch;
	}
}