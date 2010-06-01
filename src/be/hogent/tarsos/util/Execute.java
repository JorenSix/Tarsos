package be.hogent.tarsos.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.logging.Level;
import java.util.logging.Logger;

import be.hogent.tarsos.apps.Tarsos;

public final class Execute {
    private static final Logger LOG = Logger.getLogger(Execute.class.getName());

    // disable default constructor
    private Execute() {
    }

    /**
     * Executes a command and returns the exit value of the process.
     * @param command
     *            the command to execute
     * @param redirectOutputToFile
     *            redirects the output to this file. Leave empty or set to null
     *            when no output file is wanted.
     * @return the exit value of the process. 0 means everything went OK. Other
     *         values depend on the operating system and process.
     */
    public static int command(final String command, final String redirectOutputToFile) {
        int exitValue = -1;
        BufferedReader stdout = null;
        try {
            final String[] cmd = buildCommand(command, redirectOutputToFile);
            LOG.info("Executing " + cmd[0] + " " + cmd[1] + " " + cmd[2]);
            final Runtime rt = Runtime.getRuntime();
            final Process proc = rt.exec(cmd);
            stdout = new BufferedReader(new InputStreamReader(proc.getInputStream()));
            String line = stdout.readLine();
            while (line != null) {
                Tarsos.println(line);
                line = stdout.readLine();
            }
            exitValue = proc.waitFor();
        } catch (final IOException e) {
            LOG.log(Level.SEVERE, "Error while communicating with process", e);
        } catch (final InterruptedException e) {
            LOG.log(Level.SEVERE, "Process interuppted", e);
        } finally {
            try {
                stdout.close();
            } catch (final IOException e) {
                LOG.log(Level.SEVERE, "Could not close communication channel with proc.", e);
            } catch (final NullPointerException e) {
                LOG.log(Level.SEVERE, "Failed to initialize communication channel with proc.", e);
            }
        }
        if (exitValue != 0) {
            LOG.warning("Process stopped with exit value: " + exitValue);
        }
        return exitValue;
    }

    /**
     * Executes a command and returns the exit value of the process. Redirect
     * STDOUT the process to STDOUT of this process.
     * @param command
     *            the command to execute
     * @return the exit value of the process. 0 means everything went OK. Other
     *         values depend on the operating system and process.
     */
    public static int command(final String command) {
        return command(command, null);
    }

    /**
     * Checks if the external command is available in the path.
     * @param command
     *            the command to check
     * @return <code>true</code> if the command is available, false otherwise.
     */
    public static boolean executableAvailable(final String command) {
        boolean executableIsInPath = false;
        final int exitValue = Execute.command(command);
        int commandNotFoundExitCode;
        if (System.getProperty("os.name").contains("indows")) {
            commandNotFoundExitCode = Configuration.getInt(ConfKey.win_shell_executable_not_found_exit_code);
        } else {
            commandNotFoundExitCode = Configuration.getInt(ConfKey.unix_shell_executable_not_found_exit_code);
        }
        if (exitValue != commandNotFoundExitCode) {
            executableIsInPath = true;
        }
        return executableIsInPath;
    }

    private static String[] buildCommand(final String command, final String redirectOutputToFile) {
        String[] cmd;
        if (System.getProperty("os.name").contains("indows")) {
            cmd = buildWindowsCommand(command, redirectOutputToFile);
        } else {
            cmd = buildUinxCommand(command, redirectOutputToFile);
        }
        return cmd;
    }

    private static String[] buildWindowsCommand(final String command, final String redirectOutputToFile) {
        final String redirectPostfix = redirectOutputToFile == null ? "" : " > "
            + redirectOutputToFile;
        final String[] cmd = new String[3];
        cmd[0] = Configuration.get(ConfKey.win_shell_executable);
        cmd[1] = Configuration.get(ConfKey.win_shell_executable_option);
        cmd[2] = command + redirectPostfix;
        return cmd;
    }

    private static String[] buildUinxCommand(final String command, final String redirectOutputToFile) {
        final String redirectPostfix = redirectOutputToFile == null ? ""
                : "| cat > " + redirectOutputToFile;
        final String[] cmd = new String[3];
        cmd[0] = Configuration.get(ConfKey.unix_shell_executable);
        cmd[1] = Configuration.get(ConfKey.unix_shell_executable_option);
        cmd[2] = command + " 2>&1 " + redirectPostfix;
        return cmd;
    }
}
