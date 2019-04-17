package org.ianitrix.jmx.exporter;

import lombok.Getter;
import lombok.Setter;

/**
 * Mbean used to expose consumer group offset
 * @author Guillaume Waignier
 *
 */
@Getter
@Setter
public class ConsumerGroupOffset implements ConsumerGroupOffsetMBean {
	
	private long value;

}
