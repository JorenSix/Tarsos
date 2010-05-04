package be.hogent.tarsos.apps;

import gnu.getopt.Getopt;
import gnu.getopt.LongOpt;

import java.util.ArrayList;
import java.util.List;
import java.util.TreeMap;

import be.hogent.tarsos.pitch.AubioPitchDetection;
import be.hogent.tarsos.pitch.IPEMPitchDetection;
import be.hogent.tarsos.pitch.PitchDetector;
import be.hogent.tarsos.pitch.Sample;
import be.hogent.tarsos.pitch.AubioPitchDetection.AubioPitchDetectionMode;
import be.hogent.tarsos.util.AudioFile;
import be.hogent.tarsos.util.ConfKey;
import be.hogent.tarsos.util.Configuration;
import be.hogent.tarsos.util.FileUtils;
import be.hogent.tarsos.util.FileUtils.RowFilter;
import be.hogent.tarsos.util.histogram.AmbitusHistogram;
import be.hogent.tarsos.util.histogram.CorrelationMeasure;
import be.hogent.tarsos.util.histogram.ToneScaleHistogram;
import be.hogent.tarsos.util.histogram.peaks.Peak;
import be.hogent.tarsos.util.histogram.peaks.PeakDetector;

public class ToneScaleMatcher {

	private ToneScaleMatcher(){}

	public static void main(String... args){
		LongOpt[] longopts = new LongOpt[3];
		longopts[0] = new LongOpt("in", LongOpt.REQUIRED_ARGUMENT, null, 'i');
		longopts[1] = new LongOpt("detector", LongOpt.REQUIRED_ARGUMENT, null, 'd');
		longopts[2] = new LongOpt("help", LongOpt.NO_ARGUMENT, null, 'h');
		Getopt g = new Getopt("tonescalematcher", args, "-i:h:d", longopts);

		String detector = "IPEM";
		String inputFile = null;
		int c;
		while ((c = g.getopt()) != -1) {
			String arg = g.getOptarg();
			switch(c) {
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
			}
		}

		if(inputFile==null || !FileUtils.exists(inputFile)){
			printHelp();
			System.exit(-1);
		}

		List<String[]> rows = FileUtils.readCSVFile(inputFile, " ", -1);
		List<Double> toneScaleTones = new ArrayList<Double>();
		List<String> pitches = FileUtils.readColumnFromCSVData(rows, 0, new RowFilter(){
			@Override
			public boolean acceptRow(String[] row) {
				return ! row[0].contains("!") && row[0].matches("((-|\\+)?[0-9]+(\\.[0-9]+)?)+");
			}});
		for(String pitch : pitches){
			toneScaleTones.add(Double.parseDouble(pitch));
		}

		double[] peaks = new double[toneScaleTones.size()];
		for(int i = 0 ; i < toneScaleTones.size() ; i++){
			peaks[i] = toneScaleTones.get(i);
		}
		ToneScaleHistogram needleToneScale = ToneScaleHistogram.createToneScale(peaks, null, null, null);

		String pattern = Configuration.get(ConfKey.audio_file_name_pattern);
		String globDirectory = FileUtils.combine(FileUtils.getRuntimePath(),"audio");
		List<String> inputFiles = FileUtils.glob(globDirectory, pattern);
		//two priority queues with info about same histograms
		TreeMap<Double,ToneScaleHistogram> toneScaleCorrelations = new TreeMap<Double, ToneScaleHistogram>();
		TreeMap<Double,String> fileNameCorrelations = new TreeMap<Double, String>();

		for(String file:inputFiles){
			AudioFile audioFile = new AudioFile(file);
			PitchDetector pitchDetector = detector.equals("AUBIO") ?
					new AubioPitchDetection(audioFile, AubioPitchDetectionMode.YIN) :
					new IPEMPitchDetection(audioFile);
			pitchDetector.executePitchDetection();

			List<Sample> samples = pitchDetector.getSamples();
			AmbitusHistogram ambitusHistogram = Sample.ambitusHistogram(samples);
			ToneScaleHistogram toneScaleHistogram = ambitusHistogram.toneScaleHistogram();
			toneScaleHistogram.gaussianSmooth(1.0);
			List<Peak> detectedPeaks = PeakDetector.detect(toneScaleHistogram, 10, 0.8);
			peaks = new double[detectedPeaks.size()];
			for(int i = 0 ; i < detectedPeaks.size() ; i++){
				peaks[i] = detectedPeaks.get(i).getPosition();
			}
			ToneScaleHistogram hayStackHistogram = ToneScaleHistogram.createToneScale(peaks, null, null, null);

			int displacementForOptimalCorrelation = needleToneScale.displacementForOptimalCorrelation(hayStackHistogram);
			Double correlation = needleToneScale.correlationWithDisplacement(displacementForOptimalCorrelation, hayStackHistogram);

			toneScaleCorrelations.put(correlation,hayStackHistogram);
			fileNameCorrelations.put(correlation,audioFile.basename());
		}

		//print all correlations in descending order
		//best match first
		System.out.println("correlation\tfile");
		for(Double key : toneScaleCorrelations.descendingKeySet()){
			System.out.println(key+"\t"+fileNameCorrelations.get(key));
		}

		//plot best correlation
		if(toneScaleCorrelations.size()>0){
			double bestCorrelation = toneScaleCorrelations.descendingKeySet().first();
			ToneScaleHistogram hayStackHistogram = toneScaleCorrelations.get(bestCorrelation);
			needleToneScale.plotCorrelation(hayStackHistogram,CorrelationMeasure.INTERSECTION);
		}
	}

	private static void printHelp() {
		System.out.println("");
		System.out.println("Find a file in the audio directory with the best match for the defined tone scale.");
		System.out.println("");
		System.out.println("-----------------------");
		System.out.println("");
		System.out.println("java -jar tonescalematcher.jar [--in in.slc] [--help]");
		System.out.println("");
		System.out.println("-----------------------");
		System.out.println("");
		System.out.println("--in in.scl\t\tThe scala file with the tone scale.");
		System.out.println("--detector AUBIO|IPEM the pitch detector.");
		System.out.println("--help\t\tPrints this information");
		System.out.println("");
	}
}
