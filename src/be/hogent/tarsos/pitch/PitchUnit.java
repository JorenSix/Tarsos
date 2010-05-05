package be.hogent.tarsos.pitch;

/**
 * Defines the unit of the pitch value.
 * 
 */
public enum PitchUnit {
    /**
     * Oscillations per second.
     */
    HERTZ,

    /**
     * Number of cents compared to the "absolute zero" a configured, low
     * frequency. By default C0 (16Hz) is used.
     */
    ABSOLUTE_CENTS,

    /**
     * Number of cents (between 0 and 1200) relative to the start of the octave.
     * The first octave starts at "absolute zero" a configured, low frequency.
     */
    RELATIVE_CENTS,

    /**
     * An integer from 0 to 127 that represents the closest MIDI key. All Hz
     * values above 13289.7300 Hz are mapped to 127, all values below a certain
     * value are mapped to 0.
     */
    MIDI_KEY,

    /**
     * An double from 0 to 127 that represents the closest MIDI key. All values
     * above (13289.7300 Hz + 99,99... cents) are mapped to 127,99999... All
     * values below a certain value are mapped to 0.00.
     */
    MIDI_CENT;
}