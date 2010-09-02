package be.hogent.tarsos.ui.pitch;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics2D;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;

import javax.sound.midi.MidiUnavailableException;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JPanel;

import be.hogent.tarsos.ui.virtualkeyboard.VirtualKeyboard;

public class ScalaLayer implements Layer {

	private final JComponent parent;
	private final MouseDragListener mouseDrag;
	private double[] scale;
	private final double delta;

	public ScalaLayer(final JComponent component, final double[] toneScale, double pitchDelta) {
		parent = component;
		delta = pitchDelta;
		scale = toneScale;
		mouseDrag = new MouseDragListener(component, MouseEvent.BUTTON3);
		component.addMouseListener(mouseDrag);
		component.addMouseMotionListener(mouseDrag);

		try {
			new ClickForPitchListener(component, mouseDrag);
		} catch (MidiUnavailableException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
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
		parent.repaint();
	}

	public void setXOffset(final double xOffset) {
		this.mouseDrag.setXOffset(xOffset);
		parent.repaint();
	}

	JComponent ui;

	@Override
	public Component ui() {
		if (ui == null) {
			ui = new JPanel(new BorderLayout());
			JButton launchKeyboard = new JButton("Keyboard");
			launchKeyboard.addActionListener(new ActionListener() {
				VirtualKeyboard current;

				@Override
				public void actionPerformed(ActionEvent e) {
					// if (current == null) {
					current = VirtualKeyboard.createVirtualKeyboard(scale.length);
					current.connectToTunedSynth(scale);
					current.setSize(250, 30);
					// }
					ui.invalidate();
					ui.repaint();
					ui.add(current, BorderLayout.SOUTH);

				}
			});
			ui.add(launchKeyboard, BorderLayout.SOUTH);
		}
		return ui;
	}
}
