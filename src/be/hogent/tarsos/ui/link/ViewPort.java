package be.hogent.tarsos.ui.link;

import java.awt.Point;
import java.util.ArrayList;
import java.util.List;

public class ViewPort {
	
	
	private float maxFrequency = 10800;
	private float minFrequency = 3600;
	
	private float maxTime = 10000;
	private float minTime = 0;
	
	private final List<ViewPortChangedListener> listeners;
	
	private ViewPort(){
		listeners = new ArrayList<ViewPortChangedListener>();		
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
	
	public void zoom(int amount,Point zoomPoint){
		float timeDelta = getTimeDelta();
		float newTimeDelta = timeDelta + amount  * 1000;
		if(newTimeDelta > 20 && newTimeDelta < 600000) {
			setMaxTime(getMinTime() + newTimeDelta);
		}
		viewPortChanged();
	}
	
	public void drag(float millisecondAmount,float centAmount){
		setMinTime( getMinTime() + millisecondAmount);
		setMaxTime( getMaxTime() + millisecondAmount);
		
		setMinFrequency(getMinFrequencyInCents() + centAmount);
		setMaxFrequencyInCents(getMaxFrequencyInCents() + centAmount);
		viewPortChanged();
	}
	
	/**
	 * @return Time delta in milliseconds
	 */
	public float getTimeDelta(){
		return maxTime - minTime;
	}
	
	public float getFrequencyDelta() {
		return maxFrequency - minFrequency;
	}

	public void setMinTime(float f) {
		this.minTime = f;
	}

	public float getMinTime() {
		return minTime;
	}

	public void setMaxTime(float maxTime) {
		this.maxTime = maxTime;
	}

	public float getMaxTime() {
		return maxTime;
	}

	public void setMinFrequency(float f) {
		this.minFrequency = f;
	}

	public float getMinFrequencyInCents() {
		return minFrequency;
	}

	public void setMaxFrequencyInCents(float maxFrequencyInCents) {
		this.maxFrequency = maxFrequencyInCents;
	}

	public float getMaxFrequencyInCents() {
		return maxFrequency;
	}	
	
	private static ViewPort instance;
	public static ViewPort getInstance(){
		if(instance == null){
			instance = new ViewPort();
		}
		return instance;
	}

	
}
