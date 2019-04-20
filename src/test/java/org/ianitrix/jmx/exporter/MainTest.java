package org.ianitrix.jmx.exporter;

import java.io.File;
import java.util.Properties;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import com.ginsberg.junit.exit.ExpectSystemExitWithStatus;

public class MainTest {


	@Test
	@ExpectSystemExitWithStatus(1)
	public void testWrongParameterNumber() throws InterruptedException {
		Main.main(new String[0]);
	}
	
	@Test
	public void testLoadConfigurationFile() {
				
		final File f2 = new File("src/test/resources/config.properties");
		
		final Properties properties = Main.loadConfigurationFile(f2.getAbsolutePath());
		
		Assertions.assertEquals(properties.get("bootstrap.servers"), "localhost:9092");
		Assertions.assertEquals(properties.get("properties"), "some values");
	}
	
	@Test
	@ExpectSystemExitWithStatus(1)
	public void testLoadConfigurationFileError() {
				
		final File f2 = new File("src/test/resources/config2.properties");
		
		Main.loadConfigurationFile(f2.getAbsolutePath());
	}
}
