package org.ianitrix.jmx.exporter;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Properties;

import lombok.extern.slf4j.Slf4j;

/**
 * Main class
 * 
 * @author Guillaume Waignier
 *
 */
@Slf4j
public class Main {

	public static void main(String[] args) throws InterruptedException {
		log.info("Starting ...");

		checkArgument(args);

		final ConsumerGroupOffsetExporter consumerGroupOffsetExporter = new ConsumerGroupOffsetExporter(
				loadConfigurationFile(args[0]));

		Runtime.getRuntime().addShutdownHook(new Thread(consumerGroupOffsetExporter::stop));

		consumerGroupOffsetExporter.start();
	}

	private static void checkArgument(String[] args) {
		if (args.length != 1) {
			log.error("Missing argument for configuration file\nCommand line are : java - jar Main.jar config.properties");
			System.exit(1);
		}
	}

	public static Properties loadConfigurationFile(String file) {
		final File propertiesFile = new File(file);
		final Properties properties = new Properties();
		

		try (final FileInputStream inputStream = new FileInputStream(propertiesFile)) {
			properties.load(inputStream);
			return properties;
		} catch (final FileNotFoundException e) {
			log.error("Missing configuration file {}", file, e);
			System.exit(1);
		} catch (final IOException e) {
			log.error("Error when loading configuration file", e);
			System.exit(1);
		}
		
		return null;
	}
}
