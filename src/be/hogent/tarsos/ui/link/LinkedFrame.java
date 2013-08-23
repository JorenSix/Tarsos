package be.hogent.tarsos.ui.link;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.Logger;

import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;

import be.hogent.tarsos.Tarsos;
import be.hogent.tarsos.tarsossegmenter.util.io.SongFileFilter;
import be.hogent.tarsos.transcoder.ffmpeg.EncoderException;
import be.hogent.tarsos.ui.BackgroundTask;
import be.hogent.tarsos.ui.ProgressDialog;
import be.hogent.tarsos.ui.link.ViewPort.ViewPortChangedListener;
import be.hogent.tarsos.ui.link.coordinatessystems.CoordinateSystem;
import be.hogent.tarsos.ui.link.coordinatessystems.TimeAmpCoordinateSystem;
import be.hogent.tarsos.ui.link.coordinatessystems.TimeCentCoordinateSystem;
import be.hogent.tarsos.ui.link.coordinatessystems.Units;
import be.hogent.tarsos.ui.link.layers.coordinatesystemlayers.AmplitudeCoordinateSystemLayer;
import be.hogent.tarsos.ui.link.layers.coordinatesystemlayers.CentsCoordinateSystemLayer;
import be.hogent.tarsos.ui.link.layers.coordinatesystemlayers.TimeCoordinateSystemLayer;
import be.hogent.tarsos.ui.link.layers.featurelayers.ConstantQLayer;
import be.hogent.tarsos.ui.link.layers.featurelayers.FeatureLayer;
import be.hogent.tarsos.ui.link.layers.featurelayers.WaveFormLayer;
import be.hogent.tarsos.util.AudioFile;
import be.hogent.tarsos.util.ConfKey;
import be.hogent.tarsos.util.Configuration;
import be.hogent.tarsos.util.FileUtils;

//Voor mezelf:
//Per layer een aparte audiodispatcher
//FrameSize en overlapping per audiodispatcher

public class LinkedFrame extends JFrame implements ViewPortChangedListener {

	private static final long serialVersionUID = 7301610309790983406L;

	private static LinkedFrame instance;

	private static HashMap<String, LinkedPanel> panels;
	private static Logger log;
	private static final String LOG_PROPS = "/be/hogent/tarsos/util/logging.properties";
	private AudioFile audioFile;
	
	private JMenu optionMenu;
	private JMenuBar menuBar;
	private int panelID;

	public static void main(String... strings) {
		configureLogging();
		Configuration.checkForConfigurationAndWriteDefaults();
		Tarsos.configureDirectories(log);
		LinkedFrame.getInstance();
	}

	private LinkedFrame() {
		super();
		panels = new HashMap<String, LinkedPanel>();
		this.setPreferredSize(new Dimension(800,400));
	}

	public static LinkedFrame getInstance() {
		if (instance == null) {
			instance = new LinkedFrame();
			instance.initialise();
		}
		return instance;
	}

	public void initialise() {
		this.getContentPane().setLayout(new GridLayout(0, 1, 1, 1));
		
		this.setJMenuBar(createMenu());


		pack();
		setLocationRelativeTo(null);
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		setVisible(true);
	}
	
	private void addPanel(CoordinateSystem cs, Color bgColor){
		LinkedPanel p = new LinkedPanel(cs);
		p.getViewPort().addViewPortChangedListener(this);
		p.setBackgroundLayer(bgColor);
		panels.put("Panel " + panelID, p);
		this.getContentPane().add(p);
		optionMenu.add(createPanelSubMenu("Panel " + panelID));
		panelID++;
		menuBar.revalidate();
		
	}
	
	private JMenu createPanelSubMenu(final String panelName){
		final JMenu subMenu = new JMenu(panelName);
		JMenuItem addLayerMenuItem = new JMenuItem("Add layer...");
		addLayerMenuItem.addActionListener(new ActionListener(){

			public void actionPerformed(ActionEvent arg0) {
				//TODO
				AddLayerDialog ld = new AddLayerDialog(LinkedFrame.this, panels.get(panelName), true, "Add layer to " + panelName);
				if (ld.getAnswer()){
					panels.get(panelName).addLayer(ld.getLayer());
				 }
			}
			
		});
		
		JMenuItem deletePanelMenuItem = new JMenuItem("Delete Panel...");
		deletePanelMenuItem.addActionListener(new ActionListener(){

			public void actionPerformed(ActionEvent arg0) {
				int result = JOptionPane.showConfirmDialog(LinkedFrame.this, "Are you sure you want to delete this panel?");	
				if (result == JOptionPane.OK_OPTION){
					LinkedFrame.this.getContentPane().remove(panels.get(panelName));
					panels.remove(panelName);
					optionMenu.remove(subMenu);
					menuBar.revalidate();
				}
			}
			
		});
		subMenu.add(addLayerMenuItem);
		subMenu.add(deletePanelMenuItem);
		subMenu.addSeparator();
		
		return subMenu;
	}

	private static void configureLogging() {
		// a default (not configured) logger
		log = Logger.getLogger(Tarsos.class.getName());
		try {
			final InputStream stream = Tarsos.class
					.getResourceAsStream(LOG_PROPS);
			LogManager.getLogManager().readConfiguration(stream);
			// a configured logger
			log = Logger.getLogger(Tarsos.class.getName());
		} catch (final SecurityException e) {
			log.log(Level.SEVERE, e.getLocalizedMessage(), e);
		} catch (final IOException e) {
			log.log(Level.SEVERE, e.getLocalizedMessage(), e);
		}
	}

	public AudioFile getAudioFile() {
		return audioFile;
	}

	public void setAudioFile(AudioFile file) {
		this.audioFile = file;
	}

	public void setNewAudioFile(final File newFile) {
		// AnnotationPublisher.getInstance().clearTree();
		TranscodingTask transcodingTask = new TranscodingTask(newFile);
		final List<BackgroundTask> detectorTasks = new ArrayList<BackgroundTask>();
		detectorTasks.add(transcodingTask);
		transcodingTask.addHandler(new BackgroundTask.TaskHandler() {

			// @Override
			public void taskInterrupted(BackgroundTask backgroundTask,
					Exception e) {
			}

			// @Override
			public void taskDone(BackgroundTask backgroundTask) {
				if (backgroundTask instanceof TranscodingTask) {
					try {
						LinkedFrame.getInstance().setAudioFile(
								new AudioFile(
										((TranscodingTask) backgroundTask)
												.getAudioFile()
												.transcodedPath()));
					} catch (EncoderException e) {
						// @TODO: errorafhandeling
						e.printStackTrace();
					}
				}
			}
		});
		String title = "Progress: "
				+ FileUtils.basename(newFile.getAbsolutePath());

		// AnnotationPublisher.getInstance().clear();
		// AnnotationPublisher.getInstance().extractionStarted();
		final ProgressDialog dialog = new ProgressDialog(
				LinkedFrame.getInstance(), title, transcodingTask,
				detectorTasks);
		dialog.addPropertyChangeListener(new PropertyChangeListener() {

			// @Override
			public void propertyChange(PropertyChangeEvent evt) {
				if (evt.getPropertyName().equals("allTasksFinished")) {
					// onAudioFileChange();
					// AnnotationPublisher.getInstance().extractionFinished();
				}
			}
		});
		dialog.pack();
		dialog.setVisible(true);
	}

	private class TranscodingTask extends BackgroundTask {

		private final File newFile;
		AudioFile transcodedAudioFile;

		protected TranscodingTask(final File file) {
			super("Transcoding " + FileUtils.basename(file.getAbsolutePath()),
					false);
			newFile = file;
		}

		@Override
		public Void doInBackground() {
			Runnable runTranscoder = new Runnable() {

				// @Override
				public void run() {
					try {
						transcodedAudioFile = new AudioFile(
								newFile.getAbsolutePath());
					} catch (EncoderException e) {
						interrupt(TranscodingTask.this, e);
					}
				}
			};
			// Do the actual detection in the background
			Thread t = new Thread(runTranscoder, getName());
			t.start();
			setProgress(50);
			try {
				t.join();
			} catch (Exception e) {
				e.printStackTrace();
			}
			setProgress(100);
			return null;
		}

		public AudioFile getAudioFile() {
			return transcodedAudioFile;
		}
	}

	public void analyseAudioFile() {
		for (LinkedPanel p : panels.values()) {
			p.initialiseLayers();
			p.calculateLayers();
		}
	}

	private JMenuBar createMenu() {
		menuBar = new JMenuBar();

		JMenu fileMenu = new JMenu("File");

		JMenuItem loadSongMenuItem = new JMenuItem("Load song...");
		loadSongMenuItem.addActionListener(new ActionListener() {
//			@Override
			public void actionPerformed(ActionEvent e) {
				JFileChooser fc = new JFileChooser();
				fc.setAcceptAllFileFilterUsed(false);
				fc.setFileSelectionMode(JFileChooser.FILES_ONLY);
				fc.addChoosableFileFilter(new SongFileFilter());
				File dir = Configuration.getFile(ConfKey.file_import_dir);
				if (dir.exists() && dir.isDirectory()) {
					fc.setCurrentDirectory(dir);
				}
				int returnVal = fc.showOpenDialog(LinkedFrame.getInstance());
				if (returnVal == JFileChooser.APPROVE_OPTION) {
					// TODO
					// runButton.setEnabled(false);
					File file = fc.getSelectedFile();
					// Configuration.set(ConfKey.file_import_dir,
					// file.getParent());
					setNewAudioFile(file);
				}
			}
		});

		JMenuItem exitMenuItem = new JMenuItem("Exit");
		exitMenuItem.addActionListener(new ActionListener() {
			// @Override
			public void actionPerformed(ActionEvent e) {
				LinkedFrame.getInstance().dispose();
			}
		});

		JMenuItem runMenuItem = new JMenuItem("Analyse");
		runMenuItem.addActionListener(new ActionListener() {

			// @Override
			public void actionPerformed(ActionEvent e) {
				analyseAudioFile();
				for (LinkedPanel panel : panels.values()) {
					panel.repaint();
				}
			}

		});

		fileMenu.add(loadSongMenuItem);
		fileMenu.addSeparator();
		fileMenu.add(runMenuItem);
		fileMenu.addSeparator();
		fileMenu.add(exitMenuItem);

		optionMenu = new JMenu("Settings");
		
		JMenuItem panelMenuItem = new JMenuItem("Add panel...");
		panelMenuItem.addActionListener(new ActionListener() {

			public void actionPerformed(ActionEvent arg0) {
				 AddPanelDialog myDialog = new AddPanelDialog(LinkedFrame.this, true, "Create new panel");
				 if (myDialog.getAnswer()){
					 addPanel(getCoordinateSystem(myDialog.getXUnits(), myDialog.getYUnits()), Color.white);
				 }
			}

		});
		
		optionMenu.add(panelMenuItem);
		menuBar.add(fileMenu);
		menuBar.add(optionMenu);
		
		return menuBar;
	}

	private void updatePanels() {
		for (LinkedPanel panel : panels.values()) {
			panel.revalidate();
			panel.repaint();
		}
	}

	public void viewPortChanged(ViewPort newViewPort) {
		updatePanels();
	}
	
	private CoordinateSystem getCoordinateSystem(Units xUnits, Units yUnits){
		if (xUnits==Units.TIME_SSS){
			if (yUnits==Units.FREQUENCY_CENTS){
				return new TimeCentCoordinateSystem(200,8000);
			} else if (yUnits==Units.AMPLITUDE){
				return new TimeAmpCoordinateSystem(-1000, 1000);
			}
		}
		return null;
	}
}