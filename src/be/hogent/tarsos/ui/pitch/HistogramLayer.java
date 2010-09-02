/**
 */
package be.hogent.tarsos.ui.pitch;

import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JComponent;
import javax.swing.JPanel;

import be.hogent.tarsos.util.histogram.Histogram;

/**
 * @author Joren Six
 */
public final class HistogramLayer implements Layer {

	// private final int X_BORDER = 5;
	private final int Y_BORDER = 5;

	private final JComponent parent;
	private final MouseDragListener mouseDrag;
	private final Histogram histo;
	private final int maxMarkers = 50;
	private final List<Double> markerPositions;

	public HistogramLayer(final JComponent component, final Histogram histogram) {
		parent = component;
		mouseDrag = new MouseDragListener(component, MouseEvent.BUTTON1);
		histo = histogram;
		component.addMouseListener(mouseDrag);
		component.addMouseMotionListener(mouseDrag);
		markerPositions = new ArrayList<Double>();
	}

	public void setMarkers(List<Double> newMarkers) {
		// add new markers with ttl 0
		for (Double newMarker : newMarkers) {
			markerPositions.add(newMarker);
		}

		// remove old markers
		while (markerPositions.size() > maxMarkers) {
			markerPositions.remove(0);
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see be.hogent.tarsos.ui.Layer#draw()
	 */
	@Override
	public void draw(final Graphics2D graphics) {
		double xOffset = mouseDrag.calculateXOffset();
		final double delta = histo.getStop() - histo.getStart();
		final long maxCount = histo.getMaxBinCount();

		final int width = parent.getWidth();
		final int height = parent.getHeight();

		final int xOffsetPixels = (int) Math.round(xOffset * width);
		int x = xOffsetPixels;

		int y = height - Y_BORDER
				- (int) (histo.getCount(histo.getStop()) / (double) maxCount * height * 0.9);
		Point previousPoint = new Point(x, y);

		graphics.setColor(Color.RED);
		for (final double key : histo.keySet()) {
			x = (int) (key / delta * width + xOffsetPixels) % width;
			y = height - Y_BORDER - (int) (histo.getCount(key) / (double) maxCount * height * 0.9);
			if (x > previousPoint.x) {
				graphics.drawLine(previousPoint.x, previousPoint.y, x, y);
			}
			previousPoint = new Point(x, y);
		}

		// draw markers

		for (int i = markerPositions.size() / 2; i < markerPositions.size(); i++) {
			double position = markerPositions.get(i);
			x = (int) (position / delta * width + xOffsetPixels) % width;
			y = height - Y_BORDER - (int) (histo.getCount(position) / (double) maxCount * height * 0.9);
			graphics.setColor(Color.BLUE);
			graphics.drawOval(x, y, 2, 2);
		}

	}

	public double getXOffset() {
		return mouseDrag.calculateXOffset();
	}

	JComponent ui;

	@Override
	public Component ui() {
		if (ui == null) {
			ui = new JPanel();
		}
		return ui;
	}
}
