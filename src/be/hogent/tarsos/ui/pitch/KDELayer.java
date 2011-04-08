/**
 */
package be.hogent.tarsos.ui.pitch;

import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.event.MouseEvent;
import java.util.logging.Logger;

import javax.swing.JComponent;

import be.hogent.tarsos.util.AudioFile;

/**
 * @author Joren Six
 */
public final class KDELayer implements Layer, ScaleChangedListener, AudioFileChangedListener {

	private final JComponent parent;
	private final MouseDragListener mouseDrag;
	private final double[] values;
	private final float delta;

	/**
	 * Log messages.
	 */
	private static final Logger LOG = Logger.getLogger(KDELayer.class.getName());

	public KDELayer(final JComponent component, final int size) {
		parent = component;
		mouseDrag = new MouseDragListener(component, MouseEvent.BUTTON1);
		delta = size;
		values = new double[size];
		component.addMouseListener(mouseDrag);
		component.addMouseMotionListener(mouseDrag);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see be.hogent.tarsos.ui.Layer#draw()
	 */
	public void draw(final Graphics2D graphics) {
		double xOffset = mouseDrag.calculateXOffset();
		int yOffset = 20;
		double maxCount = values[0];

		for (int i = 1; i < values.length; i++) {
			maxCount = Math.max(maxCount, values[i]);
		}

		final int width = parent.getWidth();
		final int height = parent.getHeight();

		final int xOffsetPixels = (int) Math.round(xOffset * width);
		int x = xOffsetPixels;

		int y = (int) (height - yOffset - values[(int) (delta - 1)] / maxCount * height * 0.9);
		Point previousPoint = new Point(x, y);

		graphics.setColor(Color.GRAY);
		graphics.drawLine(0, height - yOffset, width, height - yOffset);

		graphics.setColor(Color.RED);

		for (int i = 0; i < values.length; i++) {
			x = (int) ((i / delta * width + xOffsetPixels) % width);
			y = height - yOffset - (int) (values[i] / maxCount * height * 0.9);
			if (x > previousPoint.x) {
				graphics.drawLine(previousPoint.x, previousPoint.y, x, y);
			}
			previousPoint = new Point(x, y);
		}

		// draw markers

		LOG.finer("KDE layer redrawn.");

	}

	public double getXOffset() {
		return mouseDrag.calculateXOffset();
	}

	double[] scale;
	AudioFile audioFile;

	public void scaleChanged(final double[] newScale, final boolean isChanging, boolean shiftHisto) {
		if (!isChanging) {
			scale = newScale;
		}
	}

	public void audioFileChanged(final AudioFile newAudioFile) {
		audioFile = newAudioFile;
	}

	public Component ui() {
		// TODO Auto-generated method stub
		return null;
	}

	public double[] getValues() {
		return values;
	}

}
