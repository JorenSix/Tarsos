package be.tarsos.ui.link.coordinatessystems.axis;

import be.tarsos.ui.link.LinkedPanel;
import be.tarsos.ui.link.layers.Layer;

public abstract class AxisLayer implements Layer {
	protected String name;
	public static final char DIRECTION_X = 'X';
	public static final char DIRECTION_Y = 'Y';

	protected final LinkedPanel parent;
	protected final char direction;
	protected final char oppositeDirection;

	public String getName(){
		return name;
	}
	
	public AxisLayer(final LinkedPanel parent, char direction) {
		if ((direction == DIRECTION_X || direction == DIRECTION_Y)
				&& parent != null) {
			this.direction = direction;
			if (direction == DIRECTION_X){
				oppositeDirection = DIRECTION_Y;
			} else {
				oppositeDirection = DIRECTION_X;
			}
			this.parent = parent;
		} else {
			throw new IllegalArgumentException(
					"Please choose a X or Y direction and make sure the parent (LinkedFeaturePanel) is supplied!");
		}
	}
}
