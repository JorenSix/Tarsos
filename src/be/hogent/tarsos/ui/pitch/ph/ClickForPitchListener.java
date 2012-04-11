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


	private final MouseDragListener mouseDrag;
	/**
	 * The delta in cents, 0 - 1200 for a tone scale (1200), 9600 for an
	 * ambitus.
	 */
	//private final double delta;

	public ClickForPitchListener(final MouseDragListener mouseDragListener) {
		mouseDrag = mouseDragListener;
	}

	@Override
	public void mouseClicked(final MouseEvent e) {

		final double pitchCents = mouseDrag.getCents(e, 1200);
		final int velocity = 100;
		//if (delta > 1200) {
			// ambitus
			//TarsosSynth.getInstance().playAbsoluteCents(pitchCents + Configuration.getInt(ConfKey.pitch_histogram_start), velocity);
		//} else {
			// tone scale
			TarsosSynth.getInstance().playRelativeCents(pitchCents, velocity);
		//}
	}
}
