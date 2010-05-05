package be.hogent.tarsos.util;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.URL;
import java.net.URLConnection;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CharsetEncoder;
import java.nio.charset.CodingErrorAction;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.logging.Logger;
import java.util.regex.Pattern;

/**
 * Exports a DatabaseResult to a CSV-file.
 * 
 * @author Joren Six
 * 
 */
public class FileUtils {
    private static final Logger log = Logger.getLogger(FileUtils.class.getName());
    private static final char pathSeparator = File.separatorChar;
    private static final char extensionSeparator = '.';

    public static String temporaryDirectory() {
        String tempDir = System.getProperty("java.io.tmpdir");
        if (tempDir.contains(" ")) {
            log.warning("Temporary directory (" + tempDir + ") contains whitespace");
        }
        return tempDir;

    }

    // disable the default constructor
    private FileUtils() {
    }

    /**
     * Joins path elements using the systems path separator. e.g. "/tmp" and
     * "test.wav" combined together should yield /tmp/test.wav on UNIX
     * 
     * @param path
     *            the path parts part
     * @return each element from path joined by the systems path separator.
     */
    public static String combine(String... path) {
        File file = new File(path[0]);
        for (int i = 1; i < path.length; i++) {
            file = new File(file, path[i]);
        }
        return file.getPath();
    }

    /**
     * 
     * @return The path where the program is executed.
     * 
     */
    public static String getRuntimePath() {
        String runtimePath = "";
        try {
            runtimePath = new File(".").getCanonicalPath();
        } catch (IOException e) {
            throw new Error(e);
        }
        return runtimePath;
    }

    /**
     * Writes a file to disk. Uses the string contents as content. Failures are
     * logged.
     * 
     * @param contents
     *            The contents of the file.
     * @param name
     *            The name of the file to create.
     * 
     */
    public static void writeFile(String contents, String name) {
        FileWriter FW = null;
        try {
            FW = new FileWriter(name);
            BufferedWriter outputStream = new BufferedWriter(FW);
            PrintWriter output = new PrintWriter(outputStream);
            output.print(contents);
            outputStream.flush();
            outputStream.close();
        } catch (IOException i1) {
            log.severe("Can't open file:" + name);
        }
    }

    /**
     * Appends a string to a file on disk. Fails silently.
     * 
     * @param contents
     *            The contents of the file.
     * @param name
     *            The name of the file to create.
     * 
     */
    public static void appendFile(String contents, String name) {
        FileWriter FW = null;
        try {
            FW = new FileWriter(name, true);
            BufferedWriter outputStream = new BufferedWriter(FW);
            PrintWriter output = new PrintWriter(outputStream);
            output.print(contents);
            outputStream.flush();
            outputStream.close();
        } catch (IOException i1) {
            log.severe("Can't open file:" + name);
        }
    }

    /**
     * Reads the contents of a file.
     * 
     * @param name
     *            the name of the file to read
     * @return the contents of the file if successful, an empty string
     *         otherwise.
     */
    public static String readFile(String name) {
        FileReader fileReader = null;
        StringBuilder contents = new StringBuilder();
        try {
            File file = new File(name);
            if (!file.exists()) {
                throw new Error("File " + name + " does not exist");
            }
            fileReader = new FileReader(file);
            BufferedReader in = new BufferedReader(fileReader);
            String inputLine;
            while ((inputLine = in.readLine()) != null) {
                contents.append(inputLine).append("\n");
            }
            in.close();
        } catch (IOException i1) {
            log.severe("Can't open file:" + name);
        }
        return contents.toString();
    }

    /**
     * Reads the contents of a file in a jar.
     * 
     * @param path
     *            the path to read e.g. /package/name/here/help.html
     * @return the contents of the file when successful, an empty string
     *         otherwise.
     */
    public static String readFileFromJar(String path) {
        StringBuilder contents = new StringBuilder();
        URL url = FileUtils.class.getResource(path);
        URLConnection connection;
        try {
            connection = url.openConnection();
            BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()));
            String inputLine;
            while ((inputLine = in.readLine()) != null) {
                contents.append(new String(inputLine.getBytes(), "UTF-8")).append("\n");
            }
            in.close();
        } catch (IOException e) {
            log.severe("Error while reading file " + path + " from jar: " + e.getMessage());
            e.printStackTrace();
        } catch (NullPointerException e) {
            log.severe("Error while reading file " + path + " from jar: " + e.getMessage());
            e.printStackTrace();
        }
        return contents.toString();
    }

    /**
     * Copy a file from a jar.
     * 
     * @param source
     *            The path to read e.g. /package/name/here/help.html
     */
    public static void copyFileFromJar(String source, String target) {
        try {
            InputStream in = new FileUtils().getClass().getResourceAsStream(source);
            OutputStream out;
            out = new FileOutputStream(target);
            byte[] buffer = new byte[4096];
            int r;
            while ((r = in.read(buffer)) != -1) {
                out.write(buffer, 0, r);
            }
            out.close();
            in.close();
        } catch (FileNotFoundException e) {
            log.severe("File not found: " + e.getMessage());
            e.printStackTrace();
        } catch (IOException e) {
            log.severe("Exception while copying file from jar" + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Reads a CSV-file from disk. The separator can be chosen.
     * 
     * @param fileName
     *            the filename, an exception if thrown if the file does not
     *            exist
     * @param separator
     *            the separator, e.g. ";" or ","
     * @param expectedNumberOfColumns
     *            The expected number of columns, user -1 if the number is
     *            unknown. An exception is thrown if there is a row with an
     *            unexpected row length.
     * @return a List of string arrays. The data of the CSV-file can be found in
     *         the arrays. Each row corresponds with an array.
     */
    public static List<String[]> readCSVFile(String fileName, String separator, int expectedNumberOfColumns) {
        List<String[]> data = new ArrayList<String[]>();
        FileReader fileReader = null;

        try {
            File file = new File(fileName);
            if (!file.exists()) {
                throw new Error("File '" + fileName + "' does not exist");
            }
            fileReader = new FileReader(file);
            BufferedReader in = new BufferedReader(fileReader);
            String inputLine;
            int lineNumber = 0;
            while ((inputLine = in.readLine()) != null) {
                lineNumber++;
                String[] row = inputLine.split(separator);
                if (expectedNumberOfColumns == -1 || expectedNumberOfColumns == row.length) {
                    data.add(row);
                } else {
                    throw new Error("Unexpected row length (line " + lineNumber + " ). " + "Expected:"
                            + expectedNumberOfColumns + " real " + row.length
                            + ". CVS-file incorrectly formatted?");
                }
            }
            in.close();
        } catch (IOException i1) {
            log.severe("Can't open file:" + fileName);
        }
        return data;
    }

    public interface RowFilter {
        public boolean acceptRow(String[] row);
    }

    public static RowFilter ACCEPT_ALL_ROWFILTER = new RowFilter() {
        @Override
        public boolean acceptRow(String[] row) {
            return true;
        }
    };

    public static List<String> readColumnFromCSVData(List<String[]> data, int columnIndex, RowFilter filter) {
        filter = filter == null ? ACCEPT_ALL_ROWFILTER : filter;
        List<String> columnData = new ArrayList<String>();
        for (String[] row : data) {
            if (filter.acceptRow(row)) {
                columnData.add(row[columnIndex]);
            }
        }
        return columnData;
    }

    public static void export(String filename, String[] header, List<Object[]> data) {

        String dateFormat = "yyyy-MM-dd hh:mm:ss";
        String numberFormat = "#0.000";
        SimpleDateFormat exportDateFormatter = new SimpleDateFormat(dateFormat);
        DecimalFormat exportDecimalFormat = new DecimalFormat(numberFormat);
        String separator = "\t";

        filename = filename + ".csv";
        FileWriter FW = null;
        try {
            FW = new FileWriter(filename);
            BufferedWriter OutputStream = new BufferedWriter(FW);
            PrintWriter output = new PrintWriter(OutputStream);

            if (header != null) {
                // HEADERS
                for (int column = 0; column < header.length; column++) {
                    Object valueObject = header[column];
                    String value = valueObject == null ? "" : valueObject.toString();
                    value = value.replace(separator, "");
                    output.print(value + separator);
                }
                output.println("");
            }

            // DATA
            for (Object[] row : data) {
                for (int column = 0; column < row.length; column++) {
                    Object valueObject = row[column];
                    String value = valueObject == null ? "" : valueObject.toString();
                    if (valueObject != null) {
                        if (valueObject instanceof Double) {
                            value = exportDecimalFormat.format(valueObject);
                        } else if (valueObject instanceof Date) {
                            value = exportDateFormatter.format(valueObject);
                        }
                    }
                    value = value.replace(separator, "");
                    output.print(value + separator);
                }
                output.println("");
            }
            OutputStream.flush();
            OutputStream.close();
        } catch (IOException i1) {
            log.severe("Can't open file:" + filename);
        }
    }

    /**
     * <p>
     * Return a list of files in directory that satisfy pattern. Pattern should
     * be a valid regular expression not a 'unix glob pattern' so in stead of
     * <code>*.wav</code> you could use <code>.*\.wav</code>
     * </p>
     * <p>
     * E.g. in a directory <code>home</code> with the files
     * <code>test.txt</code>, <code>blaat.wav</code> and <code>foobar.wav</code>
     * the pattern <code>.*\.wav</code> matches <code>blaat.wav</code> and
     * <code>foobar.wav</code>
     * </p>
     * 
     * @param directory
     *            a readable directory.
     * @param pattern
     *            a valid regular expression.
     * @return a list of filenames matching the pattern for directory.
     * @exception Error
     *                an error is thrown if the directory is not ... a
     *                directory.
     * @exception java.util.regex.PatternSyntaxException
     *                Unchecked exception thrown to indicate a syntax error in a
     *                regular-expression pattern.
     * 
     */
    public static List<String> glob(String directory, String pattern) {
        File dir = new File(directory);
        Pattern p = Pattern.compile(pattern);
        List<String> matchingFiles = new ArrayList<String>();
        if (!dir.isDirectory()) {
            throw new Error(directory + " is not a directory");
        }
        for (String file : dir.list()) {
            if (!new File(file).isDirectory() && p.matcher(file).matches() && file != null) {
                matchingFiles.add(FileUtils.combine(directory, file));
            }
        }
        // sort alphabetically
        Collections.sort(matchingFiles);
        return matchingFiles;
    }

    /**
     * Return the extension of a file.
     * 
     * @param fileName
     *            the file to get the extension for
     * @return the extension. E.g. TXT or JPEG.
     */
    public static String extension(String fileName) {
        int dot = fileName.lastIndexOf(extensionSeparator);
        return dot == -1 ? "" : fileName.substring(dot + 1);
    }

    /**
     * Returns the filename without path and without extension
     * 
     * @param fileName
     * @return the file name without extension and path
     */
    public static String basename(String fileName) {
        int dot = fileName.lastIndexOf(extensionSeparator);
        int sep = fileName.lastIndexOf(pathSeparator);
        if (sep == -1) {
            sep = fileName.lastIndexOf('\\');
        }
        if (dot == -1) {
            dot = fileName.length();
        }
        return fileName.substring(sep + 1, dot);
    }

    /**
     * Returns the path for a file.<br>
     * <code>path("/home/user/test.jpg") == "/home/user"</code><br>
     * Uses the correct pathSeparator depending on the operating system. On
     * windows c:/test/ is not c:\test\
     * 
     * @param fileName
     *            the name of the file using correct path separators.
     * @return the path of the file.
     */
    public static String path(String fileName) {
        int sep = fileName.lastIndexOf(pathSeparator);
        return fileName.substring(0, sep);
    }

    /**
     * Checks if a file exists
     * 
     * @param fileName
     *            the name of the file to check.
     * @return true if and only if the file or directory denoted by this
     *         abstract pathname exists; false otherwise
     */
    public static boolean exists(String fileName) {
        return new File(fileName).exists();
    }

    /**
     * Creates a directory and parent directories if needed
     * 
     * @param path
     *            the path of the directory to create
     * @return true if the directory was created (possibly with parent
     *         directories) , false otherwise
     */
    public static boolean mkdirs(String path) {
        return new File(path).mkdirs();
    }

    /**
     * replaces UTF-8 characters and spaces with _ . Returns the complete path.
     * 
     * <p>
     * E.g. <code>/tmp/01.��skar ton.mp3</code> is converted to:
     * <code>/tmp/01.__skar_ton.mp3</code>
     * </p>
     * 
     * @param fileName
     *            the filename to sanitize
     * @return the complete sanitized path.
     */
    public static String sanitizedFileName(String fileName) {
        String baseName = basename(fileName);
        String newBaseName = baseName.replaceAll(" ", "_");
        newBaseName = newBaseName.replaceAll("\\(", "-");
        newBaseName = newBaseName.replaceAll("\\)", "-");
        newBaseName = newBaseName.replaceAll("&", "and");
        newBaseName = filterNonAscii(newBaseName);
        return fileName.replace(baseName, newBaseName);
    }

    private static String filterNonAscii(String inString) {
        // Create the encoder and decoder for the character encoding
        Charset charset = Charset.forName("US-ASCII");
        CharsetDecoder decoder = charset.newDecoder();
        CharsetEncoder encoder = charset.newEncoder();
        // This line is the key to removing "unmappable" characters.
        encoder.replaceWith("_".getBytes());
        encoder.onUnmappableCharacter(CodingErrorAction.REPLACE);
        String result = inString;
        try {
            // Convert a string to bytes in a ByteBuffer
            ByteBuffer bbuf = encoder.encode(CharBuffer.wrap(inString));
            // Convert bytes in a ByteBuffer to a character ByteBuffer and then
            // to a string.
            CharBuffer cbuf = decoder.decode(bbuf);
            result = cbuf.toString();
        } catch (CharacterCodingException cce) {
            log.severe("Exception during character encoding/decoding: " + cce.getMessage());
        }

        return result;
    }

    /**
     * Copy from source to target.
     * 
     * @param source
     *            the source file.
     * @param target
     *            the target file.
     */
    public static void cp(String source, String target) {
        FileChannel inChannel = null;
        FileChannel outChannel = null;
        try {
            inChannel = new FileInputStream(new File(source)).getChannel();
            outChannel = new FileOutputStream(new File(target)).getChannel();
            inChannel.transferTo(0, inChannel.size(), outChannel);
        } catch (FileNotFoundException e) {
            log.severe("File " + source + " not found! " + e.getMessage());
        } catch (IOException e) {
            log.severe("Error while copying " + source + " to " + target + " : " + e.getMessage());
        } finally {
            try {
                if (inChannel != null) {
                    inChannel.close();
                }
                if (outChannel != null) {
                    outChannel.close();
                }
            } catch (IOException e) {
                // ignore
            }
        }
    }

    /**
     * Removes a file from disk
     * 
     * @param fileName
     *            the file to remove
     * @return true if and only if the file or directory is successfully
     *         deleted; false otherwise
     */
    public static boolean rm(String fileName) {
        return new File(fileName).delete();
    }
}
