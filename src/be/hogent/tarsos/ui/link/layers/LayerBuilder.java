package be.hogent.tarsos.ui.link.layers;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import be.hogent.tarsos.tarsossegmenter.model.AASModel;
import be.hogent.tarsos.ui.link.LinkedPanel;
import be.hogent.tarsos.ui.link.layers.featurelayers.BeatLayer;
import be.hogent.tarsos.ui.link.layers.featurelayers.ConstantQLayer;
import be.hogent.tarsos.ui.link.layers.featurelayers.PitchContourLayer;
import be.hogent.tarsos.ui.link.layers.featurelayers.WaveFormLayer;
import be.hogent.tarsos.ui.link.layers.segmentationlayers.SegmentationLayer;

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
				if (lp.getPropertyName().equals("Framesize")) {
					framesize = (Integer) lp.getSelectedValue();
				} else if (lp.getPropertyName().equals("Overlap")) {
					overlap = (Float) lp.getSelectedValue();
				} else if (lp.getPropertyName().equals("Bins/octave")) {
					cqtBins = (Integer) lp.getSelectedValue();
				}
			}
			return new ConstantQLayer(parent, framesize, Math.round(framesize
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
			return new ConstantQLayer(parent, framesize, Math.round(framesize
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
			return new SegmentationLayer(parent, AASModel.MACRO_LEVEL, 100,
					4000);
		case WAVEFORM:
			return new WaveFormLayer(parent);
		}
		return null;
	}
}
