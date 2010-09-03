package be.hogent.tarsos.ui.pitch;

import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics2D;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;

import javax.sound.midi.MidiUnavailableException;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.border.TitledBorder;

import be.hogent.tarsos.ui.virtualkeyboard.VirtualKeyboard;
import be.hogent.tarsos.util.ScalaFile;

import com.jgoodies.forms.builder.DefaultFormBuilder;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;

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
		if (keyboard != null) {
			keyboard.connectToTunedSynth(referenceScale);
		}
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

			VirtualKeyboard[] keyboards = new VirtualKeyboard[20];
			for (int i = 0; i < 20; i++) {
				keyboards[i] = VirtualKeyboard.createVirtualKeyboard(0);
			}

			keyboard = VirtualKeyboard.createVirtualKeyboard(scale.length);
			keyboard.connectToTunedSynth(scale);

			FormLayout layout = new FormLayout("right:pref, 3dlu, min:grow");
			DefaultFormBuilder builder = new DefaultFormBuilder(layout);
			builder.setDefaultDialogBorder();
			builder.setRowGroupingEnabled(true);
			builder.append("Export scala file:", exportButton, true);
			CellConstraints cc = new CellConstraints();

			builder.append("Keyboard:");
			builder.appendRow("31dlu"); // Assumes line is 14, gap is 3
			builder.add(keyboard, cc.xywh(builder.getColumn(), builder.getRow(), 1, 2));
			builder.nextLine(2);

			ui = builder.getPanel();
			ui.setBorder(new TitledBorder("Peak commands"));
		}
		return ui;
	}
}
