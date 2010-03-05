package be.hogent.tarsos.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

import be.hogent.tarsos.pitch.PitchFunctions;


public class PitchFunctionTests {
	
	@Test
	public void testConvertHertzToCent() {
		double reference_frequency = 27.5 * Math.pow(2.0,0.25);	//C1	
		List<Double> pitchValuesInHertz = new ArrayList<Double>();
		pitchValuesInHertz.add(reference_frequency);
		pitchValuesInHertz.add(reference_frequency - 10);
		pitchValuesInHertz.add(reference_frequency + 10);
		pitchValuesInHertz.add(261.626); //C4
		
		//List<Double> pitchValuesInCent = PitchFunctions.convertHertzToCent(pitchValuesInHertz);
		/*
		assertEquals(0, pitchValuesInCent.get(0),0.01);
		assertTrue(0 > pitchValuesInCent.get(1));
		assertTrue(0 < pitchValuesInCent.get(2));			
		assertEquals(0, pitchValuesInCent.get(3) % 1200,0.01);
		*/
	}

	@Test
	public void testFoldPitchValuesInCentToOneOctave() {
		List<Double> pitchValuesInCent = new ArrayList<Double>();
		pitchValuesInCent.add(4.0);
		pitchValuesInCent.add(1204.0);
		pitchValuesInCent.add(1800.0);
		pitchValuesInCent.add(-200.0);
		pitchValuesInCent.add(1249.87);
		//PitchFunctions.foldPitchValuesInCentToOneOctave(pitchValuesInCent);
		/*
		assertTrue(4.0 == (double) pitchValuesInCent.get(0));
		assertTrue(4.0 == (double) pitchValuesInCent.get(1));
		assertTrue(600.0   == (double) pitchValuesInCent.get(2));
		assertTrue(1000.0  == (double) pitchValuesInCent.get(3));
		assertEquals(49.87, pitchValuesInCent.get(4),0.01);
		*/
	}

	@Test
	public void testMedian() {		
		List<Double> pitchValuesInCent = new ArrayList<Double>();	
		pitchValuesInCent.add(1204.0);
		pitchValuesInCent.add(1800.0);
		pitchValuesInCent.add(-200.0);
		pitchValuesInCent.add(1249.87);
		
		// -200   1204  1249.87 1800 => (1204 + 1249.87)/2 
		double actual = PitchFunctions.median(pitchValuesInCent);
		double expected = 1226.935; 
		assertEquals(expected,actual,0.01);
	}

	@Test
	public void testMedianFilter() {		
		List<Double> pitchValuesInCent = new ArrayList<Double>();	
		pitchValuesInCent.add(3.0);
		pitchValuesInCent.add(9.0);
		pitchValuesInCent.add(7.0);
		
		pitchValuesInCent = PitchFunctions.medianFilter(pitchValuesInCent,3);
		assertEquals(3.0, pitchValuesInCent.get(0),0.01);
		assertEquals(7.0, pitchValuesInCent.get(1),0.01);
		assertEquals(7.0, pitchValuesInCent.get(2),0.01);		
	}
	
	@Test
	public void testGaussianFilter() {		
		List<Double> pitchValuesInCent = new ArrayList<Double>();	
		pitchValuesInCent.add(3.0);
		pitchValuesInCent.add(9.0);
		pitchValuesInCent.add(7.0);
		
		pitchValuesInCent = PitchFunctions.gaussianFilter(pitchValuesInCent);
		//assertEquals(3.0, pitchValuesInCent.get(0),0.01);
		//assertEquals(7.0, pitchValuesInCent.get(1),0.01);
		//assertEquals(7.0, pitchValuesInCent.get(2),0.01);		
	}

	
	@Test
	public void testBandwithFilter() {		
		List<Double> pitchValuesInCent = new ArrayList<Double>();	
		pitchValuesInCent.add(3.0);
		pitchValuesInCent.add(9.0);
		pitchValuesInCent.add(7.0);
		
		PitchFunctions.bandwithFilter(pitchValuesInCent, 4, 8);
		assertTrue(pitchValuesInCent.size()==1);
		assertTrue(pitchValuesInCent.get(0)==7);
	}
	
	@Test
	public void testCreateFrequencyTable() {		
//		List<Double> values = new ArrayList<Double>();
//		values.add(-3.0);
//		values.add(3.0);
//		values.add(7.0);
//		values.add(7.2);
//		double classWidth = 5;
//		HashMap<Double,Double> hashMap = PitchFunctions.createFrequencyTable(values, classWidth);
//		assertTrue(hashMap.get(-2.5) == 1);		
//		assertTrue(hashMap.get(2.5) == 1);
//		assertTrue(hashMap.get(7.5) == 2);
//		
//		classWidth = 1.0;
//		hashMap = PitchFunctions.createFrequencyTable(values, classWidth);
//		assertTrue(hashMap.get(-2.5) == 1);
//		assertTrue(hashMap.get(3.5) == 1);
//		assertTrue(hashMap.get(7.5) == 2);
//		
//		classWidth = 2.0;
//		hashMap = PitchFunctions.createFrequencyTable(values, classWidth);
//		assertTrue(hashMap.get(-3.0) == 1);
//		assertTrue(hashMap.get(1.0) == 0);
//		assertTrue(hashMap.get(3.0) == 1);
//		assertTrue(hashMap.get(7.0) == 2);		
	}

}
