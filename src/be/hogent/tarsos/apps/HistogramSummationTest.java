package be.hogent.tarsos.apps;

import java.util.List;

import be.hogent.tarsos.pitch.AubioPitchDetection;
import be.hogent.tarsos.pitch.IPEMPitchDetection;
import be.hogent.tarsos.pitch.PitchDetector;
import be.hogent.tarsos.pitch.Sample;
import be.hogent.tarsos.pitch.YinPitchDetection;
import be.hogent.tarsos.pitch.AubioPitchDetection.AubioPitchDetectionMode;
import be.hogent.tarsos.util.AudioFile;
import be.hogent.tarsos.util.histogram.AmbitusHistogram;
import be.hogent.tarsos.util.histogram.ToneScaleHistogram;

public class HistogramSummationTest {

	/**
	 * Tests if summing histograms constructed with different pitch extractors
	 * is useful.
	 */
	public static void main(String[] args) {
		AudioFile audioFile = new AudioFile("audio\\maghreb\\4_ABERDAG___LA_DANSE.wav");
		PitchDetector pitchDetector = new YinPitchDetection(audioFile);
		pitchDetector.executePitchDetection();
		List<Sample> samples = pitchDetector.getSamples();
		AmbitusHistogram ambitusHistogram = Sample.ambitusHistogram(samples);
		ToneScaleHistogram toneScaleHistogramTarsosYin = ambitusHistogram.toneScaleHistogram();

		pitchDetector = new AubioPitchDetection(audioFile, AubioPitchDetectionMode.YIN);
		pitchDetector.executePitchDetection();
		samples = pitchDetector.getSamples();
		ambitusHistogram = Sample.ambitusHistogram(samples);
		ToneScaleHistogram toneScaleHistogramAbioYin = ambitusHistogram.toneScaleHistogram();

		pitchDetector = new IPEMPitchDetection(audioFile);
		pitchDetector.executePitchDetection();
		samples = pitchDetector.getSamples();
		ambitusHistogram = Sample.ambitusHistogram(samples);
		ToneScaleHistogram toneScaleHistogramIPEM = ambitusHistogram.toneScaleHistogram();

		toneScaleHistogramTarsosYin.normalize();
		toneScaleHistogramAbioYin.normalize();
		toneScaleHistogramIPEM.normalize();

		toneScaleHistogramTarsosYin.plot("data/tests/tarsos_yin.png", "tarsos_yin");
		toneScaleHistogramAbioYin.plot("data/tests/audbio_yin.png", "aubio_yin");
		toneScaleHistogramIPEM.plot("data/tests/ipem.png", "ipem");

		toneScaleHistogramTarsosYin.add(toneScaleHistogramAbioYin).add(toneScaleHistogramIPEM).normalize().gaussianSmooth(1.0).plot(
				"data/tests/added.png", "Aubio + Tarsos + Ipem");
	}
}
