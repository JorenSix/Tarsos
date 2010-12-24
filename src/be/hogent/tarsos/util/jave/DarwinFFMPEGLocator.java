package be.hogent.tarsos.util.jave;

import it.sauronsoftware.jave.FFMPEGLocator;

import java.io.File;
import java.io.IOException;

public class DarwinFFMPEGLocator extends FFMPEGLocator {

	final boolean isMac;
	final String path;

	public DarwinFFMPEGLocator() {
		String os = System.getProperty("os.name").toLowerCase();
		isMac = os.indexOf("mac") != -1;
		if (isMac) {
			path = getMacPath();
		} else {
			path = new DefaultFFMPEGLocator().getFFMPEGExecutablePath();
		}

	}

	private String getMacPath() {
		File destination = new File(System.getProperty("java.io.tmpdir"), "jave-ffmpeg");
		if (!destination.exists()) {
			DefaultFFMPEGLocator.copyFile("ffmpeg_mac_os_x_universal_binary", destination);
		}

		// Make sure executable is ... executable by executing chmod and setting
		// the executable bit.
		Runtime runtime = Runtime.getRuntime();
		try {
			runtime.exec(new String[] { "/bin/chmod", "755", destination.getAbsolutePath() });
		} catch (IOException e) {
			e.printStackTrace();
		}
		// return the path
		return destination.getAbsolutePath();
	}

	@Override
	protected String getFFMPEGExecutablePath() {
		return path;
	}
}
