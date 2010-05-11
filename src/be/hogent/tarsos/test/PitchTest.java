package be.hogent.tarsos.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import be.hogent.tarsos.pitch.Pitch;
import be.hogent.tarsos.pitch.PitchUnit;

public class PitchTest {

    @Test
    public void testIsWesternMusicalPitch() {
        Pitch[] invalid = { Pitch.getInstance(PitchUnit.MIDI_CENT, 69.5),
                Pitch.getInstance(PitchUnit.MIDI_CENT, 69.2), Pitch.getInstance(PitchUnit.MIDI_CENT, 68.8) };
        for (Pitch p : invalid) {
            assertFalse(p.isWesternMusicalPitch());
        }

        Pitch[] valid = { Pitch.getInstance(PitchUnit.MIDI_CENT, 69),
                Pitch.getInstance(PitchUnit.MIDI_CENT, 70.1), Pitch.getInstance(PitchUnit.MIDI_CENT, 0.1),
                Pitch.getInstance(PitchUnit.MIDI_CENT, 112.1999),
                Pitch.getInstance(PitchUnit.MIDI_CENT, 111.9056), };

        for (Pitch p : valid) {
            assertTrue(p.isWesternMusicalPitch());
        }

    }

    @Test
    public void testNoteNames() {
        assertEquals("A-1", Pitch.getInstance(PitchUnit.HERTZ, 13.75).noteName());
        assertEquals("A0", Pitch.getInstance(PitchUnit.HERTZ, 27.5).noteName());
        assertEquals("A1", Pitch.getInstance(PitchUnit.HERTZ, 55.0).noteName());
        assertEquals("A2", Pitch.getInstance(PitchUnit.HERTZ, 110.0).noteName());
        assertEquals("A3", Pitch.getInstance(PitchUnit.HERTZ, 220.0).noteName());
        assertEquals("A4", Pitch.getInstance(PitchUnit.HERTZ, 440.0).noteName());
        assertEquals("A4", Pitch.getInstance(PitchUnit.HERTZ, 435.0).noteName());
        assertEquals("A4", Pitch.getInstance(PitchUnit.HERTZ, 441.0).noteName());
        assertEquals("A5", Pitch.getInstance(PitchUnit.HERTZ, 880.0).noteName());
        assertEquals("A6", Pitch.getInstance(PitchUnit.HERTZ, 1760.0).noteName());
        assertEquals("C-1", Pitch.getInstance(PitchUnit.HERTZ, 8.1785).noteName());
        assertEquals("D#4/Eb4", Pitch.getInstance(PitchUnit.HERTZ, 311.13).noteName());
    }

}
