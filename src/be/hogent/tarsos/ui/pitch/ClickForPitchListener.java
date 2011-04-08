/**
 */
package be.hogent.tarsos.ui.pitch;

import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.sound.midi.MidiUnavailableException;
import javax.swing.JComponent;

import be.hogent.tarsos.midi.TarsosSynth;
import be.hogent.tarsos.util.ConfKey;
import be.hogent.tarsos.util.Configuration;

/**
 * Generates a MIDI event when clicking. It should generate a pitch.
 */
class ClickForPitchListener extends MouseAdapter {

	private final JComponent parent;
	private final MouseDragListener mouseDrag;
	/**
	 * The delta in cents, 0 - 1200 for a tone scale (1200), 9600 for an
	 * ambitus.
	 */
	private final double delta;

	public ClickForPitchListener(final JComponent component, final MouseDragListener mouseDragListener,
			final double histogramDelta) throws MidiUnavailableException {
		parent = component;
		delta = histogramDelta;
		mouseDrag = mouseDragListener;
		component.addMouseListener(this);
	}

	@Override
	public void mouseClicked(final MouseEvent e) {
		final int height = parent.getHeight();
		final double pitchCents = mouseDrag.getCents(e, delta);
		final int velocity = (int) (e.getY() / (double) height * 127);
		if (delta > 1200) {
			// ambitus
			TarsosSynth.getInstance().playAbsoluteCents(pitchCents + Configuration.getInt(ConfKey.pitch_histogram_start), velocity);
		} else {
			// tone scale
			TarsosSynth.getInstance().playRelativeCents(pitchCents, velocity);
		}

	}
}