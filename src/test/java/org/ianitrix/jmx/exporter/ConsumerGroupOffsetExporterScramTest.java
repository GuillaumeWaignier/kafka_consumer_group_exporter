package org.ianitrix.jmx.exporter;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.classic.spi.LoggingEvent;
import ch.qos.logback.core.Appender;
import org.awaitility.Awaitility;
import org.awaitility.Duration;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.*;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.io.File;
import java.util.NoSuchElementException;
import java.util.Properties;

@Testcontainers
public class ConsumerGroupOffsetExporterScramTest {

	@Container
	private static final KafkaContainer kafka = new KafkaContainer(Utils.CONFLUENT_VERSION)
			.withEnv("sasl.enabled.mechanisms", "SCRAM-SHA-512")
			.withEnv("sasl.mechanism.inter.broker.protocol","SCRAM-512")
			.withEnv("security.inter.broker.protocol","SASL_PLAINTEXT")
			.withEnv("listeners","SASL_PLAINTEXT://:9093")
			.withEnv("advertised.listeners","SASL_PLAINTEXT://localhost:9093")
			.withEnv("listener.name.sasl_plaintext.scram-sha-512.sasl.jaas.config","org.apache.kafka.common.security.scram.ScramLoginModule required username=test password=test-secret;")
			.withExposedPorts(9093);


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
	public void testWithWrongRight() throws Exception {

		final Properties properties = new Properties();
		properties.put("bootstrap.servers","localhost:9093");
		properties.put("sasl.mechanism","SCRAM-512");
		properties.put("security.protocol","SASL_PLAINTEXT");
		properties.put("sasl.jaas.config","org.apache.kafka.common.security.scram.ScramLoginModule required username=test password=test-secret;");
		properties.put("request.timeout.ms",1000);
		final ConsumerGroupOffsetExporter consumerGroupOffsetExporter = new ConsumerGroupOffsetExporter(properties);
		consumerGroupOffsetExporter.start();


		Awaitility.await().atMost(Duration.FIVE_SECONDS).until(() -> Utils.logContains(this.mockAppender, this.captorLoggingEvent,"Impossible to list all consumer group"));

		Mockito.verify(this.mockAppender, Mockito.atLeastOnce()).doAppend(this.captorLoggingEvent.capture());
		final LoggingEvent loggingEvent = captorLoggingEvent
				.getAllValues()
				.stream()
				.filter(event -> event.getMessage().startsWith("Impossible to list all consumer group"))
				.findFirst()
				.get();

		Assertions.assertEquals("Impossible to list all consumer group",loggingEvent.getMessage());

		consumerGroupOffsetExporter.stop();

	}


}
