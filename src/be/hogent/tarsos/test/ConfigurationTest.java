package be.hogent.tarsos.test;

import static org.junit.Assert.assertTrue;

import org.junit.Test;

import be.hogent.tarsos.util.Configuration;
import be.hogent.tarsos.util.Configuration.Config;


public class ConfigurationTest {

	@Test
	public void testGet() {
		assertTrue(Configuration.get(Config.histogram_bin_width).equals("6"));
		Configuration.set(Config.histogram_bin_width,8+"");
		assertTrue(Configuration.get(Config.histogram_bin_width).equals("8"));
		Configuration.set(Config.histogram_bin_width,6+"");
	}

	@Test
	public void testGetInt() {
		assertTrue( Configuration.getInt(Config.histogram_bin_width) == 6 );
		Configuration.set(Config.histogram_bin_width,8+"");
		assertTrue( Configuration.getInt(Config.histogram_bin_width) == 8 );
		Configuration.set(Config.histogram_bin_width,6+"");
	}

	@Test
	public void testGetDouble() {
		assertTrue( Configuration.getDouble(Config.histogram_bin_width) == 6.0 );
		Configuration.set(Config.histogram_bin_width,8.5+"");
		assertTrue( Configuration.getDouble(Config.histogram_bin_width) == 8.5 );
		Configuration.set(Config.histogram_bin_width,6+"");
	}
}
