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

package be.tarsos.ui.pitch.ph;

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
	 * Jumps to a new xOffset. A value in [0,1.0] representing the x offset in percentage.
	 * 
	 * @param newXOffset
	 *            The offset to jump to. A value in [0,1.0] representing the x offset in percentage.
	 */
	public void setXOffset(final double newXOffset) {
		this.xOffset = newXOffset;
	}

	/**
	 * Returns a value between 0 and delta.
	 * 
	 * @param e
	 *            The mouse event.
	 * @param pitchDelta
	 *            The pitchDelta. E.g. 1200 for a tone scale, 9600 for an
	 *            ambitus.
	 * @return A value between 0 and delta (inclusive) representing a value in
	 *         cents.
	 */
	public double getCents(final MouseEvent e, final double pitchDelta) {
		double currentXOffset = calculateXOffset();
		final int width = parent.getWidth();

		double xOffsetCents = currentXOffset * pitchDelta;

		if (currentXOffset < 0) {
			currentXOffset = 1.0 + currentXOffset;
		}
		double pitchInCents = e.getX() * pitchDelta / width;

		pitchInCents = (pitchInCents - xOffsetCents) % pitchDelta;
		if (pitchInCents < 0) {
			pitchInCents += pitchDelta;
		}

		return pitchInCents;

	}

	public void mouseMoved(final MouseEvent arg0) {
		// do nothing
	}

}
