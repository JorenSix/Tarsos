package be.tarsos.ui.link;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionListener;
import java.awt.geom.Point2D;
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
import javax.swing.ButtonGroup;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRadioButtonMenuItem;
import javax.swing.JSplitPane;
import javax.swing.UIManager;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import be.tarsos.Tarsos;
import be.tarsos.tarsossegmenter.util.io.SegmentationFileFilter;
import be.tarsos.tarsossegmenter.util.io.SongFileFilter;
import be.tarsos.transcoder.ffmpeg.EncoderException;
import be.tarsos.ui.link.ViewPort.ViewPortChangedListener;
import be.tarsos.ui.link.coordinatessystems.CoordinateSystem;
import be.tarsos.ui.link.coordinatessystems.ICoordinateSystem;
import be.tarsos.ui.link.coordinatessystems.Quantity;
import be.tarsos.ui.link.coordinatessystems.Unit;
import be.tarsos.ui.link.io.SegmentationFileParser;
import be.tarsos.ui.link.layers.Layer;
import be.tarsos.ui.link.layers.LayerUtilities;
import be.tarsos.ui.link.layers.featurelayers.FeatureLayer;
import be.tarsos.ui.link.layers.featurelayers.WaveFormLayer;
import be.tarsos.ui.link.layers.segmentationlayers.SegmentationLayer;
import be.tarsos.ui.link.segmentation.Segmentation;
import be.tarsos.ui.util.BackgroundTask;
import be.tarsos.ui.util.ProgressDialog;
import be.tarsos.util.AudioFile;
import be.tarsos.util.ConfKey;
import be.tarsos.util.Configuration;
import be.tarsos.util.FileUtils;

//Voor mezelf:
//Per layer een aparte audiodispatcher
//FrameSize en overlapping per audiodispatcher

public class LinkedFrame extends JFrame implements ViewPortChangedListener,
		MouseMotionListener {

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
	private int mouseX;
	private JSplitPane contentPane;
	private JSplitPane lastSplitPane;
	private JLabel statusLabel;

	private float lowerFrequencyLimit;
	private float upperFrequencyLimit;

	public float getLowerFrequencyLimit() {
		return lowerFrequencyLimit;
	}

	public void setLowerFrequencyLimit(float lowerFrequencyLimit) {
		this.lowerFrequencyLimit = lowerFrequencyLimit;
	}

	public float getUpperFrequencyLimit() {
		return upperFrequencyLimit;
	}

	public void setUpperFrequencyLimit(float upperFrequencyLimit) {
		this.upperFrequencyLimit = upperFrequencyLimit;
	}

	public static void main(String... strings) {
		configureLogging();
		Configuration.checkForConfigurationAndWriteDefaults();
		Tarsos.configureDirectories(log);
		UIManager.getDefaults().put("SplitPane.border", BorderFactory.createEmptyBorder());
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
		for (Quantity q: Quantity.values()){
			q.setUnit(Unit.NONE);
		}
		this.setMinimumSize(new Dimension(800, 400));
		this.setContentPane(new JPanel(new BorderLayout()));
		contentPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
//		contentPane.set
		this.lastSplitPane = contentPane;
		this.getContentPane().add(contentPane, BorderLayout.CENTER);
		this.setJMenuBar(createMenu());

		setLocationRelativeTo(null);
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		pack();
		contentPane.setDividerLocation(0);
		buildStdSetUp();
		this.getContentPane().add(this.createStatusBar(), BorderLayout.SOUTH);
		this.setFocusable(true);
		setVisible(true);

		this.lowerFrequencyLimit = 150;
		this.upperFrequencyLimit = 4000;
		

	}

	public void createNewSplitPane() {
		lastSplitPane.setDividerSize(2);
		JSplitPane sp = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
		sp.setDividerSize(0);
		lastSplitPane.add(sp, JSplitPane.BOTTOM);
		lastSplitPane = sp;
	}

	private void doSplitPaneLayout() {
		JSplitPane tempPane = contentPane;
		int count = 0;
		while (tempPane != null) {
			tempPane.setDividerLocation((double) (1.0 / (double) (linkedPanelCount - count)));
			tempPane = (JSplitPane) tempPane.getRightComponent();
			count++;
		}
	}

	private JPanel createStatusBar() {
		JPanel statusBar = new JPanel(new BorderLayout());
		statusBar.setPreferredSize(new Dimension(0, 16));
		statusBar.setBorder(BorderFactory.createMatteBorder(1, 1, 1, 1,
				Color.black));
		statusBar.setBackground(Color.LIGHT_GRAY);

		statusLabel = new JLabel();
		statusBar.add(statusLabel, BorderLayout.EAST);
		return statusBar;
	}

	public LinkedPanel addPanel(Quantity x, Quantity y, Color bgColor) {
		LinkedPanel lastPanel = (LinkedPanel)lastSplitPane.getTopComponent();
		if (linkedPanelCount != 0) {
			createNewSplitPane();
		}
		LinkedPanel p = new LinkedPanel();
		if (lastPanel != null){
			p.setUpperPanel(lastPanel);
			lastPanel.setLowerPanel(p);
		}
		p.addMouseMotionListener(this);
		p.initialise(x, y);
		p.getViewPort().addViewPortChangedListener(this);
		panels.put("Panel " + panelID, p);
		lastSplitPane.add(p, JSplitPane.TOP);
		linkedPanelCount++;
		doSplitPaneLayout();
		viewMenu.add(createPanelSubMenu("Panel " + panelID));
		panelID++;
		return p;
	}

//	public void setCurrentStatus(String panelName, double x, double y,
//			Quantity xUnits, Quantity yUnits) {
//		DecimalFormat nft = new DecimalFormat("#0.##");
//		nft.setDecimalSeparatorAlwaysShown(false);
//		statusLabel
//				.setText(panelName + " - X: " + nft.format(x)
//						+ xUnits.getUnit().getUnit() + ", Y: " + nft.format(y)
//						+ yUnits.getUnit().getUnit());
//	}
	
	public void setCurrentStatus(String panelName, Point2D p, Quantity xUnits, Quantity yUnits) {
		statusLabel.setText(panelName + " - X: " + xUnits.getFormattedString(p.getX()) + ", Y: " + yUnits.getFormattedString(p.getY()));
//		DecimalFormat nft = new DecimalFormat("#0.##");
//		nft.setDecimalSeparatorAlwaysShown(false);
//		statusLabel
//				.setText(panelName + " - X: " + nft.format(x)
//						+ xUnits.getUnit().getUnit() + ", Y: " + nft.format(y)
//						+ yUnits.getUnit().getUnit());
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
							"Are you sure you want to delete this layer?",
							"Delete layer?", JOptionPane.OK_CANCEL_OPTION);
					if (result == JOptionPane.OK_OPTION) {
						panels.get(subMenu.getText()).deleteLayer(l);
						LinkedFrame.instance.updatePanelMenu(subMenu);
					}
				}

			});
			if (l instanceof FeatureLayer) {
				JMenuItem calculateLayerMenuItem = new JMenuItem("Calculate");
				calculateLayerMenuItem.addActionListener(new ActionListener() {
					public void actionPerformed(ActionEvent arg0) {
						if (LinkedFrame.getInstance().getAudioFile() != null) {
							((FeatureLayer) l).initialise();
							((FeatureLayer) l).run();
							updatePanels();
						} else {
							JOptionPane.showMessageDialog(
									LinkedFrame.getInstance(),
									"Please, first load a audio file.",
									"No audio file loaded!",
									JOptionPane.ERROR_MESSAGE);
						}
					}
				});
				layerMenuItem.add(calculateLayerMenuItem);
			}
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
					JSplitPane sp = (JSplitPane) LinkedFrame.this.contentPane;
					LinkedPanel panelToRemove = panels.get(panelName);
					if (panelToRemove.getUpperPanel() != null){
						panelToRemove.getUpperPanel().setLowerPanel(panelToRemove.getLowerPanel());
						if (panelToRemove.getLowerPanel() != null){
							panelToRemove.getLowerPanel().setUpperPanel(panelToRemove.getUpperPanel());
						}
					}
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
		Segmentation.getInstance().clear();
		Segmentation.getInstance().setCalculated(false);
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

					// File dir =
					// Configuration.getFile(ConfKey.file_import_dir);
					File file = fc.getSelectedFile();
					Configuration.set(ConfKey.file_import_dir, file.getParent());
					// file.getParent());
					setNewAudioFile(file);
				}
			}
		});

		JMenuItem loadSegmentationMenuItem = new JMenuItem(
				"Load segmentation file...");
		loadSegmentationMenuItem.addActionListener(new ActionListener() {
			// @Override
			public void actionPerformed(ActionEvent e) {
				if (LinkedFrame.getInstance().getAudioFile() != null) {
					JFileChooser fc = new JFileChooser();
					fc.setAcceptAllFileFilterUsed(false);
					fc.setFileSelectionMode(JFileChooser.FILES_ONLY);
					fc.addChoosableFileFilter(new SegmentationFileFilter());
					File dir = Configuration.getFile(ConfKey.file_import_dir);
					if (dir.exists() && dir.isDirectory()) {
						fc.setCurrentDirectory(dir);
					}
					int returnVal = fc.showOpenDialog(LinkedFrame.this);
					if (returnVal == JFileChooser.APPROVE_OPTION) {
						File file = fc.getSelectedFile();
						// model.getSegmentation().clearAll();
						Configuration.set(ConfKey.file_import_dir,
								file.getParent());
						if (file.getName().toLowerCase().endsWith(".textgrid")) {
							SegmentationFileParser.parseFile(file
									.getAbsolutePath());
							Segmentation.getInstance().setCalculated(true);
						} else if (file.getName().toLowerCase()
								.endsWith(".csv")) {
							// TODO
							 SegmentationFileParser.parseCSVFile(
							 file.getAbsolutePath());
							 Segmentation.getInstance().setCalculated(true);
						}
						
						for (LinkedPanel p : panels.values()) {
							for (Layer l : p.getLayers()) {
								if (l instanceof SegmentationLayer) { // TODO ||
																		// l
																		// instanceof
																		// BeatLayer
									((SegmentationLayer) l).run();
								}
							}
						}
						updatePanels();
						updatePanelMenus();
					}
				} else {
					JOptionPane.showMessageDialog(LinkedFrame.this,
							"Please open a soundfile first!",
							"Error: No Soundfile found",
							JOptionPane.ERROR_MESSAGE);
				}
			}
		});
		//
		// JMenuItem saveMenuItem = new JMenuItem("Save segmentation as...");
		// saveMenuItem.addActionListener(new ActionListener() {
		// // @Override
		// public void actionPerformed(ActionEvent e) {
		// JFileChooser fc = new JFileChooser();
		// fc.setAcceptAllFileFilterUsed(false);
		// fc.setFileSelectionMode(JFileChooser.FILES_ONLY);
		// fc.addChoosableFileFilter(SegmentationFileFilter.getTextGridFileFilter());
		// fc.addChoosableFileFilter(SegmentationFileFilter.getCSVFileFilter());
		//
		// File dir = Configuration.getFile(ConfKey.file_export_dir);
		// if (dir.exists() && dir.isDirectory()) {
		// fc.setCurrentDirectory(dir);
		// }
		//
		// int returnVal = fc.showSaveDialog(LinkedFrame.this);
		// if (returnVal == JFileChooser.APPROVE_OPTION) {
		// File file = fc.getSelectedFile();
		// String extension = fc.getFileFilter().getDescription();
		// Configuration.set(ConfKey.file_export_dir, file.getParent());
		// if (extension.equals("*.TextGrid")) {
		// SegmentationFileParser.writeToFile(file.getParent() + "/" +
		// file.getName().split("\\.")[0] + ".TextGrid",
		// model.getSegmentation());
		// }
		// if (extension.equals("*.csv")) {
		// SegmentationFileParser.writeToCSVFile(file.getParent() + "/" +
		// file.getName().split("\\.")[0] + ".csv", model.getSegmentation());
		// }
		// }
		// }
		// });

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
		fileMenu.add(loadSegmentationMenuItem);
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
		
		JMenu settingsMenu = new JMenu("Settings");
		Unit.SECONDS.getQuantity();
		JMenu settingsTimeUnit = new JMenu("Time unit");
		ButtonGroup group = new ButtonGroup();
		for(final Unit u: Unit.getUnitsForQuantity(Quantity.TIME)){
			JRadioButtonMenuItem rb = new JRadioButtonMenuItem(u.getUnit());
			rb.addChangeListener(new ChangeListener(){

				public void stateChanged(ChangeEvent arg0) {
					if (((JRadioButtonMenuItem)arg0.getSource()).isSelected())
						Quantity.TIME.setUnit(u);
				}
				
			});
			group.add(rb);
			settingsTimeUnit.add(rb);
			if (u == Unit.SECONDS){
				rb.setSelected(true);
			}
		}
       
		JMenu settingsFrequencyUnit = new JMenu("Frequency unit");
		ButtonGroup group2 = new ButtonGroup();
		for(final Unit u: Unit.getUnitsForQuantity(Quantity.FREQUENCY)){
			JRadioButtonMenuItem rb = new JRadioButtonMenuItem(u.getUnit());
			rb.addChangeListener(new ChangeListener(){

				public void stateChanged(ChangeEvent arg0) {
					if (((JRadioButtonMenuItem)arg0.getSource()).isSelected())
						Quantity.FREQUENCY.setUnit(u);
				}
				
			});
			group2.add(rb);
			settingsFrequencyUnit.add(rb);
			if (u == Unit.CENTS){
				rb.setSelected(true);
			}
		}
		
		settingsMenu.add(settingsTimeUnit);
		settingsMenu.add(settingsFrequencyUnit);

		menuBar.add(settingsMenu);
		
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
		this.addPanel(Quantity.TIME, Quantity.AMPLITUDE, Color.WHITE);
		LinkedPanel panel = (LinkedPanel) this.lastSplitPane.getTopComponent();
		panel.addLayer(new WaveFormLayer(panel));
		this.addPanel(Quantity.TIME, Quantity.FREQUENCY, Color.WHITE);
		updatePanelMenus();
	}

	public void mouseClicked(MouseEvent arg0) {
		// TODO Auto-generated method stub

	}

	public void mouseEntered(MouseEvent arg0) {
		// TODO Auto-generated method stub

	}

	public void mouseExited(MouseEvent arg0) {
		// TODO Auto-generated method stub

	}

	public void mousePressed(MouseEvent arg0) {
		// TODO Auto-generated method stub

	}

	public void mouseReleased(MouseEvent arg0) {
		// TODO Auto-generated method stub

	}

	public void mouseDragged(MouseEvent arg0) {
		// TODO Auto-generated method stub

	}

	public void mouseMoved(MouseEvent e) {
		mouseX = e.getPoint().x;
		this.repaint();
		LinkedPanel lp = ((LinkedPanel) (e.getSource()));

		Graphics2D g = (Graphics2D) lp.getGraphics().create();
		g.setTransform(lp.updateTransform(g.getTransform()));
		Point2D currentPoint = LayerUtilities
				.pixelsToUnits(g, mouseX, e.getY());
		ICoordinateSystem cs = lp.getCoordinateSystem();
		String layerName = "No Layer";
		if (lp.getLayerNames() != null && lp.getLayerNames().size() > 0) {
			layerName = lp.getLayerNames().get(lp.getLayerNames().size() - 1);
		}
		setCurrentStatus(layerName, currentPoint, cs.getQuantityForAxis(CoordinateSystem.X_AXIS),
				cs.getQuantityForAxis(CoordinateSystem.Y_AXIS));
		lp.mouseMoved(currentPoint);
		g.dispose();
	}

	public int getMouseX() {
		return mouseX;
	}
	
	public HashMap<String, LinkedPanel> getPanels(){
		return panels;
	}
}