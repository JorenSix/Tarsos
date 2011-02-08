package be.hogent.tarsos.ui.pitch;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;
import java.util.Vector;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.sound.midi.MidiDevice;
import javax.sound.midi.MidiSystem;
import javax.sound.sampled.Mixer;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JTextField;

import be.hogent.tarsos.midi.MidiCommon;
import be.hogent.tarsos.midi.MidiCommon.MoreMidiInfo;
import be.hogent.tarsos.sampled.SampledAudioUtilities;
import be.hogent.tarsos.sampled.pitch.PitchDetectionMode;
import be.hogent.tarsos.util.ConfKey;
import be.hogent.tarsos.util.Configuration;
import be.hogent.tarsos.util.Configuration.ConfigChangeListener;

import com.jgoodies.forms.builder.DefaultFormBuilder;
import com.jgoodies.forms.layout.FormLayout;

/**
 * A panel to configure the configuration parameters.
 * 
 * @author joren
 * 
 */
public class ConfigurationPanel extends JPanel {
	private static final long serialVersionUID = 773663466708366659L;
	private final HashMap<JTextField, ConfKey> configurationTextFields;

	/**
	 * Log messages.
	 */
	private static final Logger LOG = Logger.getLogger(ConfigurationPanel.class.getName());

	public ConfigurationPanel() {
		super(new BorderLayout());
		FormLayout layout = new FormLayout("right:pref, 3dlu, min:grow");
		DefaultFormBuilder builder = new DefaultFormBuilder(layout);
		configurationTextFields = new HashMap<JTextField, ConfKey>();
		builder.setDefaultDialogBorder();
		builder.setRowGroupingEnabled(true);
		addConfigurationComponents(builder);
		addConfigurationTextFields(builder);
		JComponent center = builder.getPanel();
		add(center, BorderLayout.CENTER);

		Configuration.addListener(new ConfigChangeListener() {
			public void configurationChanged(final ConfKey key) {
				for (Entry<JTextField, ConfKey> entry : configurationTextFields.entrySet()) {
					if (entry.getValue() == key) {
						String value = Configuration.get(key);
						entry.getKey().setText(value);
					}
				}
			}
		});
	}

	private static class MidiDevicePolling implements Runnable {
		private final JComboBox optionsComboBox;
		private final boolean midiInput;
		private final boolean midiOutput;

		public MidiDevicePolling(final JComboBox comboBox, final boolean input, final boolean output) {
			optionsComboBox = comboBox;
			midiInput = input;
			midiOutput = output;
		}

		public void run() {
			while (true) {
				try {
					Thread.sleep(1000);
					poll();
				} catch (InterruptedException e) {
					LOG.log(Level.SEVERE, String.format("Thread %s execution interrupted!", Thread
							.currentThread().getName()), e);
				}
			}
		}

		private void poll() {
			DefaultComboBoxModel model = (DefaultComboBoxModel) optionsComboBox.getModel();
			Vector<MoreMidiInfo> infos = MidiCommon.listDevices(midiInput, midiOutput);
			// Add new MIDI device if it is not already present in the
			// current list.
			for (MoreMidiInfo info : infos) {
				if (model.getIndexOf(info) == -1) {
					model.addElement(info);
				}
			}
			// Remove a device if the MIDI device is not there any more.
			for (int i = 0; i < model.getSize(); i++) {
				MoreMidiInfo info = (MoreMidiInfo) model.getElementAt(i);
				if (!infos.contains(info)) {
					model.removeElementAt(i);
					i--;
				}
			}
		}
	}

	private static class MixerDevicePolling implements Runnable {
		private final JComboBox optionsComboBox;
		private final boolean supportsRecording;
		private final boolean supportsPlayback;

		public MixerDevicePolling(final JComboBox comboBox, final boolean recording, final boolean playback) {
			optionsComboBox = comboBox;
			supportsRecording = recording;
			supportsPlayback = playback;
		}

		public void run() {
			while (true) {
				try {
					Thread.sleep(6000);
					poll();
				} catch (InterruptedException e) {
					LOG.log(Level.SEVERE, String.format("Thread %s execution interrupted!", Thread
							.currentThread().getName()), e);
				}
			}
		}

		private void poll() {
			DefaultComboBoxModel model = (DefaultComboBoxModel) optionsComboBox.getModel();
			Vector<Mixer.Info> infos = SampledAudioUtilities
					.getMixerInfo(supportsPlayback, supportsRecording);

			// Add new MIXER device if it is not already present in the
			// current list.
			for (Mixer.Info info : infos) {
				if (model.getIndexOf(info) == -1) {
					model.addElement(info);
				}
			}
			// Remove a device if the MIXER device is not there any more.
			for (int i = 0; i < model.getSize(); i++) {
				Mixer.Info info = (Mixer.Info) model.getElementAt(i);
				if (!infos.contains(info)) {
					model.removeElementAt(i);
					i--;
				}
			}
		}
	}

	private void addConfigurationComponents(final DefaultFormBuilder builder) {
		builder.appendSeparator("Runtime configuration parameters");

		final JComboBox midiInComboBox = new JComboBox(MidiCommon.listDevices(true, false));
		Runnable pollingMidiIn = new MidiDevicePolling(midiInComboBox, true, false);
		Thread pollingMidiInThread = new Thread(pollingMidiIn, "MIDI IN Device polling");
		pollingMidiInThread.start();
		MoreMidiInfo info = (MoreMidiInfo) midiInComboBox.getSelectedItem();
		int index = -1;
		MidiDevice.Info[] infos = MidiSystem.getMidiDeviceInfo();
		for (int i = 0; i < infos.length; i++) {
			if (infos[i] == info.getInfo()) {
				index = i;
			}
		}
		Configuration.set(ConfKey.midi_input_device, index);

		final JComboBox midiOutComboBox = new JComboBox(MidiCommon.listDevices(false, true));
		Runnable pollingMidiOut = new MidiDevicePolling(midiOutComboBox, false, true);
		Thread pollingMidiOutThread = new Thread(pollingMidiOut, "MIDI OUT Device polling");
		pollingMidiOutThread.start();
		midiOutComboBox.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				MoreMidiInfo info = (MoreMidiInfo) midiOutComboBox.getSelectedItem();
				int index = -1;
				MidiDevice.Info[] infos = MidiSystem.getMidiDeviceInfo();
				for (int i = 0; i < infos.length; i++) {
					if (infos[i] == info.getInfo()) {
						index = i;
					}
				}
				Configuration.set(ConfKey.midi_output_device, index);
			}
		});

		final JComboBox microphoneComboBox = new JComboBox(SampledAudioUtilities.getMixerInfo(false, true));
		microphoneComboBox.setSelectedIndex(Configuration.getInt(ConfKey.microphone_device_mixer));
		Runnable pollingMixers = new MixerDevicePolling(microphoneComboBox, true, false);

		Thread pollingMixersThread = new Thread(pollingMixers, "Mixer device polling");
		pollingMixersThread.start();
		microphoneComboBox.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				Mixer.Info info = (Mixer.Info) microphoneComboBox.getSelectedItem();
				int index = -1;
				Vector<Mixer.Info> infos = SampledAudioUtilities.getMixerInfo(false, true);
				for (int i = 0; i < infos.size(); i++) {
					if (infos.get(i) == info) {
						index = i;
					}
				}
				Configuration.set(ConfKey.microphone_device_mixer, index);
			}
		});

		final JComboBox pitchDetectorsComboBox = new JComboBox(PitchDetectionMode.values());
		pitchDetectorsComboBox.setSelectedItem(Configuration
				.getPitchDetectionMode(ConfKey.pitch_tracker_current));
		pitchDetectorsComboBox.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				PitchDetectionMode mode = (PitchDetectionMode) pitchDetectorsComboBox.getSelectedItem();
				ConfKey current = ConfKey.pitch_tracker_current;
				Configuration.set(current, mode.name());
			}
		});

		builder.append("Pitch Detector", pitchDetectorsComboBox, true);
		builder.append("MIDI IN", midiInComboBox, true);
		builder.append("MIDI OUT", midiOutComboBox, true);
		builder.append("Microphone", microphoneComboBox, true);

	}

	/**
	 * Adds a text field for each configured value to the form.
	 * 
	 * @param builder
	 *            The form builder.
	 */
	private void addConfigurationTextFields(final DefaultFormBuilder builder) {

		builder.appendSeparator("Configuration parameters");
		List<ConfKey> orderedKeys = new ArrayList<ConfKey>();
		for (ConfKey key : ConfKey.values()) {
			orderedKeys.add(key);
		}
		Collections.sort(orderedKeys, new Comparator<ConfKey>() {
			public int compare(final ConfKey o1, final ConfKey o2) {
				return o1.name().compareTo(o2.name());
			}
		});
		for (ConfKey key : orderedKeys) {
			JTextField configurationTextField = new JTextField();
			String value = Configuration.get(key);
			String tooltip = Configuration.getDescription(key);
			String label = Configuration.getHumanName(key);
			if (label == null) {
				label = key.name();
			}
			configurationTextField.setToolTipText(tooltip);
			configurationTextField.setText(value);
			builder.append(label + ":", configurationTextField, true);
			configurationTextFields.put(configurationTextField, key);
			configurationTextField.addFocusListener(new FocusListener() {
				public void focusLost(FocusEvent e) {
					JTextField textField = (JTextField) e.getSource();
					ConfKey key = configurationTextFields.get(textField);
					String value = textField.getText();
					if (!value.equals(Configuration.get(key)) && !"".equals(value.trim())) {
						Configuration.set(key, value);
					}
				}

				public void focusGained(FocusEvent e) {
				}
			});
		}
	}
}
