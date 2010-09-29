package be.hogent.tarsos.ui.pitch;

import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics2D;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionListener;
import java.util.Arrays;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.sound.midi.MidiUnavailableException;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.border.TitledBorder;

import be.hogent.tarsos.ui.virtualkeyboard.VirtualKeyboard;
import be.hogent.tarsos.util.ScalaFile;

import com.jgoodies.forms.builder.DefaultFormBuilder;
import com.jgoodies.forms.layout.FormLayout;

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

	public ScalaLayer(final JComponent component, final double[] toneScale, final double pitchDelta,
			final ScaleChangedListener scalePublisher) {
		parent = component;
		delta = pitchDelta;
		scale = toneScale;
		mouseDrag = new MouseDragListener(component, MouseEvent.BUTTON3);
		component.addMouseListener(mouseDrag);
		component.addMouseMotionListener(mouseDrag);
		editor = new ScaleEditor(mouseDrag, this, parent);
		component.addMouseListener(editor);
		component.addMouseMotionListener(editor);
		scaleChangedPublisher = scalePublisher;

		try {
			new ClickForPitchListener(component, mouseDrag);
		} catch (MidiUnavailableException e1) {
			LOG.log(Level.WARNING, "MIDI device not available, disabled the click for pitch function.", e1);
		}
	}

	private static class ScaleEditor extends MouseAdapter implements MouseMotionListener {
		private final MouseDragListener mouseDrag;
		private final ScalaLayer layer;
		private final JComponent parent;
		private double movingElement = -1.0;

		ScaleEditor(final MouseDragListener mouseDrag, final ScalaLayer layer, final JComponent parent) {
			this.mouseDrag = mouseDrag;
			this.layer = layer;
			this.parent = parent;
		}

		@Override
		public void mouseDragged(MouseEvent arg0) {

		}

		@Override
		public void mouseMoved(MouseEvent e) {
			if (e.isAltDown() || e.isAltGraphDown()) {
				if (movingElement != -1.0) {
					int index = -1;
					for (int i = 0; i < layer.scale.length; i++) {
						if (layer.scale[i] == movingElement) {
							index = i;
						}
					}
					if (index == -1) {
						movingElement = -1.0;
					} else {
						layer.scale[index] = mouseDrag.getRelativeCents(e);
						movingElement = layer.scale[index];
					}

				} else {
					double[] newScale = new double[layer.scale.length + 1];
					for (int i = 0; i < layer.scale.length; i++) {
						newScale[i] = layer.scale[i];
					}
					newScale[newScale.length - 1] = mouseDrag.getRelativeCents(e);
					movingElement = newScale[newScale.length - 1];
					Arrays.sort(newScale);
					layer.scale = newScale;
				}
				parent.repaint();
				layer.scaleChangedPublisher.scaleChanged(layer.scale, true);
			} else if (e.isControlDown()) {
				if (movingElement == -1.0) {
					int index = closestIndex(mouseDrag.getRelativeCents(e));
					movingElement = layer.scale[index];
				}
				for (int i = 0; i < layer.scale.length; i++) {
					if (layer.scale[i] == movingElement) {
						layer.scale[i] = mouseDrag.getRelativeCents(e);
						movingElement = layer.scale[i];
					}
				}
				parent.repaint();
				layer.scaleChangedPublisher.scaleChanged(layer.scale, true);
			}

		}

		private int closestIndex(double key) {
			double distance = Double.MAX_VALUE;
			int index = -1;
			for (int i = 0; i < layer.scale.length; i++) {
				double currentDistance = Math.abs(key - layer.scale[i]);
				double wrappedDistance = Math.abs(key - (layer.scale[i] + 1200));
				if (Math.min(currentDistance, wrappedDistance) < distance) {
					distance = Math.min(currentDistance, wrappedDistance);
					index = i;
				}
			}
			return index;
		}

		@Override
		public void mouseReleased(MouseEvent e) {
			if (movingElement != -1.0) {
				layer.scaleChangedPublisher.scaleChanged(layer.scale, false);
			}
			movingElement = -1.0;

		}

		@Override
		public void mousePressed(MouseEvent arg0) {

		}

		public double getMovingElement() {
			return movingElement;
		}
	}

	@Override
	public void draw(final Graphics2D graphics) {
		final double xOffset = mouseDrag.calculateXOffset();
		final int yOffset = 20;
		final int yLabelsOffset = 5;

		final int width = parent.getWidth();
		final int height = parent.getHeight();
		final int xOffsetPixels = (int) Math.round(xOffset * width);

		graphics.setColor(Color.GRAY);

		for (final double reference : scale) {
			final int x = (int) (reference / delta * width + xOffsetPixels) % width;
			final String text = Integer.valueOf((int) reference).toString();
			final int labelLength = text.length();
			final double labelWidth = graphics.getFontMetrics().getStringBounds(text, graphics).getWidth();
			final int start = (int) labelWidth / 2 - labelLength / 2;
			if (editor.getMovingElement() == reference) {
				graphics.setColor(Color.BLUE);
				graphics.drawLine(x, 0, x, height - yOffset);
				graphics.drawString(text, x - start, height - yLabelsOffset);
				graphics.setColor(Color.GRAY);
			} else {
				graphics.drawLine(x, 0, x, height - yOffset);
				graphics.drawString(text, x - start, height - yLabelsOffset);
			}

		}

	}

	@Override
	public void scaleChanged(double[] newScale, boolean isChanging) {
		this.scale = newScale;
		parent.repaint();
	}

	public void setXOffset(final double xOffset) {
		this.mouseDrag.setXOffset(xOffset);
		parent.repaint();
	}

	JComponent ui;
	VirtualKeyboard keyboard;

	@Override
	public Component ui() {
		if (ui == null) {

			JButton exportButton = new JButton("Export");
			exportButton.addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent e) {
					ScalaFile file = new ScalaFile("Tarsos exported scala file", scale);
					file.write("export.scl");
				}
			});

			FormLayout layout = new FormLayout("right:default, 3dlu,default:grow");
			DefaultFormBuilder builder = new DefaultFormBuilder(layout);
			builder.setDefaultDialogBorder();
			builder.setRowGroupingEnabled(true);
			builder.append("Export scala file:", exportButton, true);

			ui = builder.getPanel();
			ui.setBorder(new TitledBorder("Peak commands"));
		}
		return ui;
	}
}
