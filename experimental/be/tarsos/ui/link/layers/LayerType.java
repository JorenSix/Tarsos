package be.tarsos.ui.link.layers;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.swing.JPanel;

import be.tarsos.ui.link.LinkedPanel;
import be.tarsos.ui.link.coordinatessystems.CoordinateSystem;
import be.tarsos.ui.link.coordinatessystems.ICoordinateSystem;
import be.tarsos.ui.link.coordinatessystems.Quantity;
import be.tarsos.ui.link.segmentation.SegmentationLevel;

public enum LayerType {
	WAVEFORM, FEATURE_FFT, FEATURE_CQT, FEATURE_PITCH, SEGMENTATION, BEAT;

	// Per LayerType:
	// Gui opstellen aan de hand van dit
	// meedelen welk CoordinateSystem -> welke layertypes in return
	// mogelijke parameters:
	// framesize
	// overlap
	// feature specifieke waarden (vb aantal bins bij CQT)

	public static LayerType[] getLayerTypes(ICoordinateSystem cs) {
		ArrayList<LayerType> lt = new ArrayList<LayerType>();
		if (cs.getQuantityForAxis(ICoordinateSystem.X_AXIS) == Quantity.TIME) {
			if (cs.getQuantityForAxis(ICoordinateSystem.Y_AXIS) == Quantity.FREQUENCY) {
				lt.add(FEATURE_CQT);
				lt.add(FEATURE_FFT);
				lt.add(FEATURE_PITCH);
			} else if (cs.getQuantityForAxis(ICoordinateSystem.Y_AXIS) == Quantity.NONE) {
				lt.add(SEGMENTATION);
				lt.add(BEAT);
			} else if (cs.getQuantityForAxis(ICoordinateSystem.Y_AXIS) == Quantity.AMPLITUDE) {
				lt.add(WAVEFORM);
			}
		}
		return lt.toArray(new LayerType[lt.size()]);
	}

	public static ArrayList<LayerProperty> getProperties(LayerType lt) {
		ArrayList<LayerProperty> properties = new ArrayList<LayerProperty>();
		switch (lt) {
		case WAVEFORM:
			break;
		case FEATURE_FFT:
			properties.add(new LayerProperty<Integer>("Framesize", Arrays
					.asList(4096, 8192, 16384, 32768, 65536)));
			properties.add(new LayerProperty<Float>("Overlap", Arrays.asList(
					0f, 0.1f, 0.2f, 0.3f, 0.4f, 0.5f, 0.6f, 0.7f, 0.8f, 0.9f)));
			properties.add(new LayerProperty<Integer>("Bins/octave", Arrays
					.asList(2, 3, 4, 6, 12, 24, 48, 96)));
			break;
		case FEATURE_CQT:
			properties.add(new LayerProperty<Float>("Overlap", Arrays.asList(
					0f, 0.1f, 0.2f, 0.3f, 0.4f, 0.5f, 0.6f, 0.7f, 0.8f, 0.9f)));
			properties.add(new LayerProperty<Integer>("Bins/octave", Arrays
					.asList(2, 3, 4, 6, 12, 24, 48, 96)));
			break;
		case FEATURE_PITCH:
			properties.add(new LayerProperty<Integer>("Framesize", Arrays
					.asList(4096, 8192, 16384, 32768, 65536)));
			properties.add(new LayerProperty<Float>("Overlap", Arrays.asList(
					0f, 0.1f, 0.2f, 0.3f, 0.4f, 0.5f, 0.6f, 0.7f, 0.8f, 0.9f)));
			break;
		case SEGMENTATION:
			SegmentationLevel[] segmentationLevels = SegmentationLevel.values();
			String [] SegmentationLevelNames = new String[segmentationLevels.length];
			for (int i =0; i <segmentationLevels.length; i++){
				SegmentationLevelNames[i] = segmentationLevels[i].getName();
			}
			properties.add(new LayerProperty<String>("Level", Arrays.asList(SegmentationLevelNames)));
			break;
		case BEAT:
			break;
		}
		return properties;
	}
}
