package be.tarsos;

import java.awt.Color;
import java.awt.Graphics2D;
import java.util.HashMap;
import java.util.Map;

import be.tarsos.dsp.ui.layers.Layer;
import be.tarsos.dsp.ui.layers.LayerUtilities;

public class HeatMapLayer implements Layer{

	final int index;
	final HashMap<Integer,Double> values;
	
	public HeatMapLayer(int index,HashMap<Integer,Double> values){
		this.index=index;
		this.values = values;
		
	}
	@Override
	public void draw(Graphics2D graphics) {
		
		float spacer = LayerUtilities.pixelsToUnits(graphics, 5, false);
		float heightOfABlock = LayerUtilities.pixelsToUnits(graphics, 30, false);
		int verticalOffsetOffset = -1 * (Math.round((index + 1) * spacer + index * heightOfABlock));

		for(Map.Entry<Integer,Double> e : values.entrySet()){
			int centsMiddle = e.getKey()*20;
			int startCents = (centsMiddle - 10)*3;
			int stopCents = (centsMiddle + 10)*3;
			
			Color backgroundColor = new Color(1.0f, 0,0, e.getValue().floatValue());
			graphics.setColor(backgroundColor);		
			
			graphics.fillRect(startCents, verticalOffsetOffset, stopCents-startCents, Math.round(heightOfABlock));
			//graphics.drawRect(startTime, verticalOffsetOffset, stopTime-startTime, Math.round(heightOfABlock));
		}
		
		
		
		//
		
	}

	@Override
	public String getName() {
		
		return "Heat map";
	}
	
	
}
