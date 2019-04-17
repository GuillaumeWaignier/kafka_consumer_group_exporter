package org.ianitrix.jmx.exporter;

import java.lang.management.ManagementFactory;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ExecutionException;

import javax.management.AttributeNotFoundException;
import javax.management.InstanceNotFoundException;
import javax.management.MBeanException;
import javax.management.MBeanServer;
import javax.management.ObjectName;
import javax.management.ReflectionException;

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
import org.awaitility.Duration;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers
public class ConsumerGroupOffsetExporterTest {

	@Container
	private static final KafkaContainer kafka = new KafkaContainer("5.1.1");

	private final MBeanServer mBeanServer = ManagementFactory.getPlatformMBeanServer();

	private static AdminClient adminClient;

	private static ConsumerGroupOffsetExporter consumerGroupOffsetExporter;

	@BeforeAll
	public static void init() {
		// Properties
		final Properties properties = new Properties();
		properties.put(AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, kafka.getBootstrapServers());

		// create
		consumerGroupOffsetExporter = new ConsumerGroupOffsetExporter(properties, 500);
		consumerGroupOffsetExporter.start();
		adminClient = KafkaAdminClient.create(properties);
	}

	@AfterAll
	public static void cleanup() {
		consumerGroupOffsetExporter.stop();
		adminClient.close();
	}

	
	@Test
	public void testForOnePartition() throws Exception {

		final String topicName = "foo";
		final int numPartitions = 1;
		final String groupId = "test1";

		this.createTopic(topicName, numPartitions);
		this.setOffsetForPartition(groupId, topicName, 0, 1);
		
		this.checkOffset(groupId, topicName, 0, 1);

	}
	
	private void createTopic(final String topicName, final int numPartitions)
			throws InterruptedException, ExecutionException {
		adminClient.createTopics(Collections.singleton(new NewTopic(topicName, numPartitions, (short) 1))).all().get();
	}
	
	
	private void setOffsetForPartition(final String groupId, final String topicName, final int partition,
			final int offset) {

		final Properties properties = new Properties();
		properties.put(AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, kafka.getBootstrapServers());
		properties.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
		properties.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
		properties.put(ConsumerConfig.GROUP_ID_CONFIG, groupId);
		final KafkaConsumer<String, String> consumer = new KafkaConsumer<>(properties);
		
		final Map<TopicPartition, OffsetAndMetadata> offsets = new HashMap<>();
		offsets.put(new TopicPartition(topicName, partition), new OffsetAndMetadata(offset));
		consumer.commitSync(offsets);
		
		consumer.close();
	}

	private void checkOffset(final String groupId, final String topicName, final int partition, final int offset)
			throws Exception {
		final String mbeanName = String.format(ConsumerGroupOffsetExporter.MBEAN_NAME_PATTERN, groupId, topicName,
				partition);
		final ObjectName objectName = new ObjectName(mbeanName);
		

		Awaitility.await().atMost(Duration.FIVE_SECONDS).until(() -> this.getBeanValue(objectName) == offset);
		
		Assertions.assertEquals(offset, this.getBeanValue(objectName));
	}
	
	private long getBeanValue(final ObjectName objectName) {
		try {
			return (long) mBeanServer.getAttribute(objectName, "Value");
		} catch (final InstanceNotFoundException | AttributeNotFoundException | ReflectionException | MBeanException e) {
			return -1;
		}
	}
}
