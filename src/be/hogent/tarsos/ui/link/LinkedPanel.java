package be.hogent.tarsos.ui.link;

import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridLayout;
import java.awt.Point;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JFrame;
import javax.swing.JPanel;

import be.hogent.tarsos.ui.link.ViewPort.ViewPortChangedListener;

public class LinkedPanel extends JPanel implements ViewPortChangedListener {

	private static final long serialVersionUID = -5055686566048886896L;
	
	
	
	private final List<Layer> layers;
	
	private final ViewPort viewPort;
	
	
	public LinkedPanel(){
		this.setPreferredSize(new Dimension(480,640));
		
		viewPort = ViewPort.getInstance();
		
		DragListener dragListener = new DragListener(this);
		ZoomListener zoomListener = new ZoomListener();
		
		addMouseWheelListener(zoomListener);		
		addMouseListener(dragListener);
		addMouseMotionListener(dragListener);
		
		layers = new ArrayList<Layer>();
		layers.add(new BackgroundLayer());
		
		layers.add(new ConstantQLayer());
		layers.add(new PitchContourLayer());
		layers.add(new CentsLabelLayer());
		
		viewPort.addViewPortChangedListener(this);
	}
	
	

	
	private static class ZoomListener implements MouseWheelListener{		

		public void mouseWheelMoved(MouseWheelEvent arg0) {
			int amount = arg0.getWheelRotation() * arg0.getScrollAmount();		
			ViewPort.getInstance().zoom(amount,arg0.getPoint());
		}
	}
	

	private static class DragListener extends MouseAdapter {
		
		LinkedPanel panel;
		Point previousPoint;
		
		private DragListener(LinkedPanel p){
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
			if(previousPoint!=null){
				Graphics2D graphics = (Graphics2D) panel.getGraphics();
				graphics.setTransform(panel.getTransform());
				Point2D unitsCurrent = LayerUtilities.pixelsToUnits(graphics,e.getX(), e.getY());
				Point2D unitsPrevious = LayerUtilities.pixelsToUnits(graphics,(int) previousPoint.getX(), (int) previousPoint.getY());
				float millisecondAmount = (float) (unitsPrevious.getX()- unitsCurrent.getX());
				float centAmount = (float) (unitsPrevious.getY()- unitsCurrent.getY());
				previousPoint = e.getPoint();
				ViewPort.getInstance().drag(millisecondAmount, centAmount);
				System.out.println("Mouse dragged over (" + millisecondAmount + " seconds," + centAmount + " cents)");
			}
		}

		@Override
		public void mouseMoved(MouseEvent e) {			
		}
	}	

	
	protected AffineTransform getTransform(){
		double timeDelta = viewPort.getTimeDelta();
		double frequencyDelta = viewPort.getFrequencyDelta();
		
		AffineTransform transform = new AffineTransform();
		transform.translate(0, getHeight());		
		transform.scale(getWidth()/timeDelta,-getHeight()/frequencyDelta);
		transform.translate(-viewPort.getMinTime(), -viewPort.getMinFrequencyInCents());
		
		return transform;
	}
	
	@Override
	public void paint(final Graphics g) {
		Graphics2D graphics = (Graphics2D) g;	
		
		graphics.setTransform(getTransform());
		
		for(Layer layer:layers){
			layer.draw(graphics);
		}
		//in layer?
		drawIndicator(graphics);
	}
	
	
	
	private void drawIndicator(Graphics2D graphics){
		//draw indicator lines
		/*
		if(indicator!=null){
			float horizontal = LayerUtilities.unitsToPixels(graphics,4, true);
			float dashHorizontal[] = {horizontal};
			BasicStroke dashedHorizontal = new BasicStroke(1.0f,BasicStroke.CAP_BUTT,BasicStroke.JOIN_MITER,horizontal, dashHorizontal, 0.0f);
			graphics.setStroke(dashedHorizontal);
			graphics.setColor(Color.red);
			graphics.drawLine(minTimeInMilliseconds, (int) indicator.getY(), maxTimeInMilliseconds,  (int)indicator.getY());
			float vertical = LayerUtilities.unitsToPixels(graphics,4, false);
			float dashVertical[] = {vertical};
			BasicStroke dashedVertical = new BasicStroke(1.0f,BasicStroke.CAP_BUTT,BasicStroke.JOIN_MITER,vertical, dashVertical, 0.0f);
			graphics.setStroke(dashedVertical);
			graphics.drawLine((int) indicator.getX(), minFrequencyInCents , (int) indicator.getX(),  maxFrequencyInCents);
		}
		*/
	}	
	

	public void viewPortChanged(ViewPort newViewPort) {
		invalidate();
		repaint();
	}
	
	
	public static void main(String...strings){
		JFrame frame = new JFrame();
		frame.setLayout(new GridLayout(0, 1));
		frame.add(new LinkedPanel());
		frame.add(new LinkedPanel());
		frame.pack();
		frame.setLocationRelativeTo(null);
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.setVisible(true);
	}
}
