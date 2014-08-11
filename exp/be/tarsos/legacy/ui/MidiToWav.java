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

/**
 */
package be.tarsos.legacy.ui;

import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridLayout;
import java.awt.Paint;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import javax.sound.midi.InvalidMidiDataException;
import javax.sound.midi.MidiUnavailableException;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;

import be.tarsos.midi.MidiToWavRenderer;
import be.tarsos.ui.TarsosFrame;
import be.tarsos.util.FileDrop;
import be.tarsos.util.FileUtils;
import be.tarsos.util.ScalaFile;
import be.tarsos.util.FileDrop.Listener;

/**
 * @author Joren Six
 */
public class MidiToWav extends JFrame implements Listener, ActionListener {

	/**
     */
	private static final long serialVersionUID = -4115909001888538815L;
	
	private final JLabel midiFiles = new JLabel();
	private final JLabel scalaFiles = new JLabel();
	
	private final Set<File> midiFileSet = new HashSet<File>();
	private final Set<File> scalaFileSet = new HashSet<File>();
	
	private final JButton synthesizeButton = new JButton("Synthesize");
	
	private final JLabel progressLabel;
	
	private final JPanel contentPanel;
	
	public MidiToWav() {
		super("MIDI and Scala to WAV");

		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		setLayout(new BorderLayout());

		JPanel progressPanel = new JPanel(new BorderLayout());
		progressLabel = new JLabel();
		progressLabel.setText("<html></html>");
		progressLabel.setVerticalAlignment(SwingConstants.TOP);
		JScrollPane progressScrollPane = new JScrollPane(progressLabel);
		progressScrollPane.setBorder(new TitledBorder("Progress"));
		progressPanel.add(progressScrollPane,BorderLayout.CENTER);
		
		//create a panel with the JLabels with lists of files
		JPanel filesPanel = new JPanel(new GridLayout(1,2,10,20));
		
		//make sure that the label takes some space...
		midiFiles.setText("<html><br><br><br><br><br><br></html>");
		midiFiles.setVerticalAlignment(SwingConstants.TOP);
		JScrollPane midiScrollPane = new JScrollPane(midiFiles);		
		midiScrollPane.setBorder(new TitledBorder("MIDI-files"));
		filesPanel.add(midiScrollPane);
		
		scalaFiles.setVerticalAlignment(SwingConstants.TOP);
		JScrollPane scalaScrollPane = new JScrollPane(scalaFiles);		
		scalaScrollPane.setBorder(new TitledBorder("Scala-files"));
		filesPanel.add(scalaScrollPane);
		
		JPanel buttonPanel = new JPanel(new GridLayout(1,3,10,20));
		buttonPanel.add(new JLabel(""));
		buttonPanel.add(new JLabel(""));
		buttonPanel.add(synthesizeButton);
		
		synthesizeButton.addActionListener(this);
		synthesizeButton.setEnabled(false);
		
		//Add the filedrop listener
		new FileDrop(this, this);
		
		contentPanel = new JPanel(new CardLayout());
		contentPanel.add(filesPanel,"files");
		contentPanel.add(progressPanel,"progress");
		
		//adds panels to the frame
		this.add(new HeaderPanel(),BorderLayout.NORTH);
		this.add(contentPanel,BorderLayout.CENTER);
		this.add(buttonPanel,BorderLayout.SOUTH);
		
		pack();
		setVisible(true);
	}
	
	public void actionPerformed(ActionEvent arg0) {
		Runnable worker = new Runnable(){
			public void run() {
				Iterator<File> midiFileIterator = midiFileSet.iterator();
				while (midiFileIterator.hasNext()) {
					Iterator<File> scalaFileIterator = scalaFileSet.iterator();
					File midiFile = midiFileIterator.next();
					while (scalaFileIterator.hasNext()) {
						synth(midiFile,scalaFileIterator.next());
					}
				}
				SwingUtilities.invokeLater(new Runnable() {
					public void run() {
						setWaitState(false);
					}
				});
			}
		};
		new Thread(worker,"Synthesizer").start();
		setWaitState(true);
	}
	
	private void setWaitState(boolean isWaiting){
		Component glassPane = this.getGlassPane();
		CardLayout cl = (CardLayout) contentPanel.getLayout();		
		synthesizeButton.setEnabled(!isWaiting);		
		if (isWaiting) {
			cl.show(contentPanel, "progress");
			glassPane.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
			glassPane.setVisible(true);
		} else {
			midiFileSet.clear();
			scalaFileSet.clear();
			midiFiles.setText("");
			scalaFiles.setText("");
			progressLabel.setText("<html></html>");
			cl.show(contentPanel, "files");
			glassPane.setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
			glassPane.setVisible(false);
		}
	}
	
	private void synth(File midiFile, File scalaFile){
		double[] tuning = new ScalaFile(scalaFile.getAbsolutePath()).getPitches();
		final String newFileName = FileUtils.basename(midiFile.getAbsolutePath()) + "_" +	FileUtils.basename(scalaFile.getAbsolutePath())+ ".wav";
		appendProgress("Start synthesizing " + newFileName);
		File wavFile = new File(newFileName);	
        try {
            MidiToWavRenderer renderer;
            renderer = new MidiToWavRenderer();
            renderer.setTuning(tuning);
            renderer.createWavFile(midiFile, wavFile);
            appendProgress("Done synthesizing " + newFileName);
        } catch (MidiUnavailableException e) {
        	e.printStackTrace();
        } catch (InvalidMidiDataException e) {
        	e.printStackTrace();
        } catch (IOException e) {
        	e.printStackTrace();
        }
	}
	
	private void appendProgress(final String message){
		SwingUtilities.invokeLater(new Runnable(){
			public void run() {
				String currentText = progressLabel.getText();
				String newText = currentText.replace("</html>",message + "<br></html>");
				progressLabel.setText(newText);
			}
        });
	}
	
	public void filesDropped(File[] files) {
		for (File file : files) {
			if (file.getName().toLowerCase().endsWith("mid")) {
				midiFileSet.add(file);
			} else if (file.getName().toLowerCase().endsWith("scl")) {
				scalaFileSet.add(file);
			}
		}
		StringBuilder midiSB = new StringBuilder();
		StringBuilder scalaSB = new StringBuilder();
		Iterator<File> fileIterator = midiFileSet.iterator();
		while (fileIterator.hasNext()) {
			midiSB.append(fileIterator.next().getName()).append("<br>");
		}
		fileIterator = scalaFileSet.iterator();
		while (fileIterator.hasNext()) {
			scalaSB.append(fileIterator.next().getName()).append("<br>");
		}
		midiFiles.setText("<html>" + midiSB.toString() + "</html>");
		scalaFiles.setText("<html>" + scalaSB.toString() + "</html>");
		
		//enable the button if there is more than one file in each list
		synthesizeButton.setEnabled(midiFileSet.size() > 0 && scalaFileSet.size() > 0);
	}

	public static void main(final String[] args) {
		javax.swing.SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				try {					
					UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
				} catch (Exception e) {
					//ignore
				}
				new MidiToWav();
			}
		});		
	}
	
	/**
	 * A panel with a fancy header: Fancy!!
	 * @author Joren
	 */
	private static class HeaderPanel extends JPanel {
		private static final long serialVersionUID = 4852976337236606173L;
		private final ImageIcon icon = TarsosFrame.createImageIcon("/be/hogent/tarsos/ui/resources/tarsos_logo_small.png");
		private final String title = "Tarsos - Synthesize MIDI and Scala-files to tuned WAV-files";
		private final String staticHelp = "Drag and drop MIDI and Scala files here to start.";

		public HeaderPanel() {
			super(new BorderLayout());
			
			JPanel titlesPanel = new JPanel(new GridLayout(3, 1));
			titlesPanel.setOpaque(false);
			titlesPanel.setBorder(new EmptyBorder(12, 0, 12, 0));

			JLabel headerTitle = new JLabel(title);
			Font police = headerTitle.getFont().deriveFont(Font.BOLD);
			headerTitle.setFont(police);
			headerTitle.setBorder(new EmptyBorder(0, 12, 0, 0));
			titlesPanel.add(headerTitle);

			JLabel message = new JLabel(staticHelp);
			titlesPanel.add(message);
			police = headerTitle.getFont().deriveFont(Font.PLAIN);
			message.setFont(police);
			message.setBorder(new EmptyBorder(0, 24, 0, 0));

			message = new JLabel(this.icon);
			message.setBorder(new EmptyBorder(0, 0, 0, 12));

			JPanel subPanel = new JPanel(new BorderLayout()){
				/**
				 * 
				 */
				private static final long serialVersionUID = 1L;

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
					g2.setPaint(new GradientPaint(icon.getIconWidth(), 0, Color.WHITE, width, 0, control));
					g2.fillRect(0, 0, width, height);
					g2.setPaint(storedPaint);
				}
			};
			
			subPanel.add(BorderLayout.WEST, titlesPanel);
			subPanel.add(BorderLayout.EAST, message);
			add(BorderLayout.NORTH,subPanel);
			add(BorderLayout.SOUTH, new JSeparator(JSeparator.HORIZONTAL));
			
			setPreferredSize(new Dimension(520, this.icon.getIconHeight() + 25));
			setMinimumSize(getPreferredSize());
		}
	}
}
