package org.ianitrix.jmx.exporter;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.classic.spi.LoggingEvent;
import ch.qos.logback.core.Appender;
import org.apache.kafka.clients.admin.AdminClientConfig;
import org.awaitility.Awaitility;
import org.awaitility.Duration;
import org.junit.jupiter.api.*;
import org.mockito.*;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.Properties;

@Testcontainers
public class ConsumerGroupOffsetExporterTest {

	@Container
	private static final KafkaContainer kafka = new KafkaContainer(Utils.CONFLUENT_VERSION);

	private static ConsumerGroupOffsetExporter consumerGroupOffsetExporter;

	/** Mocked appender */
	@Mock
	private Appender<ILoggingEvent> mockAppender;

	/** Logging event captore */
	@Captor
	private ArgumentCaptor<LoggingEvent> captorLoggingEvent;

	@BeforeAll
	public static void globalInit() {
		final Properties properties = new Properties();
		properties.put(AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, kafka.getBootstrapServers());

		// create
		Utils.init(properties);
		consumerGroupOffsetExporter = new ConsumerGroupOffsetExporter(properties, 500);
		consumerGroupOffsetExporter.start();
	}

	@AfterAll
	public static void cleanup() {
		consumerGroupOffsetExporter.stop();
		Utils.cleanup();
	}

	@BeforeEach
	public void init() {
		// mock the logger
		MockitoAnnotations.initMocks(this);
		final Logger root = (Logger) LoggerFactory.getLogger(org.slf4j.Logger.ROOT_LOGGER_NAME);
		root.addAppender(this.mockAppender);
	}
	
	@Test
	public void testForOnePartition() throws Exception {

		final String topicName = "foo";
		final int numPartitions = 1;
		final String groupId = "test1";

		// Test offset 1
		Utils.createTopic(topicName, numPartitions);
		Utils.setOffsetForPartition(groupId, topicName, 0, 1);
		Utils.checkOffset(groupId, topicName, 0, 1);

		// Increase offset
		Utils.setOffsetForPartition(groupId, topicName, 0, 5);
		Utils.checkOffset(groupId, topicName, 0, 5);

		// Check log
		Awaitility.await().atMost(Duration.FIVE_MINUTES).until(() -> Utils.logContains(this.mockAppender, this.captorLoggingEvent, "Offset for kafka.consumer:type=ConsumerOffset,groupId=test1,topic=foo,partition=0 is 5"));

		// Check bean is registered only one time
		Mockito.verify(this.mockAppender, Mockito.atLeastOnce()).doAppend(this.captorLoggingEvent.capture());
		final long loggingEventCount = captorLoggingEvent
				.getAllValues()
				.stream()
				.filter(event -> event.getMessage().startsWith("Register MBean"))
				.distinct()
				.count();


		Assertions.assertEquals(1,loggingEventCount);


	}
}
