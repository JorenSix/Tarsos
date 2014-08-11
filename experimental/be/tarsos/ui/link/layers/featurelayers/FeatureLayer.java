package be.tarsos.ui.link.layers.featurelayers;

import java.awt.Graphics2D;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.TreeMap;

import be.tarsos.dsp.AudioDispatcher;
import be.tarsos.ui.link.LinkedPanel;
import be.tarsos.ui.link.coordinatessystems.ICoordinateSystem;
import be.tarsos.ui.link.layers.Layer;
import be.tarsos.ui.link.layers.LayerProperty;

public abstract class FeatureLayer implements Layer {

	protected AudioDispatcher adp;
	private int frameSize;
	private int overlap;
	protected final TreeMap<Double, float[]> features;
	protected final LinkedPanel parent;
	protected String name;

	public abstract void initialise();
	
	public FeatureLayer(final LinkedPanel parent) {
		features = new TreeMap<Double, float[]>();
		this.parent = parent;
		frameSize = 0;
		overlap = 0;
		name = "Feature layer";
	}
	
	public String getName(){
		return this.name;
	}

	public FeatureLayer(final LinkedPanel parent, int frameSize, int overlap) {
		this(parent);
		this.frameSize = frameSize;
		this.overlap = overlap;
	}

	public void run() {
		adp.run();
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
