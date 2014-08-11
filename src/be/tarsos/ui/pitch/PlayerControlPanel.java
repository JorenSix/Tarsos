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

import java.awt.Dimension;
import java.awt.GridBagLayout;
import java.awt.KeyEventDispatcher;
import java.awt.KeyboardFocusManager;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.KeyEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.util.Locale;

import javax.swing.GroupLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSeparator;
import javax.swing.JSlider;
import javax.swing.SwingConstants;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import be.tarsos.dsp.AudioEvent;
import be.tarsos.dsp.AudioProcessor;
import be.tarsos.sampled.Player;
import be.tarsos.sampled.PlayerState;
import be.tarsos.sampled.pitch.Annotation;
import be.tarsos.sampled.pitch.AnnotationListener;
import be.tarsos.sampled.pitch.AnnotationPublisher;
import be.tarsos.util.AudioFile;

public class PlayerControlPanel extends JPanel implements AudioFileChangedListener, AnnotationListener  {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1430643166254950304L;
	
	private JButton playButton;
	private JSlider tempoSlider;
	private JSlider positionSlider;
	//position value in the slider
	private int newPositionValue;
	
	private JLabel progressLabel;
	private JLabel totalLabel;
	
	private JLabel tempoLabel;
	
	private Player player;
	
	private final WaveForm waveForm;
	
	JCheckBox loopCheckBox;

	
	private PropertyChangeListener stateChanged = new PropertyChangeListener() {
		public void propertyChange(PropertyChangeEvent evt) {
			if(evt.getPropertyName() == "state"){
				PlayerState newState = (PlayerState) evt.getNewValue();
				playButton.setEnabled(newState!=PlayerState.NO_FILE_LOADED);
				loopCheckBox.setEnabled(newState!=PlayerState.NO_FILE_LOADED);				
				if(loopCheckBox.isSelected())
					positionSlider.setEnabled(false);
				else
					positionSlider.setEnabled(newState!=PlayerState.NO_FILE_LOADED);
				if(newState == PlayerState.PLAYING) {
					playButton.setText("Pauze");
				} else if(newState == PlayerState.STOPPED)  {
					playButton.setText("Play");
				}
			}
		}
	};
	
	private PropertyChangeListener tempoChanged = new PropertyChangeListener() {
		public void propertyChange(PropertyChangeEvent evt) {
			if(evt.getPropertyName() == "tempo"){
				double tempo = (Double) evt.getNewValue();
				tempoSlider.setValue((int) (tempo*100));
			}
		}
	};
	
	final AudioProcessor reportProgressProcessor = new AudioProcessor() {
		public boolean process(AudioEvent audioEvent) {
			double timeStamp =  audioEvent.getTimeStamp();
			if(!positionSlider.getValueIsAdjusting()){
				newPositionValue = (int) (audioEvent.getProgress() * 1000);
				positionSlider.setValue(newPositionValue);
				setProgressLabelText(timeStamp,player.getDurationInSeconds());
			}
			return true;
		}
		
		public void processingFinished() {
			if(!positionSlider.getValueIsAdjusting()){
				double timeStamp =  player.getStartAt() / player.getDurationInSeconds();
				newPositionValue = (int) (timeStamp *1000); 
				positionSlider.setValue(newPositionValue);
				setProgressLabelText(timeStamp,player.getDurationInSeconds());
			}
		}
	};
	
	/**
	 * Loops the audio, restarts the player when stop is reached and resumes from start.
	 */
	final AudioProcessor loopAudioProcessor = new AudioProcessor() {
		public boolean process(AudioEvent audioEvent) {
			return true;
		}
		
		public void processingFinished() {
			double diff = Math.abs(player.getCurrentTime() - player.getStopAt());//difference in seconds
			//if the loop is completed - difference between stop and current smaller than 0.1 seconds- then restart
			if(loopCheckBox.isSelected() && player.getState() == PlayerState.STOPPED && diff < 0.1 ){
				double startAt = AnnotationPublisher.getInstance().getCurrentSelection().getStartTime();
				AnnotationPublisher.getInstance().clear();//clear annotations
				AnnotationPublisher.getInstance().alterSelection(startAt, startAt);//set annotation start
				AnnotationPublisher.getInstance().delegateAddAnnotations(startAt, startAt);//add first annotations?
				player.pauze(startAt);
				player.play();
			}
		}
	};
	
	final AudioProcessor addAnnotationsProcessor = new AudioProcessor() {
		
		double previousTime = 0;
		
		public boolean process(AudioEvent audioEvent) {
			AnnotationPublisher publisher = AnnotationPublisher.getInstance();
			double currentTime = audioEvent.getTimeStamp();
			if (currentTime - previousTime > 0.03) {
				publisher.alterSelection(publisher.getCurrentSelection().getStartTime(),currentTime);
				previousTime = previousTime == 0.0 ? currentTime : previousTime;
				publisher.delegateAddAnnotations(previousTime, currentTime);
				previousTime = currentTime;
			}
			return true;
		}
		
		public void processingFinished() {
			previousTime=0;
		}
	};
	
	public PlayerControlPanel(WaveForm waveForm){
		this.waveForm = waveForm;
		playButton = new JButton("Play");
		playButton.setEnabled(false);
		playButton.addActionListener(new ActionListener(){
			public void actionPerformed(ActionEvent event) {
				if(player.getState() == PlayerState.PLAYING) {
					player.pauze();
				} else if(player.getState() == PlayerState.STOPPED)  {
					player.play();
				}
			}});
		JPanel p = new JPanel(new GridBagLayout());
		p.add(playButton);
		createSlider();
		createProgressSlider();
		loopCheckBox =  new JCheckBox();
		loopCheckBox.setEnabled(false);
		loopCheckBox.setText("Loop selection");
		loopCheckBox.addItemListener(new ItemListener() {
			public void itemStateChanged(ItemEvent arg0) {
				if(player.getState() == PlayerState.PLAYING){
					if(loopCheckBox.isSelected()){
						double stopAt = AnnotationPublisher.getInstance().getCurrentSelection().getStopTime();
						double startAt = AnnotationPublisher.getInstance().getCurrentSelection().getStartTime();
						player.setStopAt(stopAt);
						player.pauze(startAt);
						player.play();
						positionSlider.setEnabled(false);
					} else {
						positionSlider.setEnabled(true);
						player.setStopAt(Double.MAX_VALUE);
					}
				}else{
					if(loopCheckBox.isSelected()){
						positionSlider.setEnabled(false);
						double stopAt = AnnotationPublisher.getInstance().getCurrentSelection().getStopTime();
						double startAt = AnnotationPublisher.getInstance().getCurrentSelection().getStartTime();
						player.pauze(startAt);
						player.setStopAt(stopAt);
					}else{
						positionSlider.setEnabled(true);
						player.setStopAt(Double.MAX_VALUE);
					}
				}
			}
		});
		
		//register key events
		KeyboardFocusManager manager = KeyboardFocusManager.getCurrentKeyboardFocusManager();
	    manager.addKeyEventDispatcher(new KeyEventDispatcher(){
			public boolean dispatchKeyEvent(KeyEvent e) {
				boolean consumed = false;
				if(e.getKeyChar()=='n' &&  loopCheckBox.isSelected() && e.getID() == KeyEvent.KEY_TYPED && player.getState() != PlayerState.NO_FILE_LOADED){
					e.consume();
					double currentLoopStart = player.getStartAt();
					double currentLoopStop = player.getStopAt();
					double currentLoopLengt = currentLoopStop - currentLoopStart;
					double nextLoopStart = currentLoopStop;
					double nextLoopStop = currentLoopStop + currentLoopLengt;
					AnnotationPublisher.getInstance().alterSelection(nextLoopStart, nextLoopStart);//set annotation start
					player.pauze(nextLoopStart);
					player.setStopAt(nextLoopStop);
					player.play();
					consumed = true;
				} else if(e.getKeyChar()==' ' && e.getID() == KeyEvent.KEY_TYPED && playButton.isEnabled()){
					playButton.doClick();
					consumed = true;
				} else if(e.getKeyChar()=='b' &&  loopCheckBox.isSelected() && e.getID() == KeyEvent.KEY_TYPED && player.getState() != PlayerState.NO_FILE_LOADED){
					double currentLoopStart = player.getStartAt();
					double currentLoopStop = player.getStopAt();
					double currentLoopLength = currentLoopStop - currentLoopStart;
					double nextLoopStart = currentLoopStart - currentLoopLength;
					double nextLoopStop = currentLoopStart;
					AnnotationPublisher.getInstance().alterSelection(nextLoopStart, nextLoopStart);//set annotation start
					PlayerState previousState = player.getState();
					player.setStopAt(nextLoopStop);
					player.pauze(nextLoopStart);
					if(previousState==PlayerState.PLAYING) {
						player.play();
					}
					consumed = true;
					e.consume();
				} else if(e.getKeyChar()=='c' && e.getID() == KeyEvent.KEY_TYPED && loopCheckBox.isEnabled()){
					loopCheckBox.getModel().setSelected(!loopCheckBox.isSelected());
					consumed = true;
					e.consume();
				}
				return consumed;
			}});
	    
		doGroupLayout();
		player = Player.getInstance();
		player.addProcessorBeforeTimeStrechting(reportProgressProcessor);
		player.addProcessorBeforeTimeStrechting(addAnnotationsProcessor);
		player.addProcessorBeforeTimeStrechting(loopAudioProcessor);
		player.addPropertyChangeListener(stateChanged);
	}
	

	private void doGroupLayout(){		
		playButton.setMinimumSize(new Dimension(85,10));
		tempoSlider.setMinimumSize(new Dimension(75,40));
		tempoSlider.setMaximumSize(new Dimension(75,20));
		tempoLabel.setMinimumSize(new Dimension(95,10));
		tempoLabel.setMaximumSize(new Dimension(95,40));

		
		GroupLayout layout = new GroupLayout(this);
		setLayout(layout);
		layout.setAutoCreateGaps(true);
		
		JSeparator firstSeparator = new JSeparator(SwingConstants.VERTICAL);
		firstSeparator.setMaximumSize(new Dimension(5,300));
		
		JSeparator secondSeparator = new JSeparator(SwingConstants.VERTICAL);
		secondSeparator.setMaximumSize(new Dimension(5,300));
		
		JSeparator thirdSeparator = new JSeparator(SwingConstants.VERTICAL);
		thirdSeparator.setMaximumSize(new Dimension(5,300));
		
		layout.setHorizontalGroup(
				   layout.createSequentialGroup()
				      .addComponent(playButton)
				      .addComponent(firstSeparator)
				      .addComponent(tempoLabel)
				      .addComponent(tempoSlider)
				      .addComponent(secondSeparator)
				      .addComponent(progressLabel)
				      .addComponent(positionSlider)
				      .addComponent(totalLabel)
				      .addComponent(thirdSeparator)
				      .addComponent(loopCheckBox)
		);
		
		layout.setVerticalGroup(
				   layout.createSequentialGroup()
				      .addGroup(layout.createParallelGroup(GroupLayout.Alignment.CENTER)
				           .addComponent(playButton)
				           .addComponent(firstSeparator)
				           .addComponent(tempoLabel)
				           .addComponent(tempoSlider)
				           .addComponent(secondSeparator)
				           .addComponent(progressLabel)
				           .addComponent(positionSlider)
				           .addComponent(totalLabel)
				           .addComponent(thirdSeparator)
				           .addComponent(loopCheckBox)
				           )
				);
		
	}
	
	private void createSlider() {	
		tempoSlider = new JSlider(0,300);
		tempoSlider.setValue(100);
		tempoSlider.addChangeListener(new ChangeListener() {
			public void stateChanged(ChangeEvent arg0) {
				double newTempo = tempoSlider.getValue()/100.0;
				tempoLabel.setText(String.format("Tempo: %3d",  tempoSlider.getValue()) +"%");
				player.setTempo(newTempo);
			}
		});
		Player.getInstance().addPropertyChangeListener(tempoChanged);
		tempoLabel = new JLabel("Tempo: 100%");
		tempoLabel.setToolTipText("The time stretching factor: 100% is no change, 50% is half tempo.");
	}
	
	private void createProgressSlider(){
		positionSlider = new JSlider(0,1000);
		positionSlider.setValue(0);
		positionSlider.setPaintLabels(false);
		positionSlider.setPaintTicks(false);
		positionSlider.setEnabled(false);
				
		positionSlider.addChangeListener(new ChangeListener() {
			public void stateChanged(ChangeEvent arg0) {
				int currentValue = positionSlider.getValue();
				if (newPositionValue != currentValue) {
					double promille = currentValue / 1000.0;
					double currentPosition = player.getDurationInSeconds() * promille;
					if (positionSlider.getValueIsAdjusting()) {
						setProgressLabelText(currentPosition, player.getDurationInSeconds());
						if(player.getState() != PlayerState.PLAYING){
							waveForm.setMarker(currentPosition, false);
						}
					} else {
						double secondsToSkip = currentPosition;
						PlayerState currentState = player.getState();
						player.pauze(secondsToSkip);
						AnnotationPublisher ap = AnnotationPublisher.getInstance();
						ap.clear();
						ap.alterSelection(waveForm.getMarker(true), currentPosition);
						ap.delegateAddAnnotations(waveForm.getMarker(true), currentPosition);
						if(currentState == PlayerState.PLAYING){
							player.pauze(currentPosition);
							player.play();				
						}
						newPositionValue = currentValue;
						positionSlider.setValue(currentValue);
					}
				}
			}
		});

		progressLabel = new JLabel();
		totalLabel = new JLabel();
		setProgressLabelText(0, 0);
	}
	
	private void setProgressLabelText(double current, double max){
		progressLabel.setText(formattedToString(current));
		totalLabel.setText(formattedToString(max));
	}
	
	public String formattedToString(double seconds) {
		int minutes = (int) (seconds / 60);
		int completeSeconds = (int) seconds - (minutes * 60);
		int hundred =  (int) ((seconds - (int) seconds) * 100);
		return String.format(Locale.US, "%02d:%02d:%02d", minutes , completeSeconds, hundred);
	}


	public void audioFileChanged(AudioFile newAudioFile) {
		player.load(new File(newAudioFile.transcodedPath()));
	}

	public void addAnnotation(Annotation annotation) {

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
