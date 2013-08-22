package be.hogent.tarsos.ui.link;

import java.awt.Point;
import java.util.ArrayList;
import java.util.List;

import be.hogent.tarsos.ui.link.coordinatessystems.CoordinateSystem;

public class ViewPort {
	
	private final List<ViewPortChangedListener> listeners;
	private final LinkedPanel parent;
	private CoordinateSystem cs;
	
	public ViewPort(final LinkedPanel parent){
		listeners = new ArrayList<ViewPortChangedListener>();
		this.parent = parent;
		cs = parent.getCoordinateSystem();
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
		float xDelta = cs.getDelta(CoordinateSystem.X_AXIS);
		float newXDelta = xDelta + amount * 1000;
		if(newXDelta > 20 && newXDelta < 600000) {
			cs.setMax(CoordinateSystem.X_AXIS, cs.getMin(CoordinateSystem.X_AXIS) + newXDelta);
		}
		viewPortChanged();
	}
	
	public void drag(float xAmount, float yAmount){
		cs.setMin(CoordinateSystem.X_AXIS, cs.getMin(CoordinateSystem.X_AXIS) + xAmount);
		cs.setMax(CoordinateSystem.X_AXIS, cs.getMax(CoordinateSystem.X_AXIS) + xAmount);
		
		cs.setMin(CoordinateSystem.Y_AXIS, cs.getMin(CoordinateSystem.Y_AXIS) + yAmount);
		cs.setMax(CoordinateSystem.Y_AXIS, cs.getMax(CoordinateSystem.Y_AXIS) + yAmount);
		
		viewPortChanged();
	}
	
//	public void drag(float millisecondAmount, float centAmount){
//		setMinTime( getMinTime() + millisecondAmount);
//		setMaxTime( getMaxTime() + millisecondAmount);
//		
//		setMinFrequency(getMinFrequencyInCents() + centAmount);
//		setMaxFrequencyInCents(getMaxFrequencyInCents() + centAmount);
//		viewPortChanged();
//	}
	
	/**
	 * @return Time delta in milliseconds
	 */
//	public float getTimeDelta(){
//		return maxTime - minTime;
//	}
//	
//	public float getFrequencyDelta() {
//		return maxFrequency - minFrequency;
//	}
//
//	public void setMinTime(float f) {
//		this.minTime = f;
//	}
//
//	public float getMinTime() {
//		return minTime;
//	}
//
//	public void setMaxTime(float maxTime) {
//		this.maxTime = maxTime;
//	}
//
//	public float getMaxTime() {
//		return maxTime;
//	}
//
//	public void setMinFrequency(float f) {
//		this.minFrequency = f;
//	}

//	public float getMinFrequencyInCents() {
//		return minFrequency;
//	}
//
//	public void setMaxFrequencyInCents(float maxFrequencyInCents) {
//		this.maxFrequency = maxFrequencyInCents;
//	}
//
//	public float getMaxFrequencyInCents() {
//		return maxFrequency;
//	}	
	
//	private static ViewPort instance;
//	
//	public static ViewPort getInstance(){
//		if(instance == null){
//			instance = new ViewPort();
//		}
//		return instance;
//	}

	
}
