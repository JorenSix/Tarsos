/**
 */
package be.hogent.tarsos.pitch;

import java.util.ArrayList;
import java.util.List;

import be.hogent.tarsos.util.FileUtils;

/**
 * @author Joren Six
 */
public class TartiniPitchParser {

    public List<Sample> parse(final String fileName) {
        final List<Sample> samples = new ArrayList<Sample>();
        final String contents = FileUtils.readFile(fileName);
        final String[] lines = contents.split("\n");
        for (int i = 1; i < lines.length; i++) {
            final String[] data = lines[i].split(" +");
            final double time = Double.parseDouble(data[1]);
            final double midiCents = Double.parseDouble(data[2]);
            final Pitch pitch = Pitch.getInstance(PitchUnit.MIDI_CENT, midiCents);
            final Sample s = new Sample((long) (time * 1000), pitch.getPitch(PitchUnit.HERTZ));
            samples.add(s);
        }
        return samples;
    }

}
