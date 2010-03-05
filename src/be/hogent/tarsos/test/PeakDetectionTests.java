package be.hogent.tarsos.test;

import static org.junit.Assert.assertTrue;

import org.junit.Test;

import be.hogent.peak.DifferenceScore;
import be.hogent.peak.LocalHeightScore;
import be.hogent.peak.LocalVolumeScore;
import be.hogent.peak.PeakDetector;
import be.hogent.tarsos.pitch.PitchFunctions;
import be.hogent.tarsos.util.SimplePlot;
import be.hogent.tarsos.util.histogram.CorrelationMeasure;
import be.hogent.tarsos.util.histogram.Histogram;



public class PeakDetectionTests {
	
	public Histogram buildTable(){
		Histogram table = new Histogram(0.0,5.0,5);
		
		//1 values in bin 0
		table.add(0.5);
		
		//2 values in bin 1
		table.add(1.0);
		table.add(1.2);
		
		//1 value to bin 2
		table.add(2.3);
		
		//2 values to bin 3
		table.add(3.3);		
		table.add(3.4);
		
		//3 values to bin 4
		table.add(4.3);
		table.add(4.4);
		table.add(4.4);
		
		return table;
	}
	
	@Test
	public void testHistogramSanity(){
		//just to make sure the histogram works as expected
		Histogram histo = buildTable();
		
		assertTrue(histo.getNumberOfClasses() == 5);
		assertTrue(histo.getClassWidth() == 1.0); 
		
		assertTrue(histo.getCount(0.5) == histo.getCountForClass(0));
		assertTrue(histo.getCount(0.5) == 1);		
		assertTrue(histo.getCount(1.5) == histo.getCountForClass(1));
		assertTrue(histo.getCount(1.5) == 2);		
		assertTrue(histo.getCount(2.5) == histo.getCountForClass(2));
		assertTrue(histo.getCount(2.5) == 1);		
		assertTrue(histo.getCount(3.5) == histo.getCountForClass(3));
		assertTrue(histo.getCount(3.5) == 2);		
		assertTrue(histo.getCount(4.5) == histo.getCountForClass(4));
		assertTrue(histo.getCount(4.5) == 3);
		
		assertTrue(histo.getSumFreq() == 9);
	}
	
	@Test
	public void testNewPeakDetection(){
		Histogram octaveHistogram = PitchFunctions.readFrequencyTable("src/tarsos/test/data/african_octave_frequency_table.txt");
		octaveHistogram = octaveHistogram.gaussianSmooth(0.8);
		octaveHistogram = octaveHistogram.add(octaveHistogram).add(octaveHistogram);
		Histogram peakHistogram = PeakDetector.newPeakDetection(octaveHistogram, 10, 0.05);
		octaveHistogram.plotCorrelation(peakHistogram, CorrelationMeasure.INTERSECTION);
	}
	
	@Test
	public void testDifferenceScore(){
		Histogram histo = buildTable();
		DifferenceScore peakScore = new DifferenceScore(histo,1);		
		assertTrue(peakScore.score(histo, 0, 1)== 0.0);
		assertTrue(peakScore.score(histo, 1, 1)> 0.0);//local peak!
		assertTrue(peakScore.score(histo, 2, 1)== 0.0);
		assertTrue(peakScore.score(histo, 3, 1)== 0.0);
		assertTrue(peakScore.score(histo, 4, 1)> 0.0);//global peak!
	}	
	
	@Test
	public void testLocalVolumeScore(){
		Histogram histo = buildTable();
		LocalVolumeScore peakScore = new LocalVolumeScore(histo,1);		
		
		assertTrue(peakScore.getVolumeAt(0)== 6.0);
		assertTrue(peakScore.getVolumeAt(1)== 4.0);
		assertTrue(peakScore.getVolumeAt(2)== 5.0);
		assertTrue(peakScore.getVolumeAt(3)== 6.0);
		assertTrue(peakScore.getVolumeAt(4)== 6.0);
		
		assertTrue(peakScore.score(histo, 0, 1) >  0.0);
		assertTrue(peakScore.score(histo, 1, 1) <  0.0);//local peak!
		assertTrue(peakScore.score(histo, 2, 1) == 0.0);
		assertTrue(peakScore.score(histo, 3, 1) >  0.0);
		assertTrue(peakScore.score(histo, 4, 1) == 0.0);//global peak!
		
		peakScore = new LocalVolumeScore(histo,2);		
		assertTrue(peakScore.getVolumeAt(0)== 9.0);
		assertTrue(peakScore.getVolumeAt(1)== 9.0);
		assertTrue(peakScore.getVolumeAt(2)== 9.0);
		assertTrue(peakScore.getVolumeAt(3)== 9.0);
		assertTrue(peakScore.getVolumeAt(4)== 9.0);
		
		
		Histogram octaveHistogram = PitchFunctions.readFrequencyTable("src/tarsos/test/data/other_african_octave_frequency_table.txt");
		octaveHistogram = octaveHistogram.gaussianSmooth(0.5);
		SimplePlot p = new SimplePlot("volume_score");
		p.addData(0, octaveHistogram);
		peakScore = new LocalVolumeScore(octaveHistogram,10);
		
		for(int i = 0; i< octaveHistogram.getNumberOfClasses() ; i++){
			p.addData(1, i * 6,peakScore.score(octaveHistogram, i, 10), true);
			p.addData(2, i * 6,peakScore.getVolumeAt(i)/10, true);
		}
		p.save();
	}
	
	@Test
	public void testLocalHeightScore(){
		Histogram histo = buildTable();
		LocalHeightScore peakScore = new LocalHeightScore();		
		
		assertTrue(peakScore.score(histo, 0, 1) <  0.0);
		assertTrue(peakScore.score(histo, 1, 1) >  0.0);//local peak!
		assertTrue(peakScore.score(histo, 2, 1) <  0.0);
		assertTrue(peakScore.score(histo, 3, 1) == 0.0);
		assertTrue(peakScore.score(histo, 4, 1) >  0.0);//global peak!
	}
	
	@Test
	public void visualPeakDetectionTest(){
		int winddowSize = 20;
		double gaussianSmoothingFactor = 0.8;
		double peakAcceptFactor = 1.5;
		
		Histogram octaveHistogram = PitchFunctions.readFrequencyTable("src/tarsos/test/data/other_african_octave_frequency_table.txt");
		octaveHistogram = octaveHistogram.gaussianSmooth(gaussianSmoothingFactor);		
		LocalHeightScore peakHeightScore = new LocalHeightScore();
		DifferenceScore peakDifferenceScore = new DifferenceScore(octaveHistogram,winddowSize);
		
		SimplePlot p = new SimplePlot("visual_peak_detection");
		p.addData(0, octaveHistogram);
		
		double maxHeightScore= Double.NEGATIVE_INFINITY;;
		double maxHeight = Double.NEGATIVE_INFINITY;
		for(int i = 0; i< octaveHistogram.getNumberOfClasses() ; i++){
			maxHeightScore = Math.max(peakHeightScore.score(octaveHistogram, i, winddowSize), maxHeightScore);
			maxHeight = Math.max(maxHeight,octaveHistogram.getCountForClass(i));
		}
		
		//double heigthScoreFactor = maxHeight/maxHeightScore/2.8;
		
		for(int i = 0; i< octaveHistogram.getNumberOfClasses() ; i++){
			//p.addData(1, i * 6,peakHeightScore.score(octaveHistogram, i, winddowSize) * heigthScoreFactor, true);
			p.addData(2, i * 6,peakDifferenceScore.score(octaveHistogram, i, winddowSize), true);
			
		}
		Histogram peakHistogram = PeakDetector.newPeakDetection(octaveHistogram, winddowSize,peakAcceptFactor);
		p.addData(3, peakHistogram);
		
		p.addLegend(0,"Smoothed histo");
		p.addLegend(1,"Heigthscore");
		p.addLegend(2,"Diffscore");
		p.addLegend(3,"Peaks");
		
		p.save();		
	}

}
