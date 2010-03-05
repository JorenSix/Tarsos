package be.hogent.tarsos.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import be.hogent.tarsos.util.FileUtils;


public class FileUtilsTests {
	@Test
	public void testGlob() {		 
		assertTrue(FileUtils.glob("audio/makam", ".*.wav").get(0).endsWith(".wav"));
		assertTrue(FileUtils.glob("audio/makam", ".*.wav").size()!=0);
	}
	@Test
	public void testSanitizeFilename(){
		assertEquals("te__st", FileUtils.sanitizedFileName("te  st"));
		assertEquals("t___st", FileUtils.sanitizedFileName("të  st"));
	}
}
