package be.hogent.tarsos.test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.File;

import org.junit.Test;

import be.hogent.tarsos.util.AudioTranscoder;
import be.hogent.tarsos.util.FileUtils;

public class AudioTranscoderTests {

    @Test
    public void testTranscode() {
        // a mp3 file
        String source = "src/tarsos/test/data/mp3.mp3";
        String target = FileUtils.combine(System.getProperty("java.io.tmpdir"),
                "mp3.wav");
        AudioTranscoder.transcode(source, target, 1, 22050);
        assertTrue(new File(target).exists());
        assertTrue(AudioTranscoder.getInfo(target).getChannels() == 1);
        assertTrue(AudioTranscoder.getInfo(target).getSamplingRate() == 22050);
        assertTrue(AudioTranscoder.getInfo(target).getDecoder().startsWith(
                "pcm"));
        // a wma file
        source = "src/tarsos/test/data/wma.wma";
        target = FileUtils.combine(System.getProperty("java.io.tmpdir"),
                "wma.wav");
        AudioTranscoder.transcode(source, target, 1, 22050);
        assertTrue(new File(target).exists());
        assertTrue(AudioTranscoder.getInfo(target).getChannels() == 1);
        assertTrue(AudioTranscoder.getInfo(target).getSamplingRate() == 22050);
        assertTrue(AudioTranscoder.getInfo(target).getDecoder().startsWith(
                "pcm"));
    }

    @Test
    public void testTranscodingRequired() {
        String source = "src/tarsos/test/data/mp3.mp3";
        String target = FileUtils.combine(System.getProperty("java.io.tmpdir"),
                "mp3.wav");
        assertTrue(AudioTranscoder.transcodingRequired(target, 1, 44100));
        AudioTranscoder.transcode(source, target, 1, 22050);
        assertFalse(AudioTranscoder.transcodingRequired(target, 1, 22050));
        AudioTranscoder.transcode(source, target, 1, 44100);
        assertFalse(AudioTranscoder.transcodingRequired(target, 1, 44100));
    }
}
