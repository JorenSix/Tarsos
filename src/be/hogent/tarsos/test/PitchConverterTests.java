package be.hogent.tarsos.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.Random;

import org.junit.Test;

import be.hogent.tarsos.pitch.PitchConverter;
import be.hogent.tarsos.util.ConfKey;
import be.hogent.tarsos.util.Configuration;

public class PitchConverterTests {

    @Test
    public void testHertzToMidiCent() {

        assertEquals(69, PitchConverter.hertzToMidiCent(440.0), 0.00000001);
        assertEquals(81, PitchConverter.hertzToMidiCent(880.0), 0.00000001);

        Random r = new Random();
        for (int i = 0; i < 10000; i++) {
            // random test and reference pitch +- between 20 and 20kHz
            double referencePitch = r.nextDouble() * 20000 + 20;
            double testPitch = r.nextDouble() * 20000 + 20;
            // calculate the difference in cents between the pitches
            double absoluteCentValueReference = PitchConverter.hertzToAbsoluteCent(referencePitch);
            double absoluteCentValueTest = PitchConverter.hertzToAbsoluteCent(testPitch);
            double differenceInCents = absoluteCentValueTest - absoluteCentValueReference;
            // calculate the actual and expected value
            // The reference pitch converted to midicents added to the
            // difference is expected
            double expected = PitchConverter.hertzToMidiCent(referencePitch) + differenceInCents / 100;
            double actual = PitchConverter.hertzToMidiCent(testPitch);
            assertEquals(actual, expected, 0.00000001);
        }
    }

    @Test
    public void testMidiCentToHertz() {
        assertEquals(440, PitchConverter.midiCentToHertz(69), 0.00000001);
        assertEquals(880, PitchConverter.midiCentToHertz(81), 0.00000001);

        Random r = new Random();
        for (int midiKey = 0; midiKey < 126; midiKey++) {
            double cent = r.nextDouble();
            double midiCent = midiKey + cent;
            assertTrue(PitchConverter.midiCentToHertz(midiCent) >= PitchConverter.midiKeyToHertz(midiKey));
            assertTrue(PitchConverter.midiCentToHertz(midiCent) <= PitchConverter.midiKeyToHertz(midiKey + 1));
        }
    }

    @Test
    public void testhertzToCent() {
        double reference_frequency = Configuration.getDouble(ConfKey.absolute_cents_reference_frequency);
        assertEquals(0, PitchConverter.hertzToAbsoluteCent(reference_frequency), 0.00001);
        assertTrue(0 > PitchConverter.hertzToAbsoluteCent(reference_frequency - 10));
        assertTrue(0 < PitchConverter.hertzToAbsoluteCent(reference_frequency + 10));
        assertEquals(0, PitchConverter.hertzToAbsoluteCent(261.62556531) % 1200, 0.00001); // C4
    }

}
