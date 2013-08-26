package be.hogent.tarsos.ui.link.layers.segmentationlayers;

import java.awt.Graphics2D;

import be.hogent.tarsos.tarsossegmenter.model.AASModel;
import be.hogent.tarsos.ui.link.LinkedPanel;
import be.hogent.tarsos.ui.link.layers.Layer;

public class SegmentationLayer implements Layer {

	protected AASModel model;
	private int frameSize;
	private int overlap;
	private int niveau;

	protected final LinkedPanel parent;

	public void initialise() {
		// TODO: AASMODEL: configuratieparameters setten
	}

	public SegmentationLayer(final LinkedPanel parent, int niveau) {
		this.parent = parent;
		frameSize = 0;
		overlap = 0;
		this.niveau = niveau;
	}

	public void run() {
		AASModel.getInstance().calculate();
		AASModel.getInstance().getSegmentation().getSegmentationLists(niveau);
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

	public void draw(Graphics2D graphics) {
		// TODO Auto-generated method stub

	}

}
