package be.hogent.tarsos.util.histogram;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import javax.imageio.ImageIO;

import ptolemy.plot.Plot;
import be.hogent.tarsos.util.ConfKey;
import be.hogent.tarsos.util.Configuration;
import be.hogent.tarsos.util.FileUtils;
import be.hogent.tarsos.util.SimplePlot;

/**
 * 
 * The AmbitusHistogram accepts values from 0 to 9600 cents or +- from 16Hz to
 * 40000Hz: the human hearing range is completely covered.<br>
 * The start and stop values can be configured.<br>
 * Values outside the defined range are ignored!
 * 
 * @author Joren Six
 */
public class AmbitusHistogram extends Histogram {

    private final List<ToneScaleHistogram> toneScaleHistogramPerOctave = new ArrayList<ToneScaleHistogram>();

    public AmbitusHistogram() {
        super(Configuration.getInt(ConfKey.ambitus_start), Configuration.getInt(ConfKey.ambitus_stop),
                1200 / Configuration.getInt(ConfKey.histogram_bin_width), false,// does
                // not
                // wrap
                true// ignore values outside human hearing range
        );

        // initialize the list of tone scales
        for (int value = Configuration.getInt(ConfKey.ambitus_start); value < Configuration
                .getInt(ConfKey.ambitus_stop); value += 1200) {
            toneScaleHistogramPerOctave.add(new ToneScaleHistogram());
        }

    }

    @Override
    public Histogram add(double value) {
        super.add(value);
        // keep a histogram for each octave
        int octaveIndex = (int) (value / 1200);
        if (toneScaleHistogramPerOctave.size() > octaveIndex && octaveIndex >= 0)
            toneScaleHistogramPerOctave.get(octaveIndex).add(value);
        return this;
    }

    /**
     * @param numberOfOctaves
     *            The number of energy rich octaves
     * @return a ToneScaleHistogram containing only samples from the
     *         numberOfOctaves most energy rich octaves.
     */
    public ToneScaleHistogram mostEnergyRichOctaves(int numberOfOctaves) {
        ToneScaleHistogram h = new ToneScaleHistogram();
        List<Integer> octavesOrderedByEnergy = octavesOrderedByEnergy();
        for (int i = 0; i < numberOfOctaves; i++) {
            int octaveIndex = octavesOrderedByEnergy.get(i);
            h.add(toneScaleHistogramPerOctave.get(octaveIndex));
        }
        return h;
    }

    /**
     * @return a list of octave indexes ordered by energy: the most energy rich
     *         first. The energy is defined by the number of collected samples
     *         in the interval E.g. [4,3,2,5,7,6,0,1]
     */
    private List<Integer> octavesOrderedByEnergy() {
        List<Integer> octaves = new ArrayList<Integer>();

        for (int i = 0; i < toneScaleHistogramPerOctave.size(); i++)
            octaves.add(i);

        Collections.sort(octaves, new Comparator<Integer>() {
            @Override
            public int compare(Integer o1, Integer o2) {
                Long energyFirst = AmbitusHistogram.this.toneScaleHistogramPerOctave.get(o1).getSumFreq();
                Long energySecond = AmbitusHistogram.this.toneScaleHistogramPerOctave.get(o2).getSumFreq();
                return energySecond.compareTo(energyFirst);
            }
        });
        return octaves;
    }

    /**
     * @return the ambitus folded to one octave (1200 cents)
     */
    public ToneScaleHistogram toneScaleHistogram() {
        ToneScaleHistogram summedToneScaleHistogram = new ToneScaleHistogram();
        for (ToneScaleHistogram histogram : toneScaleHistogramPerOctave) {
            summedToneScaleHistogram.add(histogram);
        }
        return summedToneScaleHistogram;
    }

    /**
     * Plot the complete ambitus
     * 
     * @param fileName
     */
    public void plotAmbitusHistogram(String fileName) {
        this.plotAmbitusHistogram(fileName, (int) getStart(), (int) getStop());
    }

    /**
     * Plot the ambitus but use only values between start and stop (cents)
     * 
     * @param fileName
     *            the plot is saved to this location.
     * @param start
     *            the starting value
     * @param stop
     *            stopping value
     */
    public void plotAmbitusHistogram(String fileName, int start, int stop) {
        SimplePlot plot = new SimplePlot();
        for (double current = start; current <= stop; current += this.getClassWidth()) {
            plot.addData(0, current, this.getCount(current));
        }
        plot.save(fileName);
    }

    /**
     * Saves a tone scale histogram plot with each octave separately or just the
     * total.
     * 
     * @param fileName
     *            the plot is saved to this location.
     * @param splitOctaves
     *            if true each octave is separated, otherwise only the total is
     *            shown.
     */
    public void plotToneScaleHistogram(String fileName, boolean splitOctaves) {
        ToneScaleHistogram summedToneScaleHistogram = toneScaleHistogram();

        if (splitOctaves) {
            Plot h = new Plot();
            h.setXRange(0, 1200);
            for (int dataset = 0; dataset < toneScaleHistogramPerOctave.size(); dataset++) {
                ToneScaleHistogram currentToneScaleHistogram = toneScaleHistogramPerOctave.get(dataset);
                for (double key : currentToneScaleHistogram.keySet()) {
                    long count = currentToneScaleHistogram.getCount(key);
                    for (int i = 0; i < dataset; i++)
                        count += toneScaleHistogramPerOctave.get(i).getCount(key);
                    h.addPoint(dataset, key, count, true);
                }
            }
            for (int dataset = 0; dataset < toneScaleHistogramPerOctave.size(); dataset++) {
                h.addLegend(dataset, "C" + dataset + "-B" + dataset + " [" + dataset * 1200 + "-"
                        + ((dataset * 1200) + 1199) + "]");
            }
            h.setSize(1680, 1050);
            h.setTitle(FileUtils.basename(fileName));
            try {
                Thread.sleep(60);
                BufferedImage image = h.exportImage();
                ImageIO.write(image, "png", new File(fileName));
            } catch (IOException e) {
                e.printStackTrace();
            } catch (InterruptedException e1) {
                e1.printStackTrace();
            }
        } else {
            SimplePlot plot = new SimplePlot();
            plot.addData(0, summedToneScaleHistogram);
            plot.save(fileName);
        }
    }

    @Override
    public void plot(String fileName, String title) {
        title = title == null ? "" : title;
        SimplePlot plot = new SimplePlot(title);
        double startingValue = getStart();
        double stoppingValue = getStop();
        boolean valuesStarted = false;
        for (double key : keySet()) {
            long count = getCount(key);
            if (count != 0l)
                stoppingValue = key;
            if (!valuesStarted && count != 0l)
                valuesStarted = true;
            if (!valuesStarted)
                startingValue = key;
        }
        plot.setXRange(startingValue, stoppingValue);
        for (double current : keySet()) {
            if (current >= startingValue && current <= stoppingValue)
                plot.addData(0, current, getCount(current));
        }
        plot.save(fileName);
    }
}