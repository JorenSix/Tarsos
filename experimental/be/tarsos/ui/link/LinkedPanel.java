package be.tarsos.ui.link;

import java.awt.Color;
import java.awt.Cursor;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.event.ComponentEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JPanel;
import javax.swing.SwingUtilities;

import be.tarsos.ui.link.coordinatessystems.CoordinateSystem;
import be.tarsos.ui.link.coordinatessystems.ICoordinateSystem;
import be.tarsos.ui.link.coordinatessystems.Quantity;
import be.tarsos.ui.link.layers.BackgroundLayer;
import be.tarsos.ui.link.layers.Layer;
import be.tarsos.ui.link.layers.LayerUtilities;
import be.tarsos.ui.link.layers.featurelayers.FeatureLayer;
import be.tarsos.ui.link.layers.segmentationlayers.SegmentationLayer;

public class LinkedPanel extends JPanel {

	private static final long serialVersionUID = -5055686566048886896L;

	private BackgroundLayer backgroundLayer;
	private List<Layer> layers;

	private ViewPort viewPort;
	private ICoordinateSystem cs;

	private SegmentationLayer mouseMovedListener;
	
	private LinkedPanel upperPanel;
	private LinkedPanel lowerPanel;

	public ViewPort getViewPort() {
		return viewPort;
	}

	public ICoordinateSystem getCoordinateSystem() {
		return cs;
	}

	public LinkedPanel() {
		super();
		layers = new ArrayList<Layer>();
	}

	public void initialise(Quantity xAxisUnits, Quantity yAxisUnits) {
		this.setDefaultBackgroundLayer();
		this.cs = new CoordinateSystem(this, xAxisUnits, yAxisUnits);
		viewPort = new ViewPort(cs);
		DragListener dragListener;
		if (yAxisUnits == Quantity.AMPLITUDE || yAxisUnits == Quantity.NONE) {
			dragListener = new HorizontalDragListener(this);
		} else {
			dragListener = new DragListener(this);
		}
		ZoomListener zoomListener = new ZoomListener();
		addMouseWheelListener(zoomListener);
		addMouseListener(dragListener);
		addMouseMotionListener(dragListener);
		this.setVisible(true);
	}

	private void setDefaultBackgroundLayer() {
		this.backgroundLayer = new BackgroundLayer(this);
	}

	public void setBackgroundLayer(Color c) {
		this.backgroundLayer = new BackgroundLayer(this, c);
	}

	public void addLayer(Layer fl) {
		this.layers.add(fl);
		if (fl instanceof SegmentationLayer) {
			mouseMovedListener = (SegmentationLayer) fl;
		}
	}

	private class ZoomListener implements MouseWheelListener {

		public void mouseWheelMoved(MouseWheelEvent arg0) {
			int amount = arg0.getWheelRotation() * arg0.getScrollAmount();
			viewPort.zoom(amount, arg0.getPoint());
		}
	}

	private class HorizontalDragListener extends DragListener{

		private SegmentationLayer sl;

		private HorizontalDragListener(LinkedPanel p) {
			super(p);
		}

		@Override
		public void mouseDragged(MouseEvent e) {
			Graphics2D graphics = (Graphics2D) panel.getGraphics().create();
			graphics.setTransform(panel.getTransform());
			if (sl == null){
				for (Layer l : layers) {
					if (l instanceof SegmentationLayer) {
						sl = ((SegmentationLayer) l);
						sl.setDragging(true);
					}
				}
			}
			if (LinkedPanel.this.getCursor().getType() == Cursor.E_RESIZE_CURSOR
					&& sl != null) {
				sl.dragSegment(graphics, e);
			} else if (previousPoint != null) {
				Point2D unitsCurrent = LayerUtilities.pixelsToUnits(graphics,
						e.getX(), (int) previousPoint.getY());
				Point2D unitsPrevious = LayerUtilities.pixelsToUnits(graphics,
						(int) previousPoint.getX(), (int) previousPoint.getY());
				float millisecondAmount = (float) (unitsPrevious.getX() - unitsCurrent
						.getX());
				previousPoint = e.getPoint();
				viewPort.drag(millisecondAmount, 0);
			}
			graphics.dispose();
		}

		@Override
		public void mouseReleased(MouseEvent e) {
			previousPoint = null;
			if (sl != null) {
				sl.setDragging(false);
				sl = null;
				repaint();
			}
		}
	}

	private class DragListener extends MouseAdapter {

		LinkedPanel panel;
		Point previousPoint;

		private DragListener(LinkedPanel p) {
			panel = p;
			previousPoint = null;
		}

		@Override
		public void mouseClicked(MouseEvent e) {
			if (LinkedPanel.this.mouseMovedListener != null
					&& SwingUtilities.isRightMouseButton(e)) {
				LinkedPanel.this.mouseMovedListener.addOrRemoveSegment(e.getX());
			}
			if (LinkedPanel.this.mouseMovedListener != null
					&& SwingUtilities.isLeftMouseButton(e)) {
				LinkedPanel.this.mouseMovedListener.extendSegment(e.getX());
			}
		}

		@Override
		public void mousePressed(MouseEvent e) {
			previousPoint = e.getPoint();

			// System.out.println("Pressed!!");
		}

		@Override
		public void mouseReleased(MouseEvent e) {
			previousPoint = null;
			// System.out.println("Released!!");
		}

		@Override
		public void mouseDragged(MouseEvent e) {
			if (previousPoint != null) {
				Graphics2D graphics = (Graphics2D) panel.getGraphics().create();
				graphics.setTransform(panel.getTransform());
				Point2D unitsCurrent = LayerUtilities.pixelsToUnits(graphics,
						e.getX(), e.getY());
				Point2D unitsPrevious = LayerUtilities.pixelsToUnits(graphics,
						(int) previousPoint.getX(), (int) previousPoint.getY());
				float millisecondAmount = (float) (unitsPrevious.getX() - unitsCurrent
						.getX());
				float centAmount = (float) (unitsPrevious.getY() - unitsCurrent
						.getY());
				previousPoint = e.getPoint();
				viewPort.drag(millisecondAmount, centAmount);
				graphics.dispose();
			}
		}

		@Override
		public void mouseMoved(MouseEvent e) {
//			if (LinkedFeaturePanel.this.mouseMovedListener != null) {
//				mouseMovedListener.mouseMoved(e);
//			}
		}
	}

	private AffineTransform getTransform() {
		double xDelta = cs.getDelta(ICoordinateSystem.X_AXIS);
		double yDelta = cs.getDelta(ICoordinateSystem.Y_AXIS);

		AffineTransform transform = new AffineTransform();
		// System.out.println(this.getHeight() + " - " + this.getWidth());
		transform.translate(0, getHeight());
		transform.scale(getWidth() / xDelta, -getHeight() / yDelta);
		transform.translate(-cs.getMin(ICoordinateSystem.X_AXIS),
				-cs.getMin(ICoordinateSystem.Y_AXIS));

		return transform;
	}

	@Override
	public void paintComponent(final Graphics g) {
		super.paintComponent(g);
		final Graphics2D graphics = (Graphics2D) g.create();
		graphics.setTransform(this.updateTransform(graphics.getTransform()));
		backgroundLayer.draw(graphics);
		if (!layers.isEmpty()) {
			for (Layer layer : layers) {
				layer.draw(graphics);
			}
		}
		cs.draw(graphics);
		drawIndicator(graphics);
		graphics.dispose();
		g.dispose();
	}
	
	public void mouseMoved(Point2D e) {
		if (LinkedPanel.this.mouseMovedListener != null) {
			mouseMovedListener.mouseMoved(e);
		}
	}

	public AffineTransform updateTransform(AffineTransform transform) {
		double xDelta = cs.getDelta(ICoordinateSystem.X_AXIS);
		double yDelta = cs.getDelta(ICoordinateSystem.Y_AXIS);

		// System.out.println(this.getHeight() + " - " + this.getWidth());
		transform.translate(0, getHeight());
		transform.scale(getWidth() / xDelta, -getHeight() / yDelta);
		transform.translate(-cs.getMin(ICoordinateSystem.X_AXIS),
				-cs.getMin(ICoordinateSystem.Y_AXIS));
		return transform;
	}

	private void drawIndicator(Graphics2D graphics) {
		// draw indicator lines
		/*
		 * if(indicator!=null){ float horizontal =
		 * LayerUtilities.unitsToPixels(graphics,4, true); float
		 * dashHorizontal[] = {horizontal}; BasicStroke dashedHorizontal = new
		 * BasicStroke
		 * (1.0f,BasicStroke.CAP_BUTT,BasicStroke.JOIN_MITER,horizontal,
		 * dashHorizontal, 0.0f); graphics.setStroke(dashedHorizontal);
		 * graphics.setColor(Color.red);
		 * graphics.drawLine(minTimeInMilliseconds, (int) indicator.getY(),
		 * maxTimeInMilliseconds, (int)indicator.getY()); float vertical =
		 * LayerUtilities.unitsToPixels(graphics,4, false); float dashVertical[]
		 * = {vertical}; BasicStroke dashedVertical = new
		 * BasicStroke(1.0f,BasicStroke
		 * .CAP_BUTT,BasicStroke.JOIN_MITER,vertical, dashVertical, 0.0f);
		 * graphics.setStroke(dashedVertical); graphics.drawLine((int)
		 * indicator.getX(), minFrequencyInCents , (int) indicator.getX(),
		 * maxFrequencyInCents); }
		 */
	}

	public void initialiseLayers() {
		for (Layer l : layers) {
			if (l instanceof FeatureLayer) {
				((FeatureLayer) l).initialise();
			}
		}
	}

	public void calculateLayers() {
		for (Layer l : layers) {
			if (l instanceof FeatureLayer) {
				((FeatureLayer) l).run();
			}
		}
	}

	public void componentHidden(ComponentEvent arg0) {
		// TODO Auto-generated method stub

	}

	public void componentMoved(ComponentEvent arg0) {
		// TODO Auto-generated method stub

	}

	public void componentShown(ComponentEvent arg0) {
		// TODO Auto-generated method stub
	}

	public ArrayList<String> getLayerNames() {
		ArrayList<String> layers = new ArrayList<String>();
		// layers.add(this.backgroundLayer.getName());
		for (Layer fl : this.layers) {
			layers.add(fl.getName());
		}
		return layers;
	}
	
	public List<Layer> getLayers() {
		return layers;
	}

	public void deleteLayer(Layer l) {
		layers.remove(l);
	}

	public LinkedPanel getUpperPanel() {
		return upperPanel;
	}

	public void setUpperPanel(LinkedPanel upperPanel) {
		this.upperPanel = upperPanel;
	}

	public LinkedPanel getLowerPanel() {
		return lowerPanel;
	}

	public void setLowerPanel(LinkedPanel lowerPanel) {
		this.lowerPanel = lowerPanel;
	}
	
	
}
