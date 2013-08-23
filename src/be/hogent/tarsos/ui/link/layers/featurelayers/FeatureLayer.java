package be.hogent.tarsos.ui.link.layers.featurelayers;

import java.awt.Graphics2D;
import java.util.TreeMap;

import be.hogent.tarsos.dsp.AudioDispatcher;
import be.hogent.tarsos.ui.link.LinkedPanel;
import be.hogent.tarsos.ui.link.coordinatessystems.CoordinateSystem;
import be.hogent.tarsos.ui.link.layers.Layer;

public abstract class FeatureLayer implements Layer {

	protected AudioDispatcher adp;
	private int frameSize;
	private int overlap;
	protected final TreeMap<Double, float[]> features;
	protected final LinkedPanel parent;

	public abstract void initialise();

	public FeatureLayer(final LinkedPanel parent) {
		features = new TreeMap<Double, float[]>();
		this.parent = parent;
		frameSize = 0;
		overlap = 0;
	}

	public FeatureLayer(final LinkedPanel parent, int frameSize, int overlap) {
		this(parent);
		this.frameSize = frameSize;
		this.overlap = overlap;
	}

	public void run() {
		if (adp != null) {
			adp.run();
		} else {
			initialise();
			adp.run();
		}
	}

	public int getFrameSize() {
		return frameSize;
	}

	public void setFrameSize(int frameSize) {
		this.frameSize = frameSize;
	}

	public int getOverlap() {
		return overlap;
	}

	public void setOverlap(int overlap) {
		this.overlap = overlap;
	}

}
