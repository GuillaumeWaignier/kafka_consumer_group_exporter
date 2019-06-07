package org.ianitrix.jmx.exporter;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
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
		final Instant startTime = Instant.now();

		checkArgument(args);

		final ConsumerGroupOffsetExporter consumerGroupOffsetExporter = new ConsumerGroupOffsetExporter(
				loadConfigurationFile(args[0]));

		Runtime.getRuntime().addShutdownHook(new Thread(consumerGroupOffsetExporter::stop));

		consumerGroupOffsetExporter.start();

		final Instant endTime = Instant.now();
		log.info("Started in {} ms", Duration.between(startTime, endTime).toMillis());

	}

	private static void checkArgument(final String[] args) {
		if (args.length != 1) {
			exitWithError("Missing argument for configuration file\nCommand line are : java - jar Main.jar config.properties", null);
		}
	}

	public static Properties loadConfigurationFile(final String file) {
		final File propertiesFile = new File(file);
		final Properties properties = new Properties();
		
		try (final FileInputStream inputStream = new FileInputStream(propertiesFile)) {
			properties.load(inputStream);
		} catch (final IOException e) {
			exitWithError("Error when loading configuration file " + file, e);
		}
		
		return properties;
	}
	
	private static final void exitWithError(final String errorMessage, final Exception exception) {
		log.error(errorMessage, exception);
		System.exit(1);
	}
}
