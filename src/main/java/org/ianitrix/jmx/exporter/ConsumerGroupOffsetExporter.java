package org.ianitrix.jmx.exporter;

import java.lang.management.ManagementFactory;
import java.time.Duration;
import java.time.Instant;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

import javax.management.InstanceAlreadyExistsException;
import javax.management.MBeanRegistrationException;
import javax.management.MBeanServer;
import javax.management.MalformedObjectNameException;
import javax.management.NotCompliantMBeanException;
import javax.management.ObjectName;

import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.ConsumerGroupListing;
import org.apache.kafka.clients.admin.KafkaAdminClient;
import org.apache.kafka.clients.admin.ListConsumerGroupOffsetsResult;
import org.apache.kafka.clients.admin.ListConsumerGroupsResult;
import org.apache.kafka.clients.consumer.OffsetAndMetadata;
import org.apache.kafka.common.TopicPartition;

import lombok.extern.slf4j.Slf4j;

/**
 * Simple program used to expose consumer group offset with JMX mbean.
 * 
 * @author Guillaume Waignier
 *
 */
@Slf4j
public class ConsumerGroupOffsetExporter implements Runnable {

	/**
	 * Pattern name for the MBEAN name.
	 */
	public static final String MBEAN_NAME_PATTERN = "kafka.consumer:type=ConsumerOffset,groupId=%s,topic=%s,partition=%d";

	/**
	 * Registered mbean for the consumer group offset
	 */
	private final Map<String, ConsumerGroupOffset> registeredConsumerGroupOffsetMbean = new HashMap<>();
	
	/**
	 * mbean server
	 */
	private final MBeanServer mBeanServer;
	
	/**
	 * kafka admin client
	 */
	private final AdminClient adminClient;
	
	private final int refreshInterval;
	
	private boolean isRunning = true;
	

	public ConsumerGroupOffsetExporter(final Properties properties) {
		this(properties, 5000);
	}
	
	public ConsumerGroupOffsetExporter(final Properties properties, final int refreshInterval) {
		this.mBeanServer = ManagementFactory.getPlatformMBeanServer();
		this.adminClient = KafkaAdminClient.create(properties);
		this.refreshInterval = refreshInterval;
	}
	
	public void start() {
		new Thread(this).start();
	}
	
	@Override
	public void run() {
		while (this.isRunning) {

			for (final String consumerGroupId : this.listConsumerGroups()) {
				this.getConsumerGroupOffset(consumerGroupId).entrySet().forEach(offset -> this.updateMbean(consumerGroupId, offset.getKey(), offset.getValue()));
			}
			try {
				Thread.sleep(this.refreshInterval);
			} catch (final InterruptedException e) {
				Thread.currentThread().interrupt();
			}
		}	
	}
	

	private List<String> listConsumerGroups() {
		final ListConsumerGroupsResult consumerGroups = this.adminClient.listConsumerGroups();
		
		try {
			final List<String> groupIds = consumerGroups.all().get().stream().map(ConsumerGroupListing::groupId)
					.collect(Collectors.toList());
			log.debug("All consumer group are : {}", groupIds);
			return groupIds;
		} catch (final ExecutionException | InterruptedException e) {
			log.error("Impossible to list all consumer group", e);
			if (e instanceof  InterruptedException) {
				Thread.currentThread().interrupt();
			}
			return Collections.emptyList();
		}
	}

	private Map<TopicPartition, OffsetAndMetadata> getConsumerGroupOffset(final String consumerGroupId) {
		final ListConsumerGroupOffsetsResult offsets = this.adminClient.listConsumerGroupOffsets(consumerGroupId);
		try {
			return offsets.partitionsToOffsetAndMetadata().get();
		} catch (final ExecutionException | InterruptedException e) {
			log.error("Impossible to get consumer group offset for {}", consumerGroupId, e);
			if (e instanceof  InterruptedException) {
				Thread.currentThread().interrupt();
			}
			return new HashMap<>();
		}
	}

	private void updateMbean(final String groupId, final TopicPartition topicPartition, final OffsetAndMetadata offset) {
		final String mbeanName = String.format(MBEAN_NAME_PATTERN, groupId, topicPartition.topic(), topicPartition.partition());

		// compute if absent
		ConsumerGroupOffset consumerGroupOffset = this.registeredConsumerGroupOffsetMbean.get(mbeanName);
		if (consumerGroupOffset == null ) {
			consumerGroupOffset = this.createConsumerGroupOffset(mbeanName);
			this.registeredConsumerGroupOffsetMbean.put(mbeanName, consumerGroupOffset);
		}

		consumerGroupOffset.setValue(offset.offset());
		log.debug("Offset for {} is {}", mbeanName, offset.offset());
	}
	
	private ConsumerGroupOffset createConsumerGroupOffset(final String mbeanName) {
		final ConsumerGroupOffset consumerGroupOffset = new ConsumerGroupOffset();
		try {
			this.mBeanServer.registerMBean(consumerGroupOffset, new ObjectName(mbeanName));
			log.debug("Register MBean {}", mbeanName);
		} catch (InstanceAlreadyExistsException | MBeanRegistrationException | NotCompliantMBeanException
				| MalformedObjectNameException e) {
			log.error("Impossible to register mbean {}", e);
		}
		return consumerGroupOffset;
	}

	public void stop() {
		final Instant startDate = Instant.now();
		log.info("Stopping ...");
		this.isRunning = false;
		this.adminClient.close();
		final Instant endDate = Instant.now();
		log.info("Successfully Stopped in {} ms", Duration.between(startDate, endDate).toMillis());
	}
	
}
