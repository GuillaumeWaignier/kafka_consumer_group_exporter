package org.ianitrix.jmx.exporter;

import java.io.File;
import java.util.Properties;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.slf4j.LoggerFactory;

import com.ginsberg.junit.exit.ExpectSystemExitWithStatus;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.classic.spi.LoggingEvent;
import ch.qos.logback.core.Appender;

class MainTest {

	/** Mocked appender */
	@Mock
	private Appender<ILoggingEvent> mockAppender;

	/** Logging event captore */
	@Captor
	private ArgumentCaptor<LoggingEvent> captorLoggingEvent;

	@BeforeEach
	public void init() {
		// mock the logger
		MockitoAnnotations.initMocks(this);
		final Logger root = (Logger) LoggerFactory.getLogger(org.slf4j.Logger.ROOT_LOGGER_NAME);
		root.addAppender(this.mockAppender);
	}

	@Test
	@ExpectSystemExitWithStatus(1)
	void testWrongParameterNumber() throws InterruptedException {
		Main.main(new String[0]);
		Assertions.fail("Exit with status 1 expected");
	}

	@Test
	void testLoadConfigurationFile() {

		final File f2 = new File("src/test/resources/config.properties");

		final Properties properties = Main.loadConfigurationFile(f2.getAbsolutePath());

		Assertions.assertEquals("localhost:9095", properties.get("bootstrap.servers"));
		Assertions.assertEquals("some values", properties.get("properties"));
	}

	@Test
	@ExpectSystemExitWithStatus(1)
	void testLoadConfigurationFileNotFound() {
		final File f2 = new File("src/test/resources/config2.properties");
		Main.loadConfigurationFile(f2.getAbsolutePath());
		Assertions.fail("Exit with status 1 expected");
	}

	@Test
	void testSuccessStart() throws InterruptedException {
		final File f2 = new File("src/test/resources/config.properties");
		Main.main(new String[] { f2.getAbsolutePath() });
		
		Mockito.verify(this.mockAppender, Mockito.atLeastOnce()).doAppend(this.captorLoggingEvent.capture());
		final LoggingEvent loggingEvent = captorLoggingEvent
				.getAllValues()
				.stream()
				.filter(event -> event.getMessage().startsWith("AdminClientConfig values:"))
				.findFirst()
				.get();

		Assertions.assertTrue(loggingEvent.getMessage().contains("localhost:9095"));

	}
}
