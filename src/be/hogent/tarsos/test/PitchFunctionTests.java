package be.hogent.tarsos.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import org.junit.Test;

import be.hogent.tarsos.pitch.PitchFunctions;


public class PitchFunctionTests {

	@Test
	public void testConvertHertzToMidiCent(){
		assertEquals(69, PitchFunctions.convertHertzToMidiCent(440.0),0.00000001);
		assertEquals(81, PitchFunctions.convertHertzToMidiCent(880.0),0.00000001);
		Random r = new Random();
		for(int i=0 ; i < 10000 ; i++){
			//random test and reference pitch +- between 20 and 20kHz
			double referencePitch = r.nextDouble() * 20000 + 20;
			double testPitch = r.nextDouble() * 20000 + 20;
			//calculate the difference in cents between the pitches
			double absoluteCentValueReference = PitchFunctions.convertHertzToAbsoluteCent(referencePitch);
			double absoluteCentValueTest = PitchFunctions.convertHertzToAbsoluteCent(testPitch);
			double differenceInCents = absoluteCentValueTest - absoluteCentValueReference;
			//calculate the actual and expected value
			//The reference pitch converted to midicents added to the difference is expected
			double expected = PitchFunctions.convertHertzToMidiCent(referencePitch) + differenceInCents / 100;
			double actual = PitchFunctions.convertHertzToMidiCent(testPitch);
			assertEquals(actual, expected,0.00000001);
		}
	}

	@Test
	public void testNoteNames(){
		assertEquals("A-1",PitchFunctions.noteName(13.75));
		assertEquals("A0",PitchFunctions.noteName(27.5));
		assertEquals("A1",PitchFunctions.noteName(55.0));
		assertEquals("A2",PitchFunctions.noteName(110.0));
		assertEquals("A3",PitchFunctions.noteName(220.0));
		assertEquals("A4",PitchFunctions.noteName(440.0));
		assertEquals("A4",PitchFunctions.noteName(435.0));
		assertEquals("A4",PitchFunctions.noteName(441.0));
		assertEquals("A5",PitchFunctions.noteName(880.0));
		assertEquals("A6",PitchFunctions.noteName(1760.0));
		assertEquals("C-1",PitchFunctions.noteName(0.05));
		assertEquals("C-1",PitchFunctions.noteName(8.1785));
		assertEquals("G9",PitchFunctions.noteName(80000));
		assertEquals("D#4/Eb4",PitchFunctions.noteName(311.13));
	}

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
