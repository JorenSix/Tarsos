package be.hogent.tarsos.apps;

import be.hogent.tarsos.pitch.Pitch;
import be.hogent.tarsos.pitch.PitchUnit;

/**
 * Generates a table with pitches in various units.
 * @author Joren Six
 */
public class PitchTable implements TarsosApplication {

    /**
     * Defines the number of available MIDI keys.
     */
    private static final int NUMBER_OF_MIDI_KEYS = 128;

    /*
     * (non-Javadoc)
     * @see be.hogent.tarsos.apps.TarsosApplication#run(java.lang.String[])
     */
    @Override
    public void run(final String... args) {
        System.out.printf("%4s %10s %16s %14s %15s %10s\n", "MIDI", "NAME", "FREQUENCY", "ABS CENTS",
                "REL CENTS", "OCTAVE");
        System.out.println("---------------------------------------------------------------------------");
        for (int i = 0; i < PitchTable.NUMBER_OF_MIDI_KEYS; i++) {
            Pitch p = Pitch.getInstance(PitchUnit.MIDI_KEY, i);

            double frequency = p.getPitch(PitchUnit.HERTZ);
            double absoluteCents = p.getPitch(PitchUnit.ABSOLUTE_CENTS);
            double relativeCents = p.getPitch(PitchUnit.RELATIVE_CENTS);
            int octaveIndex = p.octaveIndex();

            System.out.printf("%4d %10s %14.5fHz %14.0f  %14.0f %10d\n", i, p.noteName(), frequency,
                    absoluteCents, relativeCents, octaveIndex);
        }

    }

    @Override
    public String description() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public String name() {
        // TODO Auto-generated method stub
        return null;
    }
}
