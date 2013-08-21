package be.hogent.tarsos.ui.link.coordinatessystems;

public abstract class AbstractTimeCoordinateSystem implements CoordinateSystem {
	
	private Units xAxisUnits = Units.TIME_HH_MM_SS;
	private Units yAxisUnits;
	
	private float xMin = 0;
	private float xMax = 10000;
	
	private float yMin = 0;
	private float yMax = 0;
	
	public AbstractTimeCoordinateSystem(Units yAxisUnits, float yMin, float yMax){
		this.yAxisUnits = yAxisUnits;
		this.yMin = yMin;
		this.yMax = yMax;
	}
	
	@Override
	public float getDelta(char axis){
		if (axis == CoordinateSystem.X_AXIS){
			return xMax-xMin;
		} else if (axis == CoordinateSystem.Y_AXIS){
			return yMax-yMin;
		} else {
			throw new IllegalArgumentException("Axis moest be X or Y: use CoordinateSystem.X_AXIS/Y_AXIS!");
		}
	}
	
	@Override
	public Units getUnitsForAxis(char axis){
		if (axis == CoordinateSystem.X_AXIS){
			return xAxisUnits;
		} else if (axis == CoordinateSystem.Y_AXIS){
			return yAxisUnits;
		} else {
			throw new IllegalArgumentException("Axis moest be X or Y: use CoordinateSystem.X_AXIS/Y_AXIS!");
		}
	}
	
	@Override
	public float getMin(char axis){
		if (axis == CoordinateSystem.X_AXIS){
			return xMin;
		} else if (axis == CoordinateSystem.Y_AXIS){
			return yMin;
		} else {
			throw new IllegalArgumentException("Axis moest be X or Y: use CoordinateSystem.X_AXIS/Y_AXIS!");
		}
	}
	
	@Override
	public float getMax(char axis){
		if (axis == CoordinateSystem.X_AXIS){
			return xMax;
		} else if (axis == CoordinateSystem.Y_AXIS){
			return yMax;
		} else {
			throw new IllegalArgumentException("Axis moest be X or Y: use CoordinateSystem.X_AXIS/Y_AXIS!");
		}
	}
	
	@Override
	public void setMax(char axis, float value){
		if (axis == CoordinateSystem.X_AXIS){
			xMax = value;
		} else if (axis == CoordinateSystem.Y_AXIS){
			yMax = value;
		} else {
			throw new IllegalArgumentException("Axis moest be X or Y: use CoordinateSystem.X_AXIS/Y_AXIS!");
		}
	}
	
	@Override
	public void setMin(char axis, float value){
		if (axis == CoordinateSystem.X_AXIS){
			xMin = value;
		} else if (axis == CoordinateSystem.Y_AXIS){
			yMin = value;
		} else {
			throw new IllegalArgumentException("Axis moest be X or Y: use CoordinateSystem.X_AXIS/Y_AXIS!");
		}
	}
	
	
}
