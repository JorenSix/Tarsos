package be.hogent.tarsos.test;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

public class SampleTests {

	@Test
	public void testPitchesWithoutHarmonics() {

		List<Double> pitchValues = new ArrayList<Double>();
		pitchValues.add(660.0);
		pitchValues.add(220.0);
		pitchValues.add(440.0);
		pitchValues.add(110.0);
		pitchValues.add(550.0);
		pitchValues.add(1234.0);

		// new
		// Sample(pitchValues).getPitchesWithoutHarmonicsIn(PitchUnit.HERTZ,0.02);
	}

}
