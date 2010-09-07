package be.hogent.tarsos.util;

import java.io.ByteArrayInputStream;
import java.util.logging.Filter;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.SimpleFormatter;

import javax.swing.JTextArea;
import javax.swing.SwingUtilities;

/**
 * Utility class to handle logging with a JTextArea
 * 
 * @author Joren Six
 */
public class TextAreaHandler extends Handler {

	static private JTextArea jTextArea = null;

	/**
	 * Set the JTextArea to log to.
	 * 
	 * @param jTextArea
	 *            The JTextArea to log to.
	 */
	static private void setTextArea(JTextArea jTextArea) {
		TextAreaHandler.jTextArea = jTextArea;
	}

	/**
	 * 
	 * @param logJTextArea
	 *            setup logging for a JTextarea
	 */
	public static void setupLoggerHandler(JTextArea logJTextArea) {
		// This code attaches the handler to the text area
		setTextArea(logJTextArea);

		// Normally configuration would be done via a properties file
		// that would be read in with
		// LogManager.getLogManager().readConfiguration()
		// But I create an inputstream here to keep it local.
		// See JAVA_HOME/jre/lib/logging.properties for more description of
		// these settings.
		//
		StringBuffer buf = new StringBuffer();
		buf.append("handlers = be.hogent.tarsos.util.TextAreaHandler, java.util.logging.ConsoleHandler"); // A
																											// default
																											// handler
																											// and
																											// our
																											// custom
																											// handler
		buf.append("\n");
		buf.append(".level = INFO"); // Set the default logging level see:
										// C:\software\sun\jdk141_05\docs\api\index.html
		buf.append("\n");
		buf.append("be.hogent.tarsos.util.TextAreaHandler.level = INFO"); // Custom
																			// Handler
																			// logging
																			// level
		buf.append("\n");
		buf.append("java.util.logging.ConsoleHandler.level = INFO"); // Custom
																		// Handler
																		// logging
																		// level
		buf.append("\n");
		buf.append("java.util.logging.ConsoleHandler.formatter = java.util.logging.SimpleFormatter"); //

		try {
			java.util.logging.LogManager.getLogManager().readConfiguration(
					new ByteArrayInputStream(buf.toString().getBytes()));
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private Level level = Level.INFO; // The logging level for this handler,
										// which is configurable.

	/**
	 * Include filtering mechanism as it is not included in the (lame) Abstract
	 * Handler class.
	 */
	public TextAreaHandler() {
		Filter filter = new Filter() {
			@Override
			public boolean isLoggable(LogRecord record) {
				return record.getLevel().intValue() >= level.intValue();
			}
		};
		this.setFilter(filter);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.util.logging.Handler#publish(java.util.logging.LogRecord)
	 */
	@Override
	public void publish(LogRecord logRecord) {
		// Must filter our own logRecords, (lame) Abstract Handler does not do
		// it for us.
		if (!getFilter().isLoggable(logRecord)) {
			return;
		}

		final String message = new SimpleFormatter().format(logRecord);

		// Append formatted message to textareas using the Swing Thread.
		SwingUtilities.invokeLater(new Runnable() {
			@Override
			public void run() {
				jTextArea.append(message);
			}
		});
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.util.logging.Handler#close()
	 */
	@Override
	public void close() throws SecurityException {
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.util.logging.Handler#flush()
	 */
	@Override
	public void flush() {
	}

	/**
	 * Must capture level to use in our custom filter, because this is not done
	 * in the abstract class.
	 */
	@Override
	public void setLevel(Level level) {
		this.level = level;
		super.setLevel(level);
	}
}
