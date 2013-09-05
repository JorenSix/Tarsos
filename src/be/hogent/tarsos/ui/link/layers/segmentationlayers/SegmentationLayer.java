package be.hogent.tarsos.ui.link.layers.segmentationlayers;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Point;
import java.awt.RenderingHints;
import java.awt.Stroke;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;
import java.util.ArrayList;

import javax.swing.JTextField;

import be.hogent.tarsos.tarsossegmenter.model.AASModel;
import be.hogent.tarsos.tarsossegmenter.model.segmentation.Segment;
import be.hogent.tarsos.transcoder.ffmpeg.EncoderException;
import be.hogent.tarsos.ui.link.LinkedFrame;
import be.hogent.tarsos.ui.link.LinkedPanel;
import be.hogent.tarsos.ui.link.coordinatessystems.ICoordinateSystem;
import be.hogent.tarsos.ui.link.layers.Layer;
import be.hogent.tarsos.ui.link.layers.LayerUtilities;
import be.hogent.tarsos.ui.link.layers.featurelayers.FeatureLayer;
import be.hogent.tarsos.ui.link.segmentation.Segmentation;
import be.hogent.tarsos.ui.link.segmentation.SegmentationLevel;
import be.hogent.tarsos.ui.link.segmentation.SegmentationList;

public class SegmentationLayer extends FeatureLayer implements KeyListener {

	private SegmentationLevel niveau;
	private SegmentationList segments;
	private Point2D mousePoint;
	private final float TIME_TOLERANCE = 0.01f;
	private int movingSegmentIndex;
	private int editLabelSegmentIndex;
	private boolean dragging;
	private int mouseDraggingPointX;

	protected final LinkedPanel parent;

	public void initialise() {
	}

	public SegmentationLayer(final LinkedPanel parent, SegmentationLevel niveau) {
		super(parent);
		this.parent = parent;
		this.niveau = niveau;
		dragging = false;
		segments = Segmentation.getInstance().constructNewSegmentationList(niveau);
		parent.addKeyListener(this);
	}
	
	public SegmentationLayer(final LinkedPanel parent, SegmentationLevel niveau, String label) {
		super(parent);
		this.parent = parent;
		this.niveau = niveau;
		dragging = false;
		segments = Segmentation.getInstance().constructNewSegmentationList(niveau, label);
		parent.addKeyListener(this);
	}

	public void setDragging(boolean value) {
		this.dragging = value;
	}

	public void dragSegment(Graphics2D graphics, MouseEvent e) {
		if (movingSegmentIndex > 0) {
			Point2D unitsCurrent = LayerUtilities.pixelsToUnits(graphics,
					(int) Math.round(e.getX()), (int) Math.round(e.getY()));
			segments.get(movingSegmentIndex).startTime = (float) (unitsCurrent
					.getX() / (double) 1000);
			segments.get(movingSegmentIndex - 1).endTime = (float) (unitsCurrent
					.getX() / (double) 1000);
			mouseDraggingPointX = (int) Math.round(unitsCurrent.getX());
		}
		draw(graphics);
	}

	public void run() {
		segments = Segmentation.getInstance().getSegmentationList(segments.getLabel());
	}

	public void draw(Graphics2D graphics) {
		ICoordinateSystem cs = parent.getCoordinateSystem();
		final int xMin = (int) cs.getMin(ICoordinateSystem.X_AXIS);
		final int xMax = (int) cs.getMax(ICoordinateSystem.X_AXIS);
		final int yMin = (int) cs.getMin(ICoordinateSystem.Y_AXIS);
		final int yMax = (int) cs.getMax(ICoordinateSystem.Y_AXIS);
		if (segments != null && !segments.isEmpty()) {
			graphics.setStroke(new BasicStroke(Math.round(LayerUtilities
					.pixelsToUnits(graphics, 4, true))));
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

					LayerUtilities.drawString(graphics, s.label,
							(end + begin) / 2, 0, true, false);

				} else if (startMiliSec <= xMin && endMiliSec >= xMax) {
					int begin = Math.max(startMiliSec, xMin);
					int end = Math.min(endMiliSec, xMax);
					LayerUtilities.drawString(graphics, s.label,
							(end + begin) / 2, 0, true, true);
				}
			}
			int lastBoundry = Math
					.round(segments.get(segments.size() - 1).endTime * 1000);
			if (lastBoundry < xMax) {
				graphics.drawLine(Math.max(lastBoundry, xMin), yMin,
						Math.max(lastBoundry, xMin), yMax);
			}

			if (!dragging && mousePoint != null) {
				Point2D unitsCurrent = mousePoint;//LayerUtilities.pixelsToUnits(graphics,
						//(int) Math.round(mousePoint.getX()),
						//(int) Math.round(mousePoint.getY()));
//				System.out.println(unitsCurrent.getX() + " - " + unitsCurrent.getY());
				int i = getSegmentIndex(unitsCurrent.getX());
				boolean onSegmentBoundry = false;
				boolean onSegmentLabel = false;

				if (i > 0) {
					float relativeOffset = TIME_TOLERANCE
							* cs.getDelta(ICoordinateSystem.X_AXIS);
					FontMetrics metrics = graphics.getFontMetrics(graphics
							.getFont());
					int hgt = metrics.getHeight();
					int adv = metrics.stringWidth(segments.get(i).label);
					int textOffsetX = Math.round(LayerUtilities.pixelsToUnits(
							graphics, Math.max(adv, 12), false));
					int textOffsetY = Math.round(LayerUtilities.pixelsToUnits(
							graphics, Math.max(hgt, 12), true));
					// LayerUtilities.drawString(graphics, s.label, (end +
					// begin)
					// / 2 - (textOffset / 2), -hgt/2, true, false);
					double startTime = segments.get(i).startTime * 1000;
					double endTime = segments.get(i).endTime * 1000;
					if (unitsCurrent.getX() <= startTime + relativeOffset
							|| unitsCurrent.getX() >= endTime - relativeOffset) {
						if (unitsCurrent.getX() >= endTime - relativeOffset) {
							i++;
						}
						if (i > 0 && i < segments.size()) {
							onSegmentBoundry = true;
							movingSegmentIndex = i;
						}
					}
					if (!onSegmentBoundry && i >= 0) {
						startTime = Math.max(startTime, xMin);
						endTime = Math.min(endTime, xMax);
						// @TODO: hoe kom je aan de juiste offset???
						//
//						System.out.println("Position: (" + unitsCurrent.getX() + "," + unitsCurrent.getY() + ")" );
//						System.out.println("Rectangle: (" + ((startTime + endTime) / 2
//								- (textOffsetX / 4) + 2) + "," + ((startTime + endTime) / 2 + (textOffsetX / 4) + 2) + "," + (-(textOffsetY / 2) * 3) + "," + ((textOffsetY / 2) * 3) + ")"); 
//						System.out.println("Rectangle: (" + (unitsCurrent.getX() >= (startTime + endTime) / 2
//								- (textOffsetX / 4) + 2) + "," + (unitsCurrent.getX() <= (startTime + endTime)
//								/ 2 + (textOffsetX / 4) + 2) + "," + (unitsCurrent.getY() >= -(textOffsetY / 2) * 3) + "," + (unitsCurrent.getY() <= (textOffsetY / 2) * 3) + ")"); 
						if (unitsCurrent.getX() >= (startTime + endTime) / 2
								- (textOffsetX / 4) + 2
								&& unitsCurrent.getX() <= (startTime + endTime)
										/ 2 + (textOffsetX / 4) + 2
								&& unitsCurrent.getY() >= -(textOffsetY / 2) * 3
								&& unitsCurrent.getY() <= (textOffsetY / 2) * 3) {
//							System.out.println("On label!");
							onSegmentLabel = true;
							editLabelSegmentIndex = i;
						}
					}
				}

				if (onSegmentBoundry) {
					parent.setCursor(Cursor
							.getPredefinedCursor(Cursor.E_RESIZE_CURSOR));
				} else if (onSegmentLabel) {
					parent.setCursor(Cursor
							.getPredefinedCursor(Cursor.TEXT_CURSOR));
					parent.requestFocusInWindow();
				} else {
					movingSegmentIndex = -1;
					editLabelSegmentIndex = -1;
					parent.setCursor(Cursor
							.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
					graphics.setColor(Color.LIGHT_GRAY);
					graphics.setStroke(new BasicStroke(Math
							.round(LayerUtilities.pixelsToUnits(graphics, 2,
									true))));
					graphics.drawLine((int) Math.round(unitsCurrent.getX()),
							yMin, (int) Math.round(unitsCurrent.getX()), yMax);
				}
			} else if (dragging) {
				graphics.setColor(Color.DARK_GRAY);
				graphics.setStroke(new BasicStroke(Math.round(LayerUtilities
						.pixelsToUnits(graphics, 2, true))));
				graphics.drawLine((int) Math.round(mouseDraggingPointX), yMin,
						(int) Math.round(mouseDraggingPointX), yMax);
			}
		}
		graphics.setColor(Color.black);
		
		graphics.setFont(new Font(graphics.getFont().getName(), Font.BOLD, graphics.getFont().getSize())); 
		graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);
		String label = niveau.getName().split(" ")[0].toUpperCase();	
		LayerUtilities.drawVerticalString(graphics, label.substring(0, Math.min(6, label.length())), xMin, 0, false, true);
		graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_OFF);
		graphics.setFont(new Font(graphics.getFont().getName(), Font.PLAIN, graphics.getFont().getSize())); 
		graphics.setStroke(new BasicStroke(1));
	}

	private int getSegmentIndex(double x) {
		int i = 0;
		while (i < segments.size()
				&& segments.get(i).endTime * 1000 < x
				&& !(x >= segments.get(i).startTime * 1000 && x <= segments
						.get(i).endTime * 1000)) {
			i++;
		}
		if (i < segments.size()) {
			return i;
		}
		return -1;
	}

	public String getName() {
		String name = "Segmentationlayer";
		name += " - ";
		name += segments.getLabel();
		return name;
	}

	public void mouseMoved(Point2D e) {
		mousePoint = e;
		parent.repaint();
	}

	public void addOrRemoveSegment(int pixelUnitX) {
		Graphics2D g = (Graphics2D) parent.getGraphics().create();
		g.setTransform(parent.updateTransform(g.getTransform()));
		Point2D unitsCurrent = LayerUtilities.pixelsToUnits(g,
				(int) Math.round(pixelUnitX), 0);
		float time = (float) unitsCurrent.getX();
		float relativeOffset = TIME_TOLERANCE
				* parent.getCoordinateSystem().getDelta(
						ICoordinateSystem.X_AXIS);
		int i = this.getSegmentIndex(time);

		if (i > -1 && i < segments.size()) {
			if (unitsCurrent.getX() <= (segments.get(i).startTime * 1000)
					+ relativeOffset
					|| unitsCurrent.getX() >= (segments.get(i).endTime * 1000)
							- relativeOffset) {
				if (unitsCurrent.getX() >= (segments.get(i).endTime * 1000)
						- relativeOffset) {
					i++;
				}
				if (i > 0 && i < segments.size()) {
					segments.get(i - 1).endTime = segments.get(i).endTime;
					segments.remove(i);
					SegmentationLayer.this.draw(g);
				}
			}
		}
		if (time > 0
				&& time <= LinkedFrame.getInstance().getAudioFile()
						.getLengthInMilliSeconds()) {
			time /= 1000;
			if (i < segments.size() && segments.get(i).endTime > time
					&& segments.get(i).startTime < time) {
				Segment s = new Segment(time, segments.get(i).endTime, "",
						Color.WHITE);
				segments.get(i).endTime = time;
				segments.add(i + 1, s);
			} else if (i == segments.size()) {
				Segment s = new Segment(segments.get(i - 1).endTime, time, "",
						Color.WHITE);
				segments.add(s);
			}
		} else {
			Segment s = new Segment(0, time, "", Color.WHITE);
			segments.add(s);
		}
		g.dispose();
		parent.repaint();
	}

	public void keyPressed(KeyEvent arg0) {
	}

	public void keyReleased(KeyEvent arg0) {
	}

	public void keyTyped(KeyEvent e) {
		if (editLabelSegmentIndex >= 0) {
			if ((int) e.getKeyChar() == 8) {
				if (segments.get(editLabelSegmentIndex).label.length() >= 1)
					segments.get(editLabelSegmentIndex).label = String
							.valueOf(segments.get(editLabelSegmentIndex).label.subSequence(
									0,
									segments.get(editLabelSegmentIndex).label
											.length() - 1));
			} else {
				segments.get(editLabelSegmentIndex).label = segments
						.get(editLabelSegmentIndex).label.concat(String
						.valueOf(e.getKeyChar()));
			}
			parent.repaint();
		}
	}
}
