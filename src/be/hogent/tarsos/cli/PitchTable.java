package be.hogent.tarsos.cli;

import java.io.PrintStream;

import joptsimple.OptionParser;
import joptsimple.OptionSet;
import be.hogent.tarsos.sampled.pitch.Pitch;
import be.hogent.tarsos.sampled.pitch.PitchUnit;

/**
 * Generates a table with pitches in various units.
 * @author Joren Six
 */
public final class PitchTable extends AbstractTarsosApp {
    /**
     * Defines the number of available MIDI keys.
     */
    private static final int MAX_MIDI_KEY = 128;

    /*
     * (non-Javadoc)
     * @see be.hogent.tarsos.cli.AbstractTarsosApp#run(java.lang.String[])
     */
    @Override
    public void run(final String... args) {
        final OptionParser parser = new OptionParser();
        final OptionSet options = parse(args, parser, this);
        if (isHelpOptionSet(options)) {
            printHelp(parser);
        } else {
            printTable();
        }
    }

    private void printTable() {
        final PrintStream stdOut = System.out;
        stdOut.printf("%4s %10s %16s %14s %15s %10s\n", "MIDI", "NAME", "FREQUENCY", "ABS CENTS",
                "REL CENTS", "OCTAVE");
        stdOut.println("---------------------------------------------------------------------------");
        for (int i = 0; i < PitchTable.MAX_MIDI_KEY; i++) {
            final Pitch pitch = Pitch.getInstance(PitchUnit.MIDI_KEY, i);

            final double frequency = pitch.getPitch(PitchUnit.HERTZ);
            final double absoluteCents = pitch.getPitch(PitchUnit.ABSOLUTE_CENTS);
            final double relativeCents = pitch.getPitch(PitchUnit.RELATIVE_CENTS);
            final int octaveIndex = pitch.octaveIndex();

            stdOut.printf("%4d %10s %14.5fHz %14.0f  %14.0f %10d\n", i, pitch.noteName(), frequency,
                    absoluteCents, relativeCents, octaveIndex);
        }
    }


    /*
     * (non-Javadoc)
     * @see be.hogent.tarsos.cli.AbstractTarsosApp#description()
     */
    @Override
    public String description() {
        return "Prints a table with pitches in different units.";
    }

    /*
     * (non-Javadoc)
     * @see be.hogent.tarsos.cli.AbstractTarsosApp#name()
     */
    @Override
    public String name() {
        return "pitch_table";
    }
}
