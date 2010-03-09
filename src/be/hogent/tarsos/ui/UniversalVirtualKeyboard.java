package be.hogent.tarsos.ui;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.Rectangle2D;


public class UniversalVirtualKeyboard  extends VirtualKeyboard {

	private static final long serialVersionUID = -3017076399911747736L;

	public UniversalVirtualKeyboard(int numberOfKeysPerOctave){
		super(numberOfKeysPerOctave);
	}

	@Override
	protected int getMidiNote(int x, int y) {
		int w = getWidth();
		float nw = w / (float) numberOfKeys;
		int wn = (int) (x / nw);
		int oct = wn / numberOfKeysPerOctave;
		int n = oct * numberOfKeysPerOctave + wn % numberOfKeysPerOctave;
		if (n < 0)
			n = 0;
		if (n > numberOfKeys - 1)
			n = numberOfKeys - 1;
		return n;
	}
	
	@Override
	public void paint(Graphics g) {
		super.paint(g);
		Graphics2D g2 = (Graphics2D) g;
		g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
				RenderingHints.VALUE_ANTIALIAS_ON);
		g2.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS,
				RenderingHints.VALUE_FRACTIONALMETRICS_ON);

		int w = getWidth();
		int h = getHeight();

		float nw = w / (float) numberOfKeys;
		float cx = 0;
		Rectangle2D rect = new Rectangle2D.Double();
		for (int i = 0; i < numberOfKeys; i++) {

			rect.setRect(cx, 0, nw, h);
			if (isKeyDown(i))
				g2.setColor(new Color(0.8f, 0.8f, 0.95f));
			else
				g2.setColor(Color.WHITE);
			g2.fill(rect);
			g2.setColor(Color.BLACK);
			g2.draw(rect);

			if (i % this.numberOfKeysPerOctave == 0)
				g2.drawString("_", cx + 2, 12);
			
			if(i >= lowestAssignedKey){
                if(i - lowestAssignedKey < VirtualKeyboard.mappedKeys.length()){
                    g2.setColor(Color.GRAY);
                    char keyChar = VirtualKeyboard.mappedKeys.charAt(i - lowestAssignedKey);
                    g2.drawString("" + keyChar, cx + 2, h - 4);
                }
            }
			cx += nw;
		}
	}
}
