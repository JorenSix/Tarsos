package be.hogent.tarsos.ui.link.coordinatessystems;

public interface CoordinateSystem {

	public static final char X_AXIS = 'X';
	public static final char Y_AXIS = 'Y';
	
	public float getMin(char axis);
	public float getMax(char axis);
	
	public Units getUnitsForAxis(char axis);
	
	public void setMax(char axis, float value);
	public void setMin(char axis, float value);
	
	public float getDelta(char axis);
}
