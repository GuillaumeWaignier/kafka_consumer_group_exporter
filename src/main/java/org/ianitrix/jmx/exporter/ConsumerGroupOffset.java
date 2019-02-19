package org.ianitrix.jmx.exporter;

import lombok.Data;

/**
 * Mbean used to expose consumer group offset
 * @author Guillaume Waignier
 *
 */
@Data
public class ConsumerGroupOffset implements ConsumerGroupOffsetMBean {
	
	private long value;

}
