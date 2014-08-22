package be.tarsos.ui.link;

import java.awt.Point;
import java.util.ArrayList;
import java.util.List;

import be.tarsos.ui.link.coordinatessystems.ICoordinateSystem;

public class ViewPort {
	
	private final List<ViewPortChangedListener> listeners;
	private final ICoordinateSystem cs;
	
	public ViewPort(final ICoordinateSystem cs){
		listeners = new ArrayList<ViewPortChangedListener>();
		this.cs = cs;
	}
	
	public void addViewPortChangedListener(ViewPortChangedListener listener){
		listeners.add(listener);
	}
	
	static interface ViewPortChangedListener{
		void viewPortChanged(ViewPort newViewPort);
	}
	
	private void viewPortChanged(){
		for(ViewPortChangedListener listener : listeners){
			listener.viewPortChanged(this);
		}
	}
	
	public void zoom(int amount, Point zoomPoint){
		float xDelta = cs.getDelta(ICoordinateSystem.X_AXIS);
		float newXDelta = xDelta + amount * 1000;
		if(newXDelta > 20 && newXDelta < 600000) {
			cs.setMax(ICoordinateSystem.X_AXIS, cs.getMin(ICoordinateSystem.X_AXIS) + newXDelta);
		}
		viewPortChanged();
	}
	
	public void drag(float xAmount, float yAmount){
		cs.setMin(ICoordinateSystem.X_AXIS, cs.getMin(ICoordinateSystem.X_AXIS) + xAmount);
		cs.setMax(ICoordinateSystem.X_AXIS, cs.getMax(ICoordinateSystem.X_AXIS) + xAmount);
		
		cs.setMin(ICoordinateSystem.Y_AXIS, cs.getMin(ICoordinateSystem.Y_AXIS) + yAmount);
		cs.setMax(ICoordinateSystem.Y_AXIS, cs.getMax(ICoordinateSystem.Y_AXIS) + yAmount);
		
		viewPortChanged();
	}
}
