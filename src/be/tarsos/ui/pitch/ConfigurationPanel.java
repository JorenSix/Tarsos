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
* Tarsos is developed by Joren Six at IPEM, University Ghent
*  
* -----------------------------------------------------------
*
*  Info: http://tarsos.0110.be
*  Github: https://github.com/JorenSix/Tarsos
*  Releases: http://0110.be/releases/Tarsos/
*  
*  Tarsos includes some source code by various authors,
*  for credits, license and info: see README.
* 
*/



package be.tarsos.ui.pitch;

import java.awt.BorderLayout;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;

import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.JTextField;
import javax.swing.SpinnerNumberModel;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import be.tarsos.util.ConfKey;
import be.tarsos.util.Configuration;
import be.tarsos.util.Configuration.ConfigChangeListener;

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

	public ConfigurationPanel() {
		super(new BorderLayout());
		FormLayout layout = new FormLayout("right:pref, 3dlu, min:grow");
		DefaultFormBuilder builder = new DefaultFormBuilder(layout);
		configurationTextFields = new HashMap<JTextField, ConfKey>();
		builder.setDefaultDialogBorder();
		builder.setRowGroupingEnabled(true);
		
		addConfigurationTextFields(builder);
		
		addPitchConfiguration(builder);

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

	


	private void addPitchConfiguration(DefaultFormBuilder builder) {
		builder.appendSeparator("Tarsos Pitch detection parameters");
		
		final JSpinner bufferSizeSpinner = new JSpinner(new SpinnerNumberModel(Configuration.getInt(ConfKey.pitch_detector_buffer_size),1,1000,1));
		final JSpinner bufferOverlapSpinner = new JSpinner(new SpinnerNumberModel(Configuration.getInt(ConfKey.pitch_detector_buffer_overlap),1,100,1));
		final JLabel minDetectableFrequency = new JLabel(String.format("%.3f Hz", 2000.0 / (int) bufferSizeSpinner.getValue() ));
		final JLabel stepSizeInMilliseconds = new JLabel(String.format("%.3f ms",  (int) bufferOverlapSpinner.getValue() / 100.0 * (int) bufferSizeSpinner.getValue() ));
		
		ChangeListener updateLabels = new ChangeListener() {
			@Override
			public void stateChanged(ChangeEvent arg0) {
				minDetectableFrequency.setText(String.format("%.3f Hz", 2000.0 / (int) bufferSizeSpinner.getValue()));
				stepSizeInMilliseconds.setText(String.format("%.3f ms", (int) bufferOverlapSpinner.getValue() / 100.0 * (int) bufferSizeSpinner.getValue()));
				
				Configuration.set(ConfKey.pitch_detector_buffer_size, bufferSizeSpinner.getValue());
				Configuration.set(ConfKey.pitch_detector_buffer_overlap, bufferOverlapSpinner.getValue());
			}
		};
		
		bufferOverlapSpinner.addChangeListener(updateLabels);
		bufferSizeSpinner.addChangeListener(updateLabels);
		
		builder.append("Buffer size (in milliseconds)",bufferSizeSpinner, true );
		builder.append("Buffer overlap (percentage)",bufferOverlapSpinner, true);
		builder.append("Minimum detectable frequency",minDetectableFrequency, true);
		builder.append("Step size",stepSizeInMilliseconds, true);
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
