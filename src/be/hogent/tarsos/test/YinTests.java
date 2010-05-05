package be.hogent.tarsos.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import org.junit.Test;

import be.hogent.tarsos.midi.ToneSequenceBuilder;
import be.hogent.tarsos.pitch.AubioPitchDetection;
import be.hogent.tarsos.pitch.PitchDetector;
import be.hogent.tarsos.pitch.Sample;
import be.hogent.tarsos.pitch.Yin;
import be.hogent.tarsos.pitch.YinPitchDetection;
import be.hogent.tarsos.pitch.Yin.DetectedPitchHandler;
import be.hogent.tarsos.util.AudioFile;
import be.hogent.tarsos.util.FileUtils;
import be.hogent.tarsos.util.histogram.AmbitusHistogram;
import be.hogent.tarsos.util.histogram.ToneScaleHistogram;

public class YinTests {
	/**
	 * The test file is one second 220Hz at 80% power, 1 second silence and 1
	 * second 440Hz at 100% power.
	 */
	public AudioFile testAudioFile() {
		return new AudioFile(FileUtils.combine("src", "be", "hogent", "tarsos", "test", "data", "power_test.wav"));
	}

	@Test
	public void compareWithAubioYin() {
		List<AudioFile> files = AudioFile.audioFiles("makam");
		FileUtils.mkdirs("data/tests/yin_tests");
		for (AudioFile file : files) {
			PitchDetector aubioYin = new AubioPitchDetection(file, AubioPitchDetection.AubioPitchDetectionMode.YIN);
			aubioYin.executePitchDetection();
			AmbitusHistogram ambitusHistogramAubio = Sample.ambitusHistogram(aubioYin.getSamples());
			ToneScaleHistogram toneScaleHistogramAubio = ambitusHistogramAubio.toneScaleHistogram();
			toneScaleHistogramAubio.plot("data/tests/yin_tests/" + file.basename() + "_aubio.png", file.basename());

			PitchDetector javaYin = new YinPitchDetection(file);
			javaYin.executePitchDetection();
			AmbitusHistogram ambitusHistogramJava = Sample.ambitusHistogram(javaYin.getSamples());
			ToneScaleHistogram toneScaleHistogramJava = ambitusHistogramJava.toneScaleHistogram();
			toneScaleHistogramJava.plot("data/tests/yin_tests/" + file.basename() + "_yin.png", file.basename());
			System.out.println(toneScaleHistogramAubio.correlation(toneScaleHistogramJava));
			// assertTrue("Correlation too small", 0.8 <
			// toneScaleHistogramAubio.correlation(toneScaleHistogramJava));
		}

	}

	@Test
	public void testPitchDetection() throws Exception {
		Yin.processFile(testAudioFile().path(), new DetectedPitchHandler() {
			@Override
			public void handleDetectedPitch(float time, float pitch) {
				if (time < 1.0)
					assertEquals(220.0, pitch, 0.5);
				else if (time > 1.1 && time <= 2.0)
					assertEquals(-1, pitch, 0.1);
				else if (time > 2.05)
					assertEquals(440, pitch, 1.0);
			}
		});

		ToneSequenceBuilder b = new ToneSequenceBuilder();
		final List<Double> frequencies = new ArrayList<Double>();
		Random r = new Random();
		for (int i = 0; i < 10; i++) {
			double frequency = r.nextDouble() * 900 + 40;
			frequencies.add(frequency);
			b.addTone(frequency, i + 1);
		}
		String testFileName = FileUtils.combine(System.getProperty("java.io.tmpdir"), "yin_test.wav");
		b.writeFile(testFileName, 0);

		CorrectPitchHandler handler = new CorrectPitchHandler(frequencies);
		Yin.processFile(testFileName, handler);

		double percentageCorrect = (handler.correct + 0.0) / (handler.total + 0.0);
		assertTrue(percentageCorrect > 0.50);
	}

	private class CorrectPitchHandler implements DetectedPitchHandler {
		Integer correct = 0;
		Integer total = 0;
		List<Double> frequencies;

		public CorrectPitchHandler(List<Double> frequencies) {
			this.frequencies = frequencies;
		}

		@Override
		public void handleDetectedPitch(float time, float pitch) {
			double expected = frequencies.get((int) time);
			double diff = expected / 100;
			if (pitch != -1 && pitch > expected - diff && pitch < expected + diff)
				correct++;
			total++;
		}
	}
}
