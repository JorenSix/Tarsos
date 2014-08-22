package be.tarsos.ui.link.segmentation;

import java.util.ArrayList;

import be.tarsos.tarsossegmenter.model.segmentation.Segment;

public class SegmentationList extends ArrayList<Segment> {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	String label;
	SegmentationLevel level;

	public SegmentationList(SegmentationLevel level){
		this.level = level;
		label = level.getName();
	}
	
	public SegmentationList(SegmentationLevel level, String label){
		this.level = level;
		this.label = label;
	}

	public String getLabel() {
		return label;
	}

	public void setLabel(String label) {
		this.label = label;
	}

	public SegmentationLevel getLevel() {
		return level;
	}
}
