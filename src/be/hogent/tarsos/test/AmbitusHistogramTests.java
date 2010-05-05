package be.hogent.tarsos.test;

import org.junit.Test;

import be.hogent.tarsos.pitch.PitchFunctions;
import be.hogent.tarsos.util.SimplePlot;
import be.hogent.tarsos.util.histogram.AmbitusHistogram;
import be.hogent.tarsos.util.histogram.CorrelationMeasure;
import be.hogent.tarsos.util.histogram.Histogram;
import be.hogent.tarsos.util.histogram.ToneScaleHistogram;

public class AmbitusHistogramTests {

    private AmbitusHistogram createTestAmbitus() {
        Histogram range = PitchFunctions
                .readFrequencyTable("src/be/hogent/tarsos/test/data/african_range_frequency_table.txt");
        AmbitusHistogram ambitus = new AmbitusHistogram();
        for (double key : range.keySet()) {
            for (int i = 0; i < range.getCount(key); i++) {
                ambitus.add(key);
            }
        }
        return ambitus;
    }

    @Test
    public void testExportToScalaScaleFileFormat() {
        ToneScaleHistogram histo = createTestAmbitus().toneScaleHistogram();
        histo.exportToScalaScaleFileFormat("data/tests/scale.scl", "Test tone scale");
        SimplePlot plot = new SimplePlot("Exported tone scale");
        plot.addData(0, histo);
        plot.save();
    }

    @Test
    public void testMostEnergyRichOctaves() {
        AmbitusHistogram ambitus = createTestAmbitus();
        ToneScaleHistogram toneScale = ambitus.mostEnergyRichOctaves(4);

        toneScale.normalize();

        toneScale.plotCorrelation(ambitus.toneScaleHistogram().normalize(), CorrelationMeasure.INTERSECTION);

        SimplePlot correlationPlot = new SimplePlot("rich"); // plots the
        correlationPlot.addData(0, ambitus.toneScaleHistogram().normalize()); // plots
        // the
        // other
        correlationPlot.addData(1, toneScale.normalize());
        correlationPlot.save();
    }

    @Test
    public void testToneScalePlotting() {
        AmbitusHistogram ambitus = createTestAmbitus();
        ambitus.plotToneScaleHistogram("Total tone scale.png", false);
        ambitus.plotToneScaleHistogram("Split tone scale.png", true);
    }
}
