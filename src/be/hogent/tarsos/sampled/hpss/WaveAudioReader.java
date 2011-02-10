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

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.UnsupportedAudioFileException;

/**
 * Simple class to read wave files.
 */
class WaveAudioReader implements AudioReader
{
	protected AudioInputStream ais = null;

	// Get an input stream from an audio file
	WaveAudioReader(String filename, AudioFormat decodedFormat)
			throws IOException, UnsupportedAudioFileException
	{
		ais = AudioSystem.getAudioInputStream(new File(filename));
		ais = initializeAudioInputStream(ais, decodedFormat);
	}

	// Never call this
	protected WaveAudioReader()
	{
	}

	protected AudioInputStream initializeAudioInputStream(AudioInputStream ais,
			AudioFormat decodedFormat) throws IOException,
			UnsupportedAudioFileException
	{
		// Convert to the proper data format. Javasound can't convert
		// number of channels and sampling rate in one shot, so we
		// need to chain the format conversions together.
		if (!AudioSystem.isConversionSupported(ais.getFormat(), decodedFormat))
		{
			if (ais.getFormat().getChannels() != decodedFormat.getChannels())
			{
				AudioFormat newFormat = new AudioFormat(ais.getFormat()
						.getEncoding(), decodedFormat.getSampleRate(),
						decodedFormat.getSampleSizeInBits(), ais.getFormat()
								.getChannels(), decodedFormat.getFrameSize(),
						decodedFormat.getFrameRate(), decodedFormat
								.isBigEndian());

				ais = AudioSystem.getAudioInputStream(newFormat, ais);
			}

			if (ais.getFormat().getSampleRate() != decodedFormat
					.getSampleRate())
			{
				AudioFormat newFormat = new AudioFormat(ais.getFormat()
						.getEncoding(), decodedFormat.getSampleRate(),
						decodedFormat.getSampleSizeInBits(), decodedFormat
								.getChannels(), decodedFormat.getFrameSize(),
						decodedFormat.getFrameRate(), decodedFormat
								.isBigEndian());
				ais = AudioSystem.getAudioInputStream(newFormat, ais);
			}
		}

		// Convert attributes.
		ais = AudioSystem.getAudioInputStream(decodedFormat, ais);

		return ais;
	}

	public AudioFormat getFormat()
	{
		return ais.getFormat();
	}

	public long getFrameLength()
	{
		return ais.getFrameLength();
	}

	public long skipSamples(long nframes) throws IOException
	{
		AudioFormat format = ais.getFormat();
		int bytesPerFrame = format.getFrameSize() / format.getChannels();
		return ais.skip(nframes * bytesPerFrame) / bytesPerFrame;
	}

	public int readSamples(double[] samples) throws IOException
	{
		return readSamples(samples, 0, samples.length);
	}

	public int readSamples(double[] samples, int offset, int length)
			throws IOException
	{
		int bytesPerFrame = ais.getFormat().getFrameSize();
		byte[] bytes = new byte[length * bytesPerFrame];
		int retval = ais.read(bytes, 0, bytes.length);
		bytes2doubles(bytes, samples, offset);

		return retval;
	}

	public void closeStream() throws IOException
	{
		ais.close();
	}

	// Convert a byte stream into a stream of doubles. If it's
	// stereo, the channels will be interleaved with each other in the
	// double stream, as in the byte stream. offset is the offset (in
	// samples) into audioData.
	private void bytes2doubles(byte[] audioBytes, double[] audioData, int offset)
	{
		AudioFormat format = ais.getFormat();
		if (format.getSampleSizeInBits() == 16)
		{
			if (format.isBigEndian())
			{
				for (int i = 0; i < audioData.length - offset; i++)
				{
					/* First byte is MSB (high order) */
					int MSB = (int) audioBytes[2 * i];
					/* Second byte is LSB (low order) */
					int LSB = (int) audioBytes[2 * i + 1];
					audioData[offset + i] = ((double) (MSB << 8 | (255 & LSB))) / 32768.0;
				}
			}
			else
			{
				for (int i = 0; i < audioData.length - offset; i++)
				{
					/* First byte is LSB (low order) */
					int LSB = (int) audioBytes[2 * i];
					/* Second byte is MSB (high order) */
					int MSB = (int) audioBytes[2 * i + 1];
					audioData[offset + i] = ((double) (MSB << 8 | (255 & LSB))) / 32768.0;
				}
			}
		}
		else if (format.getSampleSizeInBits() == 8)
		{
			// int nlengthInSamples = audioBytes.length;
			if (format.getEncoding().toString().startsWith("PCM_SIGN"))
			{
				for (int i = 0; i < audioBytes.length - offset; i++)
					audioData[offset + i] = audioBytes[i] / 128.0;
			}
			else
			{
				for (int i = 0; i < audioBytes.length; i++)
					audioData[offset + i] = (audioBytes[i] - 128) / 128.0;
			}
		}
	}
}
