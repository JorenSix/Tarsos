package be.hogent.tarsos.test;

import org.junit.Test;

import be.hogent.tarsos.pitch.PitchFunctions;
import be.hogent.tarsos.util.SimplePlot;
import be.hogent.tarsos.util.histogram.CorrelationMeasure;
import be.hogent.tarsos.util.histogram.Histogram;
import be.hogent.tarsos.util.histogram.HistogramFunction;

public class AmbitusTests {
	@Test
	public void testMostEnergyRichOctaves(){		
		Histogram range = PitchFunctions.readFrequencyTable("src/tarsos/test/data/african_range_frequency_table.txt");
		Histogram energyRichOctaves = HistogramFunction.mostEnergyRichOctaves(range, 4);
		
		range = HistogramFunction.fold(range).normalize();
		energyRichOctaves =  HistogramFunction.fold(energyRichOctaves).normalize();
		range.plotCorrelation(energyRichOctaves,CorrelationMeasure.INTERSECTION);
		
		SimplePlot correlationPlot = new SimplePlot("ritch");
		//plots the first histogram
		correlationPlot.addData(0, range);
		//plots the other (displaced) histogram
		correlationPlot.addData(1, energyRichOctaves);
		correlationPlot.save();		
	}
}
