package be.hogent.tarsos.ui.link;

import java.awt.Color;
import java.awt.Graphics2D;

public class TimeLabelLayer implements Layer{

	public void draw(Graphics2D graphics){
		ViewPort viewport = ViewPort.getInstance();	
		
		//draw legend
		graphics.setColor(Color.black);		
		//every second
		for(int i = (int) viewport.getMinTime() ; i < viewport.getMaxTime() ; i++){
			if(i%10000 == 0){
				int lineHeight = Math.round(LayerUtilities.unitsToPixels(graphics, 8, false));
				graphics.drawLine(i, Math.round(viewport.getMinFrequencyInCents()), i, Math.round(viewport.getMinFrequencyInCents()+lineHeight));
				int textOffset = Math.round(LayerUtilities.unitsToPixels(graphics,12, false));
				String text = String.valueOf(i/1000);
				LayerUtilities.drawString(graphics,text,i,viewport.getMinFrequencyInCents()+textOffset,true,false);
			}else if(i%1000 == 0){
				int lineHeight = Math.round(LayerUtilities.unitsToPixels(graphics,4, false));
				graphics.drawLine(i, Math.round(viewport.getMinFrequencyInCents()), i, Math.round(viewport.getMinFrequencyInCents()+lineHeight));
				int textOffset = Math.round(LayerUtilities.unitsToPixels(graphics,8, false));
				String text = String.valueOf(i/1000);
				LayerUtilities.drawString(graphics,text,i, Math.round(viewport.getMinFrequencyInCents()+textOffset),true,false);
			}
		}
	}	
}
