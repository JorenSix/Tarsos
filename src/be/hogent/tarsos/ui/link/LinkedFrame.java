package be.hogent.tarsos.ui.link;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.Logger;

import javax.swing.BorderFactory;
import javax.swing.DebugGraphics;
import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JSplitPane;
import javax.swing.MenuElement;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;

import org.noos.xing.mydoggy.plaf.ui.cmp.MultiSplitPane;

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
import be.hogent.tarsos.ui.link.layers.Layer;
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
	private JSplitPane lastSplitPane;
	// private LinkedList<JSplitPane> splitPanels;
	private static final long serialVersionUID = 7301610309790983406L;

	private static LinkedFrame instance;

	private static HashMap<String, LinkedPanel> panels;
	private static Logger log;
	private static final String LOG_PROPS = "/be/hogent/tarsos/util/logging.properties";
	private AudioFile audioFile;

	private JMenu viewMenu;
	private JMenuBar menuBar;
	private int panelID;
	private int linkedPanelCount;
	private boolean drawing = false;

	public static void main(String... strings) {
		configureLogging();
		Configuration.checkForConfigurationAndWriteDefaults();
		Tarsos.configureDirectories(log);
		LinkedFrame.getInstance();
	}

	private LinkedFrame() {
		super();
		panels = new HashMap<String, LinkedPanel>();
		panelID = 0;
		linkedPanelCount = 0;
		// this.setPreferredSize(new Dimension(800, 400));

		// this.setSize(new Dimension(800, 400));
	}

	public static LinkedFrame getInstance() {
		if (instance == null) {
			instance = new LinkedFrame();
			instance.initialise();
		}
		return instance;
	}

	public void initialise() {
		this.setMinimumSize(new Dimension(800, 400));
		JSplitPane contentPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
		this.lastSplitPane = contentPane;
		this.setContentPane(contentPane);
		this.setJMenuBar(createMenu());

		setLocationRelativeTo(null);
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		pack();
		contentPane.setDividerLocation(0);
		buildStdSetUp();
		setVisible(true);
	}

	public void createNewSplitPane() {
		lastSplitPane.setDividerSize(2);
		JSplitPane sp = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
		sp.setDividerSize(0);
		lastSplitPane.add(sp, JSplitPane.BOTTOM);
		lastSplitPane = sp;
	}

	private void doSplitPaneLayout() {
		JSplitPane tempPane = (JSplitPane) this.getContentPane();
		int count = 0;
		while (tempPane != null) {
			System.out.println("Panel " + count + ": "
					+ (double) (1.0 / (double) (linkedPanelCount - count))
					+ " Height: " + tempPane.getHeight() + " - Location: "
					+ tempPane.getDividerLocation());
			tempPane.setDividerLocation((double) (1.0 / (double) (linkedPanelCount - count)));
			tempPane = (JSplitPane) tempPane.getRightComponent();
			count++;
		}
	}

	private void addPanel(CoordinateSystem cs, Color bgColor) {
		if (linkedPanelCount != 0) {
			createNewSplitPane();
		}
		LinkedPanel p = new LinkedPanel(cs);
		p.getViewPort().addViewPortChangedListener(this);
		p.setBackgroundLayer(bgColor);
		panels.put("Panel " + panelID, p);
		lastSplitPane.add(p, JSplitPane.TOP);
		linkedPanelCount++;
		doSplitPaneLayout();
		viewMenu.add(createPanelSubMenu("Panel " + panelID));
		panelID++;
	}
	
	public void updatePanelMenus(){
		for (int i = 2; i < viewMenu.getItemCount(); i++){
			updatePanelMenu((JMenu)viewMenu.getItem(i));
		}
	}
	
	private void updatePanelMenu(JMenu subMenu){
		for (int i = subMenu.getItemCount()-1; i >= 3; i--){
			subMenu.remove(subMenu.getItem(i));
		}
		for (String s : panels.get(subMenu.getText()).getLayers()){
			subMenu.add(new JMenuItem(s));
		}
	}

	private JMenu createPanelSubMenu(final String panelName) {
		final JMenu subMenu = new JMenu(panelName);
		JMenuItem addLayerMenuItem = new JMenuItem("Add layer...");
		addLayerMenuItem.addActionListener(new ActionListener() {

			public void actionPerformed(ActionEvent arg0) {
				// TODO
				AddLayerDialog ld = new AddLayerDialog(LinkedFrame.this, panels
						.get(panelName), true, "Add layer to " + panelName);
				if (ld.getAnswer()) {
					panels.get(panelName).addLayer(ld.getLayer());
					updatePanelMenu(subMenu);
				}
			}

		});

		JMenuItem deletePanelMenuItem = new JMenuItem("Delete Panel...");
		deletePanelMenuItem.addActionListener(new ActionListener() {

			public void actionPerformed(ActionEvent arg0) {
				int result = JOptionPane.showConfirmDialog(LinkedFrame.this,
						"Are you sure you want to delete this panel?");
				if (result == JOptionPane.OK_OPTION) {
					JSplitPane parent = null;
					JSplitPane sp = (JSplitPane) LinkedFrame.this
							.getContentPane();
					LinkedPanel panelToRemove = panels.get(panelName);
					while (sp.getTopComponent() != panelToRemove) {
						parent = sp;
						sp = (JSplitPane) sp.getBottomComponent();
					}
					if (parent == null) {
						LinkedFrame.this.setContentPane((JSplitPane) (sp
								.getBottomComponent()));
					} else {
						parent.remove(2);
						if (sp.getBottomComponent() != null) {
							parent.add(sp.getBottomComponent(),
									JSplitPane.BOTTOM);
						}
					}
					panels.remove(panelName);
					viewMenu.remove(subMenu);
					linkedPanelCount--;
					menuBar.revalidate();
					LinkedFrame.this.doSplitPaneLayout();
				}
			}

		});
		subMenu.add(addLayerMenuItem);
		subMenu.add(deletePanelMenuItem);
		subMenu.addSeparator();

		return subMenu;
	}

	private static void configureLogging() {
		log = Logger.getLogger(Tarsos.class.getName());
		try {
			final InputStream stream = Tarsos.class
					.getResourceAsStream(LOG_PROPS);
			LogManager.getLogManager().readConfiguration(stream);
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
		if (this.audioFile != null) {
			for (LinkedPanel p : panels.values()) {
				p.initialiseLayers();
				p.calculateLayers();
			}
		} else {
			JOptionPane.showMessageDialog(this, "Please, first load a audio file.", "No audio file loaded!", JOptionPane.ERROR_MESSAGE);
		}
	}

	private JMenuBar createMenu() {
		menuBar = new JMenuBar();

		JMenu fileMenu = new JMenu("File");

		JMenuItem loadSongMenuItem = new JMenuItem("Load song...");
		loadSongMenuItem.addActionListener(new ActionListener() {
			// @Override
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
				updatePanels();
			}

		});

		fileMenu.add(loadSongMenuItem);
		fileMenu.addSeparator();
		fileMenu.add(runMenuItem);
		fileMenu.addSeparator();
		fileMenu.add(exitMenuItem);

		viewMenu = new JMenu("View");

		JMenuItem panelMenuItem = new JMenuItem("Add panel...");
		panelMenuItem.addActionListener(new ActionListener() {

			public void actionPerformed(ActionEvent arg0) {
				AddPanelDialog myDialog = new AddPanelDialog(LinkedFrame.this,
						true, "Create new panel");
				if (myDialog.getAnswer()) {
					addPanel(
							getCoordinateSystem(myDialog.getXUnits(),
									myDialog.getYUnits()), Color.white);
					LinkedFrame.this.updatePanelMenus();
				}
			}

		});

		viewMenu.add(panelMenuItem);
		viewMenu.addSeparator();
		menuBar.add(fileMenu);
		menuBar.add(viewMenu);

		return menuBar;
	}

	private void updatePanels() {
		if (!drawing) {
			drawing = true;
			for (LinkedPanel panel : panels.values()) {
				panel.repaint();
			}
			drawing = false;
		}
	}

	public void viewPortChanged(ViewPort newViewPort) {
		updatePanels();
	}

	private CoordinateSystem getCoordinateSystem(Units xUnits, Units yUnits) {
		if (xUnits == Units.TIME_SSS) {
			if (yUnits == Units.FREQUENCY_CENTS) {
				return new TimeCentCoordinateSystem(200, 8000);
			} else if (yUnits == Units.AMPLITUDE) {
				return new TimeAmpCoordinateSystem(-1000, 1000);
			}
		}
		return null;
	}

	private void buildStdSetUp() {
		CoordinateSystem cs = new TimeAmpCoordinateSystem(-1000, 1000);
		this.addPanel(cs, Color.WHITE);
		((LinkedPanel) this.lastSplitPane.getTopComponent())
				.addLayer(new WaveFormLayer((LinkedPanel) this.lastSplitPane
						.getTopComponent()));

		CoordinateSystem cs2 = new TimeCentCoordinateSystem(300, 8000);
		this.addPanel(cs2, Color.WHITE);
		updatePanelMenus();
	}
}