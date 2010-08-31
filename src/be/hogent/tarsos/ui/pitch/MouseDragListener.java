/**
 */
package be.hogent.tarsos.ui.pitch;

import java.awt.Point;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionListener;

import javax.swing.JComponent;

class MouseDragListener extends MouseAdapter implements MouseMotionListener {
	/**
     */
	private final JComponent parent;
	private final Point referenceDragPoint;
	private final int mouseButton;
	private int prevButton;
	private int delta;
	private double xOffset;

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

}