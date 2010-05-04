package be.hogent.tarsos.apps;

import java.io.IOException;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.List;

import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.UnsupportedAudioFileException;

import be.hogent.tarsos.pitch.AubioPitchDetection;
import be.hogent.tarsos.pitch.PitchDetectionMix;
import be.hogent.tarsos.pitch.PitchDetector;
import be.hogent.tarsos.pitch.Sample;
import be.hogent.tarsos.pitch.AubioPitchDetection.AubioPitchDetectionMode;
import be.hogent.tarsos.ui.PlotThread;
import be.hogent.tarsos.util.AudioFile;
import be.hogent.tarsos.util.FileUtils;
import be.hogent.tarsos.util.MediaPlayer;
import be.hogent.tarsos.util.StopWatch;


public class ShowMelody {

	private ShowMelody(){}

	public static void testTonalShiftWithinFile(String fileName){
		/*
		PitchDetector detector;
		detector = new IPEMPitchDetection(new AudioFile(fileName));
		detector.executePitchDetection();
		List<Sample> samples =  detector.getSamples();

		//Sample.printRangeInformation(detector.getName() + "_" + FileUtils.basename(fileName) + "_range.txt", samples);
		//Sample.printOctaveInformation(detector.getName() + "_" + FileUtils.basename(fileName) + "_octave.txt", samples);

	 	//Histogram firstQuarterFrequencyTable = Sample.printOctaveInformation(detector.getName() + "_" + FileUtils.basename(fileName) + "quarter_01_octave.txt", samples.subList(0,samples.size()/2));
		//Histogram fourthQuarterFrequencyTable = Sample.printOctaveInformation(detector.getName() + "_" + FileUtils.basename(fileName) + "quarter_04_octave.txt", samples.subList(samples.size()/2,samples.size()));

		firstQuarterFrequencyTable = firstQuarterFrequencyTable.normalize();
		fourthQuarterFrequencyTable = fourthQuarterFrequencyTable.normalize();

		SimplePlot plot = new SimplePlot(FileUtils.basename(fileName));
		plot.addData(0,firstQuarterFrequencyTable);
		plot.addData(1,fourthQuarterFrequencyTable);

		int displacement = firstQuarterFrequencyTable.displacementForOptimalCorrelation(fourthQuarterFrequencyTable);
		double displacementInCents = displacement * firstQuarterFrequencyTable.getClassWidth();
		double correlation = firstQuarterFrequencyTable.correlationWithDisplacement(displacement, fourthQuarterFrequencyTable);
		System.out.println(FileUtils.basename(fileName)+ "Max correlation:\t" + correlation + " displacement:\t" + displacementInCents);

		plot.save();
		*/
	}

	public static void findBestTonalShift(List<String> fileNames){
		/*
		Histogram bestFirst = null;
		Histogram bestSecond = null;
		double  highestCorrelation = 0;
		double bestDisplacementInCents = 0;
		String bestFileName = "";
		for(String fileName: fileNames){
			PitchDetector detector;
			detector = new IPEMPitchDetection(new AudioFile(fileName));
			detector.executePitchDetection();
			List<Sample> samples =  detector.getSamples();

			Histogram firstHalf = Sample.printOctaveInformation(detector.getName() + "_" + FileUtils.basename(fileName) + "01_octave.txt", samples.subList(0,samples.size()/2));
			Histogram secondHalf = Sample.printOctaveInformation(detector.getName() + "_" + FileUtils.basename(fileName) + "02_octave.txt", samples.subList(samples.size()/2,samples.size()));

			firstHalf = firstHalf.normalize();
			secondHalf = secondHalf.normalize();

			int displacement = firstHalf.displacementForOptimalCorrelation(secondHalf);
			double displacementInCents = displacement * firstHalf.getClassWidth();
			double correlation = firstHalf.correlationWithDisplacement(displacement, secondHalf);

			//bigger shift than quart tone
			if(correlation > highestCorrelation && displacementInCents > 25){
				bestFirst = firstHalf;
				bestSecond = secondHalf;
				bestDisplacementInCents = displacementInCents;
				bestFileName = FileUtils.basename(fileName);
				highestCorrelation = correlation;
			}

			System.out.println(FileUtils.basename(fileName)+ "\tMax correlation:\t" + correlation + "\tdisplacement:\t" + displacementInCents);

		}

		SimplePlot plot = new SimplePlot(FileUtils.basename("best_tonal_shift" + bestFileName ));
		plot.addData(0,bestFirst);
		int displacement = (int) (bestDisplacementInCents/bestFirst.getClassWidth());
		plot.addData(1,bestSecond,displacement);

		plot.save();

		System.out.println("Max correlation:\t" + highestCorrelation + " displacement:\t" + bestDisplacementInCents);
		*/
	}

	public static void findBestMatch(List<String> fileNames){
		/*
		Histogram bestFirst = null;
		Histogram bestSecond = null;
		double  highestCorrelation = 0;
		double bestDisplacementInCents = 0;
		String bestFileName = "";
		for(int i = 0; i < fileNames.size();i++){
			for(int j = 0; j<fileNames.size();j++){
				if(i!=j){
					PitchDetector detector;
					List<PitchDetector> detectors;

					detectors = new ArrayList<PitchDetector>();
					detectors.add(new AubioPitchDetection(new AudioFile(fileNames.get(i)), AubioPitchDetectionMode.YIN));
					detectors.add(new IPEMPitchDetection(new AudioFile(fileNames.get(i))));
					detector = new PitchDetectionMix(detectors, 0.01);
					detector.executePitchDetection();

					List<Sample> samplesI =  detector.getSamples();


					detectors = new ArrayList<PitchDetector>();
					detectors.add(new AubioPitchDetection(new AudioFile(fileNames.get(j)), AubioPitchDetectionMode.YIN));
					detectors.add(new IPEMPitchDetection(new AudioFile(fileNames.get(j))));
					detector = new PitchDetectionMix(detectors, 0.01);
					detector.executePitchDetection();
					List<Sample> samplesJ =  detector.getSamples();

					Histogram histogramI = Sample.printOctaveInformation(detector.getName() + "_" + FileUtils.basename(fileNames.get(i)) + "_octave.txt",samplesI);

					Histogram histogramJ = Sample.printOctaveInformation(detector.getName() + "_" + FileUtils.basename(fileNames.get(j)) + "_octave.txt",samplesJ);

					histogramI = histogramI.normalize().gaussianSmooth(1.0);
					histogramJ = histogramJ.normalize().gaussianSmooth(1.0);

					int displacement = histogramI.displacementForOptimalCorrelation(histogramJ);
					double displacementInCents = displacement * histogramI.getClassWidth();
					double correlation = histogramI.correlationWithDisplacement(displacement, histogramJ);


					if(correlation > highestCorrelation){
						bestFirst = histogramI;
						bestSecond = histogramJ;
						bestDisplacementInCents = displacementInCents;
						bestFileName = FileUtils.basename(fileNames.get(i)) + "_" + FileUtils.basename(fileNames.get(j));
						highestCorrelation = correlation;
					}
					System.out.println(  fileNames.get(i) + "_" + fileNames.get(j) + "\tMax correlation:\t" + correlation + "\tdisplacement:\t" + displacementInCents);
				}
			}
		}

		SimplePlot plot = new SimplePlot("best_correlation_shift" + bestFileName);
		plot.addData(0,bestFirst);
		int displacement = (int) (bestDisplacementInCents/bestFirst.getClassWidth());
		plot.addData(1,bestSecond,displacement);

		plot.save();

		System.out.println("Max correlation:\t" + highestCorrelation + " displacement:\t" + bestDisplacementInCents);
		*/
	}

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		/*
		for(AudioFile file : AudioFile.audioFiles("formats")){
			PitchDetector detector;
			detector = new AubioPitchDetection(file,AubioPitchDetectionMode.YIN);
			detector.executePitchDetection();
			List<Sample> samples =  detector.getSamples();
			String transcodedBasename = FileUtils.basename(file.transcodedPath());
			FileUtils.mkdirs(FileUtils.combine("data","range",transcodedBasename));
			Sample.printRangeInformation(transcodedBasename  + '/' + detector.getName() + "_range.txt", samples);
			Sample.printOctaveInformation(transcodedBasename + '/' + detector.getName() +  "_octave.txt", samples);

			detector = new IPEMPitchDetection(file);
			detector.executePitchDetection();
			samples =  detector.getSamples();
			transcodedBasename = FileUtils.basename(file.transcodedPath());
			FileUtils.mkdirs(FileUtils.combine("data","range",transcodedBasename));
			Sample.printRangeInformation(transcodedBasename  + '/' + detector.getName() + "_range.txt", samples);
			Sample.printOctaveInformation(transcodedBasename + '/' + detector.getName() +  "_octave.txt", samples);
		}
		*/
	}

	public static void bestDetector(String fileName){
		/*
		List<PitchDetector> detectors;
		detectors = new ArrayList<PitchDetector>();
		detectors.add(new AubioPitchDetection(new AudioFile(fileName), AubioPitchDetectionMode.YIN));
		detectors.add(new IPEMPitchDetection(new AudioFile(fileName)));
		detectors.add(new AubioPitchDetection(new AudioFile(fileName), AubioPitchDetectionMode.MCOMB));
		detectors.add(new AubioPitchDetection(new AudioFile(fileName), AubioPitchDetectionMode.YINFFT));
		detectors.add(new PitchDetectionMix(new ArrayList<PitchDetector>(detectors.subList(0,2)), 0.01));
		detectors.add(new PitchDetectionMix(new ArrayList<PitchDetector>(detectors.subList(1,3)), 0.01));
		detectors.add(new PitchDetectionMix(new ArrayList<PitchDetector>(detectors.subList(2,4)), 0.01));

		List<Histogram> histograms = new ArrayList<Histogram>();
		for(PitchDetector d : detectors){
			d.executePitchDetection();
			Histogram histogram = Sample.printOctaveInformation(d.getName() + "_" + FileUtils.basename(fileName) + "_octave.txt",d.getSamples());
			histograms.add(histogram.normalize());
		}

		for(int i = 0;i<histograms.size();i++){
			for(int j = i + 1;j<histograms.size();j++){
				double correlation = histograms.get(i).correlationWithDisplacement(0, histograms.get(j));
				System.out.println(FileUtils.basename(fileName) + ";" + detectors.get(i).getName()+ ";" + detectors.get(j).getName() + ";" + correlation + ";"  + histograms.get(i).getEntropy()+ ";" + histograms.get(j).getEntropy());
			}
		}
		*/
	}

	public static void showMelody(String fileName){
		PitchDetector detector;
		List<PitchDetector> list = new ArrayList<PitchDetector>();
		list.add(new AubioPitchDetection(new AudioFile(fileName), AubioPitchDetectionMode.SCHMITT));
		list.add(new AubioPitchDetection(new AudioFile(fileName), AubioPitchDetectionMode.FCOMB));
		detector = new PitchDetectionMix(list,0.05);
		detector.executePitchDetection();
		List<Sample> samples =  detector.getSamples();
		try {
			MediaPlayer m = new MediaPlayer(fileName);
			StopWatch watch = new StopWatch();
			PlotThread thread = new PlotThread(FileUtils.basename(fileName), samples, watch);
			m.start();
			thread.start();
		} catch (LineUnavailableException e) {
			e.printStackTrace();
		} catch (MalformedURLException e) {
			e.printStackTrace();
		} catch (UnsupportedAudioFileException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}


}
