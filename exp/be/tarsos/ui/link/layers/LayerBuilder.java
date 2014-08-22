package be.tarsos.ui.link.layers;

import java.util.ArrayList;

import be.tarsos.ui.link.LinkedPanel;
import be.tarsos.ui.link.layers.featurelayers.BeatLayer;
import be.tarsos.ui.link.layers.featurelayers.ConstantQLayer;
import be.tarsos.ui.link.layers.featurelayers.FFTLayer;
import be.tarsos.ui.link.layers.featurelayers.PitchContourLayer;
import be.tarsos.ui.link.layers.featurelayers.WaveFormLayer;
import be.tarsos.ui.link.layers.segmentationlayers.SegmentationLayer;
import be.tarsos.ui.link.segmentation.SegmentationLevel;

public final class LayerBuilder {

	public static final Layer buildLayer(LinkedPanel parent, LayerType lt,
			ArrayList<LayerProperty> properties) {
		int framesize = -1;
		float overlap = -1f;

		switch (lt) {
		case BEAT:
			return new BeatLayer(parent);
		case FEATURE_CQT:
			int cqtBins = 0;
			for (LayerProperty lp : properties) {
				if (lp.getPropertyName().equals("Overlap")) {
					overlap = (Float) lp.getSelectedValue();
				} else if (lp.getPropertyName().equals("Bins/octave")) {
					cqtBins = (Integer) lp.getSelectedValue();
				}
			}
			return new ConstantQLayer(parent, Math.round(framesize
					* overlap), cqtBins);
		case FEATURE_FFT:
			int bins = 0;
			for (LayerProperty lp : properties) {
				if (lp.getPropertyName().equals("Framesize")) {
					framesize = (Integer) lp.getSelectedValue();
				} else if (lp.getPropertyName().equals("Overlap")) {
					overlap = (Float) lp.getSelectedValue();
				} else if (lp.getPropertyName().equals("Bins/octave")) {
					bins = (Integer) lp.getSelectedValue();
				}
			}
			return new FFTLayer(parent, framesize, Math.round(framesize
					* overlap), bins);
		case FEATURE_PITCH:
			for (LayerProperty lp : properties) {
				if (lp.getPropertyName().equals("Framesize")) {
					framesize = (Integer) lp.getSelectedValue();
				} else if (lp.getPropertyName().equals("Overlap")) {
					overlap = (Float) lp.getSelectedValue();
				}
			}
			return new PitchContourLayer(parent, framesize,
					Math.round(framesize * overlap));
		case SEGMENTATION:
			SegmentationLevel level = null;
			String label = null;
			for (LayerProperty lp : properties) {
				if (lp.getPropertyName().equals("Level")) {
					level = SegmentationLevel.getLevelByName(String.valueOf(lp
							.getSelectedValue()));
				} else if (lp.getPropertyName().equals("Label")) {
					label = String.valueOf(lp.getSelectedValue());
				}
			}
			if (label != null) {
				switch (level) {
				case BEAT:
					return new BeatLayer(parent);
				default:
					return new SegmentationLayer(parent, level, label);
				}
			} else {
				switch (level) {
				case BEAT:
					return new BeatLayer(parent);
				default:
					return new SegmentationLayer(parent, level);
				}
			}
		case WAVEFORM:
			return new WaveFormLayer(parent);
		}
		return null;
	}
}
