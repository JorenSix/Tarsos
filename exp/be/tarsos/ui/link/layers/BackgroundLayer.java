package be.tarsos.ui.link.layers;

import java.awt.Color;
import java.awt.Graphics2D;

import be.tarsos.ui.link.LinkedPanel;
import be.tarsos.ui.link.coordinatessystems.ICoordinateSystem;

public class BackgroundLayer implements Layer{

	protected final LinkedPanel parent;
	protected Color color;

	public BackgroundLayer(final LinkedPanel parent){
		this.parent = parent;
		this.color = Color.WHITE;
		
	}
	
	public BackgroundLayer(final LinkedPanel parent, Color color){
		this.parent = parent;
		this.color = color;
	}
	
	public void draw(final Graphics2D graphics) {
		//draw background
		ICoordinateSystem cs = parent.getCoordinateSystem();
		graphics.setColor(color);
		
		graphics.fillRect(
				Math.round(cs.getMin(ICoordinateSystem.X_AXIS)), 
				Math.round(cs.getMin(ICoordinateSystem.Y_AXIS)), 
				Math.round(cs.getDelta(ICoordinateSystem.X_AXIS)), 
				Math.round(cs.getDelta(ICoordinateSystem.Y_AXIS)));
//		graphics.setTransform();
//		graphics.fillRect(0,0,500,500);
	}

	public String getName() {
		return "Background layer";// - " + this.color.toString();
	}
}
