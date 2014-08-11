package be.tarsos.ui.link.coordinatessystems.axis;

import java.awt.Color;
import java.awt.Graphics2D;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.text.NumberFormat;

import be.tarsos.ui.link.LinkedPanel;
import be.tarsos.ui.link.ViewPort;
import be.tarsos.ui.link.coordinatessystems.ICoordinateSystem;
import be.tarsos.ui.link.coordinatessystems.Quantity;
import be.tarsos.ui.link.layers.LayerUtilities;

public class TimeCoordinateSystemLayer extends AxisLayer {

	private int[] intervals = { 100, 200, 500, 1000, 2000, 5000, 7500, 10000, 15000, 20000, 50000, 100000, 200000, 500000, 1000000, 2000000};
	public TimeCoordinateSystemLayer(LinkedPanel parent, char direction) {
		super(parent, direction);
		this.name = "Time axis";
	}
	
	private int getClosestDrawIndex(int stepSize){
		int i = 1;
		float previousValue = intervals[0];
		float currentValue = intervals[1];
		while(i < intervals.length && Math.abs(currentValue-stepSize) < Math.abs(previousValue-stepSize)){
			i++;
			previousValue = currentValue;
			currentValue = intervals[i];
		}
		return (i-1);
	}
	
	public static String fmt(double d)
	{
	    if(d == (int) d)
	        return String.format("%d",(int)d);
	    else {
	    	NumberFormat df = DecimalFormat.getInstance();
	    	df.setMaximumFractionDigits(2);
	    	df.setRoundingMode(RoundingMode.HALF_UP);
	        return df.format(d);
	    }
	}

	public void draw(Graphics2D graphics) {
		ICoordinateSystem cs = parent.getCoordinateSystem();
		// draw legend
		graphics.setColor(Color.black);
		// every second
		int minOpposite = Math.round(cs.getMin(oppositeDirection));
		boolean horizontal = direction == DIRECTION_X ? true : false;
		
		
		int amountToDraw = (int) Math.round(0.023*parent.getWidth());
		float deltaX = cs.getDelta(DIRECTION_X); //Breedte in milisec.
		int idealStepSize = (int)Math.round((deltaX/((double)amountToDraw)));
		int step = intervals[getClosestDrawIndex(idealStepSize)];

		for (int i = (int) cs.getMin(direction); i < cs.getMax(direction); i++) {
			if (i % (step*5) == 0) {
				int lineHeight = Math.round(LayerUtilities.pixelsToUnits(
						graphics, 8, !horizontal));
				graphics.drawLine(i, minOpposite, i, minOpposite + lineHeight);
				int textOffset = Math.round(LayerUtilities.pixelsToUnits(
						graphics, 12, !horizontal));
				String text = fmt(Quantity.TIME.getUnit().getValueInUnits((double)i));
				LayerUtilities.drawString(graphics, text, i, minOpposite
						+ textOffset, horizontal, !horizontal);
			} else if (i % step == 0) {
				int lineHeight = Math.round(LayerUtilities.pixelsToUnits(
						graphics, 4, !horizontal));
				graphics.drawLine(i, minOpposite, i, minOpposite + lineHeight);
				int textOffset = Math.round(LayerUtilities.pixelsToUnits(
						graphics, 9, !horizontal));
				String text = fmt(Quantity.NONE.getUnit().getValueInUnits((double)i));
				LayerUtilities.drawString(graphics, text, i, minOpposite
						+ textOffset, horizontal, !horizontal);
			}
		}
	}
}
