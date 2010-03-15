package be.hogent.tarsos.test;

import static org.junit.Assert.assertTrue;

import org.junit.Test;

import be.hogent.tarsos.util.SimplePlot;
import be.hogent.tarsos.util.histogram.ToneScaleHistogram;


public class ToneScaleHistogramTest {
	
	@Test
	public void createToneScaleTest(){
		//test edge wrapping:
		
		double[] peaks = {18,303,603,903,1180};
		ToneScaleHistogram histo = ToneScaleHistogram.createToneScale(peaks, null, null, null);
		assertTrue(histo.getCount(0) > 0);
		assertTrue(histo.getCount(0) == histo.getCount(1200));
		
		SimplePlot plot = new SimplePlot("Theoretic tone scale");
		plot.addData(0,histo);
		plot.save();
	}
}
