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

package be.tarsos.cli;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.sound.sampled.UnsupportedAudioFileException;

import joptsimple.OptionParser;
import joptsimple.OptionSet;
import joptsimple.OptionSpec;
import be.tarsos.sampled.pitch.Annotation;
import be.tarsos.sampled.pitch.PitchDetectionMode;
import be.tarsos.sampled.pitch.PitchDetector;
import be.tarsos.transcoder.ffmpeg.EncoderException;
import be.tarsos.util.AudioFile;
import be.tarsos.util.ConfKey;
import be.tarsos.util.Configuration;
import be.tarsos.util.FileUtils;
import be.tarsos.util.SignalPowerExtractor;
import be.tarsos.util.SimplePlot;
import be.tarsos.util.histogram.Histogram;
import be.tarsos.util.histogram.HistogramFactory;
import be.tarsos.util.histogram.PitchClassHistogram;
import be.tarsos.util.histogram.PitchHistogram;
import be.tarsos.util.histogram.peaks.Peak;
import be.tarsos.util.histogram.peaks.PeakDetector;

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
	 * @throws UnsupportedAudioFileException
	 * @throws EncoderException
	 */
	private void annotateInputFile(final String inputFile, final PitchDetectionMode detectionMode)
			throws UnsupportedAudioFileException, EncoderException {

		final AudioFile audioFile = new AudioFile(inputFile);

		final PitchDetector pitchDetector = detectionMode.getPitchDetector(audioFile);

		pitchDetector.executePitchDetection();
		final String baseName = audioFile.originalBasename();
		final String directory = FileUtils.combine("annotations", baseName);
		FileUtils.mkdirs(directory);

		final String prefix = baseName + "_" + pitchDetector.getName();

		final List<Annotation> samples = pitchDetector.getAnnotations();
		final PitchHistogram pitchHistogram = HistogramFactory.createPitchHistogram(samples);
		final String ambitusTXT = FileUtils.combine(directory, prefix + "_ambitus.txt");
		final String ambitusPNG = FileUtils.combine(directory, prefix + "_ambitus.png");
		final String toneScaleColor = prefix + "_tone_scale_colored.png";
		pitchHistogram.plotToneScaleHistogram(FileUtils.combine(directory, toneScaleColor), true);
		pitchHistogram.export(ambitusTXT);
		pitchHistogram.plot(ambitusPNG, "Ambitus " + baseName + " " + pitchDetector.getName());
		final PitchClassHistogram toneScaleHisto = pitchHistogram.pitchClassHistogram();
		final String toneScaleTxt = FileUtils.combine(directory, prefix + "_tone_scale.txt");
		final String toneScalePNG = FileUtils.combine(directory, prefix + "_tone_scale.png");
		toneScaleHisto.export(toneScaleTxt);
		toneScaleHisto.plot(toneScalePNG, "Tone scale " + baseName + " " + pitchDetector.getName());

		toneScaleHisto.gaussianSmooth(1.0);
		final List<Peak> peaks = PeakDetector.detect(toneScaleHisto, 15,15);
		final Histogram peakHistogram = PeakDetector.newPeakDetection(peaks);
		final String peaksTitle = prefix + "_peaks_" + 1.0 + "_" + 15 + "_" + 0.8;
		final SimplePlot plot = new SimplePlot(peaksTitle);
		plot.addData(0, toneScaleHisto);
		plot.addData(1, peakHistogram);
		plot.save(FileUtils.combine(directory, peaksTitle + ".png"));
		PitchClassHistogram.exportPeaksToScalaFileFormat(FileUtils.combine(directory, peaksTitle + ".scl"),
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

	@Override
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
				Collections.reverse(inputFiles);
				for(int i = 0; i < 1000; i++){
					inputFiles.remove(0);
				}
				for (final String file : inputFiles) {
					try {
						annotateInputFile(file, detectionMode);
					} catch (EncoderException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					} catch (UnsupportedAudioFileException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
			}
		}
	}

	@Override
	public String description() {
		return "Annotate can be used to annotate audio files. It transcodes "
				+ "audio to an understandable format, detects pitch and stores information about the files. "
				+ "It uses the defined files with the in "
				+ "option or all the audiofiles in the audio directory.";
	}
}
