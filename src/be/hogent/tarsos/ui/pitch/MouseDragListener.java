package be.hogent.tarsos.ui.pitch;

import java.awt.Point;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionListener;

import javax.swing.JComponent;

/**
 * A listener used to register drag events and calculate relative offset values.
 * *
 * 
 * @author Joren Six
 */
class MouseDragListener extends MouseAdapter implements MouseMotionListener {
	/**
     */
	private final JComponent parent;
	/**
	 * Point to calculate distance to.
	 */
	private final Point referenceDragPoint;
	/**
	 * The mouse button to use. Middle button = BUTTON2, left = 1 and right = 3.
	 */
	private final int mouseButton;
	/**
	 * The previous button pressed (not clicked).
	 */
	private int prevButton;
	/**
	 * The number of pixels moved.
	 */
	private int delta;
	/**
	 * The relative distance in x direction: number of pixels/width.
	 */
	private double xOffset;

	/**
	 * Create a new mouse drag listener.
	 * 
	 * @param component
	 *            The component is used to calculate the relative offset.
	 * @param button
	 *            The mouse button used (Middle button = BUTTON2, left = 1 and
	 *            right = 3).
	 */
	public MouseDragListener(final JComponent component, final int button) {
		parent = component;
		referenceDragPoint = new Point(0, 0);
		mouseButton = button;
		xOffset = 0.0;
	}

	@Override
	public void mouseClicked(final MouseEvent e) {
		// reset the x offset
		if (e.getClickCount() == 2 && e.getButton() == mouseButton) {
			xOffset = 0;
			parent.repaint();
		}
	}

	@Override
	public void mousePressed(final MouseEvent e) {

		referenceDragPoint.setLocation(e.getPoint());
		prevButton = e.getButton();

	};

	@Override
	public void mouseDragged(final MouseEvent e) {

		final boolean mouseMoved = !e.getPoint().equals(referenceDragPoint);
		final boolean correctButton = prevButton == mouseButton;
		if (mouseMoved && correctButton) {
			delta = e.getPoint().x - referenceDragPoint.x;
			referenceDragPoint.setLocation(e.getPoint());
			parent.repaint();
		}

	}

	/**
	 * @return A value in [0,1.0] representing the x offset in percentage.
	 */
	public double calculateXOffset() {
		xOffset = xOffset + delta / (double) parent.getWidth();
		if (xOffset < 0) {
			xOffset = 1.0 + xOffset;
		}
		delta = 0;
		return xOffset;
	}

	/**
	 * Jumps to a new xOffset.
	 * 
	 * @param newXOffset
	 *            The offset to jump to.
	 */
	public void setXOffset(final double newXOffset) {
		this.xOffset = newXOffset;
	}

	public double getRelativeCents(final MouseEvent e) {
		double currentXOffset = calculateXOffset();
		final int width = parent.getWidth();

		double xOffsetCents = currentXOffset * 1200.0;

		if (currentXOffset < 0) {
			currentXOffset = 1.0 + currentXOffset;
		}
		double pitchInRelativeCents = e.getX() * 1200.0 / width;

		pitchInRelativeCents = (pitchInRelativeCents - xOffsetCents) % 1200.0;
		if (pitchInRelativeCents < 0) {
			pitchInRelativeCents += 1200;
		}

		return pitchInRelativeCents;

	}

}