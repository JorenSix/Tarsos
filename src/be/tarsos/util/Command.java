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
package be.tarsos.util;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

import org.apache.commons.exec.CommandLine;
import org.apache.commons.exec.DefaultExecutor;
import org.apache.commons.exec.ExecuteWatchdog;
import org.apache.commons.exec.PumpStreamHandler;


public class Command {
	
	private static final Logger LOG = Logger.getLogger(Command.class.getName());
	
	private final ArrayList<String> args = new ArrayList<String>();
	private final ArrayList<Boolean> argIsFile = new ArrayList<Boolean>();
	private final String commandName;
	
	public Command(String name){
		commandName = name;
	}
	
	/**
	 * Adds an argument to the executable call.
	 * 
	 * @param arg
	 *            The argument.
	 * @return The command itself so methods can be chained.
	 */
	public Command addArgument(String arg) {
		args.add(arg);
		argIsFile.add(false);
		LOG.finer("Added argument " + arg + " to command " + commandName);
		return this;
	}
	
	/**
	 * Add a file to the  executable call.
	 * @param arg
	 * @return The command itself so methods can be chained.
	 */
	public Command addFileArgument(String arg){
		args.add(arg);
		argIsFile.add(true);
		LOG.finer("Added file argument " + arg + " to command " + commandName);
		return this;
	}
	
	/**
	 * Executes the command.
	 * @return The messages written on standard output.
	 * @throws IOException
	 */
	public String execute() throws IOException {
		
		CommandLine cmdLine = new CommandLine(commandName);
		
		int fileNumber=0;
		Map<String,File> map = new HashMap<String,File>();
		for (int i = 0 ;i<args.size();i++) {
			final String arg = args.get(i);
			final Boolean isFile = argIsFile.get(i);
			if(isFile){
				String key = "file" + fileNumber;
				map.put(key, new File(arg));
				cmdLine.addArgument("${" + key + "}",false);
				fileNumber++;
			} else {
				cmdLine.addArgument(arg);
			}
		}		
		cmdLine.setSubstitutionMap(map);
		

		
		DefaultExecutor executor = new DefaultExecutor();
		//15 minutes wait
		ExecuteWatchdog watchdog = new ExecuteWatchdog(60 * 1000 * 15);
		executor.setWatchdog(watchdog);
		final ByteArrayOutputStream out =  new ByteArrayOutputStream();
		final PumpStreamHandler pump = new PumpStreamHandler(out);
		executor.setStreamHandler(pump);
		executor.setExitValue(0);
		StopWatch w = new StopWatch();
		LOG.fine("Execute " + commandName + "  " + cmdLine.toString());
		executor.execute(cmdLine);
		LOG.info("Executing " + commandName + " finished in " + w.formattedToString());
		return out.toString();	
	}

}
