package be.hogent.tarsos.util;

/**
 * Wrapper for calling a hopefully Fast Fourier transform. Makes it easy to
 * switch FFT algorithm with minimal overhead.
 * 
 * @author Joren Six
 * 
 */
public class FFT {
    /**
     * Forward FFT
     */
    private final FloatFFT ffft;
    /**
     * Inverse FFT
     */
    private final FloatFFT ifft;

    public FFT(int size) {
        ffft = new FloatFFT(size, -1);
        ifft = new FloatFFT(size, 1);
    }

    /**
     * 
     * Computes forward DFT
     * 
     * @param data
     *            data to transform
     */
    public void forwardTransform(float[] data) {
        ffft.transform(data);
    }

    /**
     * Computes inverse DFT
     * 
     * @param data
     *            data to transform
     */
    public void backwardsTransform(float[] data) {
        ifft.transform(data);
    }

    /**
     * Returns the modulus of the element at index i. The modulus, magnitude or
     * absolute value is (a²+b²) ^ 0.5 with a being the real part and b the
     * imaginary part of a complex number.
     * 
     * @param data
     *            The FFT transformed data.
     * @param i
     *            The index of the element.
     * @return The modulus, magnitude or absolute value of the element at index
     *         i
     */
    public float modulus(float[] data, int i) {
        float modulus = data[i] * data[i] + data[i + data.length / 2] * data[i + data.length / 2];
        modulus = (float) Math.pow(modulus, 0.5);
        return modulus;
    }

    /*
     * Copyright 2007 Sun Microsystems, Inc. All Rights Reserved. DO NOT ALTER
     * OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
     * 
     * This code is free software; you can redistribute it and/or modify it
     * under the terms of the GNU General Public License version 2 only, as
     * published by the Free Software Foundation. Sun designates this particular
     * file as subject to the "Classpath" exception as provided by Sun in the
     * LICENSE file that accompanied this code.
     * 
     * This code is distributed in the hope that it will be useful, but WITHOUT
     * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
     * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License
     * version 2 for more details (a copy is included in the LICENSE file that
     * accompanied this code).
     * 
     * You should have received a copy of the GNU General Public License version
     * 2 along with this work; if not, write to the Free Software Foundation,
     * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
     * 
     * Please contact Sun Microsystems, Inc., 4150 Network Circle, Santa Clara,
     * CA 95054 USA or visit www.sun.com if you need additional information or
     * have any questions.
     */
    /**
     * Fast Fourier Transformer. Converted the implementation by Karl Helgason
     * to use float values instead of doubles.
     * 
     * @author Karl Helgason
     */
    private static class FloatFFT {
        private final float[] w;
        private final int fftFrameSize;
        private final int sign;
        private final int[] bitm_array;
        private final int fftFrameSize2;

        /**
         * Data = Interlaced float array to be transformed. The order is: real
         * (sin), complex (cos)
         * 
         * @param fftFrameSize
         *            Framesize must be power of 2
         * @param sign
         *            Sign = -1 is FFT, 1 is IFFT (inverse FFT)
         */
        public FloatFFT(int fftFrameSize, int sign) {
            w = computeTwiddleFactors(fftFrameSize, sign);

            this.fftFrameSize = fftFrameSize;
            this.sign = sign;
            fftFrameSize2 = fftFrameSize << 1;

            // Pre-process Bit-Reversal
            bitm_array = new int[fftFrameSize2];
            for (int i = 2; i < fftFrameSize2; i += 2) {
                int j;
                int bitm;
                for (bitm = 2, j = 0; bitm < fftFrameSize2; bitm <<= 1) {
                    if ((i & bitm) != 0) {
                        j++;
                    }
                    j <<= 1;
                }
                bitm_array[i] = j;
            }

        }

        public void transform(float[] data) {
            bitreversal(data);
            calc(fftFrameSize, data, sign, w);
        }

        private final static float[] computeTwiddleFactors(int fftFrameSize, int sign) {

            int imax = (int) (Math.log(fftFrameSize) / Math.log(2.));

            float[] warray = new float[(fftFrameSize - 1) * 4];
            int w_index = 0;

            for (int i = 0, nstep = 2; i < imax; i++) {
                int jmax = nstep;
                nstep <<= 1;

                float wr = 1.0f;
                float wi = 0.0f;

                float arg = (float) (Math.PI / (jmax >> 1));
                float wfr = (float) Math.cos(arg);
                float wfi = (float) (sign * Math.sin(arg));

                for (int j = 0; j < jmax; j += 2) {
                    warray[w_index++] = wr;
                    warray[w_index++] = wi;

                    float tempr = wr;
                    wr = tempr * wfr - wi * wfi;
                    wi = tempr * wfi + wi * wfr;
                }
            }

            // PRECOMPUTATION of wwr1, wwi1 for factor 4 Decomposition (3 *
            // complex
            // operators and 8 +/- complex operators)
            {
                w_index = 0;
                int w_index2 = warray.length >> 1;
                for (int i = 0, nstep = 2; i < (imax - 1); i++) {
                    int jmax = nstep;
                    nstep *= 2;

                    int ii = w_index + jmax;
                    for (int j = 0; j < jmax; j += 2) {
                        float wr = warray[w_index++];
                        float wi = warray[w_index++];
                        float wr1 = warray[ii++];
                        float wi1 = warray[ii++];
                        warray[w_index2++] = wr * wr1 - wi * wi1;
                        warray[w_index2++] = wr * wi1 + wi * wr1;
                    }
                }

            }

            return warray;
        }

        private final static void calc(int fftFrameSize, float[] data, int sign, float[] w) {

            final int fftFrameSize2 = fftFrameSize << 1;

            int nstep = 2;

            if (nstep >= fftFrameSize2) {
                return;
            }
            int i = nstep - 2;
            if (sign == -1) {
                calcF4F(fftFrameSize, data, i, nstep, w);
            } else {
                calcF4I(fftFrameSize, data, i, nstep, w);
            }

        }

        private final static void calcF2E(int fftFrameSize, float[] data, int i, int nstep, float[] w) {
            int jmax = nstep;
            for (int n = 0; n < jmax; n += 2) {
                float wr = w[i++];
                float wi = w[i++];
                int m = n + jmax;
                float datam_r = data[m];
                float datam_i = data[m + 1];
                float datan_r = data[n];
                float datan_i = data[n + 1];
                float tempr = datam_r * wr - datam_i * wi;
                float tempi = datam_r * wi + datam_i * wr;
                data[m] = datan_r - tempr;
                data[m + 1] = datan_i - tempi;
                data[n] = datan_r + tempr;
                data[n + 1] = datan_i + tempi;
            }
            return;

        }

        // Perform Factor-4 Decomposition with 3 * complex operators and 8 +/-
        // complex operators
        private final static void calcF4F(int fftFrameSize, float[] data, int i, int nstep, float[] w) {
            final int fftFrameSize2 = fftFrameSize << 1; // 2*fftFrameSize;
            // Factor-4 Decomposition

            int w_len = w.length >> 1;
            while (nstep < fftFrameSize2) {

                if (nstep << 2 == fftFrameSize2) {
                    // Goto Factor-4 Final Decomposition
                    // calcF4E(data, i, nstep, -1, w);
                    calcF4FE(fftFrameSize, data, i, nstep, w);
                    return;
                }
                int jmax = nstep;
                int nnstep = nstep << 1;
                if (nnstep == fftFrameSize2) {
                    // Factor-4 Decomposition not possible
                    calcF2E(fftFrameSize, data, i, nstep, w);
                    return;
                }
                nstep <<= 2;
                int ii = i + jmax;
                int iii = i + w_len;

                {
                    i += 2;
                    ii += 2;
                    iii += 2;

                    for (int n = 0; n < fftFrameSize2; n += nstep) {
                        int m = n + jmax;

                        float datam1_r = data[m];
                        float datam1_i = data[m + 1];
                        float datan1_r = data[n];
                        float datan1_i = data[n + 1];

                        n += nnstep;
                        m += nnstep;
                        float datam2_r = data[m];
                        float datam2_i = data[m + 1];
                        float datan2_r = data[n];
                        float datan2_i = data[n + 1];

                        float tempr = datam1_r;
                        float tempi = datam1_i;

                        datam1_r = datan1_r - tempr;
                        datam1_i = datan1_i - tempi;
                        datan1_r = datan1_r + tempr;
                        datan1_i = datan1_i + tempi;

                        float n2w1r = datan2_r;
                        float n2w1i = datan2_i;
                        float m2ww1r = datam2_r;
                        float m2ww1i = datam2_i;

                        tempr = m2ww1r - n2w1r;
                        tempi = m2ww1i - n2w1i;

                        datam2_r = datam1_r + tempi;
                        datam2_i = datam1_i - tempr;
                        datam1_r = datam1_r - tempi;
                        datam1_i = datam1_i + tempr;

                        tempr = n2w1r + m2ww1r;
                        tempi = n2w1i + m2ww1i;

                        datan2_r = datan1_r - tempr;
                        datan2_i = datan1_i - tempi;
                        datan1_r = datan1_r + tempr;
                        datan1_i = datan1_i + tempi;

                        data[m] = datam2_r;
                        data[m + 1] = datam2_i;
                        data[n] = datan2_r;
                        data[n + 1] = datan2_i;

                        n -= nnstep;
                        m -= nnstep;
                        data[m] = datam1_r;
                        data[m + 1] = datam1_i;
                        data[n] = datan1_r;
                        data[n + 1] = datan1_i;

                    }
                }

                for (int j = 2; j < jmax; j += 2) {
                    float wr = w[i++];
                    float wi = w[i++];
                    float wr1 = w[ii++];
                    float wi1 = w[ii++];
                    float wwr1 = w[iii++];
                    float wwi1 = w[iii++];
                    // float wwr1 = wr * wr1 - wi * wi1; // these numbers can be
                    // precomputed!!!
                    // float wwi1 = wr * wi1 + wi * wr1;

                    for (int n = j; n < fftFrameSize2; n += nstep) {
                        int m = n + jmax;

                        float datam1_r = data[m];
                        float datam1_i = data[m + 1];
                        float datan1_r = data[n];
                        float datan1_i = data[n + 1];

                        n += nnstep;
                        m += nnstep;
                        float datam2_r = data[m];
                        float datam2_i = data[m + 1];
                        float datan2_r = data[n];
                        float datan2_i = data[n + 1];

                        float tempr = datam1_r * wr - datam1_i * wi;
                        float tempi = datam1_r * wi + datam1_i * wr;

                        datam1_r = datan1_r - tempr;
                        datam1_i = datan1_i - tempi;
                        datan1_r = datan1_r + tempr;
                        datan1_i = datan1_i + tempi;

                        float n2w1r = datan2_r * wr1 - datan2_i * wi1;
                        float n2w1i = datan2_r * wi1 + datan2_i * wr1;
                        float m2ww1r = datam2_r * wwr1 - datam2_i * wwi1;
                        float m2ww1i = datam2_r * wwi1 + datam2_i * wwr1;

                        tempr = m2ww1r - n2w1r;
                        tempi = m2ww1i - n2w1i;

                        datam2_r = datam1_r + tempi;
                        datam2_i = datam1_i - tempr;
                        datam1_r = datam1_r - tempi;
                        datam1_i = datam1_i + tempr;

                        tempr = n2w1r + m2ww1r;
                        tempi = n2w1i + m2ww1i;

                        datan2_r = datan1_r - tempr;
                        datan2_i = datan1_i - tempi;
                        datan1_r = datan1_r + tempr;
                        datan1_i = datan1_i + tempi;

                        data[m] = datam2_r;
                        data[m + 1] = datam2_i;
                        data[n] = datan2_r;
                        data[n + 1] = datan2_i;

                        n -= nnstep;
                        m -= nnstep;
                        data[m] = datam1_r;
                        data[m + 1] = datam1_i;
                        data[n] = datan1_r;
                        data[n + 1] = datan1_i;
                    }
                }

                i += jmax << 1;

            }

            calcF2E(fftFrameSize, data, i, nstep, w);

        }

        // Perform Factor-4 Decomposition with 3 * complex operators and 8 +/-
        // complex operators
        private final static void calcF4I(int fftFrameSize, float[] data, int i, int nstep, float[] w) {
            final int fftFrameSize2 = fftFrameSize << 1; // 2*fftFrameSize;
            // Factor-4 Decomposition

            int w_len = w.length >> 1;
            while (nstep < fftFrameSize2) {

                if (nstep << 2 == fftFrameSize2) {
                    // Goto Factor-4 Final Decomposition
                    // calcF4E(data, i, nstep, 1, w);
                    calcF4IE(fftFrameSize, data, i, nstep, w);
                    return;
                }
                int jmax = nstep;
                int nnstep = nstep << 1;
                if (nnstep == fftFrameSize2) {
                    // Factor-4 Decomposition not possible
                    calcF2E(fftFrameSize, data, i, nstep, w);
                    return;
                }
                nstep <<= 2;
                int ii = i + jmax;
                int iii = i + w_len;
                {
                    i += 2;
                    ii += 2;
                    iii += 2;

                    for (int n = 0; n < fftFrameSize2; n += nstep) {
                        int m = n + jmax;

                        float datam1_r = data[m];
                        float datam1_i = data[m + 1];
                        float datan1_r = data[n];
                        float datan1_i = data[n + 1];

                        n += nnstep;
                        m += nnstep;
                        float datam2_r = data[m];
                        float datam2_i = data[m + 1];
                        float datan2_r = data[n];
                        float datan2_i = data[n + 1];

                        float tempr = datam1_r;
                        float tempi = datam1_i;

                        datam1_r = datan1_r - tempr;
                        datam1_i = datan1_i - tempi;
                        datan1_r = datan1_r + tempr;
                        datan1_i = datan1_i + tempi;

                        float n2w1r = datan2_r;
                        float n2w1i = datan2_i;
                        float m2ww1r = datam2_r;
                        float m2ww1i = datam2_i;

                        tempr = n2w1r - m2ww1r;
                        tempi = n2w1i - m2ww1i;

                        datam2_r = datam1_r + tempi;
                        datam2_i = datam1_i - tempr;
                        datam1_r = datam1_r - tempi;
                        datam1_i = datam1_i + tempr;

                        tempr = n2w1r + m2ww1r;
                        tempi = n2w1i + m2ww1i;

                        datan2_r = datan1_r - tempr;
                        datan2_i = datan1_i - tempi;
                        datan1_r = datan1_r + tempr;
                        datan1_i = datan1_i + tempi;

                        data[m] = datam2_r;
                        data[m + 1] = datam2_i;
                        data[n] = datan2_r;
                        data[n + 1] = datan2_i;

                        n -= nnstep;
                        m -= nnstep;
                        data[m] = datam1_r;
                        data[m + 1] = datam1_i;
                        data[n] = datan1_r;
                        data[n + 1] = datan1_i;

                    }

                }
                for (int j = 2; j < jmax; j += 2) {
                    float wr = w[i++];
                    float wi = w[i++];
                    float wr1 = w[ii++];
                    float wi1 = w[ii++];
                    float wwr1 = w[iii++];
                    float wwi1 = w[iii++];
                    // float wwr1 = wr * wr1 - wi * wi1; // these numbers can be
                    // precomputed!!!
                    // float wwi1 = wr * wi1 + wi * wr1;

                    for (int n = j; n < fftFrameSize2; n += nstep) {
                        int m = n + jmax;

                        float datam1_r = data[m];
                        float datam1_i = data[m + 1];
                        float datan1_r = data[n];
                        float datan1_i = data[n + 1];

                        n += nnstep;
                        m += nnstep;
                        float datam2_r = data[m];
                        float datam2_i = data[m + 1];
                        float datan2_r = data[n];
                        float datan2_i = data[n + 1];

                        float tempr = datam1_r * wr - datam1_i * wi;
                        float tempi = datam1_r * wi + datam1_i * wr;

                        datam1_r = datan1_r - tempr;
                        datam1_i = datan1_i - tempi;
                        datan1_r = datan1_r + tempr;
                        datan1_i = datan1_i + tempi;

                        float n2w1r = datan2_r * wr1 - datan2_i * wi1;
                        float n2w1i = datan2_r * wi1 + datan2_i * wr1;
                        float m2ww1r = datam2_r * wwr1 - datam2_i * wwi1;
                        float m2ww1i = datam2_r * wwi1 + datam2_i * wwr1;

                        tempr = n2w1r - m2ww1r;
                        tempi = n2w1i - m2ww1i;

                        datam2_r = datam1_r + tempi;
                        datam2_i = datam1_i - tempr;
                        datam1_r = datam1_r - tempi;
                        datam1_i = datam1_i + tempr;

                        tempr = n2w1r + m2ww1r;
                        tempi = n2w1i + m2ww1i;

                        datan2_r = datan1_r - tempr;
                        datan2_i = datan1_i - tempi;
                        datan1_r = datan1_r + tempr;
                        datan1_i = datan1_i + tempi;

                        data[m] = datam2_r;
                        data[m + 1] = datam2_i;
                        data[n] = datan2_r;
                        data[n + 1] = datan2_i;

                        n -= nnstep;
                        m -= nnstep;
                        data[m] = datam1_r;
                        data[m + 1] = datam1_i;
                        data[n] = datan1_r;
                        data[n + 1] = datan1_i;

                    }
                }

                i += jmax << 1;

            }

            calcF2E(fftFrameSize, data, i, nstep, w);

        }

        // Perform Factor-4 Decomposition with 3 * complex operators and 8 +/-
        // complex operators
        private final static void calcF4FE(int fftFrameSize, float[] data, int i, int nstep, float[] w) {
            final int fftFrameSize2 = fftFrameSize << 1; // 2*fftFrameSize;
            // Factor-4 Decomposition

            int w_len = w.length >> 1;
            while (nstep < fftFrameSize2) {

                int jmax = nstep;
                int nnstep = nstep << 1;
                if (nnstep == fftFrameSize2) {
                    // Factor-4 Decomposition not possible
                    calcF2E(fftFrameSize, data, i, nstep, w);
                    return;
                }
                nstep <<= 2;
                int ii = i + jmax;
                int iii = i + w_len;
                for (int n = 0; n < jmax; n += 2) {
                    float wr = w[i++];
                    float wi = w[i++];
                    float wr1 = w[ii++];
                    float wi1 = w[ii++];
                    float wwr1 = w[iii++];
                    float wwi1 = w[iii++];
                    // float wwr1 = wr * wr1 - wi * wi1; // these numbers can be
                    // precomputed!!!
                    // float wwi1 = wr * wi1 + wi * wr1;

                    int m = n + jmax;

                    float datam1_r = data[m];
                    float datam1_i = data[m + 1];
                    float datan1_r = data[n];
                    float datan1_i = data[n + 1];

                    n += nnstep;
                    m += nnstep;
                    float datam2_r = data[m];
                    float datam2_i = data[m + 1];
                    float datan2_r = data[n];
                    float datan2_i = data[n + 1];

                    float tempr = datam1_r * wr - datam1_i * wi;
                    float tempi = datam1_r * wi + datam1_i * wr;

                    datam1_r = datan1_r - tempr;
                    datam1_i = datan1_i - tempi;
                    datan1_r = datan1_r + tempr;
                    datan1_i = datan1_i + tempi;

                    float n2w1r = datan2_r * wr1 - datan2_i * wi1;
                    float n2w1i = datan2_r * wi1 + datan2_i * wr1;
                    float m2ww1r = datam2_r * wwr1 - datam2_i * wwi1;
                    float m2ww1i = datam2_r * wwi1 + datam2_i * wwr1;

                    tempr = m2ww1r - n2w1r;
                    tempi = m2ww1i - n2w1i;

                    datam2_r = datam1_r + tempi;
                    datam2_i = datam1_i - tempr;
                    datam1_r = datam1_r - tempi;
                    datam1_i = datam1_i + tempr;

                    tempr = n2w1r + m2ww1r;
                    tempi = n2w1i + m2ww1i;

                    datan2_r = datan1_r - tempr;
                    datan2_i = datan1_i - tempi;
                    datan1_r = datan1_r + tempr;
                    datan1_i = datan1_i + tempi;

                    data[m] = datam2_r;
                    data[m + 1] = datam2_i;
                    data[n] = datan2_r;
                    data[n + 1] = datan2_i;

                    n -= nnstep;
                    m -= nnstep;
                    data[m] = datam1_r;
                    data[m + 1] = datam1_i;
                    data[n] = datan1_r;
                    data[n + 1] = datan1_i;

                }

                i += jmax << 1;

            }

        }

        // Perform Factor-4 Decomposition with 3 * complex operators and 8 +/-
        // complex operators
        private final static void calcF4IE(int fftFrameSize, float[] data, int i, int nstep, float[] w) {
            final int fftFrameSize2 = fftFrameSize << 1; // 2*fftFrameSize;
            // Factor-4 Decomposition

            int w_len = w.length >> 1;
            while (nstep < fftFrameSize2) {

                int jmax = nstep;
                int nnstep = nstep << 1;
                if (nnstep == fftFrameSize2) {
                    // Factor-4 Decomposition not possible
                    calcF2E(fftFrameSize, data, i, nstep, w);
                    return;
                }
                nstep <<= 2;
                int ii = i + jmax;
                int iii = i + w_len;
                for (int n = 0; n < jmax; n += 2) {
                    float wr = w[i++];
                    float wi = w[i++];
                    float wr1 = w[ii++];
                    float wi1 = w[ii++];
                    float wwr1 = w[iii++];
                    float wwi1 = w[iii++];
                    // float wwr1 = wr * wr1 - wi * wi1; // these numbers can be
                    // precomputed!!!
                    // float wwi1 = wr * wi1 + wi * wr1;

                    int m = n + jmax;

                    float datam1_r = data[m];
                    float datam1_i = data[m + 1];
                    float datan1_r = data[n];
                    float datan1_i = data[n + 1];

                    n += nnstep;
                    m += nnstep;
                    float datam2_r = data[m];
                    float datam2_i = data[m + 1];
                    float datan2_r = data[n];
                    float datan2_i = data[n + 1];

                    float tempr = datam1_r * wr - datam1_i * wi;
                    float tempi = datam1_r * wi + datam1_i * wr;

                    datam1_r = datan1_r - tempr;
                    datam1_i = datan1_i - tempi;
                    datan1_r = datan1_r + tempr;
                    datan1_i = datan1_i + tempi;

                    float n2w1r = datan2_r * wr1 - datan2_i * wi1;
                    float n2w1i = datan2_r * wi1 + datan2_i * wr1;
                    float m2ww1r = datam2_r * wwr1 - datam2_i * wwi1;
                    float m2ww1i = datam2_r * wwi1 + datam2_i * wwr1;

                    tempr = n2w1r - m2ww1r;
                    tempi = n2w1i - m2ww1i;

                    datam2_r = datam1_r + tempi;
                    datam2_i = datam1_i - tempr;
                    datam1_r = datam1_r - tempi;
                    datam1_i = datam1_i + tempr;

                    tempr = n2w1r + m2ww1r;
                    tempi = n2w1i + m2ww1i;

                    datan2_r = datan1_r - tempr;
                    datan2_i = datan1_i - tempi;
                    datan1_r = datan1_r + tempr;
                    datan1_i = datan1_i + tempi;

                    data[m] = datam2_r;
                    data[m + 1] = datam2_i;
                    data[n] = datan2_r;
                    data[n + 1] = datan2_i;

                    n -= nnstep;
                    m -= nnstep;
                    data[m] = datam1_r;
                    data[m + 1] = datam1_i;
                    data[n] = datan1_r;
                    data[n + 1] = datan1_i;

                }

                i += jmax << 1;

            }

        }

        private final void bitreversal(float[] data) {
            if (fftFrameSize < 4) {
                return;
            }

            int inverse = fftFrameSize2 - 2;
            for (int i = 0; i < fftFrameSize; i += 4) {
                int j = bitm_array[i];

                // Performing Bit-Reversal, even v.s. even, O(2N)
                if (i < j) {

                    int n = i;
                    int m = j;

                    // COMPLEX: SWAP(data[n], data[m])
                    // Real Part
                    float tempr = data[n];
                    data[n] = data[m];
                    data[m] = tempr;
                    // Imagery Part
                    n++;
                    m++;
                    float tempi = data[n];
                    data[n] = data[m];
                    data[m] = tempi;

                    n = inverse - i;
                    m = inverse - j;

                    // COMPLEX: SWAP(data[n], data[m])
                    // Real Part
                    tempr = data[n];
                    data[n] = data[m];
                    data[m] = tempr;
                    // Imagery Part
                    n++;
                    m++;
                    tempi = data[n];
                    data[n] = data[m];
                    data[m] = tempi;
                }

                // Performing Bit-Reversal, odd v.s. even, O(N)

                int m = j + fftFrameSize; // bitm_array[i+2];
                // COMPLEX: SWAP(data[n], data[m])
                // Real Part
                int n = i + 2;
                float tempr = data[n];
                data[n] = data[m];
                data[m] = tempr;
                // Imagery Part
                n++;
                m++;
                float tempi = data[n];
                data[n] = data[m];
                data[m] = tempi;
            }

        }
    }
}
