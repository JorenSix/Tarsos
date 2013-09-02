package be.hogent.tarsos.ui.link;

import java.awt.Color;
import java.awt.Dimension;
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

import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JSplitPane;

import be.hogent.tarsos.Tarsos;
import be.hogent.tarsos.tarsossegmenter.util.io.SongFileFilter;
import be.hogent.tarsos.transcoder.ffmpeg.EncoderException;
import be.hogent.tarsos.ui.BackgroundTask;
import be.hogent.tarsos.ui.ProgressDialog;
import be.hogent.tarsos.ui.link.ViewPort.ViewPortChangedListener;
import be.hogent.tarsos.ui.link.coordinatessystems.Units;
import be.hogent.tarsos.ui.link.layers.Layer;
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

	private JMenu viewMenu;
	private JMenuBar menuBar;
	private int panelID;
	private int linkedPanelCount;

	private JSplitPane lastSplitPane;

	public static void main(String... strings) {
		configureLogging();
		Configuration.checkForConfigurationAndWriteDefaults();
		Tarsos.configureDirectories(log);
		LinkedFrame.getInstance();
	}

	protected JSplitPane getLastSplitPane() {
		return this.lastSplitPane;
	}

	private LinkedFrame() {
		super();
		panels = new HashMap<String, LinkedPanel>();
		panelID = 0;
		linkedPanelCount = 0;
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
			tempPane.setDividerLocation((double) (1.0 / (double) (linkedPanelCount - count)));
			tempPane = (JSplitPane) tempPane.getRightComponent();
			count++;
		}
	}

	private void addPanel(Units x, Units y, Color bgColor) {
		if (linkedPanelCount != 0) {
			createNewSplitPane();
		}
		LinkedPanel p = new LinkedPanel();
		p.initialise(x, y);
		p.getViewPort().addViewPortChangedListener(this);
		panels.put("Panel " + panelID, p);
		lastSplitPane.add(p, JSplitPane.TOP);
		linkedPanelCount++;
		doSplitPaneLayout();
		viewMenu.add(createPanelSubMenu("Panel " + panelID));
		panelID++;
	}

	public void updatePanelMenus() {
		for (int i = 2; i < viewMenu.getItemCount(); i++) {
			updatePanelMenu((JMenu) viewMenu.getItem(i));
		}
	}

	private void updatePanelMenu(final JMenu subMenu) {
		for (int i = subMenu.getItemCount() - 1; i >= 3; i--) {
			subMenu.remove(subMenu.getItem(i));
		}
		for (final Layer l : panels.get(subMenu.getText()).getLayers()) {
			JMenu layerMenuItem = new JMenu(l.getName());
			JMenuItem deleteLayerMenuItem = new JMenuItem("Delete...");
			deleteLayerMenuItem.addActionListener(new ActionListener() {

				public void actionPerformed(ActionEvent arg0) {
					int result = JOptionPane.showConfirmDialog(
							LinkedFrame.this,
							"Are you sure you want to delete this layer?", "Delete layer?", JOptionPane.OK_CANCEL_OPTION);
					if (result == JOptionPane.OK_OPTION) {
						panels.get(subMenu.getText()).deleteLayer(l);
						LinkedFrame.instance.updatePanelMenu(subMenu);
					}
				}

			});
			layerMenuItem.add(deleteLayerMenuItem);
			subMenu.add(layerMenuItem);
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
						// @TODO: indien laatste panel is -> nieuwe empty panel
						// maken!!
						LinkedFrame.this.setContentPane((JSplitPane) (sp
								.getBottomComponent()));
					} else {
						parent.remove(2);
						if (sp.getBottomComponent() != null) {
							parent.add(sp.getBottomComponent(),
									JSplitPane.BOTTOM);
						}
					}
					if (lastSplitPane.getTopComponent() == panelToRemove) {
						lastSplitPane = parent;
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
			JOptionPane.showMessageDialog(this,
					"Please, first load a audio file.",
					"No audio file loaded!", JOptionPane.ERROR_MESSAGE);
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
					addPanel(myDialog.getXUnits(), myDialog.getYUnits(),
							Color.white);
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
		for (LinkedPanel panel : panels.values()) {
			panel.repaint();
		}
	}

	public void viewPortChanged(ViewPort newViewPort) {
		updatePanels();
	}

	private void buildStdSetUp() {
		this.addPanel(Units.TIME, Units.AMPLITUDE, Color.WHITE);
		LinkedPanel panel = (LinkedPanel) this.lastSplitPane.getTopComponent();
		panel.addLayer(new WaveFormLayer(panel));
		this.addPanel(Units.TIME, Units.FREQUENCY, Color.WHITE);
		updatePanelMenus();
	}
}