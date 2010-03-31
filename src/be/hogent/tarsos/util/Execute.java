package be.hogent.tarsos.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.logging.Logger;

import be.hogent.tarsos.util.Configuration.Config;

public class Execute {
	private static final Logger log = Logger.getLogger(Execute.class.getName());

	//disable default constructor
	private Execute(){}

	/**
	 * Executes a command and returns the exit value of the process.
	 * @param command the command to execute
	 * @param redirectOutputToFile redirects the output to this file. Leave empty
	 * or set to null when no output file is wanted.
	 * @return the exit value of the process. 0 means everything went OK. Other values
	 * depend on the operating system and process.
	 */
	public static int command(String command, String redirectOutputToFile){
		int exitValue = -1;
		try {
			String[] cmd = buildCommand(command, redirectOutputToFile);
			log.info("Executing " + cmd[0] + " " + cmd[1] + " " + cmd[2]);
			Runtime rt = Runtime.getRuntime();
			Process proc = rt.exec(cmd);
			BufferedReader stdout = new BufferedReader(new InputStreamReader(proc.getInputStream()));
			String line = null;
			while ((line = stdout.readLine()) != null)
				System.out.println(line);
			exitValue = proc.waitFor();
		} catch (IOException e) {
			log.severe("Error while communicating with process");
			e.printStackTrace();
		} catch (InterruptedException e) {
			log.severe("Process interuppted");
			e.printStackTrace();
		}
		if(exitValue != 0)
			log.warning("Process stopped with exit value: " + exitValue);
		return exitValue;
	}

	/**
	 * Executes a command and returns the exit value of the process. Redirect STDOUT the process
	 * to STDOUT of this process.
	 * @param command the command to execute
	 * @return the exit value of the process. 0 means everything went OK. Other values
	 * depend on the operating system and process.
	 */
	public static int command(String command){
		return command(command,null);
	}

	/**
	 * Checks if the external command is available in the path.
	 * @param command the command to check
	 * @return <code>true</code> if the command is available, false otherwise.
	 */
	public static boolean executableAvailable(String command){
		boolean executableIsInPath = false;
		int exitValue = Execute.command(command);
		int commandNotFoundExitCode;
		if (System.getProperty("os.name").contains("indows")) {
			commandNotFoundExitCode = Configuration.getInt(Config.win_shell_executable_not_found_exit_code);
		} else {
			commandNotFoundExitCode = Configuration.getInt(Config.unix_shell_executable_not_found_exit_code);
		}
		if(exitValue != commandNotFoundExitCode)
			executableIsInPath = true;
		return executableIsInPath;
	}

	private static String[] buildCommand(String command, String redirectOutputToFile){
		String[] cmd;
		if (System.getProperty("os.name").contains("indows")) {
			cmd = buildWindowsCommand(command, redirectOutputToFile);
		} else {
			cmd = buildUinxCommand(command, redirectOutputToFile);
		}
		return cmd;
	}

	private static String[] buildWindowsCommand(String command, String redirectOutputToFile){
		redirectOutputToFile = redirectOutputToFile == null
		|| redirectOutputToFile.equals("") ? "" : " > "
		+ redirectOutputToFile;
		String[] cmd = new String[3];
		cmd[0] = Configuration.get(Config.win_shell_executable);
		cmd[1] = Configuration.get(Config.win_shell_executable_option);
		cmd[2] = command + redirectOutputToFile;
		return cmd;
	}

	private static String[] buildUinxCommand(String command, String redirectOutputToFile){
		redirectOutputToFile = redirectOutputToFile == null
		|| redirectOutputToFile.equals("") ? "" : "| cat > "
		+ redirectOutputToFile;
		String[] cmd = new String[3];
		cmd[0] = Configuration.get(Config.unix_shell_executable);
		cmd[1] = Configuration.get(Config.unix_shell_executable_option);
		cmd[2] = command + " 2>&1 " + redirectOutputToFile ;
		return cmd;
	}
}
