package be.hogent.tarsos.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertTrue;

import java.util.Random;

import org.junit.Test;

import be.hogent.tarsos.util.FileUtils;

public class FileUtilsTests {
    @Test
    public void testGlob() {
        assertTrue("Should end with .wav.", FileUtils.glob("audio/makam", ".*.wav").get(0).endsWith(".wav"));
        assertNotSame("Folder should contain a wav file.", 0, FileUtils.glob("audio/makam", ".*.wav").size());
    }

    @Test
    public void testSanitizeFilename() {
        assertEquals("te__st", FileUtils.sanitizedFileName("te  st"));
        assertEquals("t___st", FileUtils.sanitizedFileName("të  st"));
    }

    @Test
    public void testPath() {
        if (System.getProperty("os.name").contains("indows")) {
            assertEquals("c:\\blaat", FileUtils.path("c:\\blaat\\bla.jpg"));
        } else {
            assertEquals("/home/joren/test.jpg", FileUtils.path("/home/joren"));
        }
    }

    /**
     * Tests: write, append, exists, copy and remove.
     */
    @Test
    public void testWriteFile() {
        final String fileName = FileUtils.combine("data", "tests", "test_file.txt");

        assertFalse("File shoud not exist", FileUtils.exists(fileName));

        Random rnd = new Random();
        double d1 = rnd.nextDouble();
        String expectedContents = "test" + d1 + "\n";
        FileUtils.writeFile("test" + d1 + "\n", fileName);

        assertTrue("File shoud exist by now", FileUtils.exists(fileName));

        double d2 = rnd.nextDouble();
        FileUtils.appendFile("test" + d2 + "\n", fileName);
        expectedContents += "test" + d2 + "\n";

        String contents = FileUtils.readFile(fileName);
        assertEquals(expectedContents, contents);

        // a copy of the file should have the same contents
        String copyFileName = FileUtils.combine("data", "tests", "test_file_copy.txt");
        FileUtils.cp(fileName, copyFileName);
        contents = FileUtils.readFile(copyFileName);
        assertEquals(expectedContents, contents);

        // cleanup
        assertTrue("File could not be deleted. Closed correctly?", FileUtils.rm(copyFileName));
        assertFalse("File shoud not exist", FileUtils.exists(copyFileName));
        assertTrue("File could not be deleted. Closed correctly?", FileUtils.rm(fileName));
        assertFalse("File shoud not exist", FileUtils.exists(fileName));
    }

    @Test
    public void testExtension() {
        String fileName = FileUtils.combine("data", "tests", "test_file.txt");
        assertEquals("txt", FileUtils.extension(fileName));
        fileName = FileUtils.combine("data", "tests", "test_file");
        assertEquals("", FileUtils.extension(fileName));
    }

    @Test
    public void testBasename() {
        String fileName = FileUtils.combine("data", "tests", "test_file.txt");
        assertEquals("test_file", FileUtils.basename(fileName));
        fileName = FileUtils.combine("data", "tests", "test_file");
        assertEquals("test_file", FileUtils.basename(fileName));
    }

    @Test
    public void testReadScalaFile(){
        String scalaFile = FileUtils.combine("src", "be", "hogent", "tarsos", "test", "data",
        "tone_scale.scl");
        double[] tuning = FileUtils.readScalaFile(scalaFile);
        assertEquals(12, tuning.length);
        assertEquals(76.049, tuning[0], 0.0001);
        assertEquals(193.15686, tuning[1], 0.0001);
        assertEquals(310.26471, tuning[2], 0.0001);
        assertEquals(386.31371, tuning[3], 0.0001);
        assertEquals(2786.3137, tuning[4], 0.0001);
        assertEquals(-5.0, tuning[5], 0.0001);
        assertEquals(698.57843, tuning[6], 0.0001);
        assertEquals(772.627, tuning[7], 0.001);
        assertEquals(889.73529, tuning[8], 0.0001);
        assertEquals(1006.84314, tuning[9], 0.0001);
        assertEquals(1082.89214, tuning[10], 0.0001);
        assertEquals(1200.0, tuning[11], 0.0001);
    }

    @Test
    public void testWriteScalaFile() {
        String scalaFile = FileUtils.combine(FileUtils.temporaryDirectory(), "temp_scala.scl");
        double[] expectedTuning = { 0, 12.5, 687.5, 789.3, 900.5, 1020.3, 1100.4, 1200.0 };
        FileUtils.writeScalaFile(expectedTuning, scalaFile, "Test tone scale");
        double[] actualTuning = FileUtils.readScalaFile(scalaFile);
        for (int i = 1; i < expectedTuning.length; i++) {
            assertEquals(expectedTuning[i], actualTuning[i - 1], 0.000001);
        }
        FileUtils.rm(scalaFile);
        assertFalse(FileUtils.exists(scalaFile));
    }

    @Test
    public void testCombine() {
        if (System.getProperty("os.name").contains("indows")) {
            assertEquals("c:\\blaat\\test", FileUtils.combine("c:", "blaat", "test"));
        } else {
            assertEquals("/home/joren/test.jpg", FileUtils.combine("home", "joren", "test.jpg"));
        }
    }
}
