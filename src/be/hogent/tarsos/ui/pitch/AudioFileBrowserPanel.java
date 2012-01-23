/**
*
*  Tarsos is developed by Joren Six at 
*  The Royal Academy of Fine Arts & Royal Conservatory,
*  University College Ghent,
*  Hoogpoort 64, 9000 Ghent - Belgium
*
**/
package be.hogent.tarsos.ui.pitch;

import java.awt.Dimension;
import java.awt.LayoutManager;
import java.awt.Rectangle;

import javax.swing.JPanel;
import javax.swing.Scrollable;

import be.hogent.tarsos.util.AudioFile;

public final class AudioFileBrowserPanel extends JPanel implements Scrollable, AudioFileChangedListener {
	/**
	 * 
	 */
	private static final long serialVersionUID = 5785813358581416533L;

	public AudioFileBrowserPanel(final LayoutManager layout) {
		super(layout);
	}

	public Dimension getPreferredScrollableViewportSize() {
		return null;
	}

	public int getScrollableUnitIncrement(final Rectangle visibleRect, final int orientation,
			final int direction) {
		return AudioFileItem.getItemHeight();
	}

	public int getScrollableBlockIncrement(final Rectangle visibleRect, final int orientation,
			final int direction) {
		return AudioFileItem.getItemHeight();
	}

	public boolean getScrollableTracksViewportWidth() {
		return false;
	}

	public boolean getScrollableTracksViewportHeight() {
		return false;
	}

	public void audioFileChanged(final AudioFile newAudioFile) {
		add(new AudioFileItem(newAudioFile.originalBasename()));
	}

}
