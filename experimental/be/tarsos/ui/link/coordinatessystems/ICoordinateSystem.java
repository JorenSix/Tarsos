package be.tarsos.ui.link.coordinatessystems;

import java.awt.Graphics2D;

public interface ICoordinateSystem {

	public static final char X_AXIS = 'X';
	public static final char Y_AXIS = 'Y';
	
	public float getMin(char axis);
	public float getMax(char axis);
	
	public Quantity getQuantityForAxis(char axis);
	
	public void setMax(char axis, float value);
	public void setMin(char axis, float value);
	
	public float getDelta(char axis);
	
	public void draw(Graphics2D graphics);
}
