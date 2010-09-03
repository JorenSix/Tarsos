package be.hogent.tarsos.util;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Random;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.imageio.ImageIO;

import ptolemy.plot.Plot;
import be.hogent.tarsos.util.histogram.Histogram;

public final class SimplePlot {

	/**
	 * Log messages.
	 */
	private static final Logger LOG = Logger.getLogger(SimplePlot.class.getName());

	private final Plot plot;
	private boolean first;
	private final String title;

	public SimplePlot(final String title) {
		plot = new Plot();
		first = true;
		this.title = title;
		plot.setSize(1024, 786);
		plot.setTitle(title);
	}

	public void setSize(final int width, final int height) {
		plot.setSize(width, height);
	}

	public SimplePlot() {
		this(new SimpleDateFormat("yyyy.MM.dd-HH.mm.ss-" + new Random().nextInt(100)).format(new Date()));
	}

	public void addData(final int set, final double x, final double y) {
		plot.addPoint(set, x, y, !first);
		first = false;
	}

	public void addData(final int set, final double x, final double y, final boolean impulses) {
		if (impulses) {
			plot.setImpulses(true, set);
			plot.addPoint(set, x, y, false);
		} else {
			addData(set, x, y);
		}
	}

	public void addData(final int set, final Histogram histogram, final int displacement) {
		plot.setXRange(histogram.getStart(), histogram.getStop());
		for (final double current : histogram.keySet()) {
			final double displacedValue = (current + displacement * histogram.getClassWidth())
					% (histogram.getNumberOfClasses() * histogram.getClassWidth());
			addData(set, current, histogram.getCount(displacedValue));
		}
		first = true;
	}

	public void addData(final int set, final Histogram histogram) {
		addData(set, histogram, 0);
	}

	public void addData(final double x, final double y) {
		addData(0, x, y);
	}

	public void save() {
		final String name = title + ".png";
		save(name);
	}

	public void save(final String fileName) {
		try {
			Thread.sleep(100);
		} catch (final InterruptedException e) {
			LOG.log(Level.SEVERE, "Interrupted while sleeping.", e);
		}
		final BufferedImage image = plot.exportImage();

		try {
			ImageIO.write(image, "png", new File(fileName));
		} catch (final IOException e) {
			LOG.log(Level.SEVERE, "Could not write: " + fileName, e);
		}
	}

	public void toneScaleify(final double reference) {
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

	public void addLegend(final int set, final String name) {
		plot.addLegend(set, name);
	}

	public void setXRange(final double startingValue, final double stoppingValue) {
		plot.setXRange(startingValue, stoppingValue);
	}

	public void setYRange(final double d, final double e) {
		plot.setYRange(d, e);
	}
}
