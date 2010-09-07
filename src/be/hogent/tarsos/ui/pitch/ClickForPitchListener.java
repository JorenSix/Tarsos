/**
 */
package be.hogent.tarsos.ui.pitch;

import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.sound.midi.MidiUnavailableException;
import javax.swing.JComponent;

import be.hogent.tarsos.midi.PitchSynth;

class ClickForPitchListener extends MouseAdapter {

	private final JComponent parent;
	private final PitchSynth synth;
	private final MouseDragListener mouseDrag;

	public ClickForPitchListener(final JComponent component, final MouseDragListener mouseDragListener)
			throws MidiUnavailableException {
		parent = component;
		synth = new PitchSynth();
		mouseDrag = mouseDragListener;
		component.addMouseListener(this);
	}

	@Override
	public void mouseClicked(final MouseEvent e) {

		double xOffset = mouseDrag.calculateXOffset();
		final int width = parent.getWidth();
		final int height = parent.getHeight();

		double xOffsetCents = xOffset * 1200.0;

		if (xOffset < 0) {
			xOffset = 1.0 + xOffset;
		}
		double pitchInRelativeCents = e.getX() * 1200.0 / width;

		pitchInRelativeCents = (pitchInRelativeCents - xOffsetCents) % 1200.0;
		if (pitchInRelativeCents < 0) {
			pitchInRelativeCents += 1200;
		}

		final int velocity = (int) (e.getY() / (double) height * 127);

		synth.play(pitchInRelativeCents, velocity);
	}
}