package be.hogent.tarsos.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.Random;

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

	@Test
	public void testPath(){
		if(System.getProperty("os.name").contains("indows")){
			assertEquals("c:\\blaat", FileUtils.path("c:\\blaat\\bla.jpg"));
		}else{
			assertEquals("/home/joren/test.jpg", FileUtils.path("/home/joren"));
		}
	}

	/**
	 * Tests: write, append, exists, copy and remove
	 */
	@Test
	public void testWriteFile(){
		String fileName = FileUtils.combine("data","tests","test_file.txt");

		assertTrue("File shoud not exist", ! FileUtils.exists(fileName));

		double d1 = new Random().nextDouble();
		String expectedContents = "test" + d1 + "\n";
		FileUtils.writeFile("test" + d1 + "\n", fileName);

		assertTrue("File shoud exist by now", FileUtils.exists(fileName));

		double d2 = new Random().nextDouble();
		FileUtils.appendFile("test" + d2 + "\n", fileName);
		expectedContents += "test" + d2 + "\n";

		String contents = FileUtils.readFile(fileName);
		assertEquals(expectedContents, contents);

		//a copy of the file should have the same contents
		String copyFileName = FileUtils.combine("data","tests","test_file_copy.txt");
		FileUtils.cp(fileName, copyFileName);
		contents = FileUtils.readFile(copyFileName);
		assertEquals(expectedContents, contents);

		//cleanup
		assertTrue("File could not be deleted. Closed correctly?",FileUtils.rm(copyFileName));
		assertTrue("File shoud not exist", ! FileUtils.exists(copyFileName));
		assertTrue("File could not be deleted. Closed correctly?",FileUtils.rm(fileName));
		assertTrue("File shoud not exist", ! FileUtils.exists(fileName));
	}

	@Test
	public void testExtension(){
		String fileName = FileUtils.combine("data","tests","test_file.txt");
		assertEquals("txt", FileUtils.extension(fileName));
		fileName = FileUtils.combine("data","tests","test_file");
		assertEquals("", FileUtils.extension(fileName));
	}

	@Test
	public void testBasename(){
		String fileName = FileUtils.combine("data","tests","test_file.txt");
		assertEquals("test_file", FileUtils.basename(fileName));
		fileName = FileUtils.combine("data","tests","test_file");
		assertEquals("test_file", FileUtils.basename(fileName));
	}
}
