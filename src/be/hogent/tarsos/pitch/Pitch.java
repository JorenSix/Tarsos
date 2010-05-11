package be.hogent.tarsos.pitch;

/**
 * A class representing pitch. Can be used to convert pitch units or to base
 * pitch interval calculations on.
 * 
 * @author Joren Six
 */
public class Pitch {

    /**
     * The pitch in Hz.
     */
    private final double pitchInHertz;

    /**
     * Create a new pitch object with a certain pitch.
     * 
     * @param pitchInHertz
     */
    private Pitch(double pitchInHertz) {
        this.pitchInHertz = pitchInHertz;
    }

    /**
     * Get the pitch value in a given unit.
     * 
     * @param unit
     *            The requested unit.
     * @return A pitch value in the requested unit.
     */
    public double getPitch(PitchUnit unit) {
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

    /**
     * Calculates which octave this pitch falls into. The octave index is based
     * on MIDI keys. Keys [0,11] fall in octave -1, [11,23] in octave 0,...
     * 
     * @return The octave the pitch falls into, calculated using MIDI key.
     * @exception IllegalArgumentException
     *                If the pitch does not fall within the range of valid MIDI
     *                KEYS.
     */
    public int octaveIndex() {
        int midiKey = (int) getPitch(PitchUnit.MIDI_KEY);
        return (midiKey / 12) - 1;
    }

    /**
     * Returns the name of the MIDI key corresponding to the given hertzValue.
     * The MIDI key is the key returned by the convertHertzToMidiKey method.
     * 
     * @return A note name like C3, A4 or A3#/B3b.
     * @exception IllegalArgumentException
     *                When the hertzValue is outside the valid MIDI key range.
     */
    public String noteName() {
        String name = "";
        // The x is replaced by the octave index
        String[] noteNames = { "Cx", "C#x/Dbx", "Dx", "D#x/Ebx", "Ex", "Fx", "F#x/Gbx", "Gx", "G#x/Abx",
                "Ax", "A#x/Bbx", "Bx" };
        int midiKey = PitchConverter.hertzToMidiKey(pitchInHertz);
        int noteIndex = midiKey % 12;
        int octaveIndex = octaveIndex();
        name = noteNames[noteIndex].replace("x", "" + octaveIndex);
        return name;
    }

    /**
     * A pitch is seen as a western musical pitch if it is less than 20 cents
     * removed from the 'correct' pitch. The correct pitch is tuned using A4 =
     * 440Hz.
     * 
     * @return True if the pitch is western and musical, false otherwise.
     */
    public boolean isWesternMusicalPitch() {
        double midiCent = getPitch(PitchUnit.MIDI_CENT);
        double midiKey = getPitch(PitchUnit.MIDI_KEY);
        return Math.abs(midiCent - (int) midiKey) < 0.2;
    }

    /**
     * Return a new pitch object using value in a certain unit.
     * 
     * @param unit
     *            The unit of the pitch value.
     * @param value
     *            The value Itself.
     * @return A new Pitch object.
     * @exception IllegalArgumentException
     *                If RELATIVE_CENTS is given: Cannot convert relative cent
     *                value to absolute frequency.
     */
    public static Pitch getInstance(PitchUnit unit, double value) {
        double hertzValue = Double.MAX_VALUE;
        switch (unit) {
        case ABSOLUTE_CENTS:
            hertzValue = PitchConverter.absoluteCentToHertz(value);
            break;
        case RELATIVE_CENTS:
            throw new IllegalArgumentException(
            "Cannot convert relative cent value to absolute frequency. Pitch object creation failed.");
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
