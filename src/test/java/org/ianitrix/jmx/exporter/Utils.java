package org.ianitrix.jmx.exporter;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.classic.spi.LoggingEvent;
import ch.qos.logback.core.Appender;
import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.AdminClientConfig;
import org.apache.kafka.clients.admin.KafkaAdminClient;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.consumer.OffsetAndMetadata;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.Assertions;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import javax.management.*;
import java.lang.management.ManagementFactory;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.ExecutionException;


public final class Utils {

	public static final String CONFLUENT_VERSION = "5.2.1";

	private static final MBeanServer mBeanServer = ManagementFactory.getPlatformMBeanServer();

	private static AdminClient adminClient;

	private static Properties kafkaProperties = new Properties();

	public static void init(final Properties properties) {
		kafkaProperties = properties;
		adminClient = KafkaAdminClient.create(kafkaProperties);
	}

	public static void cleanup() {
		adminClient.close();
	}


	public static void createTopic(final String topicName, final int numPartitions)
			throws InterruptedException, ExecutionException {
		adminClient.createTopics(Collections.singleton(new NewTopic(topicName, numPartitions, (short) 1))).all().get();
	}

	public static void setOffsetForPartition(final String groupId, final String topicName, final int partition,
			final int offset) {

		final Properties properties = new Properties();
		properties.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, kafkaProperties.getProperty((AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG)));
		properties.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
		properties.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
		properties.put(ConsumerConfig.GROUP_ID_CONFIG, groupId);
		final KafkaConsumer<String, String> consumer = new KafkaConsumer<>(properties);
		
		final Map<TopicPartition, OffsetAndMetadata> offsets = new HashMap<>();
		offsets.put(new TopicPartition(topicName, partition), new OffsetAndMetadata(offset));
		consumer.commitSync(offsets);
		
		consumer.close();
	}

	public static void checkOffset(final String groupId, final String topicName, final int partition, final int offset)
			throws Exception {
		final String mbeanName = String.format(ConsumerGroupOffsetExporter.MBEAN_NAME_PATTERN, groupId, topicName,
				partition);
		final ObjectName objectName = new ObjectName(mbeanName);
		

		Awaitility.await().atMost(Duration.ofMinutes(5)).until(() -> getBeanValue(objectName) == offset);
		
		Assertions.assertEquals(offset, getBeanValue(objectName));
	}
	
	private static long getBeanValue(final ObjectName objectName) {
		try {
			return (long) mBeanServer.getAttribute(objectName, "Value");
		} catch (final InstanceNotFoundException | AttributeNotFoundException | ReflectionException | MBeanException e) {
			return -1;
		}
	}

	public static boolean logContains(final Appender<ILoggingEvent> mockAppender, final ArgumentCaptor<LoggingEvent> captorLoggingEvent, final String logMessage) {
		try {
			Mockito.verify(mockAppender, Mockito.atLeastOnce()).doAppend(captorLoggingEvent.capture());
			final LoggingEvent loggingEvent = captorLoggingEvent
					.getAllValues()
					.stream()
					.filter(event -> event.getFormattedMessage().startsWith(logMessage))
					.findFirst()
					.get();
			return true;
		} catch(final NoSuchElementException e) {
			return false;
		}
	}
}
