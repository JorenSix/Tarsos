package be.hogent.tarsos.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.logging.Logger;

public class Execute {
	private static final Logger log = Logger.getLogger(Execute.class.getName());

	//disable default constructor
	private Execute(){}

	public static void command(String command, String redirectOutputToFile){
		try {
			String[] cmd;
			if (System.getProperty("os.name").contains("indows")) {
				cmd = buildWindowsCommand(command, redirectOutputToFile);
			} else {
				cmd = buildUinxCommand(command, redirectOutputToFile);
			}
			log.info("Executing " + cmd[0] + " " + cmd[1] + " " + cmd[2]);
			Runtime rt = Runtime.getRuntime();
			Process proc = rt.exec(cmd);
			BufferedReader stdout = new BufferedReader(new InputStreamReader(
					proc.getInputStream()));
			String line = null;
			while ((line = stdout.readLine()) != null)
				System.out.println(line);
			int exitVal = proc.waitFor();
			assert exitVal == 0 : "Process stopped with exit value: " + exitVal;
		} catch (IOException e) {
			e.printStackTrace();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}

	private static String[] buildWindowsCommand(String command, String redirectOutputToFile){
		redirectOutputToFile = redirectOutputToFile == null
		|| redirectOutputToFile.equals("") ? "" : " > "
		+ redirectOutputToFile;
		String[] cmd = new String[3];
		cmd[0] = "cmd.exe";
		cmd[1] = "/C";
		cmd[2] = command + redirectOutputToFile;
		return cmd;
	}

	private static String[] buildUinxCommand(String command, String redirectOutputToFile){
		redirectOutputToFile = redirectOutputToFile == null
		|| redirectOutputToFile.equals("") ? "" : "| cat > "
		+ redirectOutputToFile;
		String[] cmd = new String[3];
		cmd[0] = "/bin/bash";
		cmd[1] = "-c";
		cmd[2] = command + " 2>&1 " + redirectOutputToFile;
		return cmd;
	}
}
