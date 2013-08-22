package be.hogent.tarsos.ui.link;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.JPanel;

import be.hogent.tarsos.ui.link.ViewPort.ViewPortChangedListener;
import be.hogent.tarsos.ui.link.coordinatessystems.CoordinateSystem;
import be.hogent.tarsos.ui.link.coordinatessystems.Units;
import be.hogent.tarsos.ui.link.layers.BackgroundLayer;
import be.hogent.tarsos.ui.link.layers.Layer;
import be.hogent.tarsos.ui.link.layers.LayerUtilities;
import be.hogent.tarsos.ui.link.layers.coordinatesystemlayers.CoordinateSystemLayer;
import be.hogent.tarsos.ui.link.layers.featurelayers.ConstantQLayer;
import be.hogent.tarsos.ui.link.layers.featurelayers.FeatureLayer;
import be.hogent.tarsos.ui.link.layers.featurelayers.PitchContourLayer;
import be.hogent.tarsos.ui.link.layers.featurelayers.WaveFormLayer;

public class LinkedPanel extends JPanel {

	private static final long serialVersionUID = -5055686566048886896L;

	private BackgroundLayer backgroundLayer;
	private List<FeatureLayer> layers;
	private CoordinateSystemLayer csLayer;

	private final ViewPort viewPort;
	private CoordinateSystem cs;

	public CoordinateSystem getCoordinateSystem() {
		return cs;
	}

	public void setCoordinateSystem(CoordinateSystem cs) {
		this.cs = cs;
	}

	public ViewPort getViewPort() {
		return viewPort;
	}

	public LinkedPanel(CoordinateSystem coordinateSystem) {
		this.setPreferredSize(new Dimension(480, 640));
		this.cs = coordinateSystem;
		viewPort = new ViewPort(this);
		DragListener dragListener = new DragListener(this);
		ZoomListener zoomListener = new ZoomListener();

		addMouseWheelListener(zoomListener);
		addMouseListener(dragListener);
		addMouseMotionListener(dragListener);
		layers = new ArrayList<FeatureLayer>();
	}

	public void addDefaultLayers() {

		this.backgroundLayer = new BackgroundLayer(this);

//		layers.add(new ConstantQLayer(this, 32768, 16384));
//		layers.add(new PitchContourLayer(this, 2048, 512));

		this.csLayer = new CoordinateSystemLayer(this, Units.TIME_SSS,
				Units.FREQUENCY_CENTS);
	}

	public void addWaveFormLayer() {
		this.backgroundLayer = new BackgroundLayer(this);
		
//		layers.add(new WaveFormLayer(this));
		
		this.csLayer = new CoordinateSystemLayer(this, Units.TIME_SSS,
				Units.AMPLITUDE);
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
			System.out.println("Pressed!!");
		}

		@Override
		public void mouseReleased(MouseEvent e) {
			previousPoint = null;
			System.out.println("Released!!");
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
				System.out.println("Mouse dragged over (" + millisecondAmount
						+ " seconds," + centAmount + " cents)");
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
		transform.translate(0, getHeight());
		transform.scale(getWidth() / xDelta, -getHeight() / yDelta);
		transform.translate(-cs.getMin(CoordinateSystem.X_AXIS),
				-cs.getMin(CoordinateSystem.Y_AXIS));

		return transform;
	}

	@Override
	public void paintComponent(final Graphics g) {
		super.paintComponent(g);
		Graphics2D graphics = (Graphics2D) g;

		graphics.setTransform(getTransform());

		backgroundLayer.draw(graphics);
		if (!layers.isEmpty()) {
			for (Layer layer : layers) {
				layer.draw(graphics);
			}
		}
		csLayer.draw(graphics);

		// TODO in layer?
		drawIndicator(graphics);
		graphics.dispose();
		g.dispose();
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

}
