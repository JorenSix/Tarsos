package be.hogent.tarsos.ui.link.layers;

import java.awt.Color;
import java.awt.Graphics2D;

import be.hogent.tarsos.ui.link.LinkedPanel;
import be.hogent.tarsos.ui.link.ViewPort;
import be.hogent.tarsos.ui.link.coordinatessystems.CoordinateSystem;

public class CentsCoordinateSystemLayer extends AxisLayer{
	
	public CentsCoordinateSystemLayer(final LinkedPanel parent, char direction) {
		super(parent, direction);
	}

	public void draw(Graphics2D graphics){
		ViewPort viewport = parent.getViewPort();
		CoordinateSystem cs = parent.getCoordinateSystem();
		
		//draw legend
		graphics.setColor(Color.black);
		int minOpposite = Math.round(cs.getMin(oppositeDirection));
		boolean horizontal = direction==DIRECTION_X? true : false;
		//Every 100 and 1200 cents
		for(int i = (int) cs.getMin(direction) ; i < cs.getMax(direction) ; i++){
			if(i%1200 == 0){
				int lineWidth = Math.round(LayerUtilities.unitsToPixels(graphics,8, !horizontal));
				graphics.drawLine(minOpposite, i, minOpposite+lineWidth,i);
				String text = String.valueOf(i);
				int textOffset = Math.round(LayerUtilities.unitsToPixels(graphics,12, !horizontal));
				LayerUtilities.drawString(graphics,text,minOpposite+textOffset,i,horizontal,!horizontal);
			}else if(i%100 == 0){
				int lineWidth = Math.round(LayerUtilities.unitsToPixels(graphics,4, !horizontal));
				graphics.drawLine(minOpposite, i, minOpposite+lineWidth,i);
			}
		}
	}
	
}
