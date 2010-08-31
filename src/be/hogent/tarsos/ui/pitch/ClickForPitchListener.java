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

	public ClickForPitchListener(final JComponent component)
			throws MidiUnavailableException {
		parent = component;
		synth = new PitchSynth();
	}

	@Override
	public void mouseClicked(final MouseEvent e) {
		double pitchInRelativeCents = e.getX() * 1200.0 / parent.getWidth();
		System.out.println(pitchInRelativeCents);
		final int velocity = (int) (e.getY() / (double) parent.getHeight() * 127);
		synth.play(pitchInRelativeCents, velocity);
		/*
		 * if (this.toneScaleFrame.xReferenceOffset < 0) { //
		 * this.toneScaleFrame.xReferenceOffset = 1.0 +
		 * this.toneScaleFrame.xReferenceOffset; // } double
		 * pitchInRelativeCents = ((e.getX() / (double)
		 * this.toneScaleFrame.getWidth() -
		 * this.toneScaleFrame.xReferenceOffset) * 1200.0) % 1200; if
		 * (pitchInRelativeCents < 0) { pitchInRelativeCents += 1200; }
		 */
	}

}