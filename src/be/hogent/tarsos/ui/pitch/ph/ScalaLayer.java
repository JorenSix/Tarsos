/**
*
*  Tarsos is developed by Joren Six at 
*  The Royal Academy of Fine Arts & Royal Conservatory,
*  University College Ghent,
*  Hoogpoort 64, 9000 Ghent - Belgium
*
**/
package be.hogent.tarsos.ui.pitch.ph;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionListener;
import java.util.Arrays;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.sound.midi.MidiUnavailableException;
import javax.swing.JComponent;

import be.hogent.tarsos.ui.pitch.Layer;
import be.hogent.tarsos.ui.pitch.ScaleChangedListener;
import be.hogent.tarsos.util.ConfKey;
import be.hogent.tarsos.util.Configuration;

/**
 * The scala layer can be seen as a background grid (with on the foreground a
 * pitch class or ambitus histogram).
 * 
 * @author Joren Six
 */
public final class ScalaLayer implements Layer, ScaleChangedListener {

	/**
	 * Log messages.
	 */
	private static final Logger LOG = Logger.getLogger(ScalaLayer.class.getName());

	private final JComponent parent;
	private final MouseDragListener mouseDrag;
	private double[] scale;
	private final double delta;
	private final ScaleEditor editor;
	private final ScaleChangedListener scaleChangedPublisher;

	public ScalaLayer(final JComponent component, final double[] toneScale, final ScaleChangedListener scalePublisher) {
		parent = component;
		scale = toneScale;
		mouseDrag = new MouseDragListener(component, MouseEvent.BUTTON3);
		component.addMouseListener(mouseDrag);
		component.addMouseMotionListener(mouseDrag);
		scaleChangedPublisher = scalePublisher;
		editor = null;
		delta = 0;
		/*
		try {
			//new ClickForPitchListener mouseDrag);
		} catch (MidiUnavailableException e1) {
			LOG.log(Level.WARNING, "MIDI device not available, disabled the click for pitch function.", e1);
		}
		

		if (isAmbitus()) {
			editor = null;
		} else {
			editor = new ScaleEditor(mouseDrag, this, parent);
			component.addMouseListener(editor);
			component.addMouseMotionListener(editor);
			component.addKeyListener(editor);
		}
		*/

	}

	private boolean isAmbitus() {
		return delta > 1200;
	}

	

	public void draw(final Graphics2D graphics) {
		
		// draw octave borders
		/*
		if (delta > 1200) {
			for (int i = 1200; i < delta; i += 1200) {
				final int x = (int) (i / delta * width + xOffsetPixels) % width;
				graphics.drawLine(x, 0, x, height - yOffset);
				final String text = Integer.valueOf(i + Configuration.getInt(ConfKey.pitch_histogram_start))
						.toString();
				final int labelLength = text.length();
				final double labelWidth = graphics.getFontMetrics().getStringBounds(text, graphics)
						.getWidth();
				final int start = (int) labelWidth / 2 - labelLength / 2;
				graphics.drawString(text, x - start, height - yLabelsOffset);
				graphics.drawLine(x, 0, x, height - yOffset);
			}
		}
		*/
	}

	public void scaleChanged(double[] newScale, boolean isChanging, boolean shiftHisto) {
		this.scale = newScale;
		parent.repaint();
	}

	public void setXOffset(final double xOffset) {
		this.mouseDrag.setXOffset(xOffset);
		parent.repaint();
	}
}
