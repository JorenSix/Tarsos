package be.tarsos.ui.link.coordinatessystems;

import java.awt.Color;
import java.awt.Graphics2D;

import be.tarsos.ui.link.LinkedFrame;
import be.tarsos.ui.link.LinkedPanel;
import be.tarsos.ui.link.coordinatessystems.axis.AmplitudeCoordinateSystemLayer;
import be.tarsos.ui.link.coordinatessystems.axis.AxisLayer;
import be.tarsos.ui.link.coordinatessystems.axis.EmptyCoordinateSystemLayer;
import be.tarsos.ui.link.coordinatessystems.axis.FrequencyCoordinateSystemLayer;
import be.tarsos.ui.link.coordinatessystems.axis.TimeCoordinateSystemLayer;
import be.tarsos.ui.link.layers.LayerUtilities;

public class CoordinateSystem implements ICoordinateSystem {
	
	private Quantity xAxisUnits, yAxisUnits;
	
	private AxisLayer xAxis;
	private AxisLayer yAxis;
//	private LinkedFeaturePanel parent;

	private static float xMin = 0;
	private static float xMax = 10000;

	private float yMin;
	private float yMax;
	
	private final LinkedPanel parent;

	public CoordinateSystem(final LinkedPanel parent, Quantity xAxisUnits, Quantity yAxisUnits) {
		this.parent = parent;
		this.xAxisUnits = xAxisUnits;
		this.yAxisUnits = yAxisUnits;
		xAxis = getLayerForUnit(ICoordinateSystem.X_AXIS, xAxisUnits);
		yAxis = getLayerForUnit(ICoordinateSystem.Y_AXIS, yAxisUnits);
		this.yMin = yAxisUnits.getMin();
		this.yMax = yAxisUnits.getMax();
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
	public Quantity getQuantityForAxis(char axis) {
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
		int x = LinkedFrame.getInstance().getMouseX();
		double convertedX = LayerUtilities.pixelsToUnits(g, x, 0).getX();
		g.setColor(Color.RED);
		g.drawLine((int)Math.round(convertedX), Math.round(this.getMin(Y_AXIS)), (int)Math.round(convertedX), Math.round(this.getMax(Y_AXIS)));
//		System.out.println("yMin: " + getMin(Y_AXIS) + " - yMax: " + getMax(Y_AXIS));
	}
	
	
	//@TODO: switch case
	private AxisLayer getLayerForUnit(char direction, Quantity unit){
		if (direction != ICoordinateSystem.X_AXIS && direction != ICoordinateSystem.Y_AXIS){
			throw new IllegalArgumentException("ERROR: A AxisLayer must have a X or Y direction!");
		}
		if (unit==Quantity.TIME){
			return new TimeCoordinateSystemLayer(parent, direction);
		} else if (unit==Quantity.FREQUENCY){
			return new FrequencyCoordinateSystemLayer(parent, direction);
		} else if (unit==Quantity.AMPLITUDE){
			return new AmplitudeCoordinateSystemLayer(parent, direction);
		} else if (unit==Quantity.NONE){
			return new EmptyCoordinateSystemLayer(parent, direction);
		} else {
			throw new IllegalArgumentException("ERROR: Please choose a Unit available in the Units enum!");
		}
	}
}
