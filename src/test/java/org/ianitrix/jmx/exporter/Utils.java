package org.ianitrix.jmx.exporter;

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
import org.junit.jupiter.api.Assertions;

import javax.management.*;
import java.lang.management.ManagementFactory;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ExecutionException;


public final class Utils {


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
		

		Awaitility.await().atMost(Duration.FIVE_SECONDS).until(() -> getBeanValue(objectName) == offset);
		
		Assertions.assertEquals(offset, getBeanValue(objectName));
	}
	
	private static long getBeanValue(final ObjectName objectName) {
		try {
			return (long) mBeanServer.getAttribute(objectName, "Value");
		} catch (final InstanceNotFoundException | AttributeNotFoundException | ReflectionException | MBeanException e) {
			return -1;
		}
	}
}
