package be.hogent.tarsos.cli;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import joptsimple.OptionParser;
import joptsimple.OptionSet;
import joptsimple.OptionSpec;
import be.hogent.tarsos.sampled.pitch.PitchDetectionMode;
import be.hogent.tarsos.sampled.pitch.PitchDetector;
import be.hogent.tarsos.sampled.pitch.Sample;
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
public final class Annotate extends AbstractTarsosApp {

	private static final Logger LOG = Logger.getLogger(AbstractTarsosApp.class.getName());

	/**
	 * Annotates an input file.
	 * 
	 * @param inputFile
	 *            The file to annotate.
	 * @param detector
	 *            The detector to use.
	 */
	private void annotateInputFile(final String inputFile, final PitchDetectionMode detectionMode) {

		final AudioFile audioFile = new AudioFile(inputFile);

		final PitchDetector pitchDetector = detectionMode.getPitchDetector(audioFile);

		pitchDetector.executePitchDetection();
		final String baseName = audioFile.basename();
		final String directory = FileUtils.combine("annotations", baseName);
		FileUtils.mkdirs(directory);

		final String prefix = baseName + "_" + pitchDetector.getName();

		final List<Sample> samples = pitchDetector.getSamples();
		final AmbitusHistogram ambitusHistogram = Sample.ambitusHistogram(samples);
		final String ambitusTXT = FileUtils.combine(directory, prefix + "_ambitus.txt");
		final String ambitusPNG = FileUtils.combine(directory, prefix + "_ambitus.png");
		final String toneScaleColor = prefix + "_tone_scale_colored.png";
		ambitusHistogram.plotToneScaleHistogram(FileUtils.combine(directory, toneScaleColor), true);
		ambitusHistogram.export(ambitusTXT);
		ambitusHistogram.plot(ambitusPNG, "Ambitus " + baseName + " " + pitchDetector.getName());
		final ToneScaleHistogram toneScaleHisto = ambitusHistogram.toneScaleHistogram();
		final String toneScaleTxt = FileUtils.combine(directory, prefix + "_tone_scale.txt");
		final String toneScalePNG = FileUtils.combine(directory, prefix + "_tone_scale.png");
		toneScaleHisto.export(toneScaleTxt);
		toneScaleHisto.plot(toneScalePNG, "Tone scale " + baseName + " " + pitchDetector.getName());

		toneScaleHisto.gaussianSmooth(1.0);
		final List<Peak> peaks = PeakDetector.detect(toneScaleHisto, 15, 0.8);
		final Histogram peakHistogram = PeakDetector.newPeakDetection(peaks);
		final String peaksTitle = prefix + "_peaks_" + 1.0 + "_" + 15 + "_" + 0.8;
		final SimplePlot plot = new SimplePlot(peaksTitle);
		plot.addData(0, toneScaleHisto);
		plot.addData(1, peakHistogram);
		plot.save(FileUtils.combine(directory, peaksTitle + ".png"));
		ToneScaleHistogram.exportPeaksToScalaFileFormat(FileUtils.combine(directory, peaksTitle + ".scl"),
				peaksTitle, peaks);

		try {
			final SignalPowerExtractor powerExtractor = new SignalPowerExtractor(audioFile);
			powerExtractor.saveTextFile(FileUtils.combine(directory, prefix + "_power.txt"), true);
			// powerExtractor.saveWaveFormPlot(FileUtils.combine(directory,
			// prefix + "_wave.png"));
		} catch (final ArrayIndexOutOfBoundsException e) {
			LOG.log(Level.SEVERE, "Index out of bounds while extracting power.", e);
		}
	}

	
	public void run(final String... args) {

		final OptionParser parser = new OptionParser();

		final OptionSpec<PitchDetectionMode> detectionModeSpec = createDetectionModeSpec(parser);

		final OptionSet options = parse(args, parser, this);

		final PitchDetectionMode detectionMode = options.valueOf(detectionModeSpec);

		if (isHelpOptionSet(options)) {
			printHelp(parser);
		} else {
			final String audioPattern = Configuration.get(ConfKey.audio_file_name_pattern);
			final List<String> inputFiles = new ArrayList<String>();
			for (final String inputFile : options.nonOptionArguments()) {
				if (FileUtils.isDirectory(inputFile)) {
					final List<String> globbedFiles = FileUtils.glob(inputFile, audioPattern, false);
					inputFiles.addAll(globbedFiles);
				} else if (inputFile.matches(audioPattern)) {
					inputFiles.add(inputFile);
				}
				for (final String file : inputFiles) {
					annotateInputFile(file, detectionMode);
				}
			}
		}
	}

	
	public String description() {
		return "Annotate can be used to annotate audio files. It transcodes "
				+ "audio to an understandable format, detects pitch and stores information about the files. "
				+ "It uses the defined files with the in "
				+ "option or all the audiofiles in the audio directory.";
	}

	
	public String name() {
		return "annotate";
	}

}
