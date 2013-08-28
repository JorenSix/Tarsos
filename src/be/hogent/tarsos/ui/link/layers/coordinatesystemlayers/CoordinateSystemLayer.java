package be.hogent.tarsos.ui.link.layers.coordinatesystemlayers;

import java.awt.Color;
import java.awt.Graphics2D;

import be.hogent.tarsos.ui.link.LinkedPanel;
import be.hogent.tarsos.ui.link.coordinatessystems.CoordinateSystem;
import be.hogent.tarsos.ui.link.coordinatessystems.Units;
import be.hogent.tarsos.ui.link.layers.Layer;

public final class CoordinateSystemLayer implements Layer {

	private AxisLayer xAxis;
	private AxisLayer yAxis;
	private final LinkedPanel parent;
	
	public String getName(){
		return xAxis.getName() + "/" + yAxis.getName();
	}
	
	public CoordinateSystemLayer(final LinkedPanel parent, Units xAxisUnits, Units yAxisUnits){
		this.parent = parent;
		xAxis = getLayerForUnit(CoordinateSystem.X_AXIS, xAxisUnits);
		yAxis = getLayerForUnit(CoordinateSystem.Y_AXIS, yAxisUnits);
	}
	
//	@Override
	public void draw(Graphics2D graphics) {
		CoordinateSystem cs = parent.getCoordinateSystem();
		xAxis.draw(graphics);
		yAxis.draw(graphics);
		graphics.setColor(Color.BLACK);
		graphics.drawRect(Math.round(cs.getMin(CoordinateSystem.X_AXIS)), 
				Math.round(cs.getMin(CoordinateSystem.Y_AXIS)), 
				Math.round(cs.getDelta(CoordinateSystem.X_AXIS)), 
				Math.round(cs.getDelta(CoordinateSystem.Y_AXIS)));
	}
	
	private AxisLayer getLayerForUnit(char direction, Units unit){
		if (direction != CoordinateSystem.X_AXIS && direction != CoordinateSystem.Y_AXIS){
			throw new IllegalArgumentException("ERROR: A AxisLayer must have a X or Y direction!");
		}
		if (unit==Units.TIME_SSS){
			return new TimeCoordinateSystemLayer(parent, direction);
		} else if (unit==Units.TIME_HH_MM_SS){
			//TODO
			return null;
		} else if (unit==Units.FREQUENCY_CENTS){
			return new CentsCoordinateSystemLayer(parent, direction);
		} else if (unit==Units.FREQUENCY_NOTES){
			//TODO
			return null;
		} else if (unit==Units.FREQUENCY_HZ){
			//TODO
			return null;
		} else if (unit==Units.AMPLITUDE){
			return new AmplitudeCoordinateSystemLayer(parent, direction);
		} else if (unit==Units.NONE){
			return new EmptyCoordinateSystemLayer(parent, direction);
		} else {
			throw new IllegalArgumentException("ERROR: Please choose a Unit available in the Units enum!");
		}
	}

}
