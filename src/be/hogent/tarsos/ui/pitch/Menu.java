package be.hogent.tarsos.ui.pitch;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Vector;

import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.Mixer.Info;
import javax.sound.sampled.UnsupportedAudioFileException;
import javax.swing.ButtonGroup;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JRadioButtonMenuItem;

import be.hogent.tarsos.midi.MidiCommon;
import be.hogent.tarsos.midi.MidiCommon.MoreMidiInfo;
import be.hogent.tarsos.midi.TarsosSynth;
import be.hogent.tarsos.midi.ToneSequenceBuilder;
import be.hogent.tarsos.sampled.SampledAudioUtilities;
import be.hogent.tarsos.sampled.pitch.Annotation;
import be.hogent.tarsos.sampled.pitch.PitchDetectionMode;
import be.hogent.tarsos.sampled.pitch.PitchUnit;
import be.hogent.tarsos.util.AudioFile;
import be.hogent.tarsos.util.BareBonesBrowserLaunch;
import be.hogent.tarsos.util.ConfKey;
import be.hogent.tarsos.util.Configuration;
import be.hogent.tarsos.util.Configuration.ConfigChangeListener;
import be.hogent.tarsos.util.FileUtils;
import be.hogent.tarsos.util.MenuScroller;
import be.hogent.tarsos.util.ScalaFile;
import be.hogent.tarsos.util.SignalPowerExtractor;
import be.hogent.tarsos.util.SimplePlot;
import be.hogent.tarsos.util.histogram.PitchClassHistogram;
import be.hogent.tarsos.util.histogram.PitchHistogram;

public class Menu extends JMenuBar implements ScaleChangedListener, AudioFileChangedListener {

	/**
	 * 
	 */
	private static final long serialVersionUID = -2446134006582897883L;
	
	private double[] scale = ScalaFile.westernTuning().getPitches();
	private AudioFile audioFile;

	public Menu() {
		addFileMenu();
		addSettingsMenu();
		addHelpMenu();
	}

	/**
	 * Log messages.
	 */
	//private static final Logger LOG = Logger.getLogger(Menu.class.getName());

	private void addFileMenu() {
		final JMenu menu = new JMenu("File");
		this.add(menu);

		JMenuItem item;

		item = new JMenuItem("Open...");
		item.addActionListener(importFileAction);
		menu.add(item);

		JMenu importMenu = new JMenu("Import");
		menu.add(importMenu);

		item = new JMenuItem("Scala file...");
		item.addActionListener(importFileAction);		
		importMenu.add(item);

		JMenu exportMenu = new JMenu("Export");
		menu.add(exportMenu);

		item = new JMenuItem("Scala file...");
		item.setToolTipText("Export the current peaks as a scala file.");
		item.addActionListener(exportScalaAction);
		exportMenu.add(item);

		exportMenu.addSeparator();

		item = new JMenuItem("Annotations CSV-file...");
		item.setToolTipText("Export a CVS file with annotations (in cent) over time (in seconds).");
		item.addActionListener(exportAnnotations);
		exportMenu.add(item);

		item = new JMenuItem("Pitch Histogram CSV-file...");
		item.setToolTipText("Export a CVS file with a pitch histogram.");
		item.addActionListener(exportPitchHistogram);
		exportMenu.add(item);
		

		item = new JMenuItem("Pitch Class Histogram CSV-file...");
		item.setToolTipText("Export a CVS file with a pitch class histogram.");
		item.addActionListener(exportPitchClassHistogram);
		exportMenu.add(item);

		exportMenu.addSeparator();
		
		item = new JMenuItem("Pitch Histogram PNG or EPS-file...");
		item.setToolTipText("Export a PNG or EPS file with a pitch histogram.");
		item.addActionListener(exportPitchHistogramImage);
		exportMenu.add(item);

		item = new JMenuItem("Pitch Class Histogram PNG or EPS-file...");
		item.setToolTipText("Export a PNG or EPS file with a pitch class histogram.");
		item.addActionListener(exportPitchClassHistogramImage);
		exportMenu.add(item);
		
		item = new JMenuItem("Pitch Class Histogram with octaves divided PNG or EPS-file...");
		item.setToolTipText("Pitch Class Histogram with octaves divided PNG or EPS-file");
		item.addActionListener(exportPitchClassHistogramImageWithOctaveDivision);
		exportMenu.add(item);
		
		exportMenu.addSeparator();
		

		item = new JMenuItem("Synthesized annotations...");
		item.setToolTipText("Export an audio file synthesized using the annotations.");
		
		item.addActionListener(exportSynthesizedAnnotations);
		exportMenu.add(item);
		
		final List<String> files = Configuration.getList(ConfKey.file_recent);
		if(!files.isEmpty()){
			menu.addSeparator();	
		}
		addRecentFilesToMenu(menu);
		
		//remove and add recent file menu if a file is opened
		//or imported (by drag and drop).
		Configuration.addListener(new ConfigChangeListener(){
			@Override
			public void configurationChanged(ConfKey key) {
				if(key == ConfKey.file_recent){
					for(JMenuItem menuItem : recentFilesMenuItems){
						menu.remove(menuItem);
					}
					addRecentFilesToMenu(menu);
				}
			}});	
	}
	
	List<JMenuItem> recentFilesMenuItems;
	private void addRecentFilesToMenu(final JMenu menu) {
		final List<String> files = Configuration.getList(ConfKey.file_recent);
		JMenuItem item;
		recentFilesMenuItems = new ArrayList<JMenuItem>();
		if (!files.isEmpty()) {
			int index = 1;
			for (final String recentFile : files) {
				item = new JMenuItem(index + " "
						+ FileUtils.basename(recentFile));
				item.setActionCommand(recentFile);
				item.addActionListener(openRecentFileAction);
				menu.add(item);
				recentFilesMenuItems.add(item);
				index++;
			}
		}
		
		menu.addSeparator();
		item = new JMenuItem("Exit");
		item.addActionListener(exitAction);
		recentFilesMenuItems.add(item);
		menu.add(item);		
	}

	private void addSettingsMenu() {
		JMenu menu = new JMenu("Settings");
		this.add(menu);

		JMenuItem item;

		addMixerMenu(menu);

		addMidiMenu(menu);

		menu.addSeparator();

		JMenu detectorsMenu = new JMenu("Pitch Detectors");
		menu.add(detectorsMenu);
		
		List<PitchDetectionMode> selectedModes =  PitchDetectionMode.selected();

		for (PitchDetectionMode mode : PitchDetectionMode.values()) {
			JCheckBoxMenuItem detectorItem = new JCheckBoxMenuItem(mode.name());
			detectorItem.setSelected(selectedModes.contains(mode));
			detectorItem.setActionCommand(mode.name());
			detectorItem.addActionListener(saveSelectedDetectorsAction);
			detectorsMenu.add(detectorItem);
		}

		menu.addSeparator();

		item = new JCheckBoxMenuItem("Tarsos Live");
		((JCheckBoxMenuItem) item).setSelected(Configuration
				.getBoolean(ConfKey.tarsos_live));
		item.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent arg0) {
				JCheckBoxMenuItem checkbox = (JCheckBoxMenuItem) arg0
						.getSource();
				boolean selected = checkbox.isSelected();
				Configuration.set(ConfKey.tarsos_live, selected);
			}
		});
		menu.add(item);
		
		menu.addSeparator();
		
		item = new JCheckBoxMenuItem("Reset on import");
		((JCheckBoxMenuItem) item).setSelected(Configuration
				.getBoolean(ConfKey.reset_on_import));
		item.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent arg0) {
				JCheckBoxMenuItem checkbox = (JCheckBoxMenuItem) arg0
						.getSource();
				boolean selected = checkbox.isSelected();
				Configuration.set(ConfKey.tarsos_live, selected);
			}
		});
		menu.add(item);

	}

	private void addMidiMenu(JMenu menu) {
		JMenu midiDevicesMenu = new JMenu("MIDI Devices");
		menu.add(midiDevicesMenu);

		JMenu outputMidiDevices = new JMenu("Output");
		midiDevicesMenu.add(outputMidiDevices);
		addMidiDevicesMenu(outputMidiDevices, false, true,
				midiOutputDeviceChange, ConfKey.midi_output_device);

		JMenu inputMidiDevices = new JMenu("Input");
		midiDevicesMenu.add(inputMidiDevices);
		addMidiDevicesMenu(inputMidiDevices, true, false,
				midiInputDeviceChange, ConfKey.midi_input_device);
		
		addInstrumentsMenu(midiDevicesMenu);
	}

	private void addMidiDevicesMenu(JMenu parent, boolean input,
			boolean output, ActionListener listener, ConfKey key) {
		ButtonGroup buttonGroup = new ButtonGroup();
		Vector<MoreMidiInfo> outputMidiInfo = MidiCommon.listDevices(input,
				output);
		int deviceIndex = 0;
		int currentIndex = Configuration.getInt(key);
		for (MoreMidiInfo info : outputMidiInfo) {
			JRadioButtonMenuItem radioButton = new JRadioButtonMenuItem(info
					.getInfo().getName());
			radioButton.setToolTipText(info.toString());
			radioButton.setActionCommand(String.valueOf(deviceIndex));
			if (deviceIndex == currentIndex) {
				radioButton.setSelected(true);
			}
			radioButton.addActionListener(listener);
			parent.add(radioButton);
			buttonGroup.add(radioButton);
			deviceIndex++;
		}
		if(outputMidiInfo.size()>20){
			MenuScroller.setScrollerFor(parent, 15);			
		}
	}
	
	private void addInstrumentsMenu(final JMenu menu){
		JMenu instrumentsMenu = new JMenu("Instruments");
		
		int currentIndex = Configuration.getInt(ConfKey.midi_instrument_index);
		int index=0;
		List<String> instruments = TarsosSynth.getInstance().availableInstruments();
		
		ButtonGroup buttonGroup = new ButtonGroup();
		for (String instrument:instruments) {
			JRadioButtonMenuItem radioButton = new JRadioButtonMenuItem(
					instrument);
			if (currentIndex == index) {
				radioButton.setSelected(true);
			}
			radioButton.setActionCommand(String.valueOf(index));
			radioButton.addActionListener(midiInstrumentChange);
			instrumentsMenu.add(radioButton);
			radioButton.setToolTipText(instrument);
			buttonGroup.add(radioButton);
			index++;
		}
		if(!instruments.isEmpty()){
			menu.addSeparator();
			menu.add(instrumentsMenu);
			if(instruments.size() > 20){
				MenuScroller.setScrollerFor(instrumentsMenu, 15);
			}
		}
	}

	private void addMixerMenu(JMenu menu) {
		JMenu mixersMenu = new JMenu("Audio Devices");
		menu.add(mixersMenu);

		JMenu outputMixers = new JMenu("Output");
		mixersMenu.add(outputMixers);
		addMixerDevicesMenu(outputMixers, true, false, mixerOutputDeviceChange,
				ConfKey.mixer_output_device);

		JMenu inputMixers = new JMenu("Input");
		mixersMenu.add(inputMixers);
		addMixerDevicesMenu(inputMixers, false, true, mixerInputDeviceChange,
				ConfKey.mixer_input_device);
	}

	private void addMixerDevicesMenu(JMenu parent, boolean supportsPlayback,
			boolean supportsRecording, ActionListener listener, ConfKey key) {
		ButtonGroup buttonGroup = new ButtonGroup();
		Vector<Info> mixerDevices = SampledAudioUtilities.getMixerInfo(
				supportsPlayback, supportsRecording);
		int mixerIndex = 0;
		int currentIndex = Configuration.getInt(key);
		for (Info info : mixerDevices) {
			JRadioButtonMenuItem radioButton = new JRadioButtonMenuItem(
					info.getName());
			if (mixerIndex == currentIndex) {
				radioButton.setSelected(true);
			}
			radioButton.setToolTipText(info.getDescription());
			radioButton.setActionCommand(String.valueOf(mixerIndex));
			radioButton.addActionListener(listener);
			parent.add(radioButton);
			buttonGroup.add(radioButton);
			mixerIndex++;
		}
	}

	private void addHelpMenu() {
		JMenu menu = new JMenu("About");
		this.add(menu);

		JMenuItem item;

		item = new JMenuItem("About Tarsos...");
		item.addActionListener(showAboutDialogAction);
		menu.add(item);

		item = new JMenuItem("Tarsos website...");
		item.addActionListener(openWebsiteAction);
		
		menu.add(item);
	}
	
	private ActionListener openWebsiteAction = new ActionListener() {
		@Override
		public void actionPerformed(ActionEvent e) {
			BareBonesBrowserLaunch.openURL("http://tarsos.0110.be");				
		}
	};
	
	private ActionListener saveSelectedDetectorsAction = new ActionListener(){
		@Override
		public void actionPerformed(final ActionEvent e) {
			JCheckBoxMenuItem checkbox = (JCheckBoxMenuItem) e.getSource();
			boolean selected = checkbox.isSelected();
			ConfKey key = ConfKey.pitch_tracker_list;
			List<PitchDetectionMode> currentList = PitchDetectionMode.selected();
			PitchDetectionMode mode = PitchDetectionMode.valueOf(e.getActionCommand());
			if(selected){
				currentList.add(mode);
			}else{
				currentList.remove(mode);	
			}
			Configuration.set(key, currentList);
		}
		
	};

	private ActionListener showAboutDialogAction = new ActionListener(){
		@Override
		public void actionPerformed(final ActionEvent e) {
			JFrame parent = Frame.getInstance();
			JOptionPane.showMessageDialog(parent, "Tarsos \n\nDeveloped at University College Ghent – Faculty of Music ");
		}
	};
	
	private ActionListener midiInputDeviceChange = new ActionListener() {
		@Override
		public void actionPerformed(ActionEvent arg0) {
			Configuration.set(ConfKey.midi_input_device,
					Integer.valueOf(arg0.getActionCommand()));
		}
	};
	
	private ActionListener midiInstrumentChange = new ActionListener() {
		@Override
		public void actionPerformed(ActionEvent arg0) {
			Configuration.set(ConfKey.midi_instrument_index,
					Integer.valueOf(arg0.getActionCommand()));
		}
	};

	private ActionListener midiOutputDeviceChange = new ActionListener() {
		@Override
		public void actionPerformed(ActionEvent arg0) {
			Configuration.set(ConfKey.midi_output_device,
					Integer.valueOf(arg0.getActionCommand()));
		}
	};

	private ActionListener mixerInputDeviceChange = new ActionListener() {
		@Override
		public void actionPerformed(ActionEvent arg0) {
			Configuration.set(ConfKey.mixer_input_device,
					Integer.valueOf(arg0.getActionCommand()));
		}
	};

	private ActionListener mixerOutputDeviceChange = new ActionListener() {
		@Override
		public void actionPerformed(ActionEvent arg0) {
			Configuration.set(ConfKey.mixer_output_device,
					Integer.valueOf(arg0.getActionCommand()));
		}
	};

	private ActionListener exitAction = new ActionListener() {
		@Override
		public void actionPerformed(ActionEvent arg0) {
			System.exit(0);
		}
	};
	
	private ActionListener importFileAction = new ActionListener() {
		@Override
		public void actionPerformed(ActionEvent e) {
			String dialogTitle = "Import Audio or Scala File";
			showFileChooserDialog(dialogTitle,JFileChooser.FILES_AND_DIRECTORIES,true, new ChosenFileHandler() {				
				@Override
				public void handleFile(final File chosenFile) {
					Frame.getInstance().setNewFile(chosenFile);
				}
			});
		}
	};
	
	private ActionListener openRecentFileAction = new ActionListener() {
		@Override
		public void actionPerformed(ActionEvent e) {
			File recentFile = new File(e.getActionCommand());
			Frame.getInstance().setNewFile(recentFile);
		}
	};

	private ActionListener exportScalaAction = new ActionListener() {
		@Override
		public void actionPerformed(final ActionEvent e) {
			String dialogTitle = "Export Scala File (.scl)";
			showFileChooserDialog(dialogTitle,JFileChooser.FILES_ONLY,false, new ChosenFileHandler() {				
				@Override
				public void handleFile(final File chosenFile) {
					String title = "Tarsos export";
					if(audioFile != null){
						title = title + " " + audioFile.basename();
					}
					ScalaFile scalaFile = new ScalaFile(title, scale);
					scalaFile.write(chosenFile.getAbsolutePath());
				}
			});
		}
	};
	
	private ActionListener exportAnnotations = new ActionListener() {
		@Override
		public void actionPerformed(ActionEvent e) {
			String dialogTitle = "Export Annotations (.csv)";
			showFileChooserDialog(dialogTitle,JFileChooser.FILES_ONLY,false, new ChosenFileHandler() {				
				@Override
				public void handleFile(final File chosenFile) {
					AnnotationPublisher ap = AnnotationPublisher.getInstance();
					List<Annotation> annotations = ap.getAnnotationTree().select(ap.getCurrentSelection());
					String fileName = chosenFile.getAbsolutePath();
					FileUtils.writePitchAnnotations(fileName, annotations);
				}
			});
		}
	};
	
	private ActionListener exportSynthesizedAnnotations =  new ActionListener() {
		@Override
		public void actionPerformed(final ActionEvent e) {
			String dialogTitle = "Export synthesized annotations (.wav)";
			showFileChooserDialog(dialogTitle,JFileChooser.FILES_ONLY,false, new ChosenFileHandler() {				
				@Override
				public void handleFile(final File chosenFile) {
					ToneSequenceBuilder builder = new ToneSequenceBuilder();
					AnnotationPublisher ap = AnnotationPublisher.getInstance();
					List<Annotation> annotations = ap.getAnnotationTree().select(ap.getCurrentSelection());
					final SignalPowerExtractor extr = new SignalPowerExtractor(audioFile);
					for(final Annotation annotation:annotations){
						double frequency = annotation.getPitch(PitchUnit.HERTZ);
						double realTime = annotation.getStart();
						builder.addTone(frequency, realTime, extr.powerAt(realTime, true));
					}
					try {
						builder.writeFile(chosenFile.getAbsolutePath(), 5);
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					} catch (UnsupportedAudioFileException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					} catch (LineUnavailableException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
			});		
		}
	};
	
	private ActionListener exportPitchHistogram = new ActionListener() {
		@Override
		public void actionPerformed(ActionEvent e) {
			String dialogTitle = "Export Pitch Histogram (.csv)";
			showFileChooserDialog(dialogTitle,JFileChooser.FILES_ONLY,false, new ChosenFileHandler() {				
				@Override
				public void handleFile(final File chosenFile) {
					AnnotationPublisher ap = AnnotationPublisher.getInstance();
					List<Annotation> annotations = ap.getAnnotationTree().select(ap.getCurrentSelection());
					PitchHistogram pitchHistogram = Annotation.pitchHistogram(annotations);
					String fileName = chosenFile.getAbsolutePath();
					pitchHistogram.export(fileName);
				}
			});
		}
	};
	
	private ActionListener exportPitchHistogramImage = new ActionListener() {
		@Override
		public void actionPerformed(ActionEvent e) {
			String dialogTitle = "Export Pitch Histogram (.png)";
			showFileChooserDialog(dialogTitle,JFileChooser.FILES_ONLY,false, new ChosenFileHandler() {				
				@Override
				public void handleFile(final File chosenFile) {
					AnnotationPublisher ap = AnnotationPublisher.getInstance();
					List<Annotation> annotations = ap.getAnnotationTree().select(ap.getCurrentSelection());
					PitchHistogram pitchHistogram = Annotation.pitchHistogram(annotations);
					String fileName = chosenFile.getAbsolutePath();
					SimplePlot plot = new SimplePlot();
					plot.addData(0, pitchHistogram);
					plot.save(fileName);
				}
			});
		}
	};
	
	private ActionListener exportPitchClassHistogramImage = new ActionListener() {
		@Override
		public void actionPerformed(ActionEvent e) {
			String dialogTitle = "Export Pitch Class Histogram (.png)";
			showFileChooserDialog(dialogTitle,JFileChooser.FILES_ONLY,false, new ChosenFileHandler() {				
				@Override
				public void handleFile(final File chosenFile) {
					AnnotationPublisher ap = AnnotationPublisher.getInstance();
					List<Annotation> annotations = ap.getAnnotationTree().select(ap.getCurrentSelection());
					PitchClassHistogram pitchClassHistogram = PitchClassHistogram.createToneScaleHistogram(annotations);
					String fileName = chosenFile.getAbsolutePath();
					SimplePlot plot = new SimplePlot();
					plot.addData(0, pitchClassHistogram);
					plot.save(fileName);
				}
			});
		}
	};
	
	private ActionListener exportPitchClassHistogramImageWithOctaveDivision = new ActionListener() {
		@Override
		public void actionPerformed(ActionEvent e) {
			String dialogTitle = "Export Pitch Class Histogram (.png)";
			showFileChooserDialog(dialogTitle,JFileChooser.FILES_ONLY,false, new ChosenFileHandler() {				
				@Override
				public void handleFile(final File chosenFile) {
					AnnotationPublisher ap = AnnotationPublisher.getInstance();
					List<Annotation> annotations = ap.getAnnotationTree().select(ap.getCurrentSelection());
					PitchHistogram pitchHistogram = Annotation.pitchHistogram(annotations);
					String fileName = chosenFile.getAbsolutePath();
					pitchHistogram.plotToneScaleHistogram(fileName, true);
				}
			});
		}
	};
	
	private ActionListener exportPitchClassHistogram = new ActionListener() {
		@Override
		public void actionPerformed(ActionEvent e) {
			String dialogTitle = "Export Pitch Class Histogram (.csv)";
			showFileChooserDialog(dialogTitle,JFileChooser.FILES_ONLY,false, new ChosenFileHandler() {				
				@Override
				public void handleFile(final File chosenFile) {
					AnnotationPublisher ap = AnnotationPublisher.getInstance();
					List<Annotation> annotations = ap.getAnnotationTree().select(ap.getCurrentSelection());
					String fileName = chosenFile.getAbsolutePath();
					PitchClassHistogram pitchClassHistogram = PitchClassHistogram.createToneScaleHistogram(annotations);
					pitchClassHistogram.export(fileName);
				}
			});
		}
	};
	
	/**
	 * Show a file chooser dialog (either a save or open dialog) and handle the chosen file with the handler.
	 * @param dialogTitle The title of the dialog window.
	 * @param importFile True if a file needs to be opened, false otherwise.
	 * @param handler The handler that handles the chosen file.
	 */
	private void showFileChooserDialog(String dialogTitle,int mode,boolean importFile,ChosenFileHandler handler){
		final JFileChooser fc = new JFileChooser();		
		fc.setFileSelectionMode(mode);
		final ConfKey key;
		final int returnVal;
		fc.setDialogTitle(dialogTitle);
		if(importFile){
			key = ConfKey.file_import_dir;
			fc.setCurrentDirectory(Configuration.getFile(key));
			returnVal = fc.showOpenDialog(Frame.getInstance());
		}else{
			key = ConfKey.file_export_dir;
			fc.setCurrentDirectory(Configuration.getFile(key));
			returnVal = fc.showSaveDialog(Frame.getInstance());
		}		
		if (returnVal == JFileChooser.APPROVE_OPTION) {
			File file = fc.getSelectedFile();
			// save the directory the file is in
			String dir = file.getParentFile().getAbsolutePath();
			Configuration.set(key,dir);
			handler.handleFile(file);
		}
	}
	
	/**
	 * Handle the chosen file.
	 */
	private interface ChosenFileHandler{
		void handleFile(File chosenFile);
	}
	
	
	@Override
	public void scaleChanged(double[] newScale, boolean isChanging, boolean shiftHisto) {
		if(!isChanging){
			scale = newScale;
		}
	}

	@Override
	public void audioFileChanged(AudioFile newAudioFile) {
		audioFile = newAudioFile;		
	}
}
