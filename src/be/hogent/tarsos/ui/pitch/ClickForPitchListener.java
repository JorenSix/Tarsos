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
		final int height = parent.getHeight();
		final double pitchInRelativeCents = mouseDrag.getRelativeCents(e);
		final int velocity = (int) (e.getY() / (double) height * 127);
		synth.play(pitchInRelativeCents, velocity);
	}
}