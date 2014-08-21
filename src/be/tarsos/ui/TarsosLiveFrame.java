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
package be.tarsos.ui;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.Mixer;
import javax.sound.sampled.TargetDataLine;
import javax.sound.sampled.UnsupportedAudioFileException;
import javax.swing.JComponent;
import javax.swing.JFrame;

import org.noos.xing.mydoggy.AggregationPosition;
import org.noos.xing.mydoggy.Content;
import org.noos.xing.mydoggy.ContentManager;
import org.noos.xing.mydoggy.MultiSplitConstraint;
import org.noos.xing.mydoggy.MultiSplitContentManagerUI;
import org.noos.xing.mydoggy.TabbedContentManagerUI;
import org.noos.xing.mydoggy.TabbedContentUI;
import org.noos.xing.mydoggy.ToolWindow;
import org.noos.xing.mydoggy.plaf.MyDoggyToolWindowManager;
import org.noos.xing.mydoggy.plaf.ui.content.MyDoggyMultiSplitContentManagerUI;

import be.tarsos.dsp.AudioDispatcher;
import be.tarsos.dsp.AudioEvent;
import be.tarsos.dsp.io.jvm.AudioPlayer;
import be.tarsos.dsp.io.jvm.JVMAudioInputStream;
import be.tarsos.dsp.io.jvm.WaveformWriter;
import be.tarsos.dsp.pitch.PitchDetectionHandler;
import be.tarsos.dsp.pitch.PitchDetectionResult;
import be.tarsos.dsp.pitch.PitchProcessor;
import be.tarsos.dsp.pitch.PitchProcessor.PitchEstimationAlgorithm;
import be.tarsos.sampled.SampledAudioUtilities;
import be.tarsos.sampled.pitch.Annotation;
import be.tarsos.sampled.pitch.AnnotationListener;
import be.tarsos.sampled.pitch.AnnotationPublisher;
import be.tarsos.sampled.pitch.AnnotationTree;
import be.tarsos.sampled.pitch.PitchDetectionMode;
import be.tarsos.ui.pitch.CommandPanel;
import be.tarsos.ui.pitch.ControlPanel;
import be.tarsos.ui.pitch.HeaderPanel;
import be.tarsos.ui.pitch.IntervalTable;
import be.tarsos.ui.pitch.KeyboardPanel;
import be.tarsos.ui.pitch.Menu;
import be.tarsos.ui.pitch.PitchContour;
import be.tarsos.ui.pitch.ScaleChangedListener;
import be.tarsos.ui.pitch.WaveForm;
import be.tarsos.util.ConfKey;
import be.tarsos.util.Configuration;

public class TarsosLiveFrame extends JFrame implements PitchDetectionHandler, ScaleChangedListener, AnnotationListener {

	/**
	 * Logs messages.
	 */
	//private static final Logger LOG = Logger.getLogger(TarsosLiveFrame.class.getName());

	private static final long serialVersionUID = -8189717288384206655L;

	private MyDoggyToolWindowManager toolWindowManager;

	private final List<ScaleChangedListener> scaleChangedListeners;
	

	public TarsosLiveFrame() {
		super("Tarsos Live");
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		this.addWindowListener(new WindowAdapter(){
			public void windowClosed(WindowEvent arg0) {
				System.out.println("hmm");
			}
			});
		setLocationRelativeTo(null);
		TarsosFrame.setTarsosProgramIcon(this);
		
		scaleChangedListeners = new ArrayList<ScaleChangedListener>();
		
		final Menu menu = new Menu(true);
		
		setJMenuBar(menu);

		JComponent headerPanel = new HeaderPanel();
		
		//final PitchClassHistogramPanel pitchClassHistogramPanel = new PitchClassHistogramPanel(new PitchClassHistogram(), this);
		//final PitchClassHistogramPanel ambitusPanel = new PitchClassHistogramPanel(new PitchHistogram(), this);
		
		final IntervalTable intervalTable = new IntervalTable();
		final WaveForm waveForm = new WaveForm();
		final PitchContour pitchContourPanel = new PitchContour(waveForm);
		final ControlPanel controlPanel = new ControlPanel(waveForm);
		final KeyboardPanel keyboardPanel = new KeyboardPanel();
		final CommandPanel commandPanel = new CommandPanel();

		// The annotation publisher is not a ui element.
		final AnnotationPublisher annotationPublisher = AnnotationPublisher.getInstance();
		
		//KDEData.getPitchClassHistogramInstance().setComponentToRepaint(pitchClassHistogramPanel);
		//KDEData.getPitchHistogramInstance().setComponentToRepaint(ambitusPanel);

		// patch the scale changed listeners
		//addScaleChangedListener(pitchClassHistogramPanel);
		//addScaleChangedListener(ambitusPanel);
		addScaleChangedListener(pitchContourPanel);
		addScaleChangedListener(intervalTable);
		addScaleChangedListener(keyboardPanel);
		addScaleChangedListener(menu);
		addScaleChangedListener(commandPanel);

		// Patch the audio file changed listeners.		


		// Patch the annotation listeners
		//annotationPublisher.addListener(KDEData.getPitchClassHistogramInstance());
		//annotationPublisher.addListener(KDEData.getPitchHistogramInstance());
		
		//annotationPublisher.addListener(pitchClassHistogramPanel);
		//annotationPublisher.addListener(ambitusPanel);
		annotationPublisher.addListener(pitchContourPanel);
		annotationPublisher.addListener(controlPanel);
		annotationPublisher.addListener(commandPanel);
		annotationPublisher.addListener(this);


		// initialize content and tool window manager of the 'mydoggy'
		// framework.
		// Create a new instance of MyDoggyToolWindowManager passing the frame.
		MyDoggyToolWindowManager windowManager = new MyDoggyToolWindowManager();
		toolWindowManager = windowManager;
		ContentManager contentManager = toolWindowManager.getContentManager();
		MultiSplitContentManagerUI contentManagerUI = new MyDoggyMultiSplitContentManagerUI();
		contentManager.setContentManagerUI(contentManagerUI);
		contentManagerUI.setShowAlwaysTab(true);
		contentManagerUI.setTabPlacement(TabbedContentManagerUI.TabPlacement.TOP);
		
		// add components to the window manager.

		Content content = null; 
		//= contentManager.addContent("Pitch Class Histogram", "Pitch Class Histogram", null, pitchClassHistogramPanel);
		//setDefaultTabbedContentOptions(content);
		//content.setMinimized(false);

		MultiSplitConstraint constraint;// = new MultiSplitConstraint(content, 1);
		
		
		//constraint = new MultiSplitConstraint(content, 2);
		//content = contentManager.addContent("Pitch Histogram", "Pitch Histogram", null, ambitusPanel, null, constraint);
		//setDefaultTabbedContentOptions(content);
		//content.setMinimized(false);
		
		constraint = new MultiSplitConstraint(content, 3);
		content = contentManager.addContent("Interval table", "Interval table", null, intervalTable,null,constraint);
		setDefaultTabbedContentOptions(content);
		content.setMinimized(true);
		
		constraint = new MultiSplitConstraint(AggregationPosition.RIGHT);
		content = contentManager.addContent("Commands", "Commands", null, commandPanel, null,constraint);
		setDefaultTabbedContentOptions(content);
		content.setMinimized(false);
		
		content = contentManager.addContent("Waveform", "Waveform", null, controlPanel);
		setDefaultTabbedContentOptions(content);
		content.setMinimized(false);
		
		constraint = new MultiSplitConstraint(content, 1);
		content = contentManager.addContent("Annotations", "Annotations", null, pitchContourPanel, null,
				constraint);
		setDefaultTabbedContentOptions(content);
		content.setMinimized(true);

		content = contentManager.addContent("Keyboard", "Keyboard", null, keyboardPanel, null);
		setDefaultTabbedContentOptions(content);
		content.setMinimized(true);

		// Make all tools available
		for (ToolWindow window : toolWindowManager.getToolWindows()) {
			window.setAvailable(true);
		}

		// add the components to the frame
		add(windowManager, BorderLayout.CENTER);
		add(headerPanel, BorderLayout.NORTH);

		startTarsosLiveMode();
		
	}
	private void setDefaultTabbedContentOptions(final Content content) {
		TabbedContentUI tabbedContent = (TabbedContentUI) content.getContentUI();
		tabbedContent.setCloseable(false);
		tabbedContent.setDetachable(true);
		tabbedContent.setTransparentMode(false);
		tabbedContent.setMinimizable(true);		
	}
	
	private AudioInputStream openAudioInputStream(final AudioFormat format) throws LineUnavailableException{
		final int selected = Configuration.getInt(ConfKey.mixer_input_device);
		Mixer.Info selectedMixer = SampledAudioUtilities.getMixerInfo(false,true).get(selected);
		final Mixer mixer = AudioSystem.getMixer(selectedMixer);
		
		final DataLine.Info dataLineInfo = new DataLine.Info(TargetDataLine.class, format);
		TargetDataLine line;
		line = (TargetDataLine) mixer.getLine(dataLineInfo);
		final int numberOfSamples = (int) (0.1 * 44100);
		line.open(format, numberOfSamples);
		line.start();
		final AudioInputStream stream = new AudioInputStream(line);
		return stream;
	}

	private void startTarsosLiveMode() {
		AudioInputStream stream;
		try {
			float sampleRate = 44100;
			int bufferSize = 1024;
			int overlap = 768;
			PitchEstimationAlgorithm algo = PitchEstimationAlgorithm.DYNAMIC_WAVELET;
			final AudioFormat format = new AudioFormat(44100, 16, 1, true, false);
			
			String timePart = new SimpleDateFormat("yyyy.MM.dd-HH:mm").format(new Date());
			String filename = timePart + "-tarsos_live_session.wav"; 

			stream = openAudioInputStream(format);
			JVMAudioInputStream inputStream = new JVMAudioInputStream(stream);
			AudioDispatcher dispatcher = new AudioDispatcher(inputStream, bufferSize, overlap);
			dispatcher.addAudioProcessor(new PitchProcessor(algo,sampleRate,bufferSize,this));
			dispatcher.addAudioProcessor(new WaveformWriter(format, filename));
			dispatcher.addAudioProcessor(new AudioPlayer(format));
			new Thread(dispatcher,"Tarsos Live Dispatcher").start();
		} catch (LineUnavailableException e) {
			
		} 
	}
	
	/*
	private void handleMicrophoneError(Exception e, String audioFormat){
		String message = "There is something wrong with the microphone input.\n Either the line is not available or recording at " + audioFormat + " is not possible.\n More details: \n " + e.getMessage() + "\n\nThis can be solved by choosing another microphone input.";
		String title = "Microphone input not supported";
		JOptionPane.showMessageDialog(this,message,title,JOptionPane.ERROR_MESSAGE);
	}

	private void checkInputDeviceIndex() {
		final int inputDeviceIndex = Configuration
				.getInt(ConfKey.mixer_input_device);
		final int defaultInputDeviceIndex = 0;
		if (inputDeviceIndex < 0
				|| inputDeviceIndex >= SampledAudioUtilities.getMixerInfo(
						false, true).size()) {
			Configuration.set(ConfKey.mixer_input_device,
					defaultInputDeviceIndex);
			LOG.warning("Ignored configured mixer input device ("
					+ inputDeviceIndex + ") reset to "
					+ defaultInputDeviceIndex);
		}
	}
	*/
	
	public synchronized void addScaleChangedListener(ScaleChangedListener listener) {
		scaleChangedListeners.add(listener);
	}
	

	
	public static void main(String... strings){
		TarsosLiveFrame frame = new TarsosLiveFrame();
		frame.setSize(new Dimension(640,480));
		frame.setVisible(true);
		
	}

	int i = 0;
	double prevTime = 0.0;
	private final AnnotationPublisher publisher = AnnotationPublisher.getInstance();
	private final AnnotationTree tree = publisher.getAnnotationTree();
	
	public void handlePitch(float pitch, float probability, float timeStamp,
			float progress) {
		if(pitch != -1.0){
			
		}
	}
	
	public void handlePitch(PitchDetectionResult pitchDetectionResult,
			AudioEvent audioEvent) {
		if(pitchDetectionResult.isPitched()){
			float pitch  = pitchDetectionResult.getPitch();
			double timeStamp = audioEvent.getTimeStamp();
			Annotation annotation = new Annotation(timeStamp, pitch, PitchDetectionMode.TARSOS_YIN);
			tree.add(annotation);
			double currentTime = annotation.getStart();
			publisher.alterSelection(publisher.getCurrentSelection().getStartTime(), currentTime);
			publisher.delegateAddAnnotations(prevTime,currentTime);	
			prevTime = timeStamp;
		}
		
	}

	public void scaleChanged(double[] newScale, boolean isChanging,
			boolean shiftHisto) {
		// TODO Auto-generated method stub
		
	}

	public void addAnnotation(Annotation annotation) {
		// TODO Auto-generated method stub
		
	}

	public void clearAnnotations() {
		// TODO Auto-generated method stub
		
	}

	public void annotationsAdded() {
		// TODO Auto-generated method stub
		
	}

	public void extractionStarted() {
		// TODO Auto-generated method stub
		
	}

	public void extractionFinished() {
		// TODO Auto-generated method stub
	}
	
}
