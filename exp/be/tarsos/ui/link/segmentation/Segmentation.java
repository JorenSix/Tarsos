package be.tarsos.ui.link.segmentation;

import java.util.ArrayList;
import java.util.HashMap;

import be.tarsos.tarsossegmenter.model.AASModel;
import be.tarsos.tarsossegmenter.model.segmentation.Segment;
import be.tarsos.transcoder.ffmpeg.EncoderException;
import be.tarsos.ui.link.LinkedFrame;

public class Segmentation {

	private static Segmentation instance;
	private HashMap<String, SegmentationList> segmentationLists;
	private boolean isCalculated;

	private Segmentation() {
		segmentationLists = new HashMap<String, SegmentationList>();
	}

	public static Segmentation getInstance() {
		if (instance != null) {
			return instance;
		} else {
			instance = new Segmentation();
			return instance;
		}
	}

	public void clear() {
		for (SegmentationList l : segmentationLists.values()) {
			l.clear();
		}
		setCalculated(false);
	}
	
	public void deleteAll(){
		segmentationLists.clear();
	}

	public SegmentationList getSegmentationList(String label) {
		if (!isCalculated) {
			this.calculate();
		}
		if (segmentationLists.get(label) != null && segmentationLists.get(label).isEmpty() && (segmentationLists.get(label).getLevel() == SegmentationLevel.MACRO || segmentationLists.get(label).getLevel() == SegmentationLevel.MESO || segmentationLists.get(label).getLevel() == SegmentationLevel.MICRO)){
			updateSegmentationLists();
		}
		return segmentationLists.get(label);
	}

	public SegmentationList constructNewSegmentationList(
			SegmentationLevel level) {
		SegmentationList list = new SegmentationList(level);
		String initialLabel = list.getLabel();
		int i = 1;
		while (segmentationLists.containsKey(list.getLabel())) {
			list = new SegmentationList(level, initialLabel + " #" + i);
			i++;
		}
		segmentationLists.put(list.getLabel(), list);
		return list;
	}

	public SegmentationList constructNewSegmentationList(
			SegmentationLevel level, String label) {
		SegmentationList list = new SegmentationList(level, label);
		if (!segmentationLists.containsKey(label)) {
			segmentationLists.put(list.getLabel(), list);
			return list;
		} else {
			return segmentationLists.get(label);
		}
	}

	public boolean isCalculated() {
		return isCalculated;
	}

	public void setCalculated(boolean isCalculated) {
		this.isCalculated = isCalculated;
	}
	
	public void updateSegmentationLists(){
		for (SegmentationList l : segmentationLists.values()){
			ArrayList<Segment> segments = null;
			switch (l.level){
			case MACRO:
				segments = AASModel.getInstance().getSegmentation().getSegments(AASModel.MACRO_LEVEL);
				break;
			case MESO:
				segments = AASModel.getInstance().getSegmentation().getSegments(AASModel.MESO_LEVEL);
				break;
			case MICRO:
				segments = AASModel.getInstance().getSegmentation().getSegments(AASModel.MICRO_LEVEL);
				break;
			default:
				break;
			}
			if (segments != null){
				for (Segment s : segments){
					l.add(s);
				}
			}
		}
		setCalculated(true);
	}

	public void calculate() {
		if (!isCalculated) {
			try {
				AASModel.getInstance().calculateWithDefaults(
						new be.tarsos.tarsossegmenter.model.AudioFile(
								LinkedFrame.getInstance().getAudioFile()
										.transcodedPath()),
						Math.round(LinkedFrame.getInstance()
								.getLowerFrequencyLimit()),
						Math.round(LinkedFrame.getInstance()
								.getUpperFrequencyLimit()));
			} catch (EncoderException e) {
				e.printStackTrace();
			}
		}
		updateSegmentationLists();
	}

}
