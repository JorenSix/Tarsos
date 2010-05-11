package be.hogent.tarsos.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.File;

import org.junit.Test;

import be.hogent.tarsos.util.ConfKey;
import be.hogent.tarsos.util.Configuration;

public class ConfigurationTest {

    @Test
    public void testGet() {
        assertTrue(Configuration.get(ConfKey.histogram_bin_width).equals("6"));
        Configuration.set(ConfKey.histogram_bin_width, 8 + "");
        assertTrue(Configuration.get(ConfKey.histogram_bin_width).equals("8"));
        Configuration.set(ConfKey.histogram_bin_width, 6 + "");
    }

    @Test
    public void testGetInt() {
        assertTrue(Configuration.getInt(ConfKey.histogram_bin_width) == 6);
        Configuration.set(ConfKey.histogram_bin_width, 8 + "");
        assertTrue(Configuration.getInt(ConfKey.histogram_bin_width) == 8);
        Configuration.set(ConfKey.histogram_bin_width, 6 + "");
    }

    @Test
    public void testGetDouble() {
        assertTrue(Configuration.getDouble(ConfKey.histogram_bin_width) == 6.0);
        Configuration.set(ConfKey.histogram_bin_width, 8.5 + "");
        assertTrue(Configuration.getDouble(ConfKey.histogram_bin_width) == 8.5);
        Configuration.set(ConfKey.histogram_bin_width, 6 + "");
    }

    @Test
    public void testGetBoolean() {
        String originalValue = Configuration
                .get(ConfKey.audio_file_name_pattern);
        assertEquals(false, Configuration
                .getBoolean(ConfKey.audio_file_name_pattern));

        Configuration.set(ConfKey.audio_file_name_pattern, "falsqsdfe");
        assertEquals(false, Configuration
                .getBoolean(ConfKey.audio_file_name_pattern));

        Configuration.set(ConfKey.audio_file_name_pattern, "false");
        assertEquals(false, Configuration
                .getBoolean(ConfKey.audio_file_name_pattern));

        Configuration.set(ConfKey.audio_file_name_pattern, "true");
        assertEquals(true, Configuration
                .getBoolean(ConfKey.audio_file_name_pattern));

        Configuration.set(ConfKey.audio_file_name_pattern, "  TRuE  ");
        assertEquals(true, Configuration
                .getBoolean(ConfKey.audio_file_name_pattern));

        Configuration.set(ConfKey.audio_file_name_pattern, originalValue);
        assertEquals(originalValue, Configuration
                .get(ConfKey.audio_file_name_pattern));
    }

    @Test
    public void testUseCorrectSeparator() {
        String originalValue = Configuration.get(ConfKey.data_directory);
        String incorrectSeparator = File.separatorChar == '/' ? "\\" : "/";
        Configuration.set(ConfKey.data_directory, "dire" + incorrectSeparator
                + "ctory");
        assertEquals("dire" + File.separatorChar + "ctory", Configuration
                .get(ConfKey.data_directory));
        Configuration.set(ConfKey.data_directory, originalValue);
        assertEquals(originalValue, Configuration.get(ConfKey.data_directory));
    }

}
