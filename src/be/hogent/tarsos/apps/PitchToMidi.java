/**
 */
package be.hogent.tarsos.apps;

import be.hogent.tarsos.pitch.Pitch;
import be.hogent.tarsos.pitch.PitchConverter;
import be.hogent.tarsos.pitch.PitchUnit;

/**
 * @author Joren Six
 */
public class PitchToMidi extends AbstractTarsosApp {

    /*
     * (non-Javadoc)
     * 
     * @see be.hogent.tarsos.apps.AbstractTarsosApp#description()
     */
    @Override
    public String description() {
        return "Listens to incoming audio and fires midi events if the detected "
        + "pitch is close to a predefined pitch class.";
    }

    /*
     * (non-Javadoc)
     * 
     * @see be.hogent.tarsos.apps.AbstractTarsosApp#name()
     */
    @Override
    public String name() {
        return "pitch_to_midi";
    }

    /*
     * (non-Javadoc)
     * 
     * @see be.hogent.tarsos.apps.AbstractTarsosApp#run(java.lang.String[])
     */
    @Override
    public void run(final String... args) {

        final float pitch = 0.0f;
        final double midiCentValue = PitchConverter.hertzToMidiCent(pitch);
        int newKeyDown = -1;
        // 'musical' pitch detected ?
        if (Math.abs(midiCentValue - (int) midiCentValue) < 0.3 && midiCentValue < 128 && midiCentValue >= 0) {
            newKeyDown = (int) midiCentValue;
            final String lastDetectedNote = "Name: " + Pitch.getInstance(PitchUnit.HERTZ, pitch).noteName()
                    + "\nFrequency: " + ((int) pitch) + "Hz \t" + " MIDI note:"
                    + PitchConverter.hertzToMidiCent(pitch);
        }
    }
}
