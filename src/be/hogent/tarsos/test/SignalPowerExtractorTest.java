package be.hogent.tarsos.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.List;

import org.junit.Test;

import be.hogent.tarsos.util.AudioFile;
import be.hogent.tarsos.util.FileUtils;
import be.hogent.tarsos.util.SignalPowerExtractor;

public class SignalPowerExtractorTest {

    /**
     * The test file is one second 220Hz at 80% power, 1 second silence and 1
     * second 440Hz at 100% power.
     */
    public AudioFile testAudioFile() {
        return new AudioFile(FileUtils.combine("src", "be", "hogent", "tarsos",
                "test", "data", "power_test.wav"));
    }

    @Test
    public void testPowerExtraction() {
        AudioFile audioFile = testAudioFile();
        SignalPowerExtractor spex = new SignalPowerExtractor(audioFile);
        assertEquals(0.0, spex.powerAt(1.5, true), 0.01);
        assertEquals(0.61, spex.powerAt(0.5, true), 0.01);// 220Hz only top at
        // 0.8 => 0.61
        assertEquals(1.0, spex.powerAt(2.5, true), 0.05);// 440Hz only top at
        // 1.0 => 0.95
    }

    @Test(expected = IndexOutOfBoundsException.class)
    public void testInvalidPowerExtraction() {
        AudioFile audioFile = testAudioFile();
        SignalPowerExtractor spex = new SignalPowerExtractor(audioFile);
        spex.powerAt(0.5, true);
        spex.powerAt(8.5, true);
    }

    @Test
    public void testWriteTextFile() {
        AudioFile audioFile = testAudioFile();
        SignalPowerExtractor spex = new SignalPowerExtractor(audioFile);
        String textFileName = FileUtils.combine("data", "tests",
                "power_text.txt");
        spex.saveTextFile(textFileName);
        List<String[]> data = FileUtils.readCSVFile(textFileName, ";", 2);
        int rowNumber = 0;
        for (String[] row : data) {
            if (rowNumber++ != 0 && Double.parseDouble(row[0]) == 0.50) {
                assertEquals(0.61, Double.parseDouble(row[1]), 0.01);
            }
        }
    }

    @Test
    public void testPowerPlotCreation() {
        AudioFile audioFile = testAudioFile();
        SignalPowerExtractor spex = new SignalPowerExtractor(audioFile);
        String powerPlotFileName = FileUtils.combine("data", "tests",
                "power_plot.png");
        spex.savePowerPlot(powerPlotFileName, 0.70);
        assertTrue(FileUtils.exists(powerPlotFileName));
    }

    @Test
    public void testWaveFormPlotCreation() {
        AudioFile audioFile = testAudioFile();
        SignalPowerExtractor spex = new SignalPowerExtractor(audioFile);
        String waveFormPlotFileName = FileUtils.combine("data", "tests",
                "wave_form__plot.png");
        spex.saveWaveFormPlot(waveFormPlotFileName);
        assertTrue(FileUtils.exists(waveFormPlotFileName));
    }

}
