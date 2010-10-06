package be.hogent.tarsos.cli.temp;

import gnu.getopt.Getopt;
import gnu.getopt.LongOpt;

import java.util.List;
import java.util.TreeMap;

import be.hogent.tarsos.Tarsos;
import be.hogent.tarsos.sampled.pitch.AubioPitchDetection;
import be.hogent.tarsos.sampled.pitch.IPEMPitchDetection;
import be.hogent.tarsos.sampled.pitch.PitchDetectionMode;
import be.hogent.tarsos.sampled.pitch.PitchDetector;
import be.hogent.tarsos.sampled.pitch.Sample;
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

	public static void main(final String... args) {
		final LongOpt[] longopts = new LongOpt[3];
		longopts[0] = new LongOpt("in", LongOpt.REQUIRED_ARGUMENT, null, 'i');
		longopts[1] = new LongOpt("detector", LongOpt.REQUIRED_ARGUMENT, null, 'd');
		longopts[2] = new LongOpt("help", LongOpt.NO_ARGUMENT, null, 'h');
		final Getopt g = new Getopt("tonescalematcher", args, "-bufferCount:h:d", longopts);

		String detector = "IPEM_SIX";
		String inputFile = null;
		int c;
		while ((c = g.getopt()) != -1) {
			final String arg = g.getOptarg();
			switch (c) {
			case 'i':
				inputFile = arg;
				break;
			case 'd':
				detector = arg.toUpperCase();
				break;
			case 'h':
				printHelp();
				System.exit(0);
				return;
			default:
				throw new AssertionError("Your argument is invalid.");
			}
		}

		if (inputFile == null || !FileUtils.exists(inputFile)) {
			printHelp();
			System.exit(-1);
		}

		double[] peaks = new ScalaFile(inputFile).getPitches();
		final ToneScaleHistogram needleToneScale = ToneScaleHistogram
				.createToneScale(peaks, null, null, null);

		final String pattern = Configuration.get(ConfKey.audio_file_name_pattern);
		final String globDirectory = FileUtils.combine(FileUtils.getRuntimePath(), "audio");
		final List<String> inputFiles = FileUtils.glob(globDirectory, pattern, false);
		// two priority queues with info about same histograms
		final TreeMap<Double, ToneScaleHistogram> toneScaleCorrelations;
		toneScaleCorrelations = new TreeMap<Double, ToneScaleHistogram>();
		final TreeMap<Double, String> fileNameCorrelations = new TreeMap<Double, String>();

		for (final String file : inputFiles) {
			final AudioFile audioFile = new AudioFile(file);
			final PitchDetector pitchDetector;
			if (detector.equals("AUBIO")) {
				pitchDetector = new AubioPitchDetection(audioFile, PitchDetectionMode.AUBIO_YIN);
			} else {
				pitchDetector = new IPEMPitchDetection(audioFile, PitchDetectionMode.IPEM_SIX);
			}
			pitchDetector.executePitchDetection();
			final List<Sample> samples = pitchDetector.getSamples();
			final AmbitusHistogram ambitusHistogram = Sample.ambitusHistogram(samples);
			final ToneScaleHistogram toneScaleHistogram = ambitusHistogram.toneScaleHistogram();
			toneScaleHistogram.gaussianSmooth(1.0);
			final List<Peak> detectedPeaks = PeakDetector.detect(toneScaleHistogram, 10, 0.8);
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
