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
package be.tarsos.ui.pitch.ph;

import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionListener;
import java.util.Arrays;

import javax.swing.JComponent;

import be.tarsos.ui.TarsosFrame;
import be.tarsos.ui.pitch.ScaleChangedListener;

public class ScaleEditor extends MouseAdapter implements MouseMotionListener, KeyListener, ScaleChangedListener {
	private final MouseDragListener mouseDrag;
	private final JComponent parent;
	private double movingElement = -1.0;
	private double[] scale;

	ScaleEditor(final MouseDragListener mouseDrag, final JComponent parent) {
		this.mouseDrag = mouseDrag;
		this.parent = parent;
	}

	public void mouseDragged(MouseEvent arg0) {

	}
	


	public void mouseMoved(MouseEvent e) {

		if (e.isAltDown() || e.isAltGraphDown()) {
			//request focus for the key listener to work...
			parent.requestFocus();
			// add new element
			if (movingElement != -1.0) {
				int index = -1;
				for (int i = 0; i < scale.length; i++) {
					if (scale[i] == movingElement) {
						index = i;
					}
				}
				if (index == -1) {
					movingElement = -1.0;
				} else {
					scale[index] = mouseDrag.getCents(e, 1200.0);
					movingElement = scale[index];
				}
			} else {
				double[] newScale = new double[scale.length + 1];
				for (int i = 0; i < scale.length; i++) {
					newScale[i] = scale[i];
				}
				newScale[newScale.length - 1] = mouseDrag.getCents(e, 1200.0);
				movingElement = newScale[newScale.length - 1];
				Arrays.sort(newScale);
				scale = newScale;
			}
			parent.repaint();
			//layer.scaleChangedPublisher.scaleChanged(scale, true, false);
			TarsosFrame.getInstance().scaleChanged(scale, true, false);
		} else if (e.isControlDown()) {
			//request focus for the key listener to work...
			parent.requestFocus();
			// move the closest element
			if (movingElement == -1.0) {
				int index = closestIndex(mouseDrag.getCents(e, 1200.0));
				movingElement = scale[index];
			}
			for (int i = 0; i < scale.length; i++) {
				if (scale[i] == movingElement) {
					scale[i] = mouseDrag.getCents(e, 1200.0);
					movingElement = scale[i];
				}
			}
			parent.repaint();
			TarsosFrame.getInstance().scaleChanged(scale, true, false);
		}
	}

	private int closestIndex(double key) {
		double distance = Double.MAX_VALUE;
		int index = -1;
		for (int i = 0; i < scale.length; i++) {
			double currentDistance = Math.abs(key - scale[i]);
			double wrappedDistance = Math.abs(key - (scale[i] + 1200));
			if (Math.min(currentDistance, wrappedDistance) < distance) {
				distance = Math.min(currentDistance, wrappedDistance);
				index = i;
			}
		}
		return index;
	}

	@Override
	public void mouseReleased(MouseEvent e) {
		if (movingElement != -1.0) {
			Arrays.sort(scale);
			TarsosFrame.getInstance().scaleChanged(scale, false, false);
		}
		movingElement = -1.0;
	}

	@Override
	public void mousePressed(MouseEvent arg0) {

	}

	public double getMovingElement() {
		return movingElement;
	}

	public void keyPressed(KeyEvent arg0) {
		// TODO Auto-generated method stub

	}

	public void keyReleased(KeyEvent arg0) {
		// TODO Auto-generated method stub

	}

	public void keyTyped(KeyEvent arg0) {
		boolean elementSelected = movingElement != -1.0;
		boolean deleteKeyPressed = (arg0.getKeyChar() == 'd' || arg0.getKeyCode() == KeyEvent.VK_DELETE || arg0.getKeyChar() == 127 );
		if( elementSelected && deleteKeyPressed){
			double[] newScale = new double[scale.length-1];
			
			int j = 0;
			for (int i = 0; i < scale.length;i++) {
				if (scale[i] != movingElement) {
					newScale[j] = scale[i];  
					j++;
				}
			}
			Arrays.sort(newScale);
			scale = newScale;				
			TarsosFrame.getInstance().scaleChanged(scale, false, false);
			movingElement = -1.0;
		}
	}

	public void scaleChanged(double[] newScale, boolean isChanging,
			boolean shiftHisto) {
		this.scale = newScale;
		
	}
}
