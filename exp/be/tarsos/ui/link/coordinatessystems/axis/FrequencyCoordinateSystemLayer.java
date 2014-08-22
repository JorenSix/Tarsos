package be.tarsos.ui.link.coordinatessystems.axis;

import java.awt.Color;
import java.awt.Graphics2D;

import be.tarsos.ui.link.LinkedPanel;
import be.tarsos.ui.link.ViewPort;
import be.tarsos.ui.link.coordinatessystems.ICoordinateSystem;
import be.tarsos.ui.link.coordinatessystems.Quantity;
import be.tarsos.ui.link.layers.LayerUtilities;

public class FrequencyCoordinateSystemLayer extends AxisLayer{
	
	public FrequencyCoordinateSystemLayer(final LinkedPanel parent, char direction) {
		super(parent, direction);
		this.name = "Cents Axis";
	}

	public void draw(Graphics2D graphics){
		ViewPort viewport = parent.getViewPort();
		ICoordinateSystem cs = parent.getCoordinateSystem();
		
		//draw legend
		graphics.setColor(Color.black);
		int minOpposite = Math.round(cs.getMin(oppositeDirection));
		boolean horizontal = direction==DIRECTION_X? true : false;
		//Every 100 and 1200 cents
		for(int i = (int) cs.getMin(direction) ; i < cs.getMax(direction) ; i++){
			if(i%1200 == 0){
				int lineWidth = Math.round(LayerUtilities.pixelsToUnits(graphics,8, !horizontal));
				graphics.drawLine(minOpposite, i, minOpposite+lineWidth,i);
				String text = String.valueOf(Quantity.FREQUENCY.getUnit().getValueInUnits(i));
				int textOffset = Math.round(LayerUtilities.pixelsToUnits(graphics,12, !horizontal));
				LayerUtilities.drawString(graphics,text,minOpposite+textOffset,i,horizontal,!horizontal);
			} else if(i%100 == 0){
				int lineWidth = Math.round(LayerUtilities.pixelsToUnits(graphics,4, !horizontal));
				graphics.drawLine(minOpposite, i, minOpposite+lineWidth,i);
			}
		}
	}
}
