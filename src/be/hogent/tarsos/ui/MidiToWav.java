/**
 */
package be.hogent.tarsos.ui;

import javax.swing.DefaultListModel;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JScrollPane;

import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;

/**
 * @author Joren Six
 */
public class MidiToWav extends JFrame {

	/**
     */
	private static final long serialVersionUID = -4115909001888538815L;

	private final DefaultListModel midiFileListModel;

	public MidiToWav() {
		super("MIDI and Scala to WAV");
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		setLayout(new FormLayout("4dlu,left:60dlu:grow, 4dlu ,right:60dlu:grow,4dlu", "pref,2dlu,150dlu,2dlu"));

		// final DropTarget scalaFilesTarget;
		// final DropTarget midiFilesTarget;

		JList scalaFileList;
		scalaFileList = new JList();
		// scalaFilesTarget = new DropTarget(scalaFileList, new
		// FileDropListener(""));

		JList midiFileList;
		midiFileListModel = new DefaultListModel();
		midiFileList = new JList(midiFileListModel);
		// midiFilesTarget = new DropTarget(midiFileList, new
		// FileDropListener(""));

		final CellConstraints cc = new CellConstraints();
		add(new JLabel("MIDI"), cc.xy(2, 1));
		add(new JLabel("SCALA"), cc.xy(4, 1));
		add(new JScrollPane(midiFileList), cc.xy(2, 3));
		add(new JScrollPane(scalaFileList), cc.xy(4, 3));

		pack();
		setVisible(true);
	}

	public static void main(final String[] args) {
		new MidiToWav();
	}
}
