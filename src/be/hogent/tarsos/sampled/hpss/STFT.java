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
import java.util.ArrayList;
import java.util.Arrays;

import javax.sound.sampled.AudioFormat;

/**
 * A class to extract and hold STFT info from an AudioInputStream. Calls
 * listeners when a new frame is added. Calculates the log magnitude
 * spectrogram.
 * 
 * @author Mike Mandel (mim@ee.columbia.edu)
 */
public class STFT {
	AudioReader input;

	AudioFormat format;

	int frameLen;

	ArrayList<FrameListener> listeners = new ArrayList<FrameListener>();

	double[] re, im, window;

	static double log10 = Math.log(10);

	static double epsilon = 1e-9; // avoid log of zero

	RingMatrix freq, time;

	FFT fft;

	static double rmsTarget = 0.08;

	static double rmsAlpha = 0.001;

	double rms = 1;

	public float samplingRate;

	public int nhop;

	// The line should be open, but not started yet.
	public STFT(AudioReader input, int frameLen, int hopsize, int history) {
		this(input, frameLen, history);

		nhop = hopsize;
	}

	// The line should be open, but not started yet.
	public STFT(AudioReader input, int frameLen, int history) {
		freq = new RingMatrix(frameLen / 2 + 1, history);
		time = new RingMatrix(frameLen, history);
		this.frameLen = frameLen;

		this.input = input;
		format = input.getFormat();

		samplingRate = format.getSampleRate();

		fft = new FFT(frameLen);

		this.re = new double[frameLen];
		this.im = new double[frameLen];
		this.window = fft.getWindow();
		for (int i = 0; i < im.length; i++) {
			im[i] = 0;
		}

		nhop = frameLen;

		samplingRate = input.getFormat().getSampleRate();
	}

	// returns the total number of samples read
	public long start() {
		double[] samples = new double[frameLen];
		Arrays.fill(samples, 0);

		int noverlap = frameLen - nhop;

		int totalSamplesRead = 0;
		int samplesRead = 1;
		while (samplesRead > 0) {
			if (nhop > 0) {
				// shift samples so our overlap works out
				for (int x = 0; x < noverlap; x++) {
					samples[x] = samples[x + nhop];
				}
			}

			try {
				samplesRead = input.readSamples(samples, noverlap, nhop);
				totalSamplesRead += samplesRead;
			} catch (IOException ioe) {
				ioe.printStackTrace();
				return totalSamplesRead;
			}

			processFrame(samples);
		}

		// let the frame listeners know that we're done reading:
		notifyListeners(-1);

		return totalSamplesRead;
	}

	// Convert an address in frames into an address in samples
	public long fr2Samp(long frAddr) {
		return nhop * frAddr;
	}

	// Convert an address in samples into an address in frames
	public long samp2fr(long sampAddr) {
		return sampAddr / nhop;
	}

	public double[] getFrame(long frAddr) {
		return freq.getColumn(frAddr);
	}

	public void setFrame(long frAddr, double[] dat) {
		freq.setColumn(frAddr, dat);
	}

	public int getColumns() {
		return freq.getColumns();
	}

	public int getRows() {
		return freq.getRows();
	}

	// Dealing with FrameListeners
	public void addFrameListener(FrameListener fl) {
		listeners.add(fl);
	}

	public void notifyListeners(long frAddr) {
		for (int i = 0; i < listeners.size(); i++) {
			FrameListener list = listeners.get(i);
			list.newFrame(this, frAddr);
		}
	}

	// Convert an address in frames to an address in seconds
	public double fr2Seconds(long frAddr) {
		return fr2Samp(frAddr) / samplingRate;
	}

	// Convert an address in seconds to an address in frames
	public long seconds2fr(double sec) {
		return samp2fr((long) (sec * samplingRate));
	}

	/**
	 * Get the STFT of samples
	 */
	public static RingMatrix getSTFT(double[] samples, int nfft, int nhop) {
		RingMatrix freq = new RingMatrix(nfft / 2 + 1, samples.length / nhop);

		FFT fft = new FFT(nfft);
		double[] window = fft.getWindow();

		double[] wav = new double[nfft];
		double rms = 1;
		for (int currFrame = 0; currFrame < samples.length / nhop; currFrame++) {
			// zero pad if we run out of samples:
			int zeroPadLen = currFrame * nhop + wav.length - samples.length;
			if (zeroPadLen < 0) {
				zeroPadLen = 0;
			}
			int wavLen = wav.length - zeroPadLen;

			// for(int i = 0; i<wav.length; i++)
			// wav[i] = samples[currFrame*nhop + i];
			for (int i = 0; i < wavLen; i++) {
				wav[i] = samples[currFrame * nhop + i];
			}
			for (int i = wavLen; i < wav.length; i++) {
				wav[i] = 0;
			}

			// Normalize rms using a moving average estimate of it
			// Calculate current rms
			double rmsCur = 0;
			for (double element : wav) {
				rmsCur += element * element;
			}
			rmsCur = Math.sqrt(rmsCur / wav.length);

			// update moving average
			rms = rmsAlpha * rmsCur + (1 - rmsAlpha) * rms;

			// normalize by rms
			for (int i = 0; i < wav.length; i++) {
				wav[i] = wav[i] * rmsTarget / rms;
			}

			// window waveform
			double[] re = new double[wav.length];
			double[] im = new double[wav.length];
			for (int i = 0; i < wav.length; i++) {
				re[i] = window[i] * wav[i];
				im[i] = 0;
			}

			// take fft
			fft.fft(re, im);

			// Calculate magnitude
			double[] mag = freq.checkOutColumn();
			for (int i = 0; i < mag.length; i++) {
				mag[i] = 10 * Math.log(re[i] * re[i] + im[i] * im[i] + epsilon) / log10;
			}

			if (mag.length == 0) {
				System.out.println("RingMatrix.getSTFT mag.length == 0!!!");
			}

			freq.checkInColumn(mag);
		}
		return freq;
	}

	/**
	 * Fill this STFT object with up to the next nframes frames of data.
	 * 
	 * @param nframes
	 *            number of audio frames to read
	 * 
	 * @return the number of frames actually read (will be less than nframes if
	 *         the end of the audio data is reached first).
	 */
	public int readFrames(long nframes) {
		double[] samples = new double[frameLen];
		Arrays.fill(samples, 0);

		int noverlap = frameLen - nhop;

		int nFramesRead = 0;
		while (nFramesRead <= nframes) {
			if (nhop > 0) {
				// shift samples so our overlap works out
				for (int x = 0; x < noverlap; x++) {
					samples[x] = samples[x + nhop];
				}
			}

			try {
				input.readSamples(samples, noverlap, nhop);
				nFramesRead++;
			} catch (IOException ioe) {
				ioe.printStackTrace();
				return nFramesRead;
			}

			processFrame(samples);

		}

		return nFramesRead;
	}

	private void processFrame(double[] samples) {
		// store the unwindowed waveform for getSamples function
		double[] wav = time.checkOutColumn();
		for (int x = 0; x < samples.length; x++) {
			wav[x] = samples[x];
		}

		if (wav.length == 0) {
			System.out.println("RingMatrix.processFrame wav.length == 0!!!");
		}
		time.checkInColumn(wav);

		// Normalize rms using a moving average estimate of it
		// Calculate current rms
		double rmsCur = 0;
		for (double element : wav) {
			rmsCur += element * element;
		}
		rmsCur = Math.sqrt(rmsCur / wav.length);

		// update moving average
		rms = rmsAlpha * rmsCur + (1 - rmsAlpha) * rms;

		// normalize by rms
		for (int i = 0; i < wav.length; i++) {
			wav[i] = wav[i] * rmsTarget / rms;
		}

		// time.checkInColumn(wav);

		// window waveform
		for (int i = 0; i < wav.length; i++) {
			re[i] = window[i] * wav[i];
		}

		// take fft
		fft.fft(re, im);

		// Calculate magnitude
		double[] mag = freq.checkOutColumn();
		for (int i = 0; i < mag.length; i++) {
			mag[i] = 10 * Math.log(re[i] * re[i] + im[i] * im[i] + epsilon) / log10;
		}

		// clear im[]
		Arrays.fill(im, 0);

		// Tell everyone concerned that we've added another frame
		if (mag.length == 0) {
			System.out.println("RingMatrix.processFrame mag.length == 0!!!");
		}
		long frAddr = freq.checkInColumn(mag);
		notifyListeners(frAddr);
	}

	public double[] invert() {
		final double[] samples = new double[nhop];

		return samples;
	}

	/**
	 * Get the frame address of the last frame read into this object.
	 */
	public long getLastFrameAddress() {
		return freq.nextFrAddr - 1;
	}

	public void stop() throws IOException {
		input.closeStream();
	}
}
