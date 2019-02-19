package org.ianitrix.jmx.exporter;

/**
 * Mbean used to expose consumer group offset
 * @author Guillaume Waignier
 *
 */
public interface ConsumerGroupOffsetMBean {
		
	public long getConsumerOffset();

}
