package be.tarsos.ui.link.coordinatessystems.axis;

import java.awt.Graphics2D;

import be.tarsos.ui.link.LinkedPanel;

public class EmptyCoordinateSystemLayer extends AxisLayer {
	
	public EmptyCoordinateSystemLayer(final LinkedPanel parent, char direction) {
		super(parent, direction);
		this.name = "";
	}
	
	public void draw(Graphics2D graphics){
	}
}
