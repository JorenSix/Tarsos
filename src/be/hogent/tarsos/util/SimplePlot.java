package be.hogent.tarsos.util;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Random;

import javax.imageio.ImageIO;

import ptolemy.plot.Plot;
import be.hogent.tarsos.util.histogram.Histogram;

public class SimplePlot {

    private final Plot plot;
    private boolean first;
    private final String title;

    public SimplePlot(String title) {
        plot = new Plot();
        first = true;
        this.title = title;
        plot.setSize(1024, 786);
        plot.setTitle(title);
    }

    public void setSize(int width, int height) {
        plot.setSize(width, height);
    }

    public SimplePlot() {
        this(new SimpleDateFormat("yyyy.MM.dd-HH.mm.ss-" + new Random().nextInt(100)).format(new Date()));
    }

    public void addData(int set, double x, double y) {
        plot.addPoint(set, x, y, !first);
        first = false;
    }

    public void addData(int set, double x, double y, boolean impulses) {
        if (impulses) {
            plot.setImpulses(true, set);
            plot.addPoint(set, x, y, false);
        } else {
            addData(set, x, y);
        }
    }

    public void addData(int set, Histogram histogram, int displacement) {
        plot.setXRange(histogram.getStart(), histogram.getStop());
        for (double current : histogram.keySet()) {
            double displacedValue = (current + displacement * histogram.getClassWidth())
                    % (histogram.getNumberOfClasses() * histogram.getClassWidth());
            addData(set, current, histogram.getCount(displacedValue));
        }
        first = true;
    }

    public void addData(int set, Histogram histogram) {
        addData(set, histogram, 0);
    }

    public void addData(double x, double y) {
        addData(0, x, y);
    }

    public void save() {
        save("data/tests/" + title + ".png");
    }

    public void save(String fileName) {
        try {
            Thread.sleep(60);
            BufferedImage image = plot.exportImage();
            ImageIO.write(image, "png", new File(fileName));
        } catch (IOException e) {
            throw new Error("Could not write to:" + fileName, e);
        } catch (InterruptedException e) {
            e.printStackTrace();
            throw new Error(e);
        }
    }

    public void toneScaleify(double reference) {
        plot.setXRange(0, 1200);
        plot.setXLabel("n (cents)");
        plot.setXLabel("frequency of ocurrence");
        plot.setImpulses(true);

        plot.addPoint(1, reference - 600, 300, false);
        plot.addPoint(1, reference + 600, 300, false);

        plot.addPoint(2, reference + 300, 300, false);
        plot.addPoint(2, reference - 200, 300, false);

        plot.addLegend(0, "Tone scale");
        plot.addLegend(1, "Fifth");
        plot.addLegend(2, "Tritonus");

    }

    public void addLegend(int set, String name) {
        plot.addLegend(set, name);
    }

    public void setXRange(double startingValue, double stoppingValue) {
        plot.setXRange(startingValue, stoppingValue);
    }

    public void setYRange(double d, double e) {
        plot.setYRange(d, e);
    }
}
