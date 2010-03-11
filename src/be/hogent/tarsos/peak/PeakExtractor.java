package be.hogent.tarsos.peak;

import java.util.ArrayList;
import java.util.List;

import be.hogent.tarsos.pitch.AubioPitchDetection;
import be.hogent.tarsos.pitch.IPEMPitchDetection;
import be.hogent.tarsos.pitch.PitchDetectionMix;
import be.hogent.tarsos.pitch.PitchDetector;
import be.hogent.tarsos.pitch.Sample;
import be.hogent.tarsos.pitch.AubioPitchDetection.AubioPitchDetectionMode;
import be.hogent.tarsos.util.AudioFile;
import be.hogent.tarsos.util.FileUtils;
import be.hogent.tarsos.util.histogram.Histogram;
import be.hogent.tarsos.util.histogram.ToneScaleHistogram;


public class PeakExtractor {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		String[] globDirectories = {"dekkmma_random","makam","maghreb"};
		List<AudioFile> files = AudioFile.audioFiles(globDirectories);
		
 		System.out.println(files.size());
		
		double[] gaussians = {0.5,0.5,1.0};
		int[] windowsize = {5,10,15,20};
		double[] threshold = {0.5,0.8,1.0,1.5};		
		
		FileUtils.writeFile("file;algo;windowsize;threshold;gaussian smoothing factor;number of peaks;peakx;heightx\n","peaks.csv");
		
		for(AudioFile file: files){
					
			String baseName = FileUtils.basename(file.basename());
			System.out.println(baseName);
			FileUtils.mkdirs("data/octave/" + baseName);
			FileUtils.mkdirs("data/range/" + baseName);
			
			List<Sample> samples;
			List<PitchDetector> detectors = new ArrayList<PitchDetector>();
	
			detectors.add(new AubioPitchDetection(file, AubioPitchDetectionMode.YIN));
			detectors.add(new IPEMPitchDetection(file));
			
			PitchDetector mix = new PitchDetectionMix(new ArrayList<PitchDetector>(detectors), 0.02);
			detectors.add(mix);
			
			for(PitchDetector detector:detectors){
				detector.executePitchDetection();
				samples = detector.getSamples();
				for(int i = 0 ; i < gaussians.length ; i ++){
					Histogram octaveHistogram = new ToneScaleHistogram();
					//Sample.printOctaveInformation(baseName + '/' + baseName + "_" + detector.getName() +  "_octave.txt", samples);
					//Sample.printRangeInformation(baseName + '/' + baseName + "_" + detector.getName() +  "_range.txt", samples);
					octaveHistogram.gaussianSmooth(gaussians[i]);
					for(int j = 0 ; j < windowsize.length ; j ++)
						for(int k = 0 ; k < threshold.length ; k ++){
							FileUtils.appendFile(baseName + ";" + detector.getName() + ";" + threshold[k] + ";"+ windowsize[j] +";"+gaussians[i] +";","peaks.csv");
							PeakDetector.newPeakDetection(octaveHistogram, windowsize[j], threshold[k]);
							//Histogram peaks =
							/*
							SimplePlot p = new SimplePlot(baseName + "_peaks_" + gaussians[i] + "_" + windowsize[j] + "_" +  threshold[k] );
							p.addData(0,octaveHistogram);
							p.addData(1,peaks);
							p.save();
							*/
						}
				}
				samples.clear();
			}
		}
	}
}
