package be.hogent.tarsos.peak;

import java.util.List;

import be.hogent.tarsos.util.AudioFile;


public class PeakExtractor {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		String[] globDirectories = {"dekkmma_random","makam","maghreb"};
		List<AudioFile> files = AudioFile.audioFiles(globDirectories);
		
 		System.out.println(files.size());
		
		/*
		double[] gaussians = {0.5,0.5,1.0};
		int[] windowsize = {5,10,15,20};
		double[] threshold = {0.5,0.8,1.0,1.5};		
		
		FileUtils.writeFile("file;windowsize;threshold;gaussian smoothing factor;number of peaks;peakx;heightx\n","peaks.csv");
		
		for(AudioFile files: AudioFile){
					
			String baseName = FileUtils.basename(fileName);
			System.out.println(baseName);
			FileUtils.mkdirs("data/octave/" + baseName);
			FileUtils.mkdirs("data/range/" + baseName);
			
			List<Sample> samples;
			List<PitchDetector> detectors = new ArrayList<PitchDetector>();
	
			detectors.add(new AubioPitchDetection(fileName, AubioPitchDetectionMode.YIN));
			detectors.add(new IPEMPitchDetection(fileName));
			
			PitchDetector mix = new PitchDetectionMix(new ArrayList<PitchDetector>(detectors), 0.02);
			detectors.add(mix);
			
			for(PitchDetector detector:detectors){
				detector.executePitchDetection();
				samples = detector.getSamples();
				for(int i = 0 ; i < gaussians.length ; i ++){
					Histogram octaveHistogram = Sample.printOctaveInformation(baseName + '/' + baseName + "_" + detector.getName() +  "_octave.txt", samples);
					Sample.printRangeInformation(baseName + '/' + baseName + "_" + detector.getName() +  "_range.txt", samples);
					octaveHistogram.gaussianSmooth(gaussians[i]);
					for(int j = 0 ; j < windowsize.length ; j ++)
						for(int k = 0 ; k < threshold.length ; k ++){
							FileUtils.appendFile(baseName + ";" + threshold[k] + ";"+ windowsize[j] +";"+gaussians[i] +";","peaks.csv");
							Histogram peaks = PeakDetector.newPeakDetection(octaveHistogram, windowsize[j], threshold[k]);
							SimplePlot p = new SimplePlot(baseName + "_peaks_" + gaussians[i] + "_" + windowsize[j] + "_" +  threshold[k] );
							p.addData(0,octaveHistogram);
							p.addData(1,peaks);
							p.save();
						}
				}
				samples.clear();
			}
		}
		*/
	}
}
