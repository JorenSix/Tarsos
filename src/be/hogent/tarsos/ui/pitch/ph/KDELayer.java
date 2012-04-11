/**
*
*  Tarsos is developed by Joren Six at 
*  The Royal Academy of Fine Arts & Royal Conservatory,
*  University College Ghent,
*  Hoogpoort 64, 9000 Ghent - Belgium
*
**/
/**
 */
package be.hogent.tarsos.ui.pitch.ph;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.event.MouseEvent;
import java.util.logging.Logger;

import javax.swing.JComponent;

import be.hogent.tarsos.ui.pitch.Layer;

/**
 * @author Joren Six
 */
public final class KDELayer implements Layer {

	private final JComponent parent;
	private final MouseDragListener mouseDrag;
	private final double[] values;

	/**
	 * Log messages.
	 */
	private static final Logger LOG = Logger.getLogger(KDELayer.class.getName());

	public KDELayer(final JComponent component, final int size) {
		parent = component;
		mouseDrag = new MouseDragListener(component, MouseEvent.BUTTON1);
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


	}

	public double getXOffset() {
		return mouseDrag.calculateXOffset();
	}
	
	public double[] getValues() {
		return values;
	}
}
