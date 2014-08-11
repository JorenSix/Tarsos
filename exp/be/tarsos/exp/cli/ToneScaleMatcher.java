/*
*              _______                      
*             |__   __|                     
*                | | __ _ _ __ ___  ___  ___
*                | |/ _` | '__/ __|/ _ \/ __| 
*                | | (_| | |  \__ \ (_) \__ \    
*                |_|\__,_|_|  |___/\___/|___/    
*                                                         
* -----------------------------------------------------------
*
*  Tarsos is developed by Joren Six at 
*  The School of Arts,
*  University College Ghent,
*  Hoogpoort 64, 9000 Ghent - Belgium
*  
* -----------------------------------------------------------
*
*  Info: http://tarsos.0110.be
*  Github: https://github.com/JorenSix/Tarsos
*  Releases: http://tarsos.0110.be/releases/Tarsos/
*  
*  Tarsos includes some source code by various authors,
*  for credits and info, see README.
* 
*/

package be.tarsos.exp.cli;

import java.util.List;
import java.util.TreeMap;

import be.tarsos.Tarsos;
import be.tarsos.sampled.pitch.Annotation;
import be.tarsos.sampled.pitch.IPEMPitchDetection;
import be.tarsos.sampled.pitch.PitchDetectionMode;
import be.tarsos.sampled.pitch.PitchDetector;
import be.tarsos.sampled.pitch.VampPitchDetection;
import be.tarsos.transcoder.ffmpeg.EncoderException;
import be.tarsos.util.AudioFile;
import be.tarsos.util.ConfKey;
import be.tarsos.util.Configuration;
import be.tarsos.util.FileUtils;
import be.tarsos.util.ScalaFile;
import be.tarsos.util.histogram.HistogramFactory;
import be.tarsos.util.histogram.PitchClassHistogram;
import be.tarsos.util.histogram.PitchHistogram;
import be.tarsos.util.histogram.peaks.Peak;
import be.tarsos.util.histogram.peaks.PeakDetector;

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
		final PitchClassHistogram needleToneScale = PitchClassHistogram
				.createToneScale(peaks, null, null, null);

		final String pattern = Configuration.get(ConfKey.audio_file_name_pattern);
		final String globDirectory = FileUtils.combine(FileUtils.runtimeDirectory(), "audio");
		final List<String> inputFiles = FileUtils.glob(globDirectory, pattern, false);
		// two priority queues with info about same histograms
		final TreeMap<Double, PitchClassHistogram> toneScaleCorrelations;
		toneScaleCorrelations = new TreeMap<Double, PitchClassHistogram>();
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
			final PitchHistogram pitchHistogram = HistogramFactory.createPitchHistogram(samples);
			final PitchClassHistogram pitchClassHistogram = pitchHistogram.pitchClassHistogram();
			pitchClassHistogram.gaussianSmooth(1.0);
			final List<Peak> detectedPeaks = PeakDetector.detect(pitchClassHistogram, 10,15);
			peaks = new double[detectedPeaks.size()];
			for (int i = 0; i < detectedPeaks.size(); i++) {
				peaks[i] = detectedPeaks.get(i).getPosition();
			}
			final PitchClassHistogram hayStackHistogram = PitchClassHistogram.createToneScale(peaks, null,
					null, null);

			final int displacementForOptimalCorrelation = needleToneScale
					.displacementForOptimalCorrelation(hayStackHistogram);
			final Double correlation = needleToneScale.correlationWithDisplacement(
					displacementForOptimalCorrelation, hayStackHistogram);

			toneScaleCorrelations.put(correlation, hayStackHistogram);
			fileNameCorrelations.put(correlation, audioFile.originalBasename());
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
		// final PitchClassHistogram hayStackHistogram =
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
