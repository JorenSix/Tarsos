package be.hogent.tarsos.ui.pitch;

import be.hogent.tarsos.util.AudioFile;

public interface AudioFileChangedListener {
	void audioFileChanged(AudioFile newAudioFile);
}