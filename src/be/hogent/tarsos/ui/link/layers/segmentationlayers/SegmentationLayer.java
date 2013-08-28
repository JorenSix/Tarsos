package be.hogent.tarsos.ui.link.layers.segmentationlayers;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Stroke;
import java.util.ArrayList;

import be.hogent.tarsos.tarsossegmenter.model.AASModel;
import be.hogent.tarsos.tarsossegmenter.model.segmentation.Segment;
import be.hogent.tarsos.transcoder.ffmpeg.EncoderException;
import be.hogent.tarsos.ui.link.LinkedFrame;
import be.hogent.tarsos.ui.link.LinkedPanel;
import be.hogent.tarsos.ui.link.coordinatessystems.CoordinateSystem;
import be.hogent.tarsos.ui.link.layers.Layer;
import be.hogent.tarsos.ui.link.layers.featurelayers.FeatureLayer;

public class SegmentationLayer extends FeatureLayer {

	private int niveau;
	private ArrayList<Segment> segments;
	private int lowerFilterFreq;
	private int upperFilterFreq;

	protected final LinkedPanel parent;

	public void initialise() {

	}

	public SegmentationLayer(final LinkedPanel parent, int niveau,
			int lowerFilterFreq, int upperFilterFreq) {
		super(parent);
		this.parent = parent;
		this.niveau = niveau;
		this.lowerFilterFreq = lowerFilterFreq;
		this.upperFilterFreq = upperFilterFreq;
//		segments = new ArrayList<Segment>();
	}

	public void run() {
		be.hogent.tarsos.tarsossegmenter.model.AudioFile af = null;
		try {
			af = new be.hogent.tarsos.tarsossegmenter.model.AudioFile(
					LinkedFrame.getInstance().getAudioFile().transcodedPath());
		} catch (EncoderException e) {
			e.printStackTrace();
		}
		AASModel.getInstance().calculateWithDefaults(af, lowerFilterFreq,
				upperFilterFreq);
		segments = AASModel.getInstance().getSegmentation().getSegments(niveau);
	}

	public void draw(Graphics2D graphics) {
		
		if (segments != null && !segments.isEmpty()){
			
			CoordinateSystem cs = parent.getCoordinateSystem();
			final int xMin = (int) cs.getMin(CoordinateSystem.X_AXIS);
			final int xMax = (int) cs.getMax(CoordinateSystem.X_AXIS);
			final int yMin = (int) cs.getMin(CoordinateSystem.Y_AXIS);
			final int yMax = (int) cs.getMax(CoordinateSystem.Y_AXIS);
//			final float scaleFactor = ((float)xMax)/((float)parent.getWidth());
//			final int height = parent.getHeight();
			for (Segment s : segments){
				int startMiliSec = Math.round(s.startTime*1000);
				int endMiliSec = Math.round(s.endTime*1000);
				if ( startMiliSec >= xMin && endMiliSec <= xMax){
//					int startPixel = Math.round((s.startTime-xMin)/scaleFactor);
					graphics.setColor(Color.LIGHT_GRAY);
					graphics.fillRect(startMiliSec, yMin, endMiliSec, yMax);
//					graphics.setStroke(new Stroke(10));
					graphics.setColor(Color.BLACK);
					graphics.drawLine(startMiliSec, yMin, startMiliSec, yMax);
				}
			}
		}

	}

	public String getName() {
		String name = "Segmentationlayer";
		switch (this.niveau){
		case AASModel.MACRO_LEVEL:
			name += " - macro";
			break;
		case AASModel.MESO_LEVEL:
			name += " - meso";
			break;
		case AASModel.MICRO_LEVEL:
			name += " - micro";
			break;
		}
		return name;
	}

}
