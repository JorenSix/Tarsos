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

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.TreeMap;

import joptsimple.OptionParser;
import joptsimple.OptionSet;
import joptsimple.OptionSpec;
import be.tarsos.Tarsos;
import be.tarsos.sampled.pitch.Annotation;
import be.tarsos.sampled.pitch.PitchDetectionMode;
import be.tarsos.sampled.pitch.PitchDetector;
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

/**
 * Ranks a list of audio files on tone scale similarity with an input file
 * (scala or audio). The audio file with the most similar tone scale (using
 * histogram correlation) is listed first.
 * 
 * @author Joren Six
 */
public final class Rank extends AbstractTarsosApp {

	@Override
	public String description() {
		return "Ranks a list of audio files on tone scale similarity "
				+ "with an input file (scala or audio). The audio file with the "
				+ "most similar tone scale (using histogram correlation) is listed first.";
	}

	@Override
	public void run(final String... args) {

		final OptionParser parser = new OptionParser();
		final OptionSpec<File> needleSpec = parser
				.accepts("needle", "A scala or audio file that defines the tone scale to look for.")
				.withRequiredArg().ofType(File.class);

		final OptionSpec<File> haystackSpec = parser
				.accepts(
						"haystack",
						"A list of audio or scala files to match needle with. If one of the files is a"
								+ " directory it is traversed recursively.").withRequiredArg()
				.ofType(File.class).withValuesSeparatedBy(' ');

		final OptionSpec<PitchDetectionMode> detectionModeSpec = createDetectionModeSpec(parser);

		final OptionSet options = parse(args, parser, this);

		if (isHelpOptionSet(options) || !options.has(needleSpec) || !options.has(haystackSpec)) {
			printHelp(parser);
		} else {
			final File needleFile = options.valueOf(needleSpec);
			final List<File> hayStack = new ArrayList<File>();
			for (final File hay : options.valuesOf(haystackSpec)) {
				if (hay.isDirectory()) {
					iterateDirectory(hay, hayStack);
				} else {
					if(hay.getName().endsWith("txt")){
						String[] data = FileUtils.readFile(hay.getAbsolutePath()).split("\n");
						for(String csvFile:data){
							try{
								hayStack.add(new File(csvFile));
							}catch(Exception e){
								e.printStackTrace();
							}
						}
					}else{
						hayStack.add(hay);	
					}
					
				}
			}
			for (final String nonArgumentOption : options.nonOptionArguments()) {
				final File hay = new File(nonArgumentOption);
				if (hay.isDirectory()) {
					iterateDirectory(hay, hayStack);
				} else {
					hayStack.add(hay);
				}
			}
			final PitchDetectionMode detectionMode = options.valueOf(detectionModeSpec);

			final PitchClassHistogram needleHisto = createHisto(needleFile, detectionMode);

			final TreeMap<Double, String> tree = new TreeMap<Double, String>();

			for (final File hay : hayStack) {
				System.out.println("Handling " + hay);
				final PitchClassHistogram hayHisto = createHisto(hay, detectionMode);
				//final int displacement = needleHisto.displacementForOptimalCorrelation(hayHisto);
				final Double correlation = needleHisto.correlationWithDisplacement(0, hayHisto);
				//final String plotFileName = hay.getName() + "_" + needleFile.getName() + ".png";
				//final String title = correlation.toString();
				//needleHisto.plotCorrelation(hayHisto, CorrelationMeasure.INTERSECTION, plotFileName, title);
				tree.put(correlation, hay.getName());
			}

			int max = 100;
			int count =0;
			ArrayList<Double> keys = new ArrayList<Double>(tree.keySet());
			Collections.reverse(keys);
			for (final Double correlation : keys) {
				if(count < max){
					Tarsos.println(count +";" + correlation + ";" + tree.get(correlation));
				}
				count++;
			}
		}
	}

	private void iterateDirectory(final File file, final List<File> files) {
		if (file.isDirectory()) {
			for (final String child : file.list()) {
				iterateDirectory(new File(file, child), files);
			}
		} else {
			final String path = file.getAbsolutePath();
			final String extension = FileUtils.extension(path);
			final String audioFilePattern = Configuration.get(ConfKey.audio_file_name_pattern);
			final boolean isScala = extension.equalsIgnoreCase("scl");
			final boolean isAudio = !isScala && path.matches(audioFilePattern);
			if (isScala || isAudio) {
				files.add(file);
			}
		}
	}

	private PitchClassHistogram createHisto(final File file, final PitchDetectionMode detectionMode) {
		PitchClassHistogram histo = null;
		final String path = file.getAbsolutePath();
		final String extension = FileUtils.extension(path);
		if (extension.equalsIgnoreCase("scl")) {
			histo = HistogramFactory.createPitchClassHistogram(new ScalaFile(path));
		} else if (path.matches(Configuration.get(ConfKey.audio_file_name_pattern))) {
			AudioFile audioFile;
			try {
				audioFile = new AudioFile(path);
				final PitchDetector pitchDetector = detectionMode.getPitchDetector(audioFile);
				pitchDetector.executePitchDetection();
				final List<Annotation> samples = pitchDetector.getAnnotations();
				final PitchHistogram pitchHistogram = HistogramFactory.createPitchHistogram(samples);
				final List<Peak> peakList = PeakDetector.detect(pitchHistogram.pitchClassHistogram()
						.gaussianSmooth(0.8), 15,15);
				final double[] peaks = new double[peakList.size()];
				for (int i = 0; i < peaks.length; i++) {
					peaks[i] = peakList.get(i).getPosition();
				}
				histo = PitchClassHistogram.createToneScale(peaks);
				return histo;
			} catch (EncoderException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

		} else {
			throw new IllegalArgumentException("Tone scale creation failed: " + path
					+ " should be a scala or audio file!");
		}
		return histo;
	}

}
