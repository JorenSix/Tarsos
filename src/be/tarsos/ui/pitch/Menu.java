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

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;
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
import javax.swing.KeyStroke;

import be.tarsos.midi.MidiCommon;
import be.tarsos.midi.TarsosSynth;
import be.tarsos.midi.ToneSequenceBuilder;
import be.tarsos.midi.MidiCommon.MoreMidiInfo;
import be.tarsos.sampled.Player;
import be.tarsos.sampled.SampledAudioUtilities;
import be.tarsos.sampled.pitch.Annotation;
import be.tarsos.sampled.pitch.AnnotationPublisher;
import be.tarsos.sampled.pitch.PitchDetectionMode;
import be.tarsos.sampled.pitch.PitchUnit;
import be.tarsos.ui.TarsosFrame;
import be.tarsos.util.AudioFile;
import be.tarsos.util.BareBonesBrowserLaunch;
import be.tarsos.util.ConfKey;
import be.tarsos.util.Configuration;
import be.tarsos.util.FileUtils;
import be.tarsos.util.MenuScroller;
import be.tarsos.util.ScalaFile;
import be.tarsos.util.SimplePlot;
import be.tarsos.util.StringUtils;
import be.tarsos.util.Configuration.ConfigChangeListener;
import be.tarsos.util.histogram.HistogramFactory;
import be.tarsos.util.histogram.PitchClassHistogram;
import be.tarsos.util.histogram.PitchHistogram;

public class Menu extends JMenuBar implements ScaleChangedListener, AudioFileChangedListener {

	/**
	 * 
	 */
	private static final long serialVersionUID = -2446134006582897883L;
	
	private double[] scale = ScalaFile.westernTuning().getPitches();
	private AudioFile audioFile;
	
	private final boolean live; 

	public Menu(boolean forTarsosLive) {
		live = forTarsosLive;
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

		if(!live){
			item = new JMenuItem("Open...");
			item.addActionListener(importFileAction);
			menu.add(item);
		}

		JMenu importMenu = new JMenu("Import");
		menu.add(importMenu);

		item = new JMenuItem("Scala file...");
		item.addActionListener(importFileAction);		
		importMenu.add(item);
		
		importMenu.addSeparator();
		addScalaExamplesMenu(importMenu);

		JMenu exportMenu = new JMenu("Export");
		menu.add(exportMenu);
		
		
		/* Annotations sub menu */
		JMenu annotationsMenu = new JMenu("Annotations");
		exportMenu.add(annotationsMenu);
		
		item = new JMenuItem("Annotations CSV-file...");
		item.setToolTipText("Export a CVS file with annotations (in cent) over time (in seconds).");
		item.addActionListener(exportAnnotationsCSV);
		annotationsMenu.add(item);
		
		item = new JMenuItem("Annotations EPS-file...");
		item.setToolTipText("Export an EPS file file with annotations over time (in seconds).");
		item.addActionListener(exportAnnotationsEPS);
		annotationsMenu.add(item);
				
		item = new JMenuItem("Synthesized annotations...");
		item.setToolTipText("Export an audio file synthesized using the annotations.");
		item.addActionListener(exportSynthesizedAnnotations);
		annotationsMenu.add(item);		
		
		/*Pitch Histogram sub menu*/
		
		JMenu pitchHistogram = new JMenu("Pitch Histogram");
		exportMenu.add(pitchHistogram);
		
		item = new JMenuItem("Pitch Histogram CSV-file...");
		item.setToolTipText("Export a CVS file with a pitch histogram.");
		item.addActionListener(exportPitchHistogram);
		pitchHistogram.add(item);
		
		item = new JMenuItem("Pitch Histogram PNG...");
		item.setToolTipText("Export a PNG file with a pitch histogram.");
		item.addActionListener(exportPitchHistogramImage);
		pitchHistogram.add(item);
		
		item = new JMenuItem("Pitch Histogram EPS...");
		item.setToolTipText("Export a EPS file with a pitch histogram.");
		item.addActionListener(exportPitchHistogramImage);
		pitchHistogram.add(item);
		
		item = new JMenuItem("Pitch Histogram Latex...");
		item.setToolTipText("Export a Tex file with a pitch histogram.");
		
		item.addActionListener(exportPitchHistogramLatex);
		pitchHistogram.add(item);
		
		/* Pitch Class Histogram sub menu */
		JMenu pitchClassHistogram = new JMenu("Pitch Class Histogram");
		exportMenu.add(pitchClassHistogram);
		
		item = new JMenuItem("Pitch Class Histogram CSV-file...");
		item.setToolTipText("Export a CVS file with a pitch class histogram.");
		item.addActionListener(exportPitchClassHistogram);
		pitchClassHistogram.add(item);	

		item = new JMenuItem("Pitch Class Histogram PNG-file...");
		item.setToolTipText("Export a PNG file with a pitch class histogram.");
		item.addActionListener(exportPitchClassHistogramImage);
		pitchClassHistogram.add(item);
		
		item = new JMenuItem("Pitch Class Histogram EPS-file...");
		item.setToolTipText("Export a EPS file with a pitch class histogram.");
		item.addActionListener(exportPitchClassHistogramImage);
		pitchClassHistogram.add(item);
		
		item = new JMenuItem("Pitch Class Histogram with octaves divided PNG-file...");
		item.setToolTipText("Pitch Class Histogram with octaves divided PNG-file");
		item.addActionListener(exportPitchClassHistogramImageWithOctaveDivision);
		pitchClassHistogram.add(item);
		
		item = new JMenuItem("Pitch Class Histogram Latex-file...");
		item.setToolTipText("Pitch Class Histogram TIKZ Latex-file");
		item.addActionListener(exportPitchClassHistogramLatex);
		pitchClassHistogram.add(item);
		
		/* Pitch Class Data sub menu */
		
		JMenu pitchClassDataMenu = new JMenu("Pitch Class Data");
		exportMenu.add(pitchClassDataMenu);

		item = new JMenuItem("Scala file...");
		item.setToolTipText("Export the current peaks as a scala file.");
		item.addActionListener(exportScalaAction);
		pitchClassDataMenu.add(item);
		
		item = new JMenuItem("Interval matrix Latex...");
		item.setToolTipText("Export the current peaks as an matrix.");
		item.addActionListener(exportIntervalMatrixLatexAction);
		pitchClassDataMenu.add(item);
		
		if(!live){
			final List<String> files = Configuration.getList(ConfKey.file_recent);
			if(!files.isEmpty()){
				menu.addSeparator();	
			}
	
			addRecentFilesToMenu(menu);
					
			//remove and add recent file menu if a file is opened
			//or imported (by drag and drop).
			Configuration.addListener(new ConfigChangeListener(){
				public void configurationChanged(ConfKey key) {
					if(key == ConfKey.file_recent){
						for(JMenuItem menuItem : recentFilesMenuItems){
							menu.remove(menuItem);
						}
						addRecentFilesToMenu(menu);
					}
				}});
		}
		//exit action
		menu.addSeparator();
		item = new JMenuItem("Exit");
		item.addActionListener(exitAction);
		menu.add(item);
	}
	
	private void addScalaExamplesMenu(JMenu importMenu) {
		String target = FileUtils.combine(FileUtils.temporaryDirectory(),"scales");
		if(!FileUtils.exists(target)){
			FileUtils.mkdirs(target);
			FileUtils.copyDirFromJar("be/tarsos/ui/resources/scales",target);
		}
		addScalaExamplesMenu(importMenu,new File(target));
	}
	
	private void addScalaExamplesMenu(JMenu importMenu,File file) {
		if(file.isDirectory()){
			JMenu exampleMenu = new JMenu(file.getName());
			importMenu.add(exampleMenu);
			for(File subFile : file.listFiles()){
				addScalaExamplesMenu(exampleMenu,subFile);
			}
		} else {
			JMenuItem scalaFileMenuItem = new JMenuItem(file.getName());
			scalaFileMenuItem.addActionListener(openRecentFileAction);
			scalaFileMenuItem.setActionCommand(file.getAbsolutePath());
			try{
				String toolTip = new ScalaFile(file.getAbsolutePath()).getDescription();
				scalaFileMenuItem.setToolTipText(toolTip);
			} catch(Exception e) {
				//ignore
			}
			importMenu.add(scalaFileMenuItem);
		}
	}

	List<JMenuItem> recentFilesMenuItems;
	private void addRecentFilesToMenu(final JMenu menu) {
		final List<String> files = Configuration.getList(ConfKey.file_recent);
		JMenuItem item;
		recentFilesMenuItems = new ArrayList<JMenuItem>();
		if (!files.isEmpty()) {
			int index = 1;
			for (final String recentFile : files) {
				// keep only existing files
				if (FileUtils.exists(recentFile)) {
					item = new JMenuItem(index + " "
							+ FileUtils.basename(recentFile));
					item.setActionCommand(recentFile);
					item.addActionListener(openRecentFileAction);
					menu.add(item);
					recentFilesMenuItems.add(item);
					index++;
				}
			}
		}
	}

	private void addSettingsMenu() {
		JMenu menu = new JMenu("Settings");
		this.add(menu);

		addMixerMenu(menu);

		addMidiMenu(menu);

		if(!live){
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
		}
		menu.addSeparator();
		
		JMenuItem increaseVolume = new JMenuItem("Increase Volume");
		increaseVolume.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_UP, ActionEvent.CTRL_MASK));
		increaseVolume.addActionListener(increaseVolumeAction);
		menu.add(increaseVolume);
		
		JMenuItem decreaseVolume = new JMenuItem("Decrease Volume");		
		decreaseVolume.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_DOWN, ActionEvent.CTRL_MASK));
		decreaseVolume.addActionListener(decreaseVolumeAction);
		menu.add(decreaseVolume);
		
		menu.addSeparator();
		
		JMenuItem increaseTempo = new JMenuItem("Increase Tempo");		
		increaseTempo.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_UP, ActionEvent.ALT_MASK));
		increaseTempo.addActionListener(increaseTempoAction);
		menu.add(increaseTempo);
		
		JMenuItem decreaseTempo = new JMenuItem("Decrease Tempo");		
		decreaseTempo.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_DOWN, ActionEvent.ALT_MASK));
		decreaseTempo.addActionListener(decreaseTempoAction);
		menu.add(decreaseTempo);
	}

	private void addMidiMenu(JMenu menu) {
		JMenu midiDevicesMenu = new JMenu("MIDI Devices");
		menu.add(midiDevicesMenu);

		JMenu outputMidiDevices = new JMenu("Output");
		midiDevicesMenu.add(outputMidiDevices);
		addMidiOutputDevicesMenu(outputMidiDevices,midiOutputDeviceChange);

		JMenu inputMidiDevices = new JMenu("Input");
		midiDevicesMenu.add(inputMidiDevices);
		addMidiInputDevicesMenu(inputMidiDevices,midiInputDeviceChange);
		
		addInstrumentsMenu(midiDevicesMenu);
	}

	private void addMidiInputDevicesMenu(JMenu parent,ActionListener listener) {
		ButtonGroup buttonGroup = new ButtonGroup();
		Vector<MoreMidiInfo> outputMidiInfo = MidiCommon.listDevices(true,false);
		int deviceIndex = 0;
		int currentIndex = Configuration.getInt(ConfKey.midi_input_device);
		for (MoreMidiInfo info : outputMidiInfo) {
			JMenuItem radioButton = new JRadioButtonMenuItem(info
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
	
	private void addMidiOutputDevicesMenu(JMenu parent, ActionListener listener) {
		Vector<MoreMidiInfo> outputMidiInfo = MidiCommon.listDevices(false,true);
		int deviceIndex = 0;
		List<String> currentIndexes = Configuration.getList(ConfKey.midi_output_devices);
		for (MoreMidiInfo info : outputMidiInfo) {
			JCheckBoxMenuItem checkBoxButton = new JCheckBoxMenuItem(info
					.getInfo().getName());
			checkBoxButton.setToolTipText(info.toString());
			checkBoxButton.setActionCommand(String.valueOf(deviceIndex));
			for(String currentIndex : currentIndexes) {
				if (String.valueOf(deviceIndex).equals(currentIndex)) {
					checkBoxButton.setSelected(true);
				}
				checkBoxButton.addActionListener(listener);
				parent.add(checkBoxButton);
			}
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
		public void actionPerformed(ActionEvent e) {
			BareBonesBrowserLaunch.openURL("http://tarsos.0110.be");				
		}
	};
	
	private ActionListener saveSelectedDetectorsAction = new ActionListener(){
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
		public void actionPerformed(final ActionEvent e) {
			JFrame parent = TarsosFrame.getInstance();
			
			/*
			String contents = FileUtils.readFileFromJar("/be/tarsos/ui/resources/help.html");
			JEditorPane helpLabel = new JEditorPane();
			helpLabel.setEditable(false);
			helpLabel.setContentType("text/html");
			helpLabel.setPreferredSize(new Dimension(500, 300));
			helpLabel.setText(contents);
			helpLabel.setCaretPosition(0);
			*/
			
			JOptionPane.showMessageDialog(parent, "Tarsos \n\nDeveloped at University College Ghent – Faculty of Music ");
		}
	};
	
	private ActionListener midiInputDeviceChange = new ActionListener() {
		public void actionPerformed(ActionEvent arg0) {
			Configuration.set(ConfKey.midi_input_device,
					Integer.valueOf(arg0.getActionCommand()));
		}
	};
	
	private ActionListener midiInstrumentChange = new ActionListener() {
		public void actionPerformed(ActionEvent arg0) {
			Configuration.set(ConfKey.midi_instrument_index,
					Integer.valueOf(arg0.getActionCommand()));
		}
	};

	private ActionListener midiOutputDeviceChange = new ActionListener() {
		public void actionPerformed(ActionEvent arg0) {
			JCheckBoxMenuItem checkbox = (JCheckBoxMenuItem) arg0.getSource();
			boolean selected = checkbox.isSelected();
			String deviceIndex = arg0.getActionCommand();
			ConfKey key = ConfKey.midi_output_devices;
			List<String> currentList = Configuration.getList(key);
			if(selected){
				if(!currentList.contains(deviceIndex))
					currentList.add(deviceIndex);
			}else{
				currentList.remove(deviceIndex);	
			}
			Configuration.set(key, currentList);
		}
	};

	private ActionListener mixerInputDeviceChange = new ActionListener() {
		public void actionPerformed(ActionEvent arg0) {
			Configuration.set(ConfKey.mixer_input_device,
					Integer.valueOf(arg0.getActionCommand()));
		}
	};

	private ActionListener mixerOutputDeviceChange = new ActionListener() {
		public void actionPerformed(ActionEvent arg0) {
			Configuration.set(ConfKey.mixer_output_device,
					Integer.valueOf(arg0.getActionCommand()));
		}
	};

	private ActionListener exitAction = new ActionListener() {
		public void actionPerformed(ActionEvent arg0) {
			System.exit(0);
		}
	};
	
	private ActionListener increaseVolumeAction = new ActionListener() {
		public void actionPerformed(ActionEvent arg0) {
			Player.getInstance().increaseGain(0.05);
		}
	};
	
	private ActionListener decreaseVolumeAction = new ActionListener() {
		public void actionPerformed(ActionEvent arg0) {
			Player.getInstance().increaseGain(-0.05);
		}
	};
	
	private ActionListener increaseTempoAction = new ActionListener() {
		public void actionPerformed(ActionEvent arg0) {
			Player.getInstance().increaseTempo(0.05);
		}
	};
	
	private ActionListener decreaseTempoAction = new ActionListener() {
		public void actionPerformed(ActionEvent arg0) {
			Player.getInstance().increaseTempo(-0.05);
		}
	};
	
	private ActionListener importFileAction = new ActionListener() {
		public void actionPerformed(ActionEvent e) {
			String dialogTitle = "Import Audio or Scala File";
			String defaultFileName = null;
			showFileChooserDialog(dialogTitle,JFileChooser.FILES_AND_DIRECTORIES,true,defaultFileName, new ChosenFileHandler() {				
				
				public void handleFile(final File chosenFile) {
					TarsosFrame.getInstance().setNewFile(chosenFile);
				}
			});
		}
	};
	
	private ActionListener openRecentFileAction = new ActionListener() {
		public void actionPerformed(ActionEvent e) {
			File recentFile = new File(e.getActionCommand());
			TarsosFrame.getInstance().setNewFile(recentFile);
		}
	};


	
	/*
	 * 
	 * ANNOTATIONS export functions
	 *  
	 */
	
	private ActionListener exportAnnotationsCSV = new ActionListener() {
		public void actionPerformed(ActionEvent e) {
			String dialogTitle = "Export Annotations (.csv)";
			String defaultFileName = audioFile.originalBasename() + "_annotations.csv";
			showFileChooserDialog(dialogTitle,JFileChooser.FILES_ONLY,false,defaultFileName, new ChosenFileHandler() {
				public void handleFile(final File chosenFile) {
					AnnotationPublisher ap = AnnotationPublisher.getInstance();
					List<Annotation> annotations = ap.getAnnotationTree().select(ap.getCurrentSelection());
					//sort by time
					Collections.sort(annotations);
					String fileName = chosenFile.getAbsolutePath();
					FileUtils.writePitchAnnotations(fileName, annotations);
				}
			});
		}
	};
	
	private ActionListener exportAnnotationsEPS = new ActionListener() {
		public void actionPerformed(ActionEvent e) {
			String dialogTitle = "Export Annotations (.eps)";
			String defaultFileName = audioFile.originalBasename() + "_annotations.eps";
			showFileChooserDialog(dialogTitle,JFileChooser.FILES_ONLY,false,defaultFileName, new ChosenFileHandler() {
				public void handleFile(final File chosenFile) {
					AnnotationPublisher ap = AnnotationPublisher.getInstance();
										
					String fileName = chosenFile.getAbsolutePath();
					PitchContour pc = new PitchContour(new WaveForm());
					pc.audioFileChanged(audioFile);
					pc.scaleChanged(scale, false, false);
					
					//adds the annotations also
					pc.setXRange(ap.getCurrentSelection().getStartTime(), ap.getCurrentSelection().getStopTime());
					pc.setYRange(ap.getCurrentSelection().getStartPitch(), ap.getCurrentSelection().getStopPitch());
					
					// EPS
					OutputStream out;
					try {
						out = new BufferedOutputStream(new FileOutputStream(new File(fileName + ".eps")));
						pc.export(out);
						out.close();
					} catch (FileNotFoundException e) {
						e.printStackTrace();
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
			});
		}
	};
	
	
	private ActionListener exportSynthesizedAnnotations =  new ActionListener() {
		public void actionPerformed(final ActionEvent e) {
			String dialogTitle = "Export synthesized annotations (.wav)";
			String defaultFileName = audioFile.originalBasename() + "_resynth.wav";
			showFileChooserDialog(dialogTitle,JFileChooser.FILES_ONLY,false,defaultFileName, new ChosenFileHandler() {				
				public void handleFile(final File chosenFile) {
					ToneSequenceBuilder builder = new ToneSequenceBuilder();
					AnnotationPublisher ap = AnnotationPublisher.getInstance();
					List<Annotation> annotations = ap.getAnnotationTree().select(ap.getCurrentSelection());
					Collections.sort(annotations);
					
					for(final Annotation annotation:annotations){
						double frequency = annotation.getPitch(PitchUnit.HERTZ);
						double realTime = annotation.getStart();
						double toneTime = realTime; //-startTime
						builder.addTone(frequency, toneTime);
					}
					try {
						builder.writeFile(chosenFile.getAbsolutePath(), 5,audioFile.transcodedPath());
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
	
	/*
	 * 
	 * PITCH HISTOGRAM export functions
	 *  
	 */
	
	private ActionListener exportPitchHistogram = new ActionListener() {
		public void actionPerformed(ActionEvent e) {
			String dialogTitle = "Export Pitch Histogram (.csv)";
			String defaultFileName = audioFile.originalBasename() + "_pitch_histogram.csv";
			showFileChooserDialog(dialogTitle,JFileChooser.FILES_ONLY,false,defaultFileName, new ChosenFileHandler() {				
				public void handleFile(final File chosenFile) {
					AnnotationPublisher ap = AnnotationPublisher.getInstance();
					List<Annotation> annotations = ap.getAnnotationTree().select(ap.getCurrentSelection());
					PitchHistogram pitchHistogram = HistogramFactory.createPitchHistogram(annotations);
					String fileName = chosenFile.getAbsolutePath();
					pitchHistogram.export(fileName);
				}
			});
		}
	};
	
	private ActionListener exportPitchHistogramImage = new ActionListener() {
		public void actionPerformed(ActionEvent e) {
			final String extension;
			if(((JMenuItem) e.getSource()).getText().toLowerCase().contains("png")) {
				extension = "png";
			} else {
				extension = "eps";
			}
			String dialogTitle = "Export Pitch Histogram (." + extension + ")";
			
			String defaultFileName = audioFile.originalBasename() + "_pitch_histogram." + extension;
			showFileChooserDialog(dialogTitle,JFileChooser.FILES_ONLY,false,defaultFileName, new ChosenFileHandler() {
				public void handleFile(final File chosenFile) {
					AnnotationPublisher ap = AnnotationPublisher.getInstance();
					List<Annotation> annotations = ap.getAnnotationTree().select(ap.getCurrentSelection());
					PitchHistogram pitchHistogram = HistogramFactory.createPitchHistogram(annotations);
					String fileName = chosenFile.getAbsolutePath();
					SimplePlot plot = new SimplePlot();
					
					plot.setYLabel("Number of Annotations (#)");
					plot.setXLabel("Pitch (cent)");
					
					plot.setTitle("Pitch Histogram for " + audioFile.originalBasename());
					
					plot.addData(0, pitchHistogram);
					
					plot.save(fileName);
				}
			});
		}
	};
	
	/*
	 * 
	 * PITCH CLASS HISTOGRAM export functions
	 *  
	 */
	
	private ActionListener exportPitchClassHistogramImage = new ActionListener() {
		public void actionPerformed(ActionEvent e) {
			String dialogTitle = "Export Pitch Class Histogram (.png)";
			String defaultFileName = audioFile.originalBasename() + "_pitch_class_histogram.png";
			showFileChooserDialog(dialogTitle,JFileChooser.FILES_ONLY,false,defaultFileName, new ChosenFileHandler() {
				public void handleFile(final File chosenFile) {
					AnnotationPublisher ap = AnnotationPublisher.getInstance();
					List<Annotation> annotations = ap.getAnnotationTree().select(ap.getCurrentSelection());
					PitchClassHistogram pitchClassHistogram = HistogramFactory.createPitchClassHistogram(annotations);
					String fileName = chosenFile.getAbsolutePath();
					SimplePlot plot = new SimplePlot();
					plot.addData(0, pitchClassHistogram);
					plot.pitchClassHistogramify();
				
					for(double pitchClass : scale){
						plot.addXTick(String.valueOf((int)pitchClass), pitchClass);
					}
					plot.addXTick("0", 0.0);
					plot.addXTick("1200", 1200);
					plot.setTitle("Pitch Class Histogram for " + audioFile.originalBasename());
					plot.save(fileName);
				}
			});
		}
	};	
	private ActionListener exportPitchClassHistogramImageWithOctaveDivision = new ActionListener() {
		public void actionPerformed(ActionEvent e) {
			final String extension;
			if(((JMenuItem) e.getSource()).getText().toLowerCase().contains("png")) {
				extension = "png";
			} else {
				extension = "eps";
			}
			String dialogTitle = "Export Pitch Class Histogram (." + extension + ")";
			String defaultFileName = audioFile.originalBasename() + "_pitch_class_histogram_with_octaves." + extension;
			showFileChooserDialog(dialogTitle,JFileChooser.FILES_ONLY,false,defaultFileName, new ChosenFileHandler() {	
				public void handleFile(final File chosenFile) {
					AnnotationPublisher ap = AnnotationPublisher.getInstance();
					List<Annotation> annotations = ap.getAnnotationTree().select(ap.getCurrentSelection());
					PitchHistogram pitchHistogram = HistogramFactory.createPitchHistogram(annotations);
					String fileName = chosenFile.getAbsolutePath();
					pitchHistogram.plotToneScaleHistogram(fileName, true);
				}
			});
		}
	};	
	private ActionListener exportPitchClassHistogram = new ActionListener() {
		public void actionPerformed(ActionEvent e) {
			String dialogTitle = "Export Pitch Class Histogram (.csv)";
			String defaultFileName = audioFile.originalBasename() + "_pitch_class_histogram.csv";
			showFileChooserDialog(dialogTitle,JFileChooser.FILES_ONLY,false,defaultFileName, new ChosenFileHandler() {
				public void handleFile(final File chosenFile) {
					AnnotationPublisher ap = AnnotationPublisher.getInstance();
					List<Annotation> annotations = ap.getAnnotationTree().select(ap.getCurrentSelection());
					String fileName = chosenFile.getAbsolutePath();
					PitchClassHistogram pitchClassHistogram = HistogramFactory.createPitchClassHistogram(annotations);
					pitchClassHistogram.export(fileName);
				}
			});
		}
	};	
	
	
	private ActionListener exportPitchHistogramLatex = new ActionListener(){

		public void actionPerformed(ActionEvent arg0) {
			String dialogTitle = "Export tikz image for Latex  (.tex)";
			String defaultFileName = audioFile.originalBasename() + "_pitch_histogram.tex";
			showFileChooserDialog(dialogTitle,JFileChooser.FILES_ONLY,false,defaultFileName, new ChosenFileHandler() {
				public void handleFile(final File chosenFile) {
					String temporaryTarget = new File(FileUtils.temporaryDirectory() , "tikz_ph.tex").getAbsolutePath();
					FileUtils.copyFileFromJar("/be/tarsos/ui/resources/tikz_ph.te" , temporaryTarget);
					String contents = FileUtils.readFile(temporaryTarget); 
					
					String datFileName = audioFile.originalBasename() + ".ph.dat";
					String datFileTarget = FileUtils.combine(chosenFile.getParent(),datFileName);
					
					HashMap<String,String> map = new HashMap<String,String>();
					map.put("%dat.file.dat%",datFileName);
					List<Integer> scaleAsList = new ArrayList<Integer>();
					for(int i = 3600 ; i <= 8400 ; i+=1200){
						for(double pitchClass : scale){
							scaleAsList.add(i + (int) Math.round(pitchClass));
						}
					}
					map.put("%comma_separated_pitch_classes%",StringUtils.join(scaleAsList,","));
					
					for(Entry<String,String> entry : map.entrySet()){
						contents = contents.replace(entry.getKey(), entry.getValue());
					}
					
					AnnotationPublisher ap = AnnotationPublisher.getInstance();
					List<Annotation> annotations = ap.getAnnotationTree().select(ap.getCurrentSelection());
					PitchHistogram pitchHistogram = HistogramFactory.createPitchHistogram(annotations,10);
					
					//This makes sure the highest peak is at 600, which is what is expected
					//by the tex file.
					pitchHistogram.multiply(600.0/Double.valueOf(pitchHistogram.getMaxBinCount()));
					
					StringBuilder sb = new StringBuilder();
					sb.append("# Pitch  Histogram Data for ");
					sb.append(audioFile.originalBasename());
					sb.append("\n");
					
					ArrayList<Double> keys = new ArrayList<Double>(pitchHistogram.keySet());
					Collections.sort(keys);
					for(Double key : keys){
						long count = pitchHistogram.getCount(key);
						sb.append(key).append(" ").append(count).append("\n");
					}
					
					FileUtils.writeFile(sb.toString(),datFileTarget);
					FileUtils.writeFile(contents, chosenFile.getAbsolutePath());
				}
			});
			
		}
		
	};
	
	private ActionListener exportPitchClassHistogramLatex = new ActionListener() {
		public void actionPerformed(ActionEvent e) {
			String dialogTitle = "Export tikz image for Latex  (.tex)";
			String defaultFileName = audioFile.originalBasename() + "_pitch_class_histogram.tex";
			showFileChooserDialog(dialogTitle,JFileChooser.FILES_ONLY,false,defaultFileName, new ChosenFileHandler() {
				public void handleFile(final File chosenFile) {
					String temporaryTarget = new File(FileUtils.temporaryDirectory() , "tikz.tex").getAbsolutePath();
					FileUtils.copyFileFromJar("/be/tarsos/ui/resources/tikz.te" , temporaryTarget);
					String contents = FileUtils.readFile(temporaryTarget); 
					
					String datFileName = audioFile.originalBasename() + ".dat";
					String datFileTarget = FileUtils.combine(chosenFile.getParent(),datFileName);
					
					HashMap<String,String> map = new HashMap<String,String>();
					map.put("%dat.file.dat%",datFileName);
					List<Integer> scaleAsList = new ArrayList<Integer>();
					for(double pitchClass : scale){
						scaleAsList.add((int) Math.round(pitchClass));
					}
					map.put("%comma_separated_pitch_classes%",StringUtils.join(scaleAsList,","));
					
					for(Entry<String,String> entry : map.entrySet()){
						contents = contents.replace(entry.getKey(), entry.getValue());
					}
					
					AnnotationPublisher ap = AnnotationPublisher.getInstance();
					List<Annotation> annotations = ap.getAnnotationTree().select(ap.getCurrentSelection());
					PitchClassHistogram pitchClassHistogram = HistogramFactory.createPitchClassHistogram(annotations);
					
					//This makes sure the highest peak is at 600, which is what is expected
					//by the tex file.
					pitchClassHistogram.multiply(600.0/Double.valueOf(pitchClassHistogram.getMaxBinCount()));
					
					StringBuilder sb = new StringBuilder();
					sb.append("# Pitch class Histogram Data for ");
					sb.append(audioFile.originalBasename());
					sb.append("\n");
					
					ArrayList<Double> keys = new ArrayList<Double>(pitchClassHistogram.keySet());
					Collections.sort(keys);
					for(Double key : keys){
						long count = pitchClassHistogram.getCount(key);
						sb.append(key).append(" ").append(count).append("\n");
					}
					
					FileUtils.writeFile(sb.toString(),datFileTarget);
					FileUtils.writeFile(contents, chosenFile.getAbsolutePath());
				}
			});
		}
	};
	
	
	/*
	 * 
	 * PITCH CLASS DATA export functions
	 *  
	 */
	
	private ActionListener exportScalaAction = new ActionListener() {
		
		public void actionPerformed(final ActionEvent e) {
			String dialogTitle = "Export Scala File (.scl)";
			String defaultFileName = audioFile.originalBasename() + ".scl";
			showFileChooserDialog(dialogTitle,JFileChooser.FILES_ONLY,false,defaultFileName, new ChosenFileHandler() {				
				
				public void handleFile(final File chosenFile) {
					String title = "Tarsos export";
					if(audioFile != null){
						title = title + " " + audioFile.originalBasename();
					}
					ScalaFile scalaFile = new ScalaFile(title, scale);
					scalaFile.write(chosenFile.getAbsolutePath());
				}
			});
		}
	};
	
	private ActionListener exportIntervalMatrixLatexAction = new ActionListener() {
		
		public void actionPerformed(final ActionEvent e) {
			String dialogTitle = "Export Interval Matrix (.tex)";
			String defaultFileName = audioFile.originalBasename() + "_interval_matrix.tex";
			showFileChooserDialog(dialogTitle,JFileChooser.FILES_ONLY,false,defaultFileName, new ChosenFileHandler() {				
				
				public void handleFile(final File chosenFile) {
					String temporaryTarget = new File(FileUtils.temporaryDirectory() , "interval_matrix.tex").getAbsolutePath();
					FileUtils.copyFileFromJar("/be/tarsos/ui/resources/interval_matrix.te" , temporaryTarget);
					String contents = FileUtils.readFile(temporaryTarget); 
					
					List<Integer> scaleAsList = new ArrayList<Integer>();
					List<String> rows = new ArrayList<String>();
					String cees = "";
					for(double pitchClass : scale){
						scaleAsList.add((int) Math.round(pitchClass));
						cees = cees + " c ";
					}
					
					for(int i = 0; i < scale.length ; i++){
						List<Integer> values = new ArrayList<Integer>();
						values.add((int) Math.round(scale[i]));
						for(int j = 0; j < scale.length ; j++){
							final int value; 
							if ( j >= i ){
								value = (int) Math.round(scale[j] - scale[i]);
							}else{
								value = (int) Math.round(1200 - scale[i] + scale[j]);
							}
							values.add(value);
						}
						rows.add(StringUtils.join(values, " & "));
					}
					
					contents = contents.replace("%cees%", cees);
					contents = contents.replace("%pitch_classes%",StringUtils.join(scaleAsList, " & "));
					contents = contents.replace("%data%",StringUtils.join(rows, "\\\\\n"));
								
					
					FileUtils.writeFile(contents, chosenFile.getAbsolutePath());
				}
			});
		}
	};
	
	
	
	/**
	 * Show a file chooser dialog (either a save or open dialog) and handle the chosen file with the handler.
	 * @param dialogTitle The title of the dialog window.
	 * @param importFile True if a file needs to be opened, false otherwise.
	 * @param handler The handler that handles the chosen file.
	 * @param defaultFileName The name thet is used automatically to save a file.
	 */
	private void showFileChooserDialog(String dialogTitle,int mode,boolean importFile,String defaultFileName,ChosenFileHandler handler){
		final JFileChooser fc = new JFileChooser();		
		
		fc.setFileSelectionMode(mode);
		final ConfKey key;
		final int returnVal;
		fc.setDialogTitle(dialogTitle);
		if(importFile){
			key = ConfKey.file_import_dir;
			//if the import directory does not exist, change 
			//to home dir
			if(!FileUtils.exists(Configuration.get(key))){
				Configuration.set(key, System.getProperty("user.home"));				
			}
			fc.setCurrentDirectory(Configuration.getFile(key));
			returnVal = fc.showOpenDialog(TarsosFrame.getInstance());
		}else{
			key = ConfKey.file_export_dir;
			//if the export directory does not exist, change 
			//to home dir
			if(!FileUtils.exists(Configuration.get(key))){
				Configuration.set(key, System.getProperty("user.home"));				
			}
			fc.setCurrentDirectory(Configuration.getFile(key));
			fc.setSelectedFile(new File(defaultFileName));
			returnVal = fc.showSaveDialog(TarsosFrame.getInstance());
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
	
	
	
	public void scaleChanged(double[] newScale, boolean isChanging, boolean shiftHisto) {
		if(!isChanging){
			scale = newScale;
		}
	}

	
	public void audioFileChanged(AudioFile newAudioFile) {
		audioFile = newAudioFile;		
	}
}
