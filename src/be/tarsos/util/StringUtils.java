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

import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CharsetEncoder;
import java.nio.charset.CodingErrorAction;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.List;

public final class StringUtils {

	/**
	 * Hides the default constructor.
	 */
	private StringUtils() {
	}

	/**
	 * Replaces UTF-8 characters with _.
	 * 
	 * @param inString
	 *            The string to filter.
	 * @return A string with non ASCII chars replaced by underscore.
	 */
	private static String filterNonAscii(final String inString) {
		// Create the encoder and decoder for the character attributes
		final Charset charset = Charset.forName("US-ASCII");
		final CharsetDecoder decoder = charset.newDecoder();
		final CharsetEncoder encoder = charset.newEncoder();
		// This line is the key to removing "unmappable" characters.
		encoder.replaceWith("_".getBytes());
		encoder.onUnmappableCharacter(CodingErrorAction.REPLACE);
		String result = inString;
		try {
			// Convert a string to bytes in a ByteBuffer
			final ByteBuffer bbuf = encoder.encode(CharBuffer.wrap(inString));
			// Convert bytes in a ByteBuffer to a character ByteBuffer and then
			// to a string.
			final CharBuffer cbuf = decoder.decode(bbuf);
			result = cbuf.toString();
		} catch (final CharacterCodingException cce) {
			FileUtils.LOG.severe("Exception during character attributes/decoding: " + cce.getMessage());
		}

		return result;
	}

	/**
	 * replaces UTF-8 characters and spaces with _ . Returns the complete path.
	 * <p>
	 * E.g. <code>/tmp/01.��skar ton.mp3</code> is converted to:
	 * <code>/tmp/01.__skar_ton.mp3</code>
	 * </p>
	 * 
	 * @param data
	 *            the data to sanitize
	 * @return the complete sanitized path.
	 */
	public static String sanitize(final String data) {
		final String baseName = FileUtils.basename(data);
		String newBaseName = baseName.replaceAll(" ", "_");
		newBaseName = newBaseName.replaceAll("\\(", "-");
		newBaseName = newBaseName.replaceAll("\\)", "-");
		newBaseName = newBaseName.replaceAll("&", "and");
		newBaseName = newBaseName.replaceAll("#", "_");
		newBaseName = newBaseName.replaceAll("'", "_");
		newBaseName = filterNonAscii(newBaseName);
		return data.replace(baseName, newBaseName);
	}
	
	
	/**
	 * Joins elements in a list with separator.
	 * @param data
	 * @param separator
	 * @return A joined list, joined by the separator.
	 */
	public static String join(final List<?> data,String separator) {
		StringBuilder sb = new StringBuilder();
		for(int i = 0 ; i < data.size();i++){
			sb.append(data.get(i));
			//append with separator unless last element
			if (i != data.size() - 1 ){
				sb.append(separator);
			}
		}
		return sb.toString();
	}

	/**
	 * Calculates an MD5 hash for a text.
	 * 
	 * @param dataToEncode
	 *            The data to encode.
	 * @return A text representation of a hexadecimal value of length 32.
	 */
	public static String messageDigestFive(final String dataToEncode) {
		MessageDigest m;
		try {
			m = MessageDigest.getInstance("MD5");
			byte[] data = dataToEncode.getBytes();
			m.update(data, 0, data.length);
			BigInteger i = new BigInteger(1, m.digest());
			return String.format("%1$032X", i);
		} catch (NoSuchAlgorithmException e) {
			// MD5 Should be supported by the runtime!
			throw new IllegalStateException(e);
		}
	}

}
