package be.hogent.tarsos.ui.pitch;

import java.awt.BorderLayout;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;

import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.event.CaretEvent;
import javax.swing.event.CaretListener;

import be.hogent.tarsos.util.ConfKey;
import be.hogent.tarsos.util.Configuration;

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
		builder.appendSeparator("Configuratieparameters");
		List<ConfKey> orderedKeys = new ArrayList<ConfKey>();
		for (ConfKey key : ConfKey.values()) {
			orderedKeys.add(key);
		}
		Collections.sort(orderedKeys, new Comparator<ConfKey>() {
			@Override
			public int compare(final ConfKey o1, final ConfKey o2) {
				return o1.name().compareTo(o2.name());
			}
		});
		for (ConfKey key : orderedKeys) {
			JTextField configurationTextField = new JTextField();
			String value = Configuration.get(key);
			configurationTextField.setText(value);
			builder.append(key.name() + ":", configurationTextField, true);
			configurationTextFields.put(configurationTextField, key);
			configurationTextField.addCaretListener(new CaretListener() {
				@Override
				public void caretUpdate(CaretEvent e) {
					JTextField textField = (JTextField) e.getSource();
					ConfKey key = configurationTextFields.get(textField);
					String value = textField.getText();
					Configuration.set(key, value);
				}
			});
		}
		JComponent center = builder.getPanel();
		add(center, BorderLayout.CENTER);
	}

}
