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

import java.io.IOException;

import javax.sound.sampled.AudioFormat;

/**
 * Wrapper around AudioInputStream to allow reading double samples instead of
 * unparsed byte streams.
 */
public interface AudioReader {
	AudioFormat getFormat();

	long getFrameLength();

	long skipSamples(long nframes) throws IOException;

	int readSamples(double[] samples) throws IOException;

	int readSamples(double[] samples, int offset, int length) throws IOException;

	void closeStream() throws IOException;
}
