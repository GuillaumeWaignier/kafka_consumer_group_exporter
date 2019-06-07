package org.ianitrix.jmx.exporter;

import org.apache.kafka.clients.admin.AdminClientConfig;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.Properties;

@Testcontainers
public class ConsumerGroupOffsetExporterTest {

	@Container
	private static final KafkaContainer kafka = new KafkaContainer(Utils.CONFLUENT_VERSION);

	private static ConsumerGroupOffsetExporter consumerGroupOffsetExporter;

	@BeforeAll
	public static void init() {
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
	
	@Test
	public void testForOnePartition() throws Exception {

		final String topicName = "foo";
		final int numPartitions = 1;
		final String groupId = "test1";

		Utils.createTopic(topicName, numPartitions);
		Utils.setOffsetForPartition(groupId, topicName, 0, 1);
		
		Utils.checkOffset(groupId, topicName, 0, 1);
	}
}
