package be.hogent.tarsos.ui.link.coordinatessystems;

import java.awt.Color;
import java.awt.Graphics2D;

import be.hogent.tarsos.ui.link.LinkedPanel;
import be.hogent.tarsos.ui.link.coordinatessystems.axis.AmplitudeCoordinateSystemLayer;
import be.hogent.tarsos.ui.link.coordinatessystems.axis.AxisLayer;
import be.hogent.tarsos.ui.link.coordinatessystems.axis.CentsCoordinateSystemLayer;
import be.hogent.tarsos.ui.link.coordinatessystems.axis.EmptyCoordinateSystemLayer;
import be.hogent.tarsos.ui.link.coordinatessystems.axis.TimeCoordinateSystemLayer;

public class CoordinateSystem implements ICoordinateSystem {
	
	private Units xAxisUnits, yAxisUnits;
	
	private AxisLayer xAxis;
	private AxisLayer yAxis;
//	private LinkedPanel parent;

	private static float xMin = 0;
	private static float xMax = 10000;

	private float yMin;
	private float yMax;
	
	private final LinkedPanel parent;

	public CoordinateSystem(final LinkedPanel parent, Units xAxisUnits, Units yAxisUnits) {
		this.parent = parent;
		this.xAxisUnits = xAxisUnits;
		this.yAxisUnits = yAxisUnits;
		xAxis = getLayerForUnit(ICoordinateSystem.X_AXIS, xAxisUnits);
		yAxis = getLayerForUnit(ICoordinateSystem.Y_AXIS, yAxisUnits);
		this.yMin = Units.getMin(yAxisUnits);
		this.yMax = Units.getMax(yAxisUnits);
	}
	
	public float getDelta(char axis) {
		if (axis == ICoordinateSystem.X_AXIS) {
			return xMax - xMin;
		} else if (axis == ICoordinateSystem.Y_AXIS) {
			return yMax - yMin;
		} else {
			throw new IllegalArgumentException(
					"Axis moest be X or Y: use CoordinateSystem.X_AXIS/Y_AXIS!");
		}
	}

	// @Override
	public Units getUnitsForAxis(char axis) {
		if (axis == ICoordinateSystem.X_AXIS) {
			return xAxisUnits;
		} else if (axis == ICoordinateSystem.Y_AXIS) {
			return yAxisUnits;
		} else {
			throw new IllegalArgumentException(
					"Axis moest be X or Y: use CoordinateSystem.X_AXIS/Y_AXIS!");
		}
	}

	// @Override
	public float getMin(char axis) {
		if (axis == ICoordinateSystem.X_AXIS) {
			return xMin;
		} else if (axis == ICoordinateSystem.Y_AXIS) {
			return yMin;
		} else {
			throw new IllegalArgumentException(
					"Axis moest be X or Y: use CoordinateSystem.X_AXIS/Y_AXIS!");
		}
	}

	// @Override
	public float getMax(char axis) {
		if (axis == ICoordinateSystem.X_AXIS) {
			return xMax;
		} else if (axis == ICoordinateSystem.Y_AXIS) {
			return yMax;
		} else {
			throw new IllegalArgumentException(
					"Axis moest be X or Y: use CoordinateSystem.X_AXIS/Y_AXIS!");
		}
	}

	// @Override
	public void setMax(char axis, float value) {
		if (axis == ICoordinateSystem.X_AXIS) {
			xMax = value;
		} else if (axis == ICoordinateSystem.Y_AXIS) {
			yMax = value;
		} else {
			throw new IllegalArgumentException(
					"Axis moest be X or Y: use CoordinateSystem.X_AXIS/Y_AXIS!");
		}
	}

	// @Override
	public void setMin(char axis, float value) {
		if (axis == ICoordinateSystem.X_AXIS) {
			xMin = value;
		} else if (axis == ICoordinateSystem.Y_AXIS) {
			yMin = value;
		} else {
			throw new IllegalArgumentException(
					"Axis moest be X or Y: use CoordinateSystem.X_AXIS/Y_AXIS!");
		}
	}

	public void draw(Graphics2D g) {
		xAxis.draw(g);
		yAxis.draw(g);
		g.setColor(Color.BLACK);
		g.drawRect(Math.round(getMin(ICoordinateSystem.X_AXIS)), 
				Math.round(getMin(ICoordinateSystem.Y_AXIS)), 
				Math.round(getDelta(ICoordinateSystem.X_AXIS)), 
				Math.round(getDelta(ICoordinateSystem.Y_AXIS)));
	}
	
	
	//@TODO: switch case
	private AxisLayer getLayerForUnit(char direction, Units unit){
		if (direction != ICoordinateSystem.X_AXIS && direction != ICoordinateSystem.Y_AXIS){
			throw new IllegalArgumentException("ERROR: A AxisLayer must have a X or Y direction!");
		}
		if (unit==Units.TIME){
			return new TimeCoordinateSystemLayer(parent, direction);
		} else if (unit==Units.FREQUENCY){
			return new CentsCoordinateSystemLayer(parent, direction);
		} else if (unit==Units.AMPLITUDE){
			return new AmplitudeCoordinateSystemLayer(parent, direction);
		} else if (unit==Units.NONE){
			return new EmptyCoordinateSystemLayer(parent, direction);
		} else {
			throw new IllegalArgumentException("ERROR: Please choose a Unit available in the Units enum!");
		}
	}
}
