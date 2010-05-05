package be.hogent.tarsos.apps;

import gnu.getopt.Getopt;
import gnu.getopt.LongOpt;

import java.util.List;

import be.hogent.tarsos.pitch.AubioPitchDetection;
import be.hogent.tarsos.pitch.IPEMPitchDetection;
import be.hogent.tarsos.pitch.PitchDetector;
import be.hogent.tarsos.pitch.Sample;
import be.hogent.tarsos.pitch.YinPitchDetection;
import be.hogent.tarsos.pitch.AubioPitchDetection.AubioPitchDetectionMode;
import be.hogent.tarsos.util.AudioFile;
import be.hogent.tarsos.util.ConfKey;
import be.hogent.tarsos.util.Configuration;
import be.hogent.tarsos.util.FileUtils;
import be.hogent.tarsos.util.SignalPowerExtractor;
import be.hogent.tarsos.util.SimplePlot;
import be.hogent.tarsos.util.histogram.AmbitusHistogram;
import be.hogent.tarsos.util.histogram.Histogram;
import be.hogent.tarsos.util.histogram.ToneScaleHistogram;
import be.hogent.tarsos.util.histogram.peaks.Peak;
import be.hogent.tarsos.util.histogram.peaks.PeakDetector;

/**
 * Annotates an audio file with everything Tarsos is capable off Pitch
 * information, ambitus, tone scale peaks, waveform, scala files...
 * 
 * @author Joren Six
 */
public class Annotate {

	private Annotate() {
	}

	public static void main(String... args) {
		LongOpt[] longopts = new LongOpt[3];
		longopts[0] = new LongOpt("in", LongOpt.REQUIRED_ARGUMENT, null, 'i');
		longopts[1] = new LongOpt("detector", LongOpt.REQUIRED_ARGUMENT, null, 'd');
		longopts[2] = new LongOpt("help", LongOpt.NO_ARGUMENT, null, 'h');

		Getopt g = new Getopt("annotate", args, "-i:d:h", longopts);

		String inputFile = null;
		String detector = "TARSOS";

		int c;
		while ((c = g.getopt()) != -1) {
			String arg = g.getOptarg();
			switch (c) {
			case 'i':
				inputFile = arg;
				break;
			case 'd':
				detector = arg.toUpperCase();
				break;
			case 'h':
				printHelp();
				return;
			}
		}

		if (inputFile != null && !FileUtils.exists(inputFile)) {
			printHelp();
		} else if (inputFile == null) {
			String pattern = Configuration.get(ConfKey.audio_file_name_pattern);
			String globDirectory = FileUtils.combine(FileUtils.getRuntimePath(), "audio");
			List<String> inputFiles = FileUtils.glob(globDirectory, pattern);
			inputFiles.addAll(FileUtils.glob(globDirectory, pattern.toLowerCase()));
			for (String file : inputFiles) {
				annotateInputFile(file, detector);
			}
		} else {
			annotateInputFile(inputFile, detector);
		}
	}

	private static void printHelp() {
		System.out.println("Annotate can be used to annotate audio files. It transcodes audio to an understandable"
				+ "format, detects pitch and stores information about the files. It uses the defined files with the in "
				+ "option or all the audiofiles in the audio directory.");
		System.out.println("");
		System.out.println("java -jar annotate.jar [--in in.wav] [--detector AUBIO|IPEM]");
		System.exit(0);
	}

	private static void annotateInputFile(String inputFile, String detector) {
		Configuration.createRequiredDirectories();
		AudioFile audioFile = new AudioFile(inputFile);

		PitchDetector pitchDetector = new YinPitchDetection(audioFile);
		if (detector.equals("AUBIO"))
			pitchDetector = new AubioPitchDetection(audioFile, AubioPitchDetectionMode.YIN);
		else if (detector.equals("IPEM"))
			pitchDetector = new IPEMPitchDetection(audioFile);

		pitchDetector.executePitchDetection();
		String baseName = audioFile.basename();
		String directory = FileUtils.combine("annotations", baseName);
		FileUtils.mkdirs(directory);

		String prefix = baseName + "_" + pitchDetector.getName();

		List<Sample> samples = pitchDetector.getSamples();
		AmbitusHistogram ambitusHistogram = Sample.ambitusHistogram(samples);
		String ambitusTextFileName = FileUtils.combine(directory, prefix + "_ambitus.txt");
		String ambitusPNGFileName = FileUtils.combine(directory, prefix + "_ambitus.png");
		String coloredToneScalePNGFileName = prefix + "_tone_scale_colored.png";
		ambitusHistogram.plotToneScaleHistogram(FileUtils.combine(directory, coloredToneScalePNGFileName), true);
		ambitusHistogram.export(ambitusTextFileName);
		ambitusHistogram.plot(ambitusPNGFileName, "Ambitus " + baseName + " " + pitchDetector.getName());
		ToneScaleHistogram toneScaleHistogram = ambitusHistogram.toneScaleHistogram();
		String toneScaleTextFileName = FileUtils.combine(directory, prefix + "_tone_scale.txt");
		String toneScalePNGFileName = FileUtils.combine(directory, prefix + "_tone_scale.png");
		toneScaleHistogram.export(toneScaleTextFileName);
		toneScaleHistogram.plot(toneScalePNGFileName, "Tone scale " + baseName + " " + pitchDetector.getName());

		toneScaleHistogram.gaussianSmooth(1.0);
		List<Peak> peaks = PeakDetector.detect(toneScaleHistogram, 15, 0.8);
		Histogram peakHistogram = PeakDetector.newPeakDetection(peaks);
		String peaksTitle = prefix + "_peaks_" + 1.0 + "_" + 15 + "_" + 0.8;
		SimplePlot p = new SimplePlot(peaksTitle);
		p.addData(0, toneScaleHistogram);
		p.addData(1, peakHistogram);
		p.save(FileUtils.combine(directory, peaksTitle + ".png"));
		ToneScaleHistogram.exportPeaksToScalaFileFormat(FileUtils.combine(directory, peaksTitle + ".scl"), peaksTitle, peaks);

		SignalPowerExtractor powerExtractor = new SignalPowerExtractor(audioFile);
		powerExtractor.saveTextFile(FileUtils.combine(directory, prefix + "_power.txt"));
		powerExtractor.saveWaveFormPlot(FileUtils.combine(directory, prefix + "_wave.png"));
	}

}
