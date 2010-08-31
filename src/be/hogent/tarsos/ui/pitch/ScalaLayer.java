package be.hogent.tarsos.ui.pitch;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.event.MouseEvent;

import javax.swing.JComponent;

public class ScalaLayer implements Layer {

	private final JComponent parent;
	private final MouseDragListener mouseDrag;
	private double[] scale;
	private final double delta;

	public ScalaLayer(final JComponent component, final double[] toneScale, double pitchDelta) {
		parent = component;
		delta = pitchDelta;
		mouseDrag = new MouseDragListener(component, MouseEvent.BUTTON3);
		scale = toneScale;
		component.addMouseListener(mouseDrag);
		component.addMouseMotionListener(mouseDrag);
	}

	@Override
	public void draw(Graphics2D graphics) {
		final double xOffset = mouseDrag.calculateXOffset();
		final int width = parent.getWidth();
		final int height = parent.getHeight();
		final int xOffsetPixels = (int) Math.round(xOffset * width);

		graphics.setColor(Color.GRAY);

		for (final double reference : scale) {
			final int x = (int) (reference / delta * width + xOffsetPixels) % width;
			graphics.drawLine(x, 40, x, height);
			final String text = Integer.valueOf((int) reference).toString();
			final int labelLength = text.length();
			final double labelWidth = graphics.getFontMetrics().getStringBounds(text, graphics).getWidth();
			final int start = (int) labelWidth / 2 - labelLength / 2;
			graphics.drawString(text, x - start, 20);
		}
	}

	public void setScale(final double[] referenceScale) {
		this.scale = referenceScale;
		// align the peak picking with graph
		// xReferenceOffset = xOffset;
	}

}
