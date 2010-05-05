package be.hogent.tarsos.test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Random;

import org.junit.Test;

import be.hogent.tarsos.pitch.PitchFunctions;
import be.hogent.tarsos.util.SimplePlot;
import be.hogent.tarsos.util.histogram.Histogram;
import be.hogent.tarsos.util.histogram.ToneScaleHistogram;

public class ToneScaleHistogramTest {

    @Test
    public void createToneScaleTest() {
        // test edge wrapping:

        double[] peaks = { 18, 303, 603, 903, 1180 };
        ToneScaleHistogram histo = ToneScaleHistogram.createToneScale(peaks, null, null, null);
        assertTrue(histo.getCount(0) > 0);
        assertTrue(histo.getCount(0) == histo.getCount(1200));

        SimplePlot plot = new SimplePlot("Theoretic tone scale");
        plot.addData(0, histo);
        plot.save();
    }

    @Test
    public void isToneScaleMelodicTest() {
        double[] peaks = { 18, 303, 603, 903, 1180 };
        ToneScaleHistogram melodicToneScale = ToneScaleHistogram.createToneScale(peaks, null, null, null);
        assertTrue(melodicToneScale.isMelodic());

        ToneScaleHistogram noiseToneScale = new ToneScaleHistogram();
        Random random = new Random();
        for (Double key : noiseToneScale.keySet()) {
            noiseToneScale.setCount(key, (long) (random.nextDouble() * 1000));
        }
        noiseToneScale.plot("data/tests/noise.png", "noise");
        assertFalse(noiseToneScale.isMelodic());

        Histogram histo = PitchFunctions
                .readFrequencyTable("src/be/hogent/tarsos/test/data/african_octave_frequency_table.txt");
        ToneScaleHistogram africanMelodicToneScale = new ToneScaleHistogram();
        for (Double key : africanMelodicToneScale.keySet()) {
            africanMelodicToneScale.setCount(key, histo.getCount(key));
        }
        assertTrue(africanMelodicToneScale.isMelodic());

    }

}
