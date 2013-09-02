package be.hogent.tarsos.ui.link.layers.segmentationlayers;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Point;
import java.awt.Stroke;
import java.awt.event.MouseEvent;
import java.awt.geom.Point2D;
import java.util.ArrayList;

import be.hogent.tarsos.tarsossegmenter.model.AASModel;
import be.hogent.tarsos.tarsossegmenter.model.segmentation.Segment;
import be.hogent.tarsos.transcoder.ffmpeg.EncoderException;
import be.hogent.tarsos.ui.link.LinkedFrame;
import be.hogent.tarsos.ui.link.LinkedPanel;
import be.hogent.tarsos.ui.link.coordinatessystems.ICoordinateSystem;
import be.hogent.tarsos.ui.link.layers.Layer;
import be.hogent.tarsos.ui.link.layers.LayerUtilities;
import be.hogent.tarsos.ui.link.layers.featurelayers.FeatureLayer;

public class SegmentationLayer extends FeatureLayer {

	private int niveau;
	private ArrayList<Segment> segments;
	private int lowerFilterFreq;
	private int upperFilterFreq;
	private be.hogent.tarsos.tarsossegmenter.model.AudioFile af;
	private Point mousePoint;
	private final float TIME_TOLERANCE = 0.01f;
	private int movingSegmentIndex;
	private boolean dragging;
	private int mouseDraggingPointX;

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
		dragging = false;
		// segments = new ArrayList<Segment>();
	}
	
	public void setDragging(boolean value){
		this.dragging = value;
	}

	public void dragSegment(Graphics2D graphics, MouseEvent e) {
		if (movingSegmentIndex > 0) {
			Point2D unitsCurrent = LayerUtilities.pixelsToUnits(graphics,
					(int) Math.round(e.getX()),
					(int) Math.round(e.getY()));
			segments.get(movingSegmentIndex).startTime = (float) (unitsCurrent
					.getX() / (double)1000);
			segments.get(movingSegmentIndex - 1).endTime = (float) (unitsCurrent
					.getX() / (double)1000);
			mouseDraggingPointX = (int)Math.round(unitsCurrent.getX());
		}
		draw(graphics);
	}

	public void run() {
		System.out.println("Starting segmentation calculation");
		AASModel.getInstance().calculateWithDefaults(af, lowerFilterFreq,
				upperFilterFreq);
		System.out.println("Segmentation calculation done");
		segments = AASModel.getInstance().getSegmentation().getSegments(niveau);
		System.out.println("Segments received - size: " + segments.size());
	}

	public void draw(Graphics2D graphics) {

		if (segments != null && !segments.isEmpty()) {
			graphics.setStroke(new BasicStroke(Math.round(LayerUtilities
					.pixelsToUnits(graphics, 4, true))));

			ICoordinateSystem cs = parent.getCoordinateSystem();
			final int xMin = (int) cs.getMin(ICoordinateSystem.X_AXIS);
			final int xMax = (int) cs.getMax(ICoordinateSystem.X_AXIS);
			final int yMin = (int) cs.getMin(ICoordinateSystem.Y_AXIS);
			final int yMax = (int) cs.getMax(ICoordinateSystem.Y_AXIS);
			for (Segment s : segments) {
				int startMiliSec = Math.round(s.startTime * 1000);
				int endMiliSec = Math.round(s.endTime * 1000);
				if (!(endMiliSec <= xMin || startMiliSec >= xMax)) {
					graphics.setColor(s.color);
					int begin = Math.max(startMiliSec, xMin);
					int end = Math.min(endMiliSec, xMax);
					graphics.fillRect(begin, yMin, end - startMiliSec, yMax
							- yMin);
					graphics.setColor(Color.DARK_GRAY);
					graphics.drawLine(begin, yMin,
							Math.max(startMiliSec, xMin), yMax);
					int textOffset = Math.round(LayerUtilities.pixelsToUnits(
							graphics, 12, false));
					LayerUtilities.drawString(graphics, s.label, (end + begin)
							/ 2 - (textOffset / 2), 0, true, false);

				} else if (startMiliSec <= xMin && endMiliSec >= xMax) {
					int begin = Math.max(startMiliSec, xMin);
					int end = Math.min(endMiliSec, xMax);
					int textOffset = Math.round(LayerUtilities.pixelsToUnits(
							graphics, 12, false));
					LayerUtilities.drawString(graphics, s.label, (end + begin)
							/ 2 - (textOffset / 2), 0, true, false);
				}
			}
			int lastBoundry = Math
					.round(segments.get(segments.size() - 1).endTime * 1000);
			if (lastBoundry < xMax) {
				graphics.drawLine(Math.max(lastBoundry, xMin), yMin,
						Math.max(lastBoundry, xMin), yMax);
			}

			if (!dragging && mousePoint != null) {
				Point2D unitsCurrent = LayerUtilities.pixelsToUnits(graphics,
						(int) Math.round(mousePoint.getX()),
						(int) Math.round(mousePoint.getY()));

				float relativeOffset = TIME_TOLERANCE
						* cs.getDelta(ICoordinateSystem.X_AXIS);
				int i = 0;
				while (i < segments.size()
						&& segments.get(i).endTime * 1000 < unitsCurrent.getX()
						&& !(unitsCurrent.getX() >= segments.get(i).startTime * 1000 && unitsCurrent
								.getX() <= segments.get(i).endTime * 1000)) {
					i++;
				}
				boolean onSegmentBoundry = false;
				if (i < segments.size()
						&& segments.get(i).endTime * 1000 >= unitsCurrent
								.getX()) {
					if (unitsCurrent.getX() <= (segments.get(i).startTime * 1000)
							+ relativeOffset
							|| unitsCurrent.getX() >= (segments.get(i).endTime * 1000)
									- relativeOffset) {
						if (unitsCurrent.getX() >= (segments.get(i).endTime * 1000)
								- relativeOffset) {
							i++;
						}
						if (i > 0 && i < segments.size()) {
							onSegmentBoundry = true;
							movingSegmentIndex = i;
						}
					}
				}

				if (onSegmentBoundry) {
					parent.setCursor(Cursor
							.getPredefinedCursor(Cursor.E_RESIZE_CURSOR));
				} else {
					movingSegmentIndex = -1;
					parent.setCursor(Cursor
							.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
					graphics.setColor(Color.LIGHT_GRAY);
					graphics.setStroke(new BasicStroke(Math
							.round(LayerUtilities.pixelsToUnits(graphics, 2,
									true))));
					graphics.drawLine((int) Math.round(unitsCurrent.getX()),
							yMin, (int) Math.round(unitsCurrent.getX()), yMax);
				}
			} else if (dragging){
				graphics.setColor(Color.DARK_GRAY);
				graphics.setStroke(new BasicStroke(Math
						.round(LayerUtilities.pixelsToUnits(graphics, 2,
								true))));
				graphics.drawLine((int) Math.round(mouseDraggingPointX),
						yMin, (int) Math.round(mouseDraggingPointX), yMax);
//				System.out.println("dragging");
			}
		}
		graphics.setStroke(new BasicStroke(1));
	}

	public String getName() {
		String name = "Segmentationlayer";
		switch (this.niveau) {
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

	public void mouseMoved(MouseEvent e) {
		mousePoint = e.getPoint();
		parent.repaint();
	}

	public void addSegment(int pixelUnitX) {
		Graphics2D g = (Graphics2D) parent.getGraphics();
		g.setTransform(parent.updateTransform(g.getTransform()));
		Point2D unitsCurrent = LayerUtilities.pixelsToUnits(g,
				(int) Math.round(pixelUnitX), 0);
		float time = (float) unitsCurrent.getX();
		if (time > 0
				&& time <= LinkedFrame.getInstance().getAudioFile()
						.getLengthInMilliSeconds()) {
			time /= 1000;
			int i = 0;
			if (segments != null && segments.size() != 0) {
				while (i < segments.size() && segments.get(i).endTime < time) {
					i++;
				}
				if (i < segments.size() && segments.get(i).endTime > time
						&& segments.get(i).startTime < time) {
					Segment s = new Segment(time, segments.get(i).endTime, "",
							Color.WHITE);
					segments.get(i).endTime = time;
					segments.add(i + 1, s);
				} else if (i == segments.size()) {
					Segment s = new Segment(segments.get(i - 1).endTime, time,
							"", Color.WHITE);
					segments.add(s);
				}
			} else {
				segments = new ArrayList<Segment>();
				Segment s = new Segment(0, time, "", Color.WHITE);
				segments.add(s);
			}
			parent.repaint();
		}
	}
}
