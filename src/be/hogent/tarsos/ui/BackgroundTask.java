package be.hogent.tarsos.ui;

import java.awt.GridLayout;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.SwingWorker;


public abstract class BackgroundTask extends SwingWorker<Void, Void>   implements PropertyChangeListener{
	
	public interface TaskHandler{
		void taskDone(BackgroundTask backgroundTask);
		void taskInterrupted(BackgroundTask backgroundTask,Exception e);
	}

	private final JPanel ui;
	private final JProgressBar progressBar;
	private final List<BackgroundTask.TaskHandler> handlers;
	private final boolean determinedLength;
	private final String name;
	

	protected BackgroundTask(String taskName, boolean lengthDetermined) {
		ui = new JPanel(new GridLayout(0,1));
		handlers = new ArrayList<BackgroundTask.TaskHandler>();
		progressBar = new JProgressBar();
		determinedLength = lengthDetermined;
		name = taskName;
		ui.add(new JLabel(name));
		ui.add(progressBar);

		this.addPropertyChangeListener(this);
		
		progressBar.setStringPainted(true);
		if(!lengthDetermined){
			progressBar.setString("");
		}
	}
	
	public void addHandler(final BackgroundTask.TaskHandler handler){
		handlers.add(handler);
	}
	
	public void done(){
		progressBar.setIndeterminate(false);
		progressBar.setValue(100);
		progressBar.setString("Done");
		for(TaskHandler handler : handlers){
			handler.taskDone(this);
		}
	}
	
	/**
	 * Invoked when task's progress property changes. Executed in event
	 * dispatch thread
	 */
	public void propertyChange(PropertyChangeEvent evt) {
		if ("progress" == evt.getPropertyName()) {
			int progress = (Integer) evt.getNewValue();
			if(!determinedLength){
				progressBar.setIndeterminate(true);
				progressBar.setValue(progress);
				//prevents the progress bar from drawing 
				//the progress
				progressBar.setString("");
			} else {
				progressBar.setValue(progress);
			}	
		}
	}
	
	/*
	 * Main task. Executed in background thread.
	 */
	@Override
	public abstract Void doInBackground();
	
	
	public void interrupt(BackgroundTask backgroundTask,Exception e){
		progressBar.setIndeterminate(false);
		progressBar.setValue(100);
		progressBar.setString("Failed");
		for(TaskHandler handler : handlers){
			handler.taskInterrupted(this,e);
		}
	}

	public JComponent ui() {
		return ui;
	}
	
	public boolean lengthIsDetermined(){
		return determinedLength;
	}
	
	public String getName(){
		return name;
	}
}