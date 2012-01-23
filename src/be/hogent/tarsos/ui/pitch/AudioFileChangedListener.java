/**
*
*  Tarsos is developed by Joren Six at 
*  The Royal Academy of Fine Arts & Royal Conservatory,
*  University College Ghent,
*  Hoogpoort 64, 9000 Ghent - Belgium
*
**/
package be.hogent.tarsos.ui.pitch;

import be.hogent.tarsos.util.AudioFile;

public interface AudioFileChangedListener {
	void audioFileChanged(AudioFile newAudioFile);
}
