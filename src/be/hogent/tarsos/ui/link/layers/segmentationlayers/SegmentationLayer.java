package be.hogent.tarsos.ui.link.layers.segmentationlayers;

import java.awt.Graphics2D;
import java.util.ArrayList;

import be.hogent.tarsos.tarsossegmenter.model.AASModel;
import be.hogent.tarsos.tarsossegmenter.model.segmentation.Segment;
import be.hogent.tarsos.ui.link.LinkedPanel;
import be.hogent.tarsos.ui.link.layers.Layer;

public class SegmentationLayer implements Layer {

	private int niveau;
	private ArrayList<Segment> segments;
	
	protected final LinkedPanel parent;

	public void initialise() {
		
	}

	public SegmentationLayer(final LinkedPanel parent, int niveau) {
		this.parent = parent;
		this.niveau = niveau;
		segments = new ArrayList<Segment>();
	}

	public void run() {
		AASModel.getInstance().calculate();
		AASModel.getInstance().getSegmentation().getSegmentationLists(niveau);
	}

	public void draw(Graphics2D graphics) {
		// TODO Auto-generated method stub

	}

	public String getName() {
		// TODO Auto-generated method stub
		return null;
	}

}
