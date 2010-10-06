package be.hogent.tarsos.sampled;

import javax.sound.sampled.AudioFormat;

import com.sun.media.sound.AudioFloatConverter;

public final class BandPassFilter implements AudioProcessor {

	private final AudioFloatConverter converter;
	private final AudioFormat format;

	int n;
	double wc1, wc2, mult, peak;
	double resp[];
	boolean invert;
	int window = 2;
	DirectFilter f;

	public BandPassFilter(final AudioFormat audioFormat, final int floatBufferSize, final double from,
			final double to) {
		this.converter = AudioFloatConverter.getConverter(audioFormat);
		this.format = audioFormat;

		// center frequency
		double wcmid = 100 * Math.PI / 1000.;
		// band width
		double width = 100 * Math.PI / 1000.;

		wc1 = wcmid - width;
		wc2 = wcmid + width;
		if (wc1 < 0) {
			wc1 = 0;
		}
		if (wc2 > Math.PI) {
			wc2 = Math.PI;
		}
		// filter order
		n = 100;
		f = genFilter();

	}

	private DirectFilter genFilter() {
		DirectFilter f = new DirectFilter();
		f.aList = new double[n + 1];
		double xlist[] = new double[n + 1];
		int n2 = n / 2;
		int i;

		// generate low-pass filter
		double sum = 0;
		for (i = 0; i != n; i++) {
			int ii = i - n2;
			f.aList[i] = (ii == 0 ? wc1 : Math.sin(wc1 * ii) / ii) * getWindow(i, n);
			sum += f.aList[i];
		}
		if (sum > 0) {
			// normalize
			for (i = 0; i != n; i++) {
				f.aList[i] /= sum;
			}
		}

		// generate high-pass filter
		sum = 0;
		for (i = 0; i != n; i++) {
			int ii = i - n2;
			xlist[i] = (ii == 0 ? wc2 : Math.sin(wc2 * ii) / ii) * getWindow(i, n);
			sum += xlist[i];
		}
		// normalize
		for (i = 0; i != n; i++) {
			xlist[i] /= sum;
		}
		// invert and combine with lopass
		for (i = 0; i != n; i++) {
			f.aList[i] -= xlist[i];
		}
		f.aList[n2] += 1;
		if (invert) {
			for (i = 0; i != n; i++) {
				f.aList[i] = -f.aList[i];
			}
			f.aList[n2] += 1;
		}
		if (n == 1) {
			f.aList[0] = 1;
		}
		setResponse(f);
		return f;
	}

	double response[];

	void setResponse(DirectFilter f) {
		response = new double[8192];
		int i;
		if (f.nList.length != f.aList.length) {
			f.nList = new int[f.aList.length];
			for (i = 0; i != f.aList.length; i++) {
				f.nList[i] = i;
			}
		}
		for (i = 0; i != f.aList.length; i++) {
			response[f.nList[i] * 2] = f.aList[i];
		}
		new FFT(response.length / 2).transform(response, false);
		double maxresp = 0;
		int j;
		for (j = 0; j != response.length; j += 2) {
			double r2 = response[j] * response[j] + response[j + 1] * response[j + 1];
			if (maxresp < r2) {
				maxresp = r2;
			}
		}
		// normalize response
		maxresp = Math.sqrt(maxresp);
		for (j = 0; j != response.length; j++) {
			response[j] /= maxresp;
		}
		for (j = 0; j != f.aList.length; j++) {
			f.aList[j] /= maxresp;
		}
	}

	double getWindow(int i, int n) {
		if (n == 1) {
			return 1;
		}
		double x = 2 * Math.PI * i / (n - 1);
		double n2 = n / 2; // int
		switch (window) {
		case 0:// rect
			return 1;
		case 1:// hamming
			return .54 - .46 * Math.cos(x);
		case 2:// hann
			return .5 - .5 * Math.cos(x);
		case 3:// blackman
			return .42 - .5 * Math.cos(x) + .08 * Math.cos(2 * x);
		case 4:// bartlett
			return i < n2 ? i / n2 : 2 - i / n2;
		case 5: {// welch
			double xt = (i - n2) / n2;
			return 1 - xt * xt;
		}
		}
		return 0;
	}

	public void processFull(final float[] audioFloatBuffer, final byte[] audioByteBuffer) {

		delayLine = new double[response.length];
		processReal(audioFloatBuffer, audioByteBuffer);
	}

	public void processOverlapping(final float[] audioFloatBuffer, final byte[] audioByteBuffer) {
		processReal(audioFloatBuffer, audioByteBuffer);
	}

	float out[];
	private double[] delayLine;
	int sample = 0;

	public void processReal(final float[] audioFloatBuffer, final byte[] audioByteBuffer) {

		for (int i = 0; i < audioFloatBuffer.length; i++) {
			double output = getOutputSample(audioFloatBuffer[i]);
			audioFloatBuffer[i] = (float) output;
		}
		converter.toByteArray(audioFloatBuffer, audioByteBuffer);
	}

	double getOutputSample(double inputSample) {
		delayLine[sample] = inputSample;
		double result = 0.0;
		int index = sample;
		for (double element : response) {
			result += element * delayLine[index--];
			if (index < 0) {
				index = response.length - 1;
			}
		}
		if (++sample >= response.length) {
			sample = 0;
		}
		return result;
	}

	public void processingFinished() {
	}

	class DirectFilter {
		double aList[], bList[];
		int nList[];

		DirectFilter() {
			aList = new double[] { 1 };
			bList = null;
			nList = new int[] { 0 };
		}

		int getLength() {
			return aList.length;
		}

		boolean useConvolve() {
			return bList == null && aList.length > 25;
		}

		void dump() {
			System.out.print("a ");
			dump(aList);
			if (bList != null) {
				System.out.print("b ");
				dump(bList);
			}
		}

		void dump(double x[]) {
			int i;
			for (i = 0; i != x.length; i++) {
				System.out.print(x[i] + " ");
			}
			System.out.println("");
		}

		void run(float[] audioFloatBuffer, float[] out, long sample) {
			int j;
			int fi2, i20;
			double q = 0;

			int i2;
			for (i2 = 0; i2 != out.length; i2++) {
				fi2 = i2;
				i20 = fi2;

				q = audioFloatBuffer[i20] * aList[0];

				for (j = 1; j < aList.length; j++) {
					int ji = fi2 - nList[j];
					q += audioFloatBuffer[ji] * aList[j];
				}

				out[i20] = (float) q;
			}
		}

		boolean isSimpleAList() {
			if (bList != null) {
				return false;
			}
			return nList[nList.length - 1] == nList.length - 1;
		}

		int getImpulseOffset() {
			if (isSimpleAList()) {
				return 0;
			}
			return getStepOffset();
		}

		int getStepOffset() {
			int i;
			int offset = 0;
			for (i = 0; i != aList.length; i++) {
				if (nList[i] > offset) {
					offset = nList[i];
				}
			}
			return offset;
		}

	}

	class FFT {
		double wtabf[];
		double wtabi[];
		int size;

		FFT(int sz) {
			size = sz;
			if ((size & size - 1) != 0) {
				System.out.println("size must be power of two!");
			}
			calcWTable();
		}

		void calcWTable() {
			// calculate table of powers of w
			wtabf = new double[size];
			wtabi = new double[size];
			int i;
			for (i = 0; i != size; i += 2) {
				double pi = 3.1415926535;
				double th = pi * i / size;
				wtabf[i] = Math.cos(th);
				wtabf[i + 1] = Math.sin(th);
				wtabi[i] = wtabf[i];
				wtabi[i + 1] = -wtabf[i + 1];
			}
		}

		void transform(double data[], boolean inv) {
			int i;
			int j = 0;
			int size2 = size * 2;

			if ((size & size - 1) != 0) {
				System.out.println("size must be power of two!");
			}

			// bit-reversal
			double q;
			int bit;
			for (i = 0; i != size2; i += 2) {
				if (i > j) {
					q = data[i];
					data[i] = data[j];
					data[j] = q;
					q = data[i + 1];
					data[i + 1] = data[j + 1];
					data[j + 1] = q;
				}
				// increment j by one, from the left side (bit-reversed)
				bit = size;
				while ((bit & j) != 0) {
					j &= ~bit;
					bit >>= 1;
				}
				j |= bit;
			}

			// amount to skip through w table
			int tabskip = size << 1;
			double wtab[] = inv ? wtabi : wtabf;

			int skip1, skip2, ix, j2;
			double wr, wi, d1r, d1i, d2r, d2i, d2wr, d2wi;

			// unroll the first iteration of the main loop
			for (i = 0; i != size2; i += 4) {
				d1r = data[i];
				d1i = data[i + 1];
				d2r = data[i + 2];
				d2i = data[i + 3];
				data[i] = d1r + d2r;
				data[i + 1] = d1i + d2i;
				data[i + 2] = d1r - d2r;
				data[i + 3] = d1i - d2i;
			}
			tabskip >>= 1;

			// unroll the second iteration of the main loop
			int imult = inv ? -1 : 1;
			for (i = 0; i != size2; i += 8) {
				d1r = data[i];
				d1i = data[i + 1];
				d2r = data[i + 4];
				d2i = data[i + 5];
				data[i] = d1r + d2r;
				data[i + 1] = d1i + d2i;
				data[i + 4] = d1r - d2r;
				data[i + 5] = d1i - d2i;
				d1r = data[i + 2];
				d1i = data[i + 3];
				d2r = data[i + 6] * imult;
				d2i = data[i + 7] * imult;
				data[i + 2] = d1r - d2i;
				data[i + 3] = d1i + d2r;
				data[i + 6] = d1r + d2i;
				data[i + 7] = d1i - d2r;
			}
			tabskip >>= 1;

			for (skip1 = 16; skip1 <= size2; skip1 <<= 1) {
				// skip2 = length of subarrays we are combining
				// skip1 = length of subarray after combination
				skip2 = skip1 >> 1;
				tabskip >>= 1;
				for (i = 0; i != 1000; i++) {
					;
				}
				// for each subarray
				for (i = 0; i < size2; i += skip1) {
					ix = 0;
					// for each pair of complex numbers (one in each subarray)
					for (j = i; j != i + skip2; j += 2, ix += tabskip) {
						wr = wtab[ix];
						wi = wtab[ix + 1];
						d1r = data[j];
						d1i = data[j + 1];
						j2 = j + skip2;
						d2r = data[j2];
						d2i = data[j2 + 1];
						d2wr = d2r * wr - d2i * wi;
						d2wi = d2r * wi + d2i * wr;
						data[j] = d1r + d2wr;
						data[j + 1] = d1i + d2wi;
						data[j2] = d1r - d2wr;
						data[j2 + 1] = d1i - d2wi;
					}
				}
			}
		}
	}

}
