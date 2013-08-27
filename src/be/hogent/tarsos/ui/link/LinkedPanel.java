package be.hogent.tarsos.ui.link;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
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

import be.hogent.tarsos.ui.link.coordinatessystems.CoordinateSystem;
import be.hogent.tarsos.ui.link.layers.BackgroundLayer;
import be.hogent.tarsos.ui.link.layers.Layer;
import be.hogent.tarsos.ui.link.layers.LayerUtilities;
import be.hogent.tarsos.ui.link.layers.coordinatesystemlayers.CoordinateSystemLayer;
import be.hogent.tarsos.ui.link.layers.featurelayers.FeatureLayer;

public class LinkedPanel extends JPanel {

	private static final long serialVersionUID = -5055686566048886896L;

	private BackgroundLayer backgroundLayer;
	private List<FeatureLayer> layers;
	private CoordinateSystemLayer csLayer;

	private final ViewPort viewPort;
	private CoordinateSystem cs;

	// private boolean isDrawing = false;

	public CoordinateSystem getCoordinateSystem() {
		return cs;
	}

	public void setCoordinateSystem(CoordinateSystem cs) {
		this.cs = cs;
		this.csLayer = new CoordinateSystemLayer(this,
				cs.getUnitsForAxis(CoordinateSystem.X_AXIS),
				cs.getUnitsForAxis(CoordinateSystem.Y_AXIS));
	}

	public ViewPort getViewPort() {
		return viewPort;
	}

	public LinkedPanel(CoordinateSystem coordinateSystem) {
		super();
		layers = new ArrayList<FeatureLayer>();
		setCoordinateSystem(coordinateSystem);
		viewPort = new ViewPort(this);
		DragListener dragListener = new DragListener(this);
		ZoomListener zoomListener = new ZoomListener();
		addMouseWheelListener(zoomListener);
		addMouseListener(dragListener);
		addMouseMotionListener(dragListener);
		this.setVisible(true);
	}

	public void setDefaultBackgroundLayer() {
		this.backgroundLayer = new BackgroundLayer(this);
	}

	public void setBackgroundLayer(Color c) {
		this.backgroundLayer = new BackgroundLayer(this, c);
	}

	public void addLayer(FeatureLayer fl) {
		this.layers.add(fl);

	}

	private class ZoomListener implements MouseWheelListener {

		public void mouseWheelMoved(MouseWheelEvent arg0) {
			int amount = arg0.getWheelRotation() * arg0.getScrollAmount();
			viewPort.zoom(amount, arg0.getPoint());
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
				Graphics2D graphics = (Graphics2D) panel.getGraphics();
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
				// graphics.dispose();
				// System.out.println("Mouse dragged over (" + millisecondAmount
				// + " seconds," + centAmount + " cents)");
			}
		}

		@Override
		public void mouseMoved(MouseEvent e) {
		}
	}

	private AffineTransform getTransform() {
		double xDelta = cs.getDelta(CoordinateSystem.X_AXIS);
		double yDelta = cs.getDelta(CoordinateSystem.Y_AXIS);

		AffineTransform transform = new AffineTransform();
		// System.out.println(this.getHeight() + " - " + this.getWidth());
		transform.translate(0, getHeight());
		transform.scale(getWidth() / xDelta, -getHeight() / yDelta);
		transform.translate(-cs.getMin(CoordinateSystem.X_AXIS),
				-cs.getMin(CoordinateSystem.Y_AXIS));

		return transform;
	}

	// @Override
	// public void paint(final Graphics g){
	// Graphics2D graphics = (Graphics2D) g;
	//
	// graphics.setTransform(getTransform());
	// // graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
	// // RenderingHints.VALUE_ANTIALIAS_ON);
	// // graphics.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS,
	// // RenderingHints.VALUE_FRACTIONALMETRICS_ON);
	//
	// backgroundLayer.draw(graphics);
	// if (!layers.isEmpty()) {
	// for (Layer layer : layers) {
	// layer.draw(graphics);
	// }
	// }
	// csLayer.draw(graphics);
	//
	// // TODO in layer?
	// drawIndicator(graphics);
	//
	// graphics.dispose();
	// g.dispose();
	// }

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
		csLayer.draw(graphics);
		drawIndicator(graphics);

	}

	public AffineTransform updateTransform(AffineTransform transform) {
		double xDelta = cs.getDelta(CoordinateSystem.X_AXIS);
		double yDelta = cs.getDelta(CoordinateSystem.Y_AXIS);

		// System.out.println(this.getHeight() + " - " + this.getWidth());
		transform.translate(0, getHeight());
		transform.scale(getWidth() / xDelta, -getHeight() / yDelta);
		transform.translate(-cs.getMin(CoordinateSystem.X_AXIS),
				-cs.getMin(CoordinateSystem.Y_AXIS));
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
	
	public ArrayList<String> getLayers(){
		ArrayList<String> layers = new ArrayList<String>();
		layers.add(this.backgroundLayer.getName());
		for (FeatureLayer fl : this.layers){
			layers.add(fl.getName());
		}
		layers.add(this.csLayer.getName());
		return layers;
	}
}
