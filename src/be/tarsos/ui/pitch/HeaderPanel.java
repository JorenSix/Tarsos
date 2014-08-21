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

package be.tarsos.ui.pitch;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridLayout;

import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;

import be.tarsos.ui.TarsosFrame;

public class HeaderPanel extends JPanel {

	/**
	 * 
	 */
	private static final long serialVersionUID = 4852979337236606173L;
	private final ImageIcon icon;
	private final String title = "Tarsos";
	private final String staticHelp = "Drag and drop audio files here to start.";

	public HeaderPanel() {
		super(new BorderLayout());

		icon = TarsosFrame.createImageIcon("/be/tarsos/ui/resources/tarsos_logo_small.png");

		JPanel titlesPanel = new JPanel(new GridLayout(3, 1));
		titlesPanel.setOpaque(false);
		titlesPanel.setBorder(new EmptyBorder(8, 0, 4, 0));

		JLabel headerTitle = new JLabel(title);
		Font police = headerTitle.getFont().deriveFont(Font.BOLD);
		headerTitle.setFont(police);
		headerTitle.setBorder(new EmptyBorder(0, 8, 0, 0));
		titlesPanel.add(headerTitle);

		JLabel message;
		message = new JLabel(staticHelp);
		
		titlesPanel.add(message);
		police = headerTitle.getFont().deriveFont(Font.PLAIN);
		message.setFont(police);
		message.setBorder(new EmptyBorder(0, 16, 0, 0));

		message = new JLabel(this.icon);
		message.setBorder(new EmptyBorder(0, 0, 0, 4));
		
		add(BorderLayout.WEST, titlesPanel);
		add(BorderLayout.EAST, message);	

		setPreferredSize(new Dimension(640, this.icon.getIconHeight() + 4));
	}
}
