package be.hogent.tarsos.ui.link.layers;

import java.awt.Color;
import java.awt.Graphics2D;

import be.hogent.tarsos.ui.link.LinkedPanel;
import be.hogent.tarsos.ui.link.coordinatessystems.CoordinateSystem;

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
	
	public void draw(Graphics2D graphics) {
		//draw background
		CoordinateSystem cs = parent.getCoordinateSystem();
		graphics.setColor(color);
		
		graphics.fillRect(
				Math.round(cs.getMin(CoordinateSystem.X_AXIS)), 
				Math.round(cs.getMin(CoordinateSystem.Y_AXIS)), 
				Math.round(cs.getDelta(CoordinateSystem.X_AXIS)), 
				Math.round(cs.getDelta(CoordinateSystem.Y_AXIS)));
	}
}
