package be.hogent.tarsos.cli.temp;

import java.util.List;
import java.util.TreeMap;

import be.hogent.tarsos.Tarsos;
import be.hogent.tarsos.sampled.pitch.Annotation;
import be.hogent.tarsos.sampled.pitch.IPEMPitchDetection;
import be.hogent.tarsos.sampled.pitch.PitchDetectionMode;
import be.hogent.tarsos.sampled.pitch.PitchDetector;
import be.hogent.tarsos.sampled.pitch.VampPitchDetection;
import be.hogent.tarsos.transcoder.ffmpeg.EncoderException;
import be.hogent.tarsos.util.AudioFile;
import be.hogent.tarsos.util.ConfKey;
import be.hogent.tarsos.util.Configuration;
import be.hogent.tarsos.util.FileUtils;
import be.hogent.tarsos.util.ScalaFile;
import be.hogent.tarsos.util.histogram.AmbitusHistogram;
import be.hogent.tarsos.util.histogram.ToneScaleHistogram;
import be.hogent.tarsos.util.histogram.peaks.Peak;
import be.hogent.tarsos.util.histogram.peaks.PeakDetector;

public final class ToneScaleMatcher {

	/**
	 * Disables the default constructor.
	 */
	private ToneScaleMatcher() {
	}

	public static void main(final String... args) throws EncoderException {
		
		String detector = "IPEM_SIX";
		String inputFile = null;
		
		if (inputFile == null || !FileUtils.exists(inputFile)) {
			printHelp();
			System.exit(-1);
		}

		double[] peaks = new ScalaFile(inputFile).getPitches();
		final ToneScaleHistogram needleToneScale = ToneScaleHistogram
				.createToneScale(peaks, null, null, null);

		final String pattern = Configuration.get(ConfKey.audio_file_name_pattern);
		final String globDirectory = FileUtils.combine(FileUtils.runtimeDirectory(), "audio");
		final List<String> inputFiles = FileUtils.glob(globDirectory, pattern, false);
		// two priority queues with info about same histograms
		final TreeMap<Double, ToneScaleHistogram> toneScaleCorrelations;
		toneScaleCorrelations = new TreeMap<Double, ToneScaleHistogram>();
		final TreeMap<Double, String> fileNameCorrelations = new TreeMap<Double, String>();

		for (final String file : inputFiles) {
			final AudioFile audioFile = new AudioFile(file);
			final PitchDetector pitchDetector;
			if (detector.equals("AUBIO")) {
				pitchDetector = new VampPitchDetection(audioFile, PitchDetectionMode.VAMP_YIN);
			} else {
				pitchDetector = new IPEMPitchDetection(audioFile, PitchDetectionMode.IPEM_SIX);
			}
			pitchDetector.executePitchDetection();
			final List<Annotation> samples = pitchDetector.getAnnotations();
			final AmbitusHistogram ambitusHistogram = Annotation.ambitusHistogram(samples);
			final ToneScaleHistogram toneScaleHistogram = ambitusHistogram.toneScaleHistogram();
			toneScaleHistogram.gaussianSmooth(1.0);
			final List<Peak> detectedPeaks = PeakDetector.detect(toneScaleHistogram, 10);
			peaks = new double[detectedPeaks.size()];
			for (int i = 0; i < detectedPeaks.size(); i++) {
				peaks[i] = detectedPeaks.get(i).getPosition();
			}
			final ToneScaleHistogram hayStackHistogram = ToneScaleHistogram.createToneScale(peaks, null,
					null, null);

			final int displacementForOptimalCorrelation = needleToneScale
					.displacementForOptimalCorrelation(hayStackHistogram);
			final Double correlation = needleToneScale.correlationWithDisplacement(
					displacementForOptimalCorrelation, hayStackHistogram);

			toneScaleCorrelations.put(correlation, hayStackHistogram);
			fileNameCorrelations.put(correlation, audioFile.basename());
		}

		// print all correlations in descending order
		// best match first
		Tarsos.println("correlation\tfile");
		for (final Double key : toneScaleCorrelations.keySet()) {
			Tarsos.println(key + "\t" + fileNameCorrelations.get(key));
		}

		// plot best correlation
		// if (toneScaleCorrelations.size() > 0) {
		// final double bestCorrelation =
		// toneScaleCorrelations.descendingKeySet().first();
		// final ToneScaleHistogram hayStackHistogram =
		// toneScaleCorrelations.get(bestCorrelation);
		// needleToneScale.plotCorrelation(hayStackHistogram,
		// CorrelationMeasure.INTERSECTION);
		// }
	}

	private static void printHelp() {
		Tarsos.println("");
		Tarsos.println("Find a file in the audio directory with the best match for the defined tone scale.");
		Tarsos.println("");
		Tarsos.println("-----------------------");
		Tarsos.println("");
		Tarsos.println("java -jar tonescalematcher.jar [--in in.slc] [--help]");
		Tarsos.println("");
		Tarsos.println("-----------------------");
		Tarsos.println("");
		Tarsos.println("--in in.scl\t\tThe scala file with the tone scale.");
		Tarsos.println("--detector AUBIO|IPEM_SIX the pitch detector.");
		Tarsos.println("--help\t\tPrints this information");
		Tarsos.println("");
	}
}
