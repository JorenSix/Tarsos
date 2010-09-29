/*
 *  Copyright 2006-2007 Columbia University.
 *
 *  This file is part of MEAPsoft.
 *
 *  MEAPsoft is free software; you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License version 2 as
 *  published by the Free Software Foundation.
 *
 *  MEAPsoft is distributed in the hope that it will be useful, but
 *  WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *  General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with MEAPsoft; if not, write to the Free Software
 *  Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 *  02110-1301 USA
 *
 *  See the file "COPYING" for the text of the license.
 */

package be.hogent.tarsos.sampled.hpss;

import java.io.File;
import java.io.IOException;

import javax.sound.sampled.AudioFileFormat;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.UnsupportedAudioFileException;

public final class AudioReaderFactory {
	private AudioReaderFactory() {
	}

	// Factory method to create an AudioReader from an audio file in
	// the given format.
	public static AudioReader getAudioReader(final String filename, final AudioFormat decodedFormat)
			throws IOException, UnsupportedAudioFileException {
		AudioFileFormat fileFormat = AudioSystem.getAudioFileFormat(new File(filename));
		if (fileFormat.getType() != AudioFileFormat.Type.WAVE) {
			return new CachedAudioReader(filename, decodedFormat);
		} else {
			return new WaveAudioReader(filename, decodedFormat);
		}
	}
}
