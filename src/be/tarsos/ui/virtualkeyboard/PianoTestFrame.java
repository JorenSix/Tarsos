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

import java.awt.BorderLayout;
import java.awt.Dimension;

import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;

public class PianoTestFrame extends JFrame {

	private static final long serialVersionUID = 6063312726815482475L;

	public PianoTestFrame(final VirtualKeyboard keyboard, final double[] tuning) {
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		setLayout(new BorderLayout());
		final Dimension dimension = new Dimension(650, 100);
		setPreferredSize(dimension);
		setMinimumSize(dimension);
		setMaximumSize(dimension);

		final JPanel keyboardPanel = new JPanel(new BorderLayout());
		keyboardPanel.setBorder(new EmptyBorder(10, 20, 10, 5));
		keyboardPanel.add(keyboard, BorderLayout.CENTER);

		this.add(keyboard, BorderLayout.CENTER);

		keyboard.connectToTunedSynth(tuning);
	}

	public static void main(final String... strings) {
		final double[] tuning = { 0, 100, 200, 300, 400, 500, 600 };
		new PianoTestFrame(VirtualKeyboard.createVirtualKeyboard(7), tuning).setVisible(true);
	}
}
