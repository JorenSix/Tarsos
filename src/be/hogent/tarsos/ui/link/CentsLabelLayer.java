package be.hogent.tarsos.ui.link;

import java.awt.Color;
import java.awt.Graphics2D;

public class CentsLabelLayer implements Layer{

	
	public void draw(Graphics2D graphics){
		ViewPort viewport = ViewPort.getInstance();	
		
		//draw legend
		graphics.setColor(Color.black);
		
		//Every 100 and 1200 cents
		for(int i = (int) viewport.getMinFrequencyInCents() ; i < viewport.getMaxFrequencyInCents() ; i++){
			if(i%1200 == 0){
				int lineWidth = Math.round(LayerUtilities.unitsToPixels(graphics,8, true));
				int minTime = Math.round(viewport.getMinTime());
				graphics.drawLine(minTime, i, minTime+lineWidth,i);
				String text = String.valueOf(i);
				int textOffset = Math.round(LayerUtilities.unitsToPixels(graphics,12, true));
				LayerUtilities.drawString(graphics,text,minTime+textOffset,i,false,true);
			}else if(i%100 == 0){
				int lineWidth = Math.round(LayerUtilities.unitsToPixels(graphics,4, true));
				int minTime = Math.round(viewport.getMinTime());
				graphics.drawLine(minTime, i, minTime+lineWidth,i);
			}
		}
	}
	
}
