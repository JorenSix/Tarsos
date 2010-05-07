package be.hogent.tarsos.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;

import org.junit.Test;

import be.hogent.tarsos.pitch.PitchFunctions;
import be.hogent.tarsos.util.SimplePlot;
import be.hogent.tarsos.util.histogram.CorrelationMeasure;
import be.hogent.tarsos.util.histogram.Histogram;
import be.hogent.tarsos.util.histogram.ToneScaleHistogram;

public class HistogramTests {

    public Histogram buildTable() {
        Histogram table = new Histogram(0, 2, 2);

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
    public void testCreateToneScale() {
        double[] peaksA = { 100, 450, 500 };
        Histogram a = ToneScaleHistogram.createToneScale(peaksA, null, null, null);

        double[] peaksB = { 200, 550 };
        double[] weights = { 200, 300 };
        double[] widths = { 25, 30 };
        double[] standardDeviations = { 1, 1.2 };

        Histogram b = ToneScaleHistogram.createToneScale(peaksB, weights, widths, standardDeviations);
        a.plotCorrelation(b, CorrelationMeasure.INTERSECTION);

        SimplePlot p = new SimplePlot("Gaussian_test_a");
        p.addData(0, a);
        p.save();

        p = new SimplePlot("Gaussian_test_b");
        p.addData(0, b);
        p.save();
    }

    /**
     * Tests the correlation between two FrequencyTables (histograms) with
     * intersection.
     */
    @Test
    public void testCorrelation() {
        Histogram table = buildTable();
        Histogram otherTable = buildTable();

        assertTrue(1.0 == table.correlation(otherTable));
        assertTrue(1.0 == otherTable.correlation(table));

        otherTable.add(0.5);

        assertTrue(1.0 != table.correlation(otherTable));
        assertTrue(otherTable.correlation(table) == table.correlation(otherTable));
        // area otherTable = 5*1 + 4*1 = 10
        // area thisTable = 4*1 + 5*1 = 9
        // correlation = matching area/biggestArea
        // matching area = 9
        // biggest area = 10
        assertTrue(otherTable.correlation(table) == 9.0 / 10.0);

        // area otherTable = 5*1 + *1 = 10
        // area thisTable = 6*1 + 5*1 = 11
        // correlation = matching area/biggestArea
        // matching area = 9
        // biggest area = 11
        otherTable.add(1.3);
        assertTrue(otherTable.correlation(table) == 9.0 / 11.0);
    }

    @Test
    public void testCorrelationWithDisplacement() {

        System.out.println();
        Histogram table = new Histogram(0, 3, 3);
        // 1 value in bin 1
        table.add(0.5);
        // 3 values in bin 2
        table.add(1.5);
        table.add(1.4);
        table.add(1.6);
        // 1 values in bin 3
        table.add(2.3);

        Histogram otherTable = new Histogram(table);
        // 1 value in bin 1
        otherTable.add(0.5);
        // 1 values in bin 2
        otherTable.add(1.5);
        // 3 values in bin 3
        otherTable.add(2.3);
        otherTable.add(2.4);
        otherTable.add(2.6);

        assertTrue(3.0 / 5.0 == table.correlation(otherTable));
        assertTrue(5.0 / 5.0 == table.correlationWithDisplacement(1, otherTable));
        assertTrue(3.0 / 5.0 == table.correlationWithDisplacement(2, otherTable));

        assertTrue(table.correlation(otherTable) == table.correlationWithDisplacement(3, otherTable));

        assertTrue(1 == table.displacementForOptimalCorrelation(otherTable));
        assertTrue(1.0 == table.correlationWithDisplacement(1, otherTable));
        assertTrue(table.correlationWithDisplacement(2, otherTable) == table.correlationWithDisplacement(-1,
                otherTable));
        assertTrue(3.0 / 5.0 == table.correlationWithDisplacement(0, otherTable));
        assertTrue(3.0 / 5.0 == table.correlationWithDisplacement(2, otherTable));

        otherTable = new Histogram(otherTable);
        // 3 value in bin 1
        otherTable.add(0.5);
        otherTable.add(0.4);
        otherTable.add(0.6);
        // 1 values in bin 2
        otherTable.add(1.5);
        // 1 values in bin 3
        otherTable.add(2.3);

        assertTrue(1.0 == table.correlationWithDisplacement(2, otherTable));

        // realistic data test
        // data = octave [0-1200]
        // classwidth = 6
        table = PitchFunctions
        .readFrequencyTable("src/be/hogent/tarsos/test/data/african_octave_frequency_table.txt");
        otherTable = new Histogram(table);

        // create an other table with the same values but 30 cents higher
        for (Double key : table.keySet()) {
            Double displacedKey = (key + 30) % table.getStop();
            otherTable.setCount(displacedKey, table.getCount(key));// 30 cents
            // displaced
        }

        // calculate optimal displacement
        int displacement = table.displacementForOptimalCorrelation(otherTable);
        // check if it is 30 cents
        assertTrue(30 == (int) (displacement * table.getClassWidth()));
        // correlation without displacement should be smaller
        assertTrue(table.correlation(otherTable) < table
                .correlationWithDisplacement(displacement, otherTable));
        // correlation with displacement should be one: all the same values are
        // used
        assertTrue(1.0 == table.correlationWithDisplacement(displacement, otherTable));

        // same test as above but with negative displacement
        for (Double key : table.keySet()) {
            Double displacedKey = (key - 30 + table.getStop()) % table.getStop();
            otherTable.setCount(displacedKey, table.getCount(key));// 30 cents
            // displaced
        }
        displacement = table.displacementForOptimalCorrelation(otherTable);
        assertTrue(-30 == (int) (displacement * table.getClassWidth()));
        assertTrue(table.correlation(otherTable) < table
                .correlationWithDisplacement(displacement, otherTable));
        assertTrue(1.0 == table.correlationWithDisplacement(displacement, otherTable));

        table = PitchFunctions.readFrequencyTable("src/tarsos/test/data/african_octave_frequency_table.txt");
        otherTable = PitchFunctions
        .readFrequencyTable("src/tarsos/test/data/other_african_octave_frequency_table.txt");
        table = table.normalize();
        otherTable = otherTable.normalize();
        displacement = table.displacementForOptimalCorrelation(otherTable);
        SimplePlot plot = new SimplePlot();
        plot.addData(0, table);

        for (double current = otherTable.getStart() + otherTable.getClassWidth() / 2; current <= otherTable
        .getStop(); current += otherTable.getClassWidth()) {
            double displacedValue = (current + displacement * otherTable.getClassWidth())
            % (otherTable.getNumberOfClasses() * otherTable.getClassWidth());
            plot.addData(1, current, otherTable.getCount(displacedValue));
        }
        plot.save();
    }

    /**
     * Tests if the table assigns the values to the right bins and checks if the
     * counts are valid.
     */
    @Test
    public void testCorrectCount() {
        Histogram table = buildTable();
        assertEquals(5, table.getCount(0.5));
        assertEquals(4, table.getCount(1.5));
    }

    @Test
    public void testHistogramSum() {
        Histogram table = buildTable();
        Histogram sum = table.add(table);
        assertEquals(sum.getCount(0.1), 10);
        assertEquals(sum.getCount(1.2), 8);

        /*
         * Visual test:
         * 
         * 
         * table =PitchFunctions.readFrequencyTable(
         * "src/tarsos/test/data/african_octave_frequency_table.txt"); table =
         * table.normalize(); Histogram otherTable =
         * PitchFunctions.readFrequencyTable
         * ("src/tarsos/test/data/other_african_octave_frequency_table.txt");
         * otherTable = otherTable.normalize(); sum = table.add(otherTable);
         * SimplePlot p = new SimplePlot("histogram_sum"); p.addData(0, table);
         * p.addData(1, otherTable); p.addData(2, sum); p.save();
         */
    }

    @Test
    public void testSmoothed() {
        Histogram table = PitchFunctions
        .readFrequencyTable("src/be/hogent/tarsos/test/data/other_african_octave_frequency_table.txt");
        // table = table.normalize();
        Histogram smoothedWeighted = table.smooth(true, 2);
        Histogram smoothed = table.smooth(false, 2);
        SimplePlot p = new SimplePlot("histogram_smoothed");
        p.addData(0, table);
        p.addLegend(0, "original");
        p.addData(1, smoothedWeighted);
        p.addLegend(1, "Smoothed weighted");
        p.addData(2, smoothed);
        p.addLegend(2, "Smoothed");
        p.save();
    }

    @Test
    public void testGaussianFilter() {
        Histogram table = PitchFunctions
        .readFrequencyTable("src/be/hogent/tarsos/test/data/other_african_octave_frequency_table.txt");
        Histogram gaussian = table.gaussianSmooth(1.0);
        Histogram doubleGaussian = gaussian.gaussianSmooth(1.0);
        SimplePlot p = new SimplePlot("histogram_gaussian");
        p.addData(0, table);
        p.addLegend(0, "original");
        p.addData(1, gaussian);
        p.addLegend(1, "gaussian");
        p.addData(2, doubleGaussian);
        p.addLegend(2, "double gaussian");
        p.save();
    }

    @Test
    public void testWrappingBehaviour() {
        Histogram h = new Histogram(1.0, 13.0, 12, true, false);
        // add some values for class with class middle
        // 4 at value 1.5
        h.add(1.0);
        h.add(13.0);
        h.add(24.6);
        h.add(25.2);
        h.add(25.5);
        assertEquals(4, h.getCount(1.0));
        assertEquals(4, h.getCount(13.0));
        assertEquals(h.getCount(25.5), h.getCount(1.0));

        h.add(14.0);
        h.add(2.0);
        assertEquals(h.getCount(2.0), h.getCount(14.0));
        assertEquals(2, h.getCount(14.0));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testNonWrappingIllegalArgument() {
        Histogram h = new Histogram(1.0, 13.0, 12, false, false);
        h.add(24.6);
    }

    @Test
    public void testIgnoreValuesOusideRange() {
        Histogram h = new Histogram(7.0, 13.0, 12, false, true);
        h.add(5656);
        h.add(565);
        h.add(791);
        h.add(2);
        h.add(8);
        assertEquals(1, h.getSumFreq());
    }

    @Test
    public void testHistogramMean() {

        ArrayList<Histogram> histograms = new ArrayList<Histogram>();
        histograms.add(buildTable());
        histograms.add(buildTable());
        histograms.add(buildTable());
        histograms.add(buildTable());

        Histogram mean = Histogram.mean(histograms);

        assertEquals(mean.getCount(0.1), 5);
        assertEquals(mean.getCount(1.2), 4);

        histograms.add(buildTable());
        mean = Histogram.mean(histograms);
        assertEquals(mean.getCount(1.2), 4);

        /*
         * Visual test:
         * 
         * 
         * Histogram table =PitchFunctions.readFrequencyTable(
         * "src/tarsos/test/data/african_octave_frequency_table.txt"); table =
         * table.normalize(); Histogram otherTable =
         * PitchFunctions.readFrequencyTable
         * ("src/tarsos/test/data/other_african_octave_frequency_table.txt");
         * otherTable = otherTable.normalize();
         * 
         * histograms = new ArrayList<Histogram>(); histograms.add(table);
         * histograms.add(otherTable);
         * 
         * mean = Histogram.mean(histograms);
         * 
         * SimplePlot p = new SimplePlot("histogram_mean"); p.addData(0, table);
         * p.addData(1, otherTable); p.addData(2, mean); p.save();
         */
    }

    @Test
    public void testRaiseHistogram() {
        Histogram histogram = buildTable();
        histogram.raise(2);
        assertEquals(25, histogram.getCount(0.1));
        assertEquals(16, histogram.getCount(1.2));
    }

    /**
     * Tests some cases with fractional class widths.
     */
    @Test
    public void testHistogramDoubleKey() {
        // two class middles : 0.5 and 1.0
        Histogram h = new Histogram(0.25, 1.25, 2);
        h.add(1.0).add(1.1).add(0.9).add(0.75);
        h.add(0.5).add(0.5).add(0.5).add(0.6);
        System.out.println(h);
        assertEquals(4, h.getCount(0.5));
        assertEquals(4, h.getCount(1.0));
        h = new Histogram(0, 5, 5);
        assertEquals(0.5, h.keySet().toArray()[0]);
        assertEquals(5, h.keySet().size());

        h = new Histogram(0.5, 1.5, 10);
        for (double d = 0.5; d < 1.5; d = Math.floor((d + 0.001) * 10000.0) / 10000.0) {
            long before = h.getCount(d);
            h.add(d);
            long after = h.getCount(d);
            assertEquals("key : " + d, before + 1, after);
        }
        h.add(0.5).add(0.5).add(1.2).add(0.9);
        System.out.println(h.toString(true));
    }

}
