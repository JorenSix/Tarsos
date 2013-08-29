package be.hogent.tarsos.ui.link.layers.segmentationlayers;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
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
import be.hogent.tarsos.ui.link.layers.LayerUtilities;
import be.hogent.tarsos.ui.link.layers.featurelayers.FeatureLayer;

public class SegmentationLayer extends FeatureLayer {

	private int niveau;
	private ArrayList<Segment> segments;
	private int lowerFilterFreq;
	private int upperFilterFreq;
	private be.hogent.tarsos.tarsossegmenter.model.AudioFile af;

	protected final LinkedPanel parent;

	public void initialise() {
		try {
			af = new be.hogent.tarsos.tarsossegmenter.model.AudioFile(
					LinkedFrame.getInstance().getAudioFile().transcodedPath());
		} catch (EncoderException e) {
			e.printStackTrace();
		}
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
		System.out.println("Starting segmentation calculation");
		AASModel.getInstance().calculateWithDefaults(af, lowerFilterFreq,
				upperFilterFreq);
		System.out.println("Segmentation calculation done");
		segments = AASModel.getInstance().getSegmentation().getSegments(niveau);
		System.out.println("Segments received - size: " + segments.size()) ;
	}

	public void draw(Graphics2D graphics) {
		
		if (segments != null && !segments.isEmpty()){
			graphics.setStroke(new BasicStroke(Math.round(LayerUtilities.pixelsToUnits(graphics,4, true))));
			
//			Font oldfont = graphics.getFont();
//			graphics.setFont(new Font ("Garamond", Font.BOLD , 36));
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
				if (!(endMiliSec <= xMin || startMiliSec >= xMax)){
					graphics.setColor(s.color);
					int begin = Math.max(startMiliSec, xMin);
					int end = Math.min(endMiliSec, xMax);
					graphics.fillRect(begin, yMin, end-startMiliSec, yMax-yMin);
					graphics.setColor(Color.DARK_GRAY);
					
					graphics.drawLine(begin, yMin, Math.max(startMiliSec, xMin), yMax);
					System.out.println("Printing " + s.label + " @(" + (end+begin)/2+","+0+")");
					int textOffset = Math.round(LayerUtilities.pixelsToUnits(graphics,12, false));
					LayerUtilities.drawString(graphics,s.label,(end+begin)/2-(textOffset/2),0,true,false);
					
				} else if (startMiliSec <= xMin && endMiliSec >= xMax ){
					int begin = Math.max(startMiliSec, xMin);
					int end = Math.min(endMiliSec, xMax);
					int textOffset = Math.round(LayerUtilities.pixelsToUnits(graphics,12, false));
					LayerUtilities.drawString(graphics,s.label,(end+begin)/2-(textOffset/2),0,true,false);
				}
			}
			int lastBoundry = Math.round(segments.get(segments.size()-1).endTime*1000);
			if (lastBoundry < xMax){
				graphics.drawLine(Math.max(lastBoundry, xMin), yMin, Math.max(lastBoundry, xMin), yMax);
			}
//			graphics.setFont(oldfont);
			
		}
		graphics.setStroke(new BasicStroke(1));
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
