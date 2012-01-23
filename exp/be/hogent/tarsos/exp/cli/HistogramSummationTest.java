/**
*
*  Tarsos is developed by Joren Six at 
*  The Royal Academy of Fine Arts & Royal Conservatory,
*  University College Ghent,
*  Hoogpoort 64, 9000 Ghent - Belgium
*
**/
package be.hogent.tarsos.exp.cli;

import java.util.List;

import javax.sound.sampled.UnsupportedAudioFileException;

import be.hogent.tarsos.sampled.pitch.Annotation;
import be.hogent.tarsos.sampled.pitch.IPEMPitchDetection;
import be.hogent.tarsos.sampled.pitch.PitchDetectionMode;
import be.hogent.tarsos.sampled.pitch.PitchDetector;
import be.hogent.tarsos.sampled.pitch.TarsosPitchDetection;
import be.hogent.tarsos.sampled.pitch.VampPitchDetection;
import be.hogent.tarsos.transcoder.ffmpeg.EncoderException;
import be.hogent.tarsos.util.AudioFile;
import be.hogent.tarsos.util.histogram.HistogramFactory;
import be.hogent.tarsos.util.histogram.PitchHistogram;
import be.hogent.tarsos.util.histogram.PitchClassHistogram;

public final class HistogramSummationTest {
	private HistogramSummationTest() {
	}

	/**
	 * Tests if summing histograms constructed with different pitch extractors
	 * is useful.
	 * @param args 
	 * @throws EncoderException 
	 * 
	 * @throws UnsupportedAudioFileException
	 */
	public static void main(final String[] args) throws EncoderException {
		final AudioFile audioFile = new AudioFile("audio\\maghreb\\4_ABERDAG___LA_DANSE.wav");
		PitchDetector pitchDetector = new TarsosPitchDetection(audioFile, PitchDetectionMode.TARSOS_YIN);
		pitchDetector.executePitchDetection();
		List<Annotation> samples = pitchDetector.getAnnotations();
		PitchHistogram pitchHistogram = HistogramFactory.createPitchHistogram(samples);
		final PitchClassHistogram toneScaleHistogramTarsosYin = pitchHistogram.pitchClassHistogram();

		pitchDetector = new VampPitchDetection(audioFile, PitchDetectionMode.VAMP_YIN);
		pitchDetector.executePitchDetection();
		samples = pitchDetector.getAnnotations();
		pitchHistogram = HistogramFactory.createPitchHistogram(samples);
		final PitchClassHistogram toneScaleHistogramAbioYin = pitchHistogram.pitchClassHistogram();

		pitchDetector = new IPEMPitchDetection(audioFile, PitchDetectionMode.IPEM_SIX);
		pitchDetector.executePitchDetection();
		samples = pitchDetector.getAnnotations();
		pitchHistogram = HistogramFactory.createPitchHistogram(samples);
		final PitchClassHistogram toneScaleHistogramIPEM = pitchHistogram.pitchClassHistogram();

		toneScaleHistogramTarsosYin.normalize();
		toneScaleHistogramAbioYin.normalize();
		toneScaleHistogramIPEM.normalize();

		toneScaleHistogramTarsosYin.plot("data/tests/tarsos_yin.png", "tarsos_yin");
		toneScaleHistogramAbioYin.plot("data/tests/audbio_yin.png", "aubio_yin");
		toneScaleHistogramIPEM.plot("data/tests/ipem.png", "ipem");

		toneScaleHistogramTarsosYin.add(toneScaleHistogramAbioYin).add(toneScaleHistogramIPEM).normalize()
				.gaussianSmooth(1.0).plot("data/tests/added.png", "Aubio + Tarsos + Ipem");
	}
}
