package be.hogent.tarsos.test;

import org.junit.Test;

import be.hogent.tarsos.pitch.PitchFunctions;
import be.hogent.tarsos.util.histogram.CorrelationMeasure;
import be.hogent.tarsos.util.histogram.Histogram;

public class CorrelationTests {

    public Histogram buildTable() {
        Histogram table = new Histogram(0, 1, 2, false, false);

        // 5 values in bin 1.0
        table.add(0.5);
        table.add(0.5);
        table.add(0.6);
        table.add(0.2);
        table.add(0.1);

        // 4 values in bin 2
        table.add(1.0);
        table.add(1.2);
        table.add(1.3);
        table.add(1.4);

        return table;
    }

    @Test
    public void testPerfectCorrelation() {
        Histogram h = buildTable();
        for (CorrelationMeasure correlationMeasure : CorrelationMeasure
                .values()) {
            System.out.println(correlationMeasure.name());
            System.out.println(h.correlation(h, correlationMeasure));
            // h.plotAutoCorrelation(correlationMeasure);
        }

        h = PitchFunctions
                .readFrequencyTable("src/tarsos/test/data/african_octave_frequency_table.txt");
        Histogram otherHistogram = PitchFunctions
                .readFrequencyTable("src/tarsos/test/data/other_african_octave_frequency_table.txt");

        otherHistogram = otherHistogram.normalize();
        h = h.normalize();

        for (CorrelationMeasure correlationMeasure : CorrelationMeasure
                .values()) {
            System.out.println(correlationMeasure.name());
            System.out.println(h
                    .correlation(otherHistogram, correlationMeasure));
            h.plotCorrelation(otherHistogram, correlationMeasure);
        }
    }
}
