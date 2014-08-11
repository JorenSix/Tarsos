/*
*              _______                      
*             |__   __|                     
*                | | __ _ _ __ ___  ___  ___
*                | |/ _` | '__/ __|/ _ \/ __| 
*                | | (_| | |  \__ \ (_) \__ \    
*                |_|\__,_|_|  |___/\___/|___/    
*                                                         
* -----------------------------------------------------------
*
*  Tarsos is developed by Joren Six at 
*  The School of Arts,
*  University College Ghent,
*  Hoogpoort 64, 9000 Ghent - Belgium
*  
* -----------------------------------------------------------
*
*  Info: http://tarsos.0110.be
*  Github: https://github.com/JorenSix/Tarsos
*  Releases: http://tarsos.0110.be/releases/Tarsos/
*  
*  Tarsos includes some source code by various authors,
*  for credits and info, see README.
* 
*/

package be.tarsos.ui.virtualkeyboard;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.Rectangle2D;

/**
 * <p>
 * Represents a keyboard with an arbitrary number of (white) keys per octave.
 * Every octave is marked.
 * </p>
 * <p>
 * Uses refactored code from the gervill package licensed under the GPL with the
 * classpath exception.
 * </p>
 * <a href="https://gervill.dev.java.net/source/browse/gervill/src.demos/">
 * Gervill source code</a>
 * 
 * @author Joren Six
 */
public final class UniversalVirtualKeyboard extends VirtualKeyboard {

	private static final long serialVersionUID = -3017076399911747736L;

	public UniversalVirtualKeyboard(final int numberOfKeysPerOctave) {
		super(numberOfKeysPerOctave);
	}

	
	protected int getMidiNote(final int x, final int y) {

		final int w = getWidth();
		final float nw = w / (float) getNumberOfKeys();
		final int wn = (int) (x / nw);
		final int oct = wn / getNumberOfKeysPerOctave();
		int n = oct * getNumberOfKeysPerOctave() + wn % getNumberOfKeysPerOctave();
		if (n < 0) {
			n = 0;
		}
		if (n > getNumberOfKeys() - 1) {
			n = getNumberOfKeys() - 1;
		}
		return n;
	}

	
	public void paint(final Graphics g) {
		super.paint(g);
		final Graphics2D g2 = (Graphics2D) g;
		g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		g2.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_ON);

		final int w = getWidth();
		final int h = getHeight();

		final float nw = w / (float) getNumberOfKeys();
		float cx = 0;
		final Rectangle2D rect = new Rectangle2D.Double();
		for (int i = 0; i < getNumberOfKeys(); i++) {

			rect.setRect(cx, 0, nw, h);
			if (isKeyDown(i)) {
				g2.setColor(new Color(0.8f, 0.8f, 0.95f));
			} else {
				g2.setColor(Color.WHITE);
			}
			g2.fill(rect);
			g2.setColor(Color.BLACK);
			g2.draw(rect);

			if (i % this.getNumberOfKeysPerOctave() == 0) {
				g2.drawString("_", cx + 2, 12);
			}

			if (i >= getLowestAssignedKey()) {
				if (i - getLowestAssignedKey() < VirtualKeyboard.getMappedKeys().length()) {
					g2.setColor(Color.GRAY);
					final char keyChar = VirtualKeyboard.getMappedKeys().charAt(i - getLowestAssignedKey());
					g2.drawString("" + keyChar, cx + 2, h - 4);
				}
			}
			cx += nw;
		}
	}



}
