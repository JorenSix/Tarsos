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
import java.util.HashMap;

import javax.sound.sampled.AudioFileFormat;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.UnsupportedAudioFileException;

/**
 * Wrapper around WaveAudioReader that first decodes an audio file in a
 * different format (i.e. MP3) into a temporary wave file. All future
 * AudioReaders instantiated on this file operate on the previously decoded temp
 * file.
 * 
 * This solves problems with very slow seeks in MP3 files.
 */
public class CachedAudioReader extends WaveAudioReader {
	private static HashMap<String, File> decodedFiles = new HashMap<String, File>();

	public CachedAudioReader(final String filename, final AudioFormat decodedFormat) throws IOException,
			UnsupportedAudioFileException {
		String key = filename;

		File file = decodedFiles.get(key);
		if (file == null) {
			file = File.createTempFile(new File(filename).getName(), ".wav");
			file.deleteOnExit();
			decodedFiles.put(key, file);

			File encodedFile = new File(filename);
			AudioInputStream decodedInputStream = AudioSystem.getAudioInputStream(encodedFile);
			// Convert to PCM
			decodedInputStream = AudioSystem.getAudioInputStream(AudioFormat.Encoding.PCM_SIGNED,
					decodedInputStream);
			AudioSystem.write(decodedInputStream, AudioFileFormat.Type.WAVE, file);
		}

		ais = AudioSystem.getAudioInputStream(file);
		ais = initializeAudioInputStream(ais, decodedFormat);
	}
}
