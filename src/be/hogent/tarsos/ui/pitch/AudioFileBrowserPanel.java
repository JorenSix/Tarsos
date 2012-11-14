/*
*              _______                      
*             |__   __|                     
*                | | __ _ _ __ ___  ___  ___
*                | |/ _` | '__/ __|/ _ \/ __| 
*                | | (_| | |  \__ \ (_) \__ \    
*                |_|\__,_|_|  |___/\___/|___/    
*                                                         
* -----------------------------------------------------------
*
*  Tarsos is developed by Joren Six at 
*  The School of Arts,
*  University College Ghent,
*  Hoogpoort 64, 9000 Ghent - Belgium
*  
* -----------------------------------------------------------
*
*  Info: http://tarsos.0110.be
*  Github: https://github.com/JorenSix/Tarsos
*  Releases: http://tarsos.0110.be/releases/Tarsos/
*  
*  Tarsos includes some source code by various authors,
*  for credits and info, see README.
* 
*/

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
