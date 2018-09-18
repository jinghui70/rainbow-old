package rainbow.core.util.converter.impl;

import java.sql.Timestamp;

import org.joda.time.DateTime;

import rainbow.core.util.converter.AbstractConverter;

/**
 * 
 * @author lijinghui
 *
 */
public class DateTime2Timestamp extends AbstractConverter<DateTime, Timestamp> {

	@Override
	public Timestamp convert(DateTime from, Class<?> toClass) {
		return new Timestamp(from.getMillis());
	}

}
