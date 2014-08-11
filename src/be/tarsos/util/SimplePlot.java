/*
*              _______                      
*             |__   __|                     
*                | | __ _ _ __ ___  ___  ___
*                | |/ _` | '__/ __|/ _ \/ __| 
*                | | (_| | |  \__ \ (_) \__ \    
*                |_|\__,_|_|  |___/\___/|___/    
*                                                         
* -----------------------------------------------------------
*
*  Tarsos is developed by Joren Six at 
*  The School of Arts,
*  University College Ghent,
*  Hoogpoort 64, 9000 Ghent - Belgium
*  
* -----------------------------------------------------------
*
*  Info: http://tarsos.0110.be
*  Github: https://github.com/JorenSix/Tarsos
*  Releases: http://tarsos.0110.be/releases/Tarsos/
*  
*  Tarsos includes some source code by various authors,
*  for credits and info, see README.
* 
*/

package be.tarsos.util;

import java.awt.image.BufferedImage;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Random;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.imageio.ImageIO;

import ptolemy.plot.Plot;
import be.tarsos.util.histogram.Histogram;

public final class SimplePlot {

	/**
	 * Log messages.
	 */
	private static final Logger LOG = Logger.getLogger(SimplePlot.class.getName());

	private final Plot plot;
	private boolean first;
	private final String title;

	public SimplePlot(final String plotTitle) {
		plot = new Plot();
		first = true;
		this.title = plotTitle;
		plot.setSize(1024, 786);
		plot.setTitle(plotTitle);
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
	
	public void addData(final int set, final KernelDensityEstimate kde) {
		plot.setXRange(0, kde.size());
		for(int i = 0 ; i < kde.size(); i ++){
			addData(set,i, kde.getValue(i));
		}
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

		try {
			if (fileName.endsWith(".eps")) {
				final OutputStream out = new BufferedOutputStream(new FileOutputStream(new File(fileName)));
				plot.export(out);
				out.close();
			} else {
				final BufferedImage image = plot.exportImage();
				ImageIO.write(image, "png", new File(fileName));
			}
		} catch (final IOException e) {
			LOG.log(Level.SEVERE, "Could not write: " + fileName, e);
		}
	}

	/**
	 * Sets the x range from 0 to 1200, names the axes.
	 */
	public void pitchClassHistogramify() {
		plot.setXRange(0, 1200);
		plot.setXLabel("Pitch Class (cent)");
		plot.setYLabel("Number of Annotations (#)");
		plot.setWrap(true);
	}
	
	
	public void addXTick(final String label, final double position){
		plot.addXTick(label, position);
	}
	
	public void addYTick(final String label, final double position){
		plot.addYTick(label, position);
	}
	
	public void setTitle(final String title){
		plot.setTitle(title);
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

	/**
	 * Sets the label for the x axis.
	 * @param label The label to use.
	 */
	public void setXLabel(final String label) {
		plot.setXLabel(label);
	}

	/**
	 * Sets the label for the y axis.
	 * @param label The label to use.
	 */
	public void setYLabel(final String label) {
		plot.setYLabel(label);
	}
}
