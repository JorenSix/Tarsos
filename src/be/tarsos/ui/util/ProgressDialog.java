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

package be.tarsos.ui.util;

import java.awt.Dimension;
import java.awt.Frame;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;

public class ProgressDialog extends JDialog {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1545656;

	private final int concurrentTasks;
	
	private final List<BackgroundTask> taskQueue;
	private final List<BackgroundTask> runningTasks;
	

	public ProgressDialog(final Frame parent,String title,BackgroundTask transcodingTask,final List<BackgroundTask> detectorQueue) {
		super(parent, title, true);
		setDefaultCloseOperation(DISPOSE_ON_CLOSE);
		setMinimumSize(new Dimension(400,120));
		taskQueue = detectorQueue;
		runningTasks = new ArrayList<BackgroundTask>();
		concurrentTasks = Runtime.getRuntime().availableProcessors();
		
		getContentPane().setLayout(new GridLayout(0, 1));		
		setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE);
		setLocationRelativeTo(parent);		
		
		transcodingTask.addHandler(new BackgroundTask.TaskHandler() {
			public void taskDone(BackgroundTask backgroundTask) {				
				startOtherTasks();
			}
			public void taskInterrupted(BackgroundTask backgroundTask,Exception e) {
				//transcoding failed => interrupt the queue
				setVisible(false);
			}
		});
		
		getContentPane().add(transcodingTask.ui());
		
		for(BackgroundTask detectorTask : detectorQueue){
			detectorTask.addHandler(handler);
			getContentPane().add(detectorTask.ui());
		}
		transcodingTask.execute();
		
		this.addWindowListener(new WindowAdapter() {
			
			@Override
			public void windowClosing(WindowEvent e) {
				System.out.println("close");
				for(BackgroundTask detectorTask : detectorQueue){
					detectorTask.cancel(true);
				}
			}
		});
	}
	
	private BackgroundTask.TaskHandler handler = new BackgroundTask.TaskHandler() {
		public void taskDone(BackgroundTask backgroundTask) {
			stopTask(backgroundTask);
			startNextInQueue();
		}

		public void taskInterrupted(BackgroundTask backgroundTask,Exception e) {
			
		}
	};
	
	public synchronized void startOtherTasks(){
		for (int i = 0; i < concurrentTasks && taskQueue.size() > 0; i++) {
			startTask(taskQueue.get(0));
		}
		//empty queue => hide dialog
		if(taskQueue.size() == 0 && runningTasks.size()==0){
			setVisible(false);
		}
	}

	private synchronized void startTask(BackgroundTask backgroundTask) {
		taskQueue.remove(backgroundTask);
		backgroundTask.execute();
		runningTasks.add(backgroundTask);
	}
	
	private synchronized void stopTask(BackgroundTask backgroundTask) {
		runningTasks.remove(backgroundTask);
		if(runningTasks.size()==0){
			//all tasks finished
			firePropertyChange("allTasksFinished", false, true);
			//hide
			setVisible(false);
		}
	}
	private synchronized void startNextInQueue(){
		if (taskQueue.size() > 0) {
			startTask(taskQueue.get(0));
		}
	}

	private static void createAndShowGUI() {
		// Create and set up the window.
		final JFrame frame = new JFrame();
		JButton button = new JButton("start");
		int totalNumberOfTasks = 4;
		final List<BackgroundTask> queue = new ArrayList<BackgroundTask>();
		//some dummy tasks
		for (int i = 0; i < totalNumberOfTasks; i++) {
			BackgroundTask backgroundTask =  new BackgroundTask("BackgroundTask" + i,i % 2 == 0){
				public Void doInBackground()
				{
					Random random = new Random();
					int progress = 0;
					// Initialize progress property.
					setProgress(0);
					while (progress < 100 && !this.isCancelled()) {
						// Sleep for up to one second.
						try {
							Thread.sleep(random.nextInt(500));
						} catch (InterruptedException ignore) {
						}
						// Make random progress.
						progress += random.nextInt(10);
						setProgress(Math.min(progress, 100));
					}
					
					return null;
				}
			};
			queue.add(backgroundTask);
		}
		
		button.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				ProgressDialog dialog = new ProgressDialog(frame,"Progress",new BackgroundTask("Transcoding",false){
					public Void doInBackground()
					{
						Random random = new Random();
						int progress = 0;
						while (progress < 100) {
							// Sleep for up to one second.
							try {
								Thread.sleep(random.nextInt(500));
							} catch (InterruptedException ignore) {
							}
							// Make random progress.
							progress += random.nextInt(10);
							setProgress(Math.min(progress, 100));
						}
						return null;
					}},queue);
				dialog.pack();
				dialog.setVisible(true);
			}
		});
		frame.add(button);
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

		// Display the window.
		frame.pack();
		frame.setVisible(true);
	}

	public static void main(String... strings) {
		// Schedule a job for the event-dispatching thread:
		// creating and showing this application's GUI.
		javax.swing.SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				createAndShowGUI();
			}
		});
	}

}
