package be.hogent.tarsos.ui.pitch;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridLayout;
import java.awt.Paint;

import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSeparator;
import javax.swing.UIManager;
import javax.swing.border.EmptyBorder;

import be.hogent.tarsos.util.ConfKey;
import be.hogent.tarsos.util.Configuration;

public class HeaderPanel extends JPanel {

	/**
	 * 
	 */
	private static final long serialVersionUID = 4852979337236606173L;
	private final ImageIcon icon;
	private final String title = "Tarsos";
	private final String staticHelp = "Drag and drop audio files here to start.";
	private final String liveHelp = "Play into the microphone for real time pitch analysis.";

	public HeaderPanel() {
		super(new BorderLayout());

		icon = Frame.createImageIcon("/be/hogent/tarsos/ui/resources/tarsos_logo_small.png");

		JPanel titlesPanel = new JPanel(new GridLayout(3, 1));
		titlesPanel.setOpaque(false);
		titlesPanel.setBorder(new EmptyBorder(12, 0, 12, 0));

		JLabel headerTitle = new JLabel(title);
		Font police = headerTitle.getFont().deriveFont(Font.BOLD);
		headerTitle.setFont(police);
		headerTitle.setBorder(new EmptyBorder(0, 12, 0, 0));
		titlesPanel.add(headerTitle);

		JLabel message;
		if (Configuration.getBoolean(ConfKey.tarsos_live)) {
			message = new JLabel(liveHelp);
		} else {
			message = new JLabel(staticHelp);
		}
		titlesPanel.add(message);
		police = headerTitle.getFont().deriveFont(Font.PLAIN);
		message.setFont(police);
		message.setBorder(new EmptyBorder(0, 24, 0, 0));

		message = new JLabel(this.icon);
		message.setBorder(new EmptyBorder(0, 0, 0, 12));
		
		JPanel subPanel = new JPanel(new BorderLayout());
		
		subPanel.add(BorderLayout.WEST, titlesPanel);
		subPanel.add(BorderLayout.EAST, message);
		add(BorderLayout.NORTH,subPanel);
		add(BorderLayout.SOUTH, new JSeparator(JSeparator.HORIZONTAL));

		setPreferredSize(new Dimension(640, this.icon.getIconHeight() + 12));
	}

	@Override
	public void paintComponent(Graphics g) {
		super.paintComponent(g);
		if (!isOpaque()) {
			return;
		}

		Color control = UIManager.getColor("control");

		int width = getWidth();
		int height = getHeight();

		Graphics2D g2 = (Graphics2D) g;
		Paint storedPaint = g2.getPaint();
		g2.setPaint(new GradientPaint(this.icon.getIconWidth(), 0, Color.WHITE, width, 0, control));
		g2.fillRect(0, 0, width, height);
		g2.setPaint(storedPaint);
	}
}
