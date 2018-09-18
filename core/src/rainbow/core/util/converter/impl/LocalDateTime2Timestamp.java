package rainbow.core.util.converter.impl;

import java.sql.Timestamp;

import org.joda.time.LocalDateTime;

import rainbow.core.util.converter.AbstractConverter;

/**
 * 
 * @author lijinghui
 *
 */
public class LocalDateTime2Timestamp extends AbstractConverter<LocalDateTime, Timestamp> {

	@Override
	public Timestamp convert(LocalDateTime from, Class<?> toClass) {
		return new Timestamp(from.toDate().getTime());
	}

}
