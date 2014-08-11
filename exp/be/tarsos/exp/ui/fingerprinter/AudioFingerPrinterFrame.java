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

package be.tarsos.exp.ui.fingerprinter;

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
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;

import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComponent;
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

import be.tarsos.exp.ui.fingerprinter.AudioFingerprinter.AudioFingerprintMatch;
import be.tarsos.ui.TarsosFrame;
import be.tarsos.util.ConfKey;
import be.tarsos.util.Configuration;
import be.tarsos.util.FileDrop;
import be.tarsos.util.FileUtils;
import be.tarsos.util.JLabelHandler;
import be.tarsos.util.StopWatch;
import be.tarsos.util.FileDrop.Listener;

public class AudioFingerPrinterFrame extends JFrame implements  ActionListener {

	/**
     */
	private static final long serialVersionUID = -4115909001888538815L;
	
	private static final Logger LOG = Logger.getLogger(AudioFingerPrinterFrame.class.getName());
	
	private final JLabel hayStackFiles = new JLabel();
	private final JLabel needleFiles = new JLabel();
	
	private final Set<File> hayStackFileSet = new HashSet<File>();
	private final Set<File> needleFileSet = new HashSet<File>();
	
	private final JButton searchButton = new JButton("Search");
	
	private final JLabel progressLabel;
	
	private final JPanel contentPanel;
	
	public AudioFingerPrinterFrame() {
		super("Tarsos - Robust Audio Fingerprinting");
		
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
		hayStackFiles.setText("<html><br><br><br><br><br><br></html>");
		hayStackFiles.setVerticalAlignment(SwingConstants.TOP);
		JScrollPane midiScrollPane = new JScrollPane(hayStackFiles);		
		midiScrollPane.setBorder(new TitledBorder("Haystack"));
		filesPanel.add(midiScrollPane);
		
		needleFiles.setVerticalAlignment(SwingConstants.TOP);
		JScrollPane scalaScrollPane = new JScrollPane(needleFiles);		
		scalaScrollPane.setBorder(new TitledBorder("Needle(s)"));
		filesPanel.add(scalaScrollPane);
		
		JPanel buttonPanel = new JPanel(new GridLayout(1,3,10,20));
		buttonPanel.add(new JLabel(""));
		buttonPanel.add(new JLabel(""));
		buttonPanel.add(searchButton);
		
		JPanel statusbarPanel = new JPanel(new BorderLayout(10,20));
		statusbarPanel.add(makeStatusBar(),BorderLayout.CENTER);
		
		JPanel buttonAndStatusBarPanel = new JPanel(new BorderLayout());
		buttonAndStatusBarPanel.add(buttonPanel,BorderLayout.NORTH);
		buttonAndStatusBarPanel.add(statusbarPanel,BorderLayout.SOUTH);
	    
		
		searchButton.addActionListener(this);
		searchButton.setEnabled(false);
		
		//Add the filedrop listener
		new FileDrop(needleFiles, neeldeFileDropListener);
		new FileDrop(hayStackFiles, hayStackFileDropListener);
		
		contentPanel = new JPanel(new CardLayout());
		contentPanel.add(filesPanel,"files");
		contentPanel.add(progressPanel,"progress");
		
		//adds panels to the frame
		this.add(new HeaderPanel(),BorderLayout.NORTH);
		this.add(contentPanel,BorderLayout.CENTER);
		this.add(buttonAndStatusBarPanel,BorderLayout.SOUTH);
		
		pack();
		setVisible(true);
		
		LOG.info("Ready for a search query...");
	}
	
	private Listener neeldeFileDropListener = new Listener(){
		
		public void filesDropped(File[] files) {
			AudioFingerPrinterFrame.this.filesDropped(files,true);
		}
	};
	
	private Listener hayStackFileDropListener = new Listener(){
		
		public void filesDropped(File[] files) {
			AudioFingerPrinterFrame.this.filesDropped(files,false);
		}
	};
	
	
	public void actionPerformed(ActionEvent arg0) {
		Runnable worker = new Runnable(){
			
			public void run() {
				Iterator<File> iterator = needleFileSet.iterator();
				int numberOfMatches = needleFileSet.size() * hayStackFileSet.size();
				appendProgress("Will do " + numberOfMatches + " fingerprint matches.");
				StopWatch w = new StopWatch();
				while (iterator.hasNext()) {
					File needle = iterator.next();
					AudioFingerprinter afp = new AudioFingerprinter(
							hayStackFileSet, needle);
					
					List<AudioFingerprintMatch> matches = afp.match();
					appendProgress("Best " + Math.min(15, matches.size()) + " matches for " + matches.get(0).getMatch().getName() + ":");
					for(int i = 0; i < Math.min(15, matches.size()) ; i++){
						AudioFingerprintMatch afpm = matches.get(i);
						if (afpm.isMatch()) {
							appendProgress("&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;" + (i+1) + ". " + "Match       (" + Math.round(afpm.getValue() * 100) + "%) with:\t" + afpm.getOriginal().getName());
						} else {
							appendProgress("&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;" + (i+1) + ". " + "Close match (" + Math.round(afpm.getValue() * 100) + "%) with:\t" + afpm.getOriginal().getName());
						}
					}
					
	
				}
				SwingUtilities.invokeLater(new Runnable() {
					public void run() {
						setWaitState(false);
					}
				});
				String timeSpan = w.formattedToString();
				appendProgress(numberOfMatches + " fingerprint matches done in " + timeSpan + ".");
			}
		};
		CardLayout cl = (CardLayout) contentPanel.getLayout();		
		if(searchButton.getText().equals("Search")){
			searchButton.setText("New search");
			new Thread(worker,"Fingerprinter").start();
			setWaitState(true);
			cl.show(contentPanel, "progress");
		}else{
			LOG.info("Ready for a new search query...");
			searchButton.setText("Search");
			progressLabel.setText("<html></html>");
			searchButton.setEnabled(false);
			cl.show(contentPanel, "files");
		}
	}
	
	private void setWaitState(boolean isWaiting){
		Component glassPane = this.getGlassPane();
		searchButton.setEnabled(!isWaiting);		
		if (isWaiting) {
			glassPane.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
			glassPane.setVisible(true);
		} else {
			hayStackFileSet.clear();
			needleFileSet.clear();
			hayStackFiles.setText("");
			needleFiles.setText("");
			glassPane.setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
			glassPane.setVisible(false);
		}
	}
	
	private JComponent makeStatusBar() {
		JLabel statusBarLabel = new JLabel();
		statusBarLabel.setForeground(Color.GRAY);
		JLabelHandler.setupLoggerHandler(statusBarLabel);
		statusBarLabel.setText("  ");
		return statusBarLabel;
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
	
	
	
	public void filesDropped(File[] files,boolean isNeedle){
		Set<File> fileSet = isNeedle ? needleFileSet : hayStackFileSet;
		for (File file : files) {
			if(file.isDirectory()){
				for(String fileInDir : FileUtils.glob(file.getAbsolutePath(), Configuration.get(ConfKey.audio_file_name_pattern), true)){
					fileSet.add(new File(fileInDir));
				}
			}
			if(FileUtils.isAudioFile(file)){
				fileSet.add(file);
			}	
		}
		StringBuilder midiSB = new StringBuilder();
		StringBuilder scalaSB = new StringBuilder();
		Iterator<File> fileIterator = hayStackFileSet.iterator();
		while (fileIterator.hasNext()) {
			midiSB.append(fileIterator.next().getName()).append("<br>");
		}
		fileIterator = needleFileSet.iterator();
		while (fileIterator.hasNext()) {
			scalaSB.append(fileIterator.next().getName()).append("<br>");
		}
		hayStackFiles.setText("<html>" + midiSB.toString() + "</html>");
		needleFiles.setText("<html>" + scalaSB.toString() + "</html>");
		
		//enable the button if there is more than one file in each list
		searchButton.setEnabled(hayStackFileSet.size() > 0 && needleFileSet.size() > 0);
	}

	
	
	/**
	 * A panel with a fancy header: Fancy!!
	 * @author Joren Six
	 */
	private static class HeaderPanel extends JPanel {
		private static final long serialVersionUID = 4852976337236606173L;
		private final ImageIcon icon = TarsosFrame.createImageIcon("/be/hogent/tarsos/ui/resources/tarsos_logo_small.png");
		private final String title = "Tarsos - Robust Audio Fingerprinting using Pitch Class Histograms";
		private final String staticHelp = "Drag and drop audio files in the haystack and search for a needle...";

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
			
			setPreferredSize(new Dimension(600, this.icon.getIconHeight() + 25));
			setMinimumSize(getPreferredSize());
		}
	}

}
